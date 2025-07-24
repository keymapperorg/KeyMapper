
/*
 * Flags that flow alongside events in the input dispatch system to help with certain
 * policy decisions such as waking from device sleep.
 *
 * These flags are also defined in frameworks/base/core/java/android/view/WindowManagerPolicy.java.
 */
enum {
    /* These flags originate in RawEvents and are generally set in the key map.
     * NOTE: If you want a flag to be able to set in a keylayout file, then you must add it to
     * InputEventLabels.h as well. */

    // Indicates that the event should wake the device.
    POLICY_FLAG_WAKE = 0x00000001,

    // Indicates that the key is virtual, such as a capacitive button, and should
    // generate haptic feedback.  Virtual keys may be suppressed for some time
    // after a recent touch to prevent accidental activation of virtual keys adjacent
    // to the touch screen during an edge swipe.
    POLICY_FLAG_VIRTUAL = 0x00000002,

    // Indicates that the key is the special function modifier.
    POLICY_FLAG_FUNCTION = 0x00000004,

    // Indicates that the key represents a special gesture that has been detected by
    // the touch firmware or driver.  Causes touch events from the same device to be canceled.
    // This policy flag prevents key events from changing touch mode state.
    POLICY_FLAG_GESTURE = 0x00000008,

    // Indicates that key usage mapping represents a fallback mapping.
    // Fallback mappings cannot be used to definitively determine whether a device
    // supports a key code. For example, a HID device can report a key press
    // as a HID usage code if it is not mapped to any linux key code in the kernel.
    // However, we cannot know which HID usage codes that device supports from
    // userspace through the evdev. We can use fallback mappings to convert HID
    // usage codes to Android key codes without needing to know if a device can
    // actually report the usage code.
    POLICY_FLAG_FALLBACK_USAGE_MAPPING = 0x00000010,

    POLICY_FLAG_RAW_MASK = 0x0000ffff,

    /* These flags are set by the input dispatcher. */

    // Indicates that the input event was injected.
    POLICY_FLAG_INJECTED = 0x01000000,

    // Indicates that the input event is from a trusted source such as a directly attached
    // input device or an application with system-wide event injection permission.
    POLICY_FLAG_TRUSTED = 0x02000000,

    // Indicates that the input event has passed through an input filter.
    POLICY_FLAG_FILTERED = 0x04000000,

    // Disables automatic key repeating behavior.
    POLICY_FLAG_DISABLE_KEY_REPEAT = 0x08000000,

    /* These flags are set by the input reader policy as it intercepts each event. */

    // Indicates that the device was in an interactive state when the
    // event was intercepted.
    POLICY_FLAG_INTERACTIVE = 0x20000000,

    // Indicates that the event should be dispatched to applications.
    // The input event should still be sent to the InputDispatcher so that it can see all
    // input events received include those that it will not deliver.
    POLICY_FLAG_PASS_TO_USER = 0x40000000,
};

namespace android {
    bool isFromSource(uint32_t source, uint32_t test);
}