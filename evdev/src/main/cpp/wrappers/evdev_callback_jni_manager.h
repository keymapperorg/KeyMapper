/*
 * JNI Manager for IEvdevCallback
 * Manages the registration and lifecycle of the evdev callback via JNI
 * and provides a simple C API for interacting with the callback
 */

#ifndef EVDEV_CALLBACK_JNI_MANAGER_H
#define EVDEV_CALLBACK_JNI_MANAGER_H

#include <stdint.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Error codes for evdev callback operations
enum EvdevCallbackError {
    EVDEV_CALLBACK_SUCCESS = 0,                          // Operation succeeded
    EVDEV_CALLBACK_ERROR_INVALID_ARG = -1,                // Invalid argument (null pointer, etc.)
    EVDEV_CALLBACK_ERROR_BINDER_CONVERSION_FAILED = -2,   // Failed to convert Java binder to AIBinder
    EVDEV_CALLBACK_ERROR_CALLBACK_CREATION_FAILED = -3,  // Failed to create callback from binder
    EVDEV_CALLBACK_ERROR_NO_CALLBACK = -1,               // No callback registered (same as INVALID_ARG)
    EVDEV_CALLBACK_ERROR_INVALID_HANDLE = -1,            // Invalid callback handle (same as INVALID_ARG)
    EVDEV_CALLBACK_ERROR_CALLBACK_FAILED = -2,           // Callback method returned error (same as BINDER_CONVERSION_FAILED)
};

// Opaque handle to IEvdevCallback
typedef void* IEvdevCallbackHandle;

// Opaque handle to AIBinder (from Android NDK)
typedef void* AIBinderHandle;

// JNI method to register the callback from Java
// Returns 0 on success, non-zero error code on failure
JNIEXPORT jint JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_registerEvdevCallbackNative(
    JNIEnv *env,
    jobject thiz,
    jobject jCallbackBinder);

// JNI method to unregister the callback
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_unregisterEvdevCallbackNative(
    JNIEnv *env,
    jobject thiz);

// Create IEvdevCallback from AIBinder (for direct use, not via JNI)
// Returns 0 on success, non-zero error code on failure
// On success, *out_handle will be set to a valid handle (must be freed with evdev_callback_destroy)
// On failure, *out_handle will be NULL
int evdev_callback_from_binder(AIBinderHandle binder, IEvdevCallbackHandle* out_handle);

// Call onEvdevEventLoopStarted
// If handle is NULL, uses the stored callback registered via JNI
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event_loop_started(IEvdevCallbackHandle handle);

// Call onEvdevEvent
// If handle is NULL, uses the stored callback registered via JNI
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event(
    IEvdevCallbackHandle handle,
    const char* device_path,
    int64_t time_sec,
    int64_t time_usec,
    int32_t type,
    int32_t code,
    int32_t value,
    int32_t android_code
);

// Call onEmergencyKillSystemBridge
// If handle is NULL, uses the stored callback registered via JNI
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_emergency_kill_system_bridge(IEvdevCallbackHandle handle);

// Convenience functions that use the stored callback (no handle parameter)
// These are simpler to use when the callback has been registered via JNI

// Call onEvdevEventLoopStarted using stored callback
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event_loop_started_stored();

// Call onEvdevEvent using stored callback
// Returns 0 on success, non-zero error code on failure
int evdev_callback_on_evdev_event_stored(
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
int evdev_callback_on_emergency_kill_system_bridge_stored();

// Destroy an IEvdevCallback handle
// Safe to call with NULL handle
void evdev_callback_destroy(IEvdevCallbackHandle handle);

#ifdef __cplusplus
}
#endif

#endif // EVDEV_CALLBACK_JNI_MANAGER_H

