use crate::bindings;
use crate::device_manager::DeviceContext;
use crate::evdev::{EvdevError, EvdevEvent};
use crate::observer::EvdevEventNotifier;
use crate::tokio_runtime;
use nix::errno::Errno;
use std::os::fd::AsRawFd;
use std::sync::Arc;
use tokio::io::unix::AsyncFd;
use tokio::task::JoinHandle;

/// Wrapper to make a raw fd work with AsyncFd
/// This is safe because we're only using it for polling, not for actual I/O
struct FdWrapper {
    fd: std::os::fd::RawFd,
}

impl AsRawFd for FdWrapper {
    fn as_raw_fd(&self) -> std::os::fd::RawFd {
        self.fd
    }
}

/// Spawn a Tokio task to handle events from a device
/// Returns a handle that can be used to cancel the task
pub fn spawn_device_task(
    device_path: String,
    device: Arc<DeviceContext>,
    notifier: Arc<EvdevEventNotifier>,
) -> Result<JoinHandle<()>, EvdevError> {
    // Wrap the file descriptor with AsyncFd
    // Note: AsyncFd requires the fd to be in non-blocking mode, which we already do
    let fd_wrapper = FdWrapper {
        fd: device.fd.as_raw_fd(),
    };
    let async_fd = AsyncFd::new(fd_wrapper)
        .map_err(|e| EvdevError::new(-(e.raw_os_error().unwrap_or(0) as i32)))?;

    // Get the runtime handle to spawn the task
    // We can't use tokio::spawn() directly because JNI methods aren't in a Tokio context
    let runtime_handle = tokio_runtime::get_runtime_handle()
        .ok_or_else(|| EvdevError::new(-(nix::errno::Errno::EINVAL as i32)))?;

    let handle = runtime_handle.spawn(async move {
        device_task_loop(device_path, device, notifier, async_fd).await;
    });

    Ok(handle)
}

/// Main loop for a device task
async fn device_task_loop(
    device_path: String,
    device: Arc<DeviceContext>,
    notifier: Arc<EvdevEventNotifier>,
    async_fd: AsyncFd<FdWrapper>,
) {
    loop {
        // Wait for the file descriptor to become readable
        match async_fd.readable().await {
            Ok(mut guard) => {
                // Clear the readiness state
                guard.clear_ready();

                // Read events from the device
                if let Err(e) = read_and_process_events(&device_path, &device, &notifier) {
                    error!("Error reading events from device {}: {}", device_path, e);
                    // If there's an error, check if it's a device disconnection
                    if e.kind() == crate::evdev::EvdevErrorCode::BadFileDescriptor
                        || e.kind() == crate::evdev::EvdevErrorCode::NoSuchDevice
                    {
                        info!("Device {} disconnected, stopping task", device_path);
                        break;
                    }
                    // For other errors, continue the loop
                }
            }
            Err(e) => {
                error!("Error waiting for device {} to become readable: {}", device_path, e);
                break;
            }
        }
    }

    info!("Device task for {} ended", device_path);
}

/// Read and process events from a device
fn read_and_process_events(
    device_path: &str,
    device: &DeviceContext,
    notifier: &EvdevEventNotifier,
) -> Result<(), EvdevError> {
    let mut input_event = bindings::input_event {
        time: bindings::timeval { tv_sec: 0, tv_usec: 0 },
        type_: 0,
        code: 0,
        value: 0,
    };

    // Read all available events from device
    loop {
        let result = unsafe {
            bindings::libevdev_next_event(
                device.evdev.as_ptr(),
                bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_NORMAL,
                &mut input_event,
            )
        };

        if result < 0 {
            if result == -(Errno::EAGAIN as i32) {
                // No more events available (EAGAIN)
                break;
            } else {
                return Err(EvdevError::new(result));
            }
        }

        if result == bindings::libevdev_read_status_LIBEVDEV_READ_STATUS_SUCCESS {
            // Create event for observers
            let event = EvdevEvent {
                time_sec: input_event.time.tv_sec,
                time_usec: input_event.time.tv_usec,
                event_type: crate::enums::EventType::from_raw(input_event.type_ as u32)
                    .unwrap_or(crate::enums::EventType::Syn),
                code: input_event.code as u32,
                value: input_event.value,
            };

            // Notify all observers
            let consumed = notifier.notify(device_path, &event);

            // If no observer consumed the event, forward to uinput
            if !consumed {
                if let Err(e) = device.write_event(
                    event.event_type.as_raw(),
                    event.code,
                    event.value,
                ) {
                    error!("Failed to write event to uinput: {}", e);
                }
            }
        } else if result == bindings::libevdev_read_status_LIBEVDEV_READ_STATUS_SYNC {
            // Handle sync event
            let sync_result = unsafe {
                bindings::libevdev_next_event(
                    device.evdev.as_ptr(),
                    bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_NORMAL
                        | bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_SYNC,
                    &mut input_event,
                )
            };
            if sync_result < 0 && sync_result != -(Errno::EAGAIN as i32) {
                return Err(EvdevError::new(sync_result));
            }
        }
    }

    Ok(())
}

