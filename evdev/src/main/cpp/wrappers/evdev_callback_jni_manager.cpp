/*
 * JNI Manager implementation for IEvdevCallback
 * Handles JNI binder registration and lifecycle management
 * Exposes simple C-style functions for interacting with the callback
 */

#include "evdev_callback_jni_manager.h"
#include "../aidl/io/github/sds100/keymapper/evdev/IEvdevCallback.h"
#include "../logging.h"
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
    LOGI("Registered evdev callback via JNI");
    return EVDEV_CALLBACK_SUCCESS;
}

// JNI method to unregister the callback
JNIEXPORT void JNICALL
Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_unregisterEvdevCallbackNative(
    JNIEnv *env,
    jobject thiz) {
    
    std::lock_guard<std::mutex> lock(g_callback_mutex);
    g_callback = nullptr;
    LOGI("Unregistered evdev callback via JNI");
}

// C-style function to get callback from binder (for Rust)
int evdev_callback_from_binder(AIBinderHandle binder, IEvdevCallbackHandle* out_handle) {
    if (!binder || !out_handle) {
        return EVDEV_CALLBACK_ERROR_INVALID_ARG;
    }

    AIBinder* ai_binder = static_cast<AIBinder*>(binder);
    const ::ndk::SpAIBinder sp_binder(ai_binder);
    std::shared_ptr<IEvdevCallback> callback = IEvdevCallback::fromBinder(sp_binder);

    if (!callback) {
        *out_handle = nullptr;
        return EVDEV_CALLBACK_ERROR_CALLBACK_CREATION_FAILED;
    }

    // Store the shared_ptr in a new pointer to keep it alive
    std::shared_ptr<IEvdevCallback>* callback_ptr = 
        new std::shared_ptr<IEvdevCallback>(callback);
    *out_handle = reinterpret_cast<IEvdevCallbackHandle>(callback_ptr);
    return EVDEV_CALLBACK_SUCCESS;
}

// C-style function to call onEvdevEventLoopStarted using stored callback
int evdev_callback_on_evdev_event_loop_started(IEvdevCallbackHandle handle) {
    std::shared_ptr<IEvdevCallback> callback;
    
    if (handle) {
        // Use provided handle
        std::shared_ptr<IEvdevCallback>* callback_ptr = 
            reinterpret_cast<std::shared_ptr<IEvdevCallback>*>(handle);
        if (!callback_ptr || !*callback_ptr) {
            return EVDEV_CALLBACK_ERROR_INVALID_HANDLE;
        }
        callback = *callback_ptr;
    } else {
        // Use stored global callback
        std::lock_guard<std::mutex> lock(g_callback_mutex);
        if (!g_callback) {
            return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
        }
        callback = g_callback;
    }

    ndk::ScopedAStatus status = callback->onEvdevEventLoopStarted();
    if (!status.isOk()) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_FAILED;
    }

    return EVDEV_CALLBACK_SUCCESS;
}

// C-style function to call onEvdevEvent using stored callback
int evdev_callback_on_evdev_event(
    IEvdevCallbackHandle handle,
    const char* device_path,
    int64_t time_sec,
    int64_t time_usec,
    int32_t type,
    int32_t code,
    int32_t value,
    int32_t android_code
) {
    if (!device_path) {
        return EVDEV_CALLBACK_ERROR_INVALID_ARG;
    }

    std::shared_ptr<IEvdevCallback> callback;
    
    if (handle) {
        // Use provided handle
        std::shared_ptr<IEvdevCallback>* callback_ptr = 
            reinterpret_cast<std::shared_ptr<IEvdevCallback>*>(handle);
        if (!callback_ptr || !*callback_ptr) {
            return EVDEV_CALLBACK_ERROR_INVALID_HANDLE;
        }
        callback = *callback_ptr;
    } else {
        // Use stored global callback
        std::lock_guard<std::mutex> lock(g_callback_mutex);
        if (!g_callback) {
            return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
        }
        callback = g_callback;
    }

    std::string device_path_str(device_path);
    bool return_value = false;
    ndk::ScopedAStatus status = callback->onEvdevEvent(
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

// C-style function to call onEmergencyKillSystemBridge using stored callback
int evdev_callback_on_emergency_kill_system_bridge(IEvdevCallbackHandle handle) {
    std::shared_ptr<IEvdevCallback> callback;
    
    if (handle) {
        // Use provided handle
        std::shared_ptr<IEvdevCallback>* callback_ptr = 
            reinterpret_cast<std::shared_ptr<IEvdevCallback>*>(handle);
        if (!callback_ptr || !*callback_ptr) {
            return EVDEV_CALLBACK_ERROR_INVALID_HANDLE;
        }
        callback = *callback_ptr;
    } else {
        // Use stored global callback
        std::lock_guard<std::mutex> lock(g_callback_mutex);
        if (!g_callback) {
            return EVDEV_CALLBACK_ERROR_NO_CALLBACK;
        }
        callback = g_callback;
    }

    ndk::ScopedAStatus status = callback->onEmergencyKillSystemBridge();
    if (!status.isOk()) {
        return EVDEV_CALLBACK_ERROR_CALLBACK_FAILED;
    }

    return EVDEV_CALLBACK_SUCCESS;
}

// C-style function to call methods using stored callback (no handle needed)
int evdev_callback_on_evdev_event_loop_started_stored() {
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

int evdev_callback_on_evdev_event_stored(
    const char* device_path,
    int64_t time_sec,
    int64_t time_usec,
    int32_t type,
    int32_t code,
    int32_t value,
    int32_t android_code
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

int evdev_callback_on_emergency_kill_system_bridge_stored() {
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

void evdev_callback_destroy(IEvdevCallbackHandle handle) {
    if (!handle) {
        return;
    }

    std::shared_ptr<IEvdevCallback>* callback_ptr = 
        reinterpret_cast<std::shared_ptr<IEvdevCallback>*>(handle);
    
    delete callback_ptr;
}

} // extern "C"

