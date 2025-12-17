use std::{
    collections::HashMap,
    error::Error,
    fs::read_dir,
    os::fd::AsRawFd,
    path::PathBuf,
    sync::{Arc, Mutex, RwLock},
};

use bimap::BiHashMap;
use evdev::{enums::EventCode, DeviceWrapper};
use mio::{unix::SourceFd, Registry};
use slab::Slab;

use crate::{
    evdev_device_info::EvdevDeviceInfo, evdev_error::EvdevError, grab_target::GrabTarget,
    grabbed_device::GrabbedDevice,
};

pub struct EvdevGrabController {
    poll_registry: Arc<Registry>,
    callback: fn(grabbed_devices: Vec<EvdevDeviceInfo>),
    grab_targets: Mutex<Vec<GrabTarget>>,
    grabbed_devices: RwLock<Slab<GrabbedDevice>>,
}

impl EvdevGrabController {
    fn new(
        poll_registry: Arc<Registry>,
        callback: fn(grabbed_devices: Vec<EvdevDeviceInfo>),
    ) -> Self {
        Self {
            poll_registry,
            callback,
            grab_targets: Mutex::new(Vec::with_capacity(64)),
            grabbed_devices: RwLock::new(Slab::with_capacity(64)),
        }
    }

    pub fn set_grab_targets(&self, targets: Vec<GrabTarget>) {
        let mut grab_targets = self.grab_targets.lock().unwrap();
        grab_targets.clear();

        for target in targets {
            grab_targets.push(target);
        }

        self.invalidate(grab_targets.as_ref());
    }

    // TODO call this function. Isnt this going to be called many times when grabbing/ungrabbing?
    pub fn on_inotify_dev_input(&self) {
        let grab_targets = self.grab_targets.lock().unwrap();
        self.invalidate(grab_targets.as_ref());
    }

    fn invalidate(&self, grab_targets: &[GrabTarget]) {
        let mut grabbed_devices = self.grabbed_devices.write().unwrap();
        let real_device_paths =
            Self::get_real_device_paths(&grabbed_devices).expect("Unable to evdev device paths");
        let device_info_path_map = Self::build_device_info_path_map(&real_device_paths);

        let keys_to_remove =
            Self::get_devices_to_ungrab(grab_targets, &grabbed_devices, device_info_path_map);

        // Ungrab devices that are no longer requested
        for key in keys_to_remove {
            let grabbed_device = grabbed_devices.remove(key);
            self.ungrab_device(grabbed_device);
        }

        for grab_target in grab_targets {}

        // TODO grab devices
        // 5. Try grabbing devices that are not already grabbed
        // 6. Call the callback with the new list of grabbed devices.
    }

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
    
    fn is_target_grabbable() {}

    fn grab_new_devices(&self, requested_devices: Vec<GrabDeviceRequest>) {
        let uinput_paths: Vec<String> = devices_slab
            .iter()
            .map(|(_, device)| device.uinput.devnode().unwrap().to_string())
            .collect();

        let device_path_map = build_device_path_map(&uinput_paths);

        for grab_request in requested_devices {
            let extra_events = Self::map_key_codes_to_event_codes(&grab_request.extra_key_codes);

            // Check whether the device is already grabbed with the same extra events.
            // Otherwise it should be regrabbed with the updated information.
            let already_grabbed = devices_slab.iter().any(|(_, device)| {
                device_id_matches_jni_fields(&device.device_info, &grab_request.device_identifier)
                    && device.extra_events == extra_events
            });

            if already_grabbed {
                info!(
                    "Device {} is already grabbed with the same extra events",
                    grab_request.device_identifier.name
                );
                continue;
            }

            let key = DeviceIdentifierKey::from(&grab_request.device_identifier);

            match device_path_map.get(&key) {
                Some(path) => match self.grab_device(devices_slab, path, &extra_events) {
                    Ok(_) => {
                        KeyLayoutMapManager::get()
                            .preload_key_layout_map(&grab_request.device_identifier)
                            .ok();
                    }

                    Err(e) => error!("Failed to grab device {}: {:?}", path, e),
                },
                None => {
                    warn!("Device not found: {:?}", grab_request);
                }
            }
        }
    }

    /// Internal method to grab a device and register it with the poll
    fn grab_device(
        &self,
        devices_slab: &mut Slab<GrabbedDevice>,
        path: &str,
        extra_events: &[EventCode],
    ) -> Result<usize, Box<dyn Error>> {
        let device = GrabbedDevice::new(path, extra_events)?;
        let fd = device.evdev.lock().unwrap().as_raw_fd();
        let key = devices_slab.insert(device);

        let mut source_fd = SourceFd(&fd);

        // Register with key + 1 because 0 is reserved for the waker
        self.registry
            .register(&mut source_fd, Token(key + 1), Interest::READABLE)
            .inspect_err(|e| {
                // Remove device on registration failure
                devices_slab.remove(key);
                error!("Failed to register device {}: {}", path, e);
            })?;

        info!("Grabbed device: {}", path);
        Ok(key)
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
            if let Ok(device) = evdev::Device::new_from_path(&path) {
                let key = EvdevDeviceInfo {
                    name: device.name().unwrap_or("").to_string(),
                    bus: device.bustype(),
                    vendor: device.vendor_id(),
                    product: device.product_id(),
                    version: device.version(),
                };

                map.insert(key, path)
            }
        }

        map
    }
}
