/*
 * JNI Manager for IEvdevCallback
 * Manages the registration and lifecycle of the evdev callback via JNI
 * and provides a simple C API for interacting with the callback
 */

#ifndef EVDEV_CALLBACK_JNI_MANAGER_H
#define EVDEV_CALLBACK_JNI_MANAGER_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// Error codes for evdev callback operations
enum EvdevCallbackError {
    EVDEV_CALLBACK_SUCCESS = 0,                          // Operation succeeded
    EVDEV_CALLBACK_ERROR_INVALID_ARG = -1,                // Invalid argument (null pointer, etc.)
    EVDEV_CALLBACK_ERROR_BINDER_CONVERSION_FAILED = -2,   // Failed to convert Java binder to AIBinder
    EVDEV_CALLBACK_ERROR_CALLBACK_CREATION_FAILED = -3,  // Failed to create callback from binder
    EVDEV_CALLBACK_ERROR_NO_CALLBACK = -4,               // No callback registered
    EVDEV_CALLBACK_ERROR_INVALID_HANDLE = -5,            // Invalid callback handle
    EVDEV_CALLBACK_ERROR_CALLBACK_FAILED = -6,           // Callback method returned error
};

// Opaque handle to IEvdevCallback
typedef void* IEvdevCallbackHandle;

// Opaque handle to AIBinder (from Android NDK)
typedef void* AIBinderHandle;

// Call onEvdevEventLoopStarted using stored callback
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event_loop_started();

// Call onEvdevEvent using stored callback
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event(
    const char* device_path,
    int64_t time_sec,
    int64_t time_usec,
    int32_t type,
    int32_t code,
    int32_t value,
    int32_t android_code
);

// Call onEmergencyKillSystemBridge using stored callback
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_emergency_kill_system_bridge();

#ifdef __cplusplus
}
#endif

#endif // EVDEV_CALLBACK_JNI_MANAGER_H

