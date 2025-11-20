/*
 * JNI Manager implementation for IEvdevCallback
 * Handles JNI binder registration and lifecycle management
 * Exposes simple C-style functions for interacting with the callback
 */

#include "evdev_callback_jni_manager.h"
#include "aidl/io/github/sds100/keymapper/evdev/IEvdevCallback.h"
#include <android/binder_ibinder_jni.h>
#include <jni.h>
#include <mutex>
#include <memory>
#include <string>

using aidl::io::github::sds100::keymapper::evdev::IEvdevCallback;

// Static storage for the callback instance
static std::shared_ptr<IEvdevCallback> g_callback = nullptr;
static std::mutex g_callback_mutex;

extern "C" {

// JNI method to register the callback from Java
JNIEXPORT jint JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_registerEvdevCallbackNative(
    JNIEnv *env,
    jobject thiz,
    jobject jCallbackBinder) {
    
    if (!jCallbackBinder) {
        return EVDEV_CALLBACK_ERROR_INVALID_ARG;
    }

    AIBinder *callbackAIBinder = AIBinder_fromJavaBinder(env, jCallbackBinder);
    if (!callbackAIBinder) {
        return EVDEV_CALLBACK_ERROR_BINDER_CONVERSION_FAILED;
    }

    const ::ndk::SpAIBinder spBinder(callbackAIBinder);
    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(spBinder);

    if (!callback) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_CREATION_FAILED;
    }

    std::lock_guard<std::mutex> lock(g_callback_mutex);
    g_callback = callback;
    return EVDEV_CALLBACK_SUCCESS;
}

// JNI method to unregister the callback
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_unregisterEvdevCallbackNative(
    JNIEnv *env,
    jobject thiz) {
    
    std::lock_guard<std::mutex> lock(g_callback_mutex);
    g_callback = nullptr;
}

int evdev_callback_on_evdev_event_loop_started() {
    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (!g_callback) {
        return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
    }

    ndk::ScopedAStatus status = g_callback->onEvdevEventLoopStarted();
    if (!status.isOk()) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_FAILED;
    }

    return EVDEV_CALLBACK_SUCCESS;
}

int evdev_callback_on_evdev_event(
    const char* device_path,
    int64_t time_sec,
    int64_t time_usec,
    int32_t type,
    int32_t code,
    int32_t value,
    uint32_t android_code
) {
    if (!device_path) {
        return EVDEV_CALLBACK_ERROR_INVALID_ARG;
    }

    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (!g_callback) {
        return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
    }

    std::string device_path_str(device_path);
    bool return_value = false;
    ndk::ScopedAStatus status = g_callback->onEvdevEvent(
        device_path_str,
        time_sec,
        time_usec,
        type,
        code,
        value,
        android_code,
        &return_value
    );

    if (!status.isOk()) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_FAILED;
    }

    return EVDEV_CALLBACK_SUCCESS;
}

int evdev_callback_on_emergency_kill_system_bridge() {
    std::lock_guard<std::mutex> lock(g_callback_mutex);
    if (!g_callback) {
        return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
    }

    ndk::ScopedAStatus status = g_callback->onEmergencyKillSystemBridge();
    if (!status.isOk()) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_FAILED;
    }

    return EVDEV_CALLBACK_SUCCESS;
}

} // extern "C"

