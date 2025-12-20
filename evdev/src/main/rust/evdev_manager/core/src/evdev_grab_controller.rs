use std::{
    error::Error,
    fs::read_dir,
    io,
    os::fd::AsRawFd,
    path::PathBuf,
    sync::{Arc, Mutex, RwLock},
};

use bimap::BiHashMap;
use evdev::{enums::EventCode, DeviceWrapper};
use mio::{unix::SourceFd, Interest, Registry, Token};
use slab::Slab;

use crate::{
    evdev_device_info::EvdevDeviceInfo, evdev_error::EvdevError, event_loop::EvdevCallback,
    grab_target::GrabTarget, grabbed_device::GrabbedDevice,
    grabbed_device_handle::GrabbedDeviceHandle,
};

pub struct EvdevGrabController {
    poll_registry: Arc<Registry>,
    callback: Arc<dyn EvdevCallback>,
    grab_targets: Mutex<Vec<GrabTarget>>,
    grabbed_devices: RwLock<Slab<GrabbedDevice>>,
}

impl EvdevGrabController {
    pub fn new(poll_registry: Arc<Registry>, callback: Arc<dyn EvdevCallback>) -> Self {
        Self {
            poll_registry,
            callback,
            grab_targets: Mutex::new(Vec::with_capacity(64)),
            grabbed_devices: RwLock::new(Slab::with_capacity(64)),
        }
    }

    pub fn set_grab_targets(&self, targets: Vec<GrabTarget>) -> Vec<GrabbedDeviceHandle> {
        let mut grab_targets = self.grab_targets.lock().unwrap();
        grab_targets.clear();

        for target in targets {
            grab_targets.push(target);
        }

        self.invalidate(grab_targets.as_ref())
    }

    pub fn on_inotify_dev_input(&self) {
        let grab_targets = self.grab_targets.lock().unwrap();
        self.invalidate(grab_targets.as_ref());
    }

    fn invalidate(&self, grab_targets: &[GrabTarget]) -> Vec<GrabbedDeviceHandle> {
        let mut grabbed_devices = self.grabbed_devices.write().unwrap();

        let real_device_paths =
            Self::get_real_device_paths(&grabbed_devices).expect("Unable to evdev device paths");
        let device_info_path_map = Self::build_device_info_path_map(&real_device_paths);

        let device_keys_to_ungrab = Self::get_devices_to_ungrab(
            grab_targets,
            &grabbed_devices,
            device_info_path_map.clone(),
        );

        // Ungrab devices that are no longer requested
        for key in device_keys_to_ungrab {
            let grabbed_device = grabbed_devices.remove(key);
            self.ungrab_device(grabbed_device);
        }

        let devices_to_grab =
            Self::get_targets_to_grab(grab_targets, &grabbed_devices, device_info_path_map);

        for (path, extra_event_codes) in devices_to_grab {
            self.try_grab_target(&path, &extra_event_codes, &mut grabbed_devices)
                .inspect_err(|err| error!("Failed to grab device {:?}: {:?}", path, err))
                .ok();
        }

        let grabbed_device_handles: Vec<GrabbedDeviceHandle> = grabbed_devices
            .iter()
            .map(|(key, device)| GrabbedDeviceHandle::new(key, device.device_info.clone()))
            .collect();

        // Release the lock before calling the callback
        drop(grabbed_devices);

        self.callback
            .on_grabbed_devices_changed(grabbed_device_handles.clone());

        grabbed_device_handles
    }

    /// Access a grabbed device by ID through a closure.
    /// Returns None if the device is not found, otherwise returns the result of the closure.
    pub fn with_grabbed_device<F, R>(&self, device_id: usize, f: F) -> Option<R>
    where
        F: FnOnce(&GrabbedDevice) -> R,
    {
        let grabbed_devices = self.grabbed_devices.read().unwrap();
        grabbed_devices.get(device_id).map(f)
    }

    // TODO test
    fn get_devices_to_ungrab(
        grab_targets: &[GrabTarget],
        grabbed_devices: &Slab<GrabbedDevice>,
        device_info_path_map: BiHashMap<EvdevDeviceInfo, PathBuf>,
    ) -> Vec<usize> {
        let mut keys_to_remove: Vec<usize> = Vec::new();

        for (key, grabbed_device) in grabbed_devices.iter() {
            let is_connected: bool =
                device_info_path_map.contains_left(&grabbed_device.device_info);

            if !is_connected {
                keys_to_remove.push(key);
                continue;
            }

            let matching_grab_target: Option<&GrabTarget> = grab_targets
                .iter()
                .find(|target| target.matches_device_info(&grabbed_device.device_info));

            match matching_grab_target {
                // Ungrab if the device should be grabbed with different event codes.
                Some(target) => {
                    if target.extra_event_codes != grabbed_device.extra_event_codes {
                        keys_to_remove.push(key);
                        continue;
                    }
                }

                // Ungrab the device if it is no longer targeted to grab
                None => {
                    keys_to_remove.push(key);
                    continue;
                }
            }

            let current_device_at_path: Option<&EvdevDeviceInfo> =
                device_info_path_map.get_by_right(&grabbed_device.device_path);

            match current_device_at_path {
                // If the device path cached in the grabbed device no longer points
                // to the same device
                Some(real_device_info) => {
                    if grabbed_device.device_info != *real_device_info {
                        keys_to_remove.push(key);
                        continue;
                    }
                }

                // The path of the grabbed device no longer exists
                None => {
                    keys_to_remove.push(key);
                    continue;
                }
            }
        }

        keys_to_remove
    }

    fn ungrab_device(&self, device: GrabbedDevice) {
        let fd = device.evdev.lock().unwrap().as_raw_fd();

        let mut source_fd = SourceFd(&fd);
        self.poll_registry
            .deregister(&mut source_fd)
            .inspect_err(|e| {
                error!(
                    "Failed to deregister device {:?}: {:?}",
                    device.device_path, e
                )
            })
            .ok();

        info!("Ungrabbed device: {:?}", device.device_path);
    }

    // TODO test
    fn get_targets_to_grab(
        grab_targets: &[GrabTarget],
        grabbed_devices: &Slab<GrabbedDevice>,
        device_info_path_map: BiHashMap<EvdevDeviceInfo, PathBuf>,
    ) -> Vec<(PathBuf, Vec<EventCode>)> {
        let mut targets_to_grab: Vec<(PathBuf, Vec<EventCode>)> = Vec::new();

        for target in grab_targets {
            let already_grabbed = grabbed_devices
                .iter()
                .any(|(_, device)| target.matches_device_info(&device.device_info));

            if already_grabbed {
                continue;
            }

            let device_info = device_info_path_map
                .left_values()
                .find(|device_info| target.matches_device_info(device_info));

            match device_info {
                // Target device not connected
                None => continue,
                // Device is connected
                Some(device_info) => {
                    let path = device_info_path_map.get_by_left(&device_info).unwrap();

                    targets_to_grab.push((path.clone(), target.extra_event_codes.clone()));
                }
            }
        }

        targets_to_grab
    }

    fn try_grab_target(
        &self,
        device_path: &PathBuf,
        extra_event_codes: &[EventCode],
        grabbed_devices: &mut Slab<GrabbedDevice>,
    ) -> Result<usize, Box<dyn Error>> {
        let device = GrabbedDevice::new(device_path, extra_event_codes)?;
        let fd = device.evdev.lock().unwrap().as_raw_fd();
        let key = self.grabbed_devices.write().unwrap().insert(device);

        let mut source_fd = SourceFd(&fd);

        // Register with key + 1 because 0 is reserved for the waker
        self.poll_registry
            .register(&mut source_fd, Token(key + 1), Interest::READABLE)
            .inspect_err(|e| {
                // Remove device on registration failure
                grabbed_devices.remove(key);
                error!("Failed to register device {:?}: {}", device_path, e);
            })?;

        info!("Grabbed device: {:?}", device_path);
        Ok(key)
    }

    pub fn get_real_devices(&self) -> Result<Vec<EvdevDeviceInfo>, EvdevError> {
        let grabbed_devices = self.grabbed_devices.read().unwrap();

        let mut list: Vec<EvdevDeviceInfo> = Vec::new();

        for path in Self::get_real_device_paths(&grabbed_devices)? {
            if let Ok(info) = Self::get_device_info(&path) {
                list.push(info);
            }
        }

        Ok(list)
    }

    /// Get the paths to all the real (non uinput) connected devices.
    fn get_real_device_paths(
        grabbed_devices: &Slab<GrabbedDevice>,
    ) -> Result<Vec<PathBuf>, EvdevError> {
        let uinput_paths: Vec<PathBuf> = grabbed_devices
            .iter()
            .map(|(_, device)| device.uinput.devnode().unwrap().into())
            .collect();

        let mut paths: Vec<PathBuf> = Vec::new();

        let dir = read_dir("/dev/input")?;

        for entry_result in dir {
            match entry_result {
                Ok(entry) => {
                    let path = entry.path();
                    // Do not return paths to uinput devices that were created.
                    if uinput_paths.contains(&path) {
                        continue;
                    }

                    paths.push(path);
                }
                Err(_) => {
                    debug!(
                        "Failed to read /dev/input entry: {}",
                        entry_result.unwrap_err()
                    );
                }
            }
        }

        debug!("EvdevManager: get real devices: {:?}", paths);
        Ok(paths)
    }

    fn build_device_info_path_map(paths: &[PathBuf]) -> BiHashMap<EvdevDeviceInfo, PathBuf> {
        let mut map: BiHashMap<EvdevDeviceInfo, PathBuf> = BiHashMap::new();

        for path in paths {
            if let Ok(info) = Self::get_device_info(path) {
                map.insert(info, path.clone());
            }
        }

        map
    }

    fn get_device_info(path: &PathBuf) -> Result<EvdevDeviceInfo, io::Error> {
        evdev::Device::new_from_path(path).map(|device| EvdevDeviceInfo {
            name: device.name().unwrap_or("").to_string(),
            bus: device.bustype(),
            vendor: device.vendor_id(),
            product: device.product_id(),
            version: device.version(),
        })
    }
}
