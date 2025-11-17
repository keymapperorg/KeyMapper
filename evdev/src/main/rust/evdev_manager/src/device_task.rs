use crate::device_manager::DeviceContext;
use crate::evdev_error::{EvdevError, EvdevErrorCode};
use crate::observer::EvdevEventNotifier;
use crate::tokio_runtime;
use evdev::util::event_code_to_int;
use evdev::{InputEvent, ReadFlag, ReadStatus};
use std::os::fd::{AsRawFd, RawFd};
use std::sync::Arc;
use tokio::io::unix::AsyncFd;
use tokio::task::JoinHandle;

/// Spawn a Tokio task to handle events from a device
/// Returns a handle that can be used to cancel the task
pub fn spawn_device_task(
    device_path: String,
    device: Arc<DeviceContext>,
    notifier: Arc<EvdevEventNotifier>,
) -> Result<JoinHandle<()>, EvdevError> {
    // Wrap the file descriptor with AsyncFd
    // Note: AsyncFd requires the fd to be in non-blocking mode, which we already do
    let async_fd = AsyncFd::new(device.fd.as_raw_fd())?;

    // Get the runtime handle to spawn the task
    // We can't use tokio::spawn() directly because JNI methods aren't in a Tokio context
    let runtime_handle = tokio_runtime::get_runtime_handle()
        .ok_or_else(|| EvdevError::from_enum(EvdevErrorCode::InvalidArgument))?;

    let handle = runtime_handle.spawn(async move {
        device_task_loop(device_path, device, notifier, async_fd).await;
    });

    Ok(handle)
}

// TODO tokio is overcomplicating it, it is important that events are processed synchronously and in the correct order. Just use one event loop with mio::Poll like in the original and this is runs forever until a call from JNI is made to kill it. One can use enums for the commands.
// TODO do not allow multiple event loops to run
// / Main loop for a device task
async fn device_task_loop(
    device_path: String,
    device: Arc<DeviceContext>,
    notifier: Arc<EvdevEventNotifier>,
    async_fd: AsyncFd<RawFd>,
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
                    if e.kind() == EvdevErrorCode::BadFileDescriptor
                        || e.kind() == EvdevErrorCode::NoSuchDevice
                    {
                        info!("Device {} disconnected, stopping task", device_path);
                        break;
                    }
                    // For other errors, continue the loop
                }
            }
            Err(e) => {
                error!(
                    "Error waiting for device {} to become readable: {}",
                    device_path, e
                );
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
    // Read all available events from device
    loop {
        match device.evdev.next_event(ReadFlag::NORMAL) {
            Ok((ReadStatus::Success, event)) => {
                // Notify all observers
                let consumed = notifier.notify(device_path, &event);

                // If no observer consumed the event, forward to uinput
                if !consumed {
                    // Extract event type and code from EventCode
                    let (ev_type, ev_code) = event_code_to_int(&event.event_code);

                    if let Err(e) = device.write_event(ev_type as u32, ev_code as u32, event.value)
                    {
                        error!("Failed to write event to uinput: {}", e);
                    }
                }
            }
            Ok((ReadStatus::Sync, _event)) => {
                // Handle sync event - read sync events until EAGAIN
                loop {
                    match device.evdev.next_event(ReadFlag::NORMAL | ReadFlag::SYNC) {
                        Ok((ReadStatus::Sync, _)) => {
                            // Continue reading sync events
                        }
                        Ok((ReadStatus::Success, _)) => {
                            // Sync complete, break inner loop
                            break;
                        }
                        Err(e) => {
                            // Check if it's EAGAIN (no more events)
                            if let Some(err) = e.raw_os_error() {
                                if err == -(libc::EAGAIN as i32) {
                                    break;
                                }
                            }
                            return Err(EvdevError::from(e));
                        }
                    }
                }
            }
            Err(e) => {
                // Check if it's EAGAIN (no more events available)
                if let Some(err) = e.raw_os_error() {
                    if err == -(libc::EAGAIN as i32) {
                        // No more events available
                        break;
                    }
                }
                return Err(EvdevError::from(e));
            }
        }
    }

    Ok(())
}
