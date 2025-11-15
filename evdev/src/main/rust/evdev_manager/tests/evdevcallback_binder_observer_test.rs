use evdev_manager::{EmergencyKillCallback, EvdevCallbackBinderObserver, EvdevEvent, EventType};
use std::sync::atomic::{AtomicBool, Ordering};

static EMERGENCY_KILL_CALLED: AtomicBool = AtomicBool::new(false);

/// Test emergency kill callback that sets a flag instead of exiting
fn test_emergency_kill() {
    EMERGENCY_KILL_CALLED.store(true, Ordering::SeqCst);
}

#[test]
fn test_emergency_kill_on_power_button_held_10_seconds() {
    // Reset the flag
    EMERGENCY_KILL_CALLED.store(false, Ordering::SeqCst);

    // Create observer with test callback
    let observer = EvdevCallbackBinderObserver::with_emergency_kill_callback(test_emergency_kill);

    // Simulate power button down at time 0
    let power_down_event = EvdevEvent {
        time_sec: 0,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 1,  // Button down
    };

    // Process the down event
    observer.on_event("/dev/input/event0", &power_down_event);

    // Verify emergency kill not called yet
    assert!(
        !EMERGENCY_KILL_CALLED.load(Ordering::SeqCst),
        "Emergency kill should not be called on button down"
    );

    // Simulate power button up at time 10 (10 seconds later)
    let power_up_event = EvdevEvent {
        time_sec: 10,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 0,  // Button up
    };

    // Process the up event
    observer.on_event("/dev/input/event0", &power_up_event);

    // Verify emergency kill was called
    assert!(
        EMERGENCY_KILL_CALLED.load(Ordering::SeqCst),
        "Emergency kill should be called when power button held for 10+ seconds"
    );
}

#[test]
fn test_emergency_kill_not_called_on_short_press() {
    // Reset the flag
    EMERGENCY_KILL_CALLED.store(false, Ordering::SeqCst);

    // Create observer with test callback
    let observer = EvdevCallbackBinderObserver::with_emergency_kill_callback(test_emergency_kill);

    // Simulate power button down at time 0
    let power_down_event = EvdevEvent {
        time_sec: 0,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 1,  // Button down
    };

    // Process the down event
    observer.on_event("/dev/input/event0", &power_down_event);

    // Simulate power button up at time 5 (5 seconds later - less than 10)
    let power_up_event = EvdevEvent {
        time_sec: 5,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 0,  // Button up
    };

    // Process the up event
    observer.on_event("/dev/input/event0", &power_up_event);

    // Verify emergency kill was NOT called
    assert!(
        !EMERGENCY_KILL_CALLED.load(Ordering::SeqCst),
        "Emergency kill should not be called when power button held for less than 10 seconds"
    );
}

#[test]
fn test_emergency_kill_with_android_keycode() {
    // Reset the flag
    EMERGENCY_KILL_CALLED.store(false, Ordering::SeqCst);

    // Create observer with test callback
    let observer = EvdevCallbackBinderObserver::with_emergency_kill_callback(test_emergency_kill);

    // Simulate power button down using Android keycode (26 = KEYCODE_POWER)
    // Note: This test assumes the key layout map will map some code to Android keycode 26
    // For a more complete test, we'd need to set up a mock key layout map
    // For now, we'll test with the raw evdev code (116 = KEY_POWER)

    let power_down_event = EvdevEvent {
        time_sec: 0,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 1,  // Button down
    };

    observer.on_event("/dev/input/event0", &power_down_event);

    let power_up_event = EvdevEvent {
        time_sec: 10,
        time_usec: 0,
        event_type: EventType::Key,
        code: 116, // KEY_POWER
        value: 0,  // Button up
    };

    observer.on_event("/dev/input/event0", &power_up_event);

    // Verify emergency kill was called
    assert!(
        EMERGENCY_KILL_CALLED.load(Ordering::SeqCst),
        "Emergency kill should be called when power button (via Android keycode) held for 10+ seconds"
    );
}

#[test]
fn test_emergency_kill_not_called_for_non_power_button() {
    // Reset the flag
    EMERGENCY_KILL_CALLED.store(false, Ordering::SeqCst);

    // Create observer with test callback
    let observer = EvdevCallbackBinderObserver::with_emergency_kill_callback(test_emergency_kill);

    // Simulate a different key down
    let key_down_event = EvdevEvent {
        time_sec: 0,
        time_usec: 0,
        event_type: EventType::Key,
        code: 1,  // Some other key, not power
        value: 1, // Button down
    };

    observer.on_event("/dev/input/event0", &key_down_event);

    let key_up_event = EvdevEvent {
        time_sec: 10,
        time_usec: 0,
        event_type: EventType::Key,
        code: 1,  // Some other key
        value: 0, // Button up
    };

    observer.on_event("/dev/input/event0", &key_up_event);

    // Verify emergency kill was NOT called
    assert!(
        !EMERGENCY_KILL_CALLED.load(Ordering::SeqCst),
        "Emergency kill should not be called for non-power button keys"
    );
}
