/*
 * C interface implementation for KeyLayoutMap
 */

#include "keylayoutmap_c.h"
#include "android/input/KeyLayoutMap.h"
#include "android/utils/Errors.h"

extern "C" {

int keylayoutmap_load(const char* filename, KeyLayoutMapHandle* out_handle) {
    if (!filename || !out_handle) {
        return -1; // Invalid argument
    }

    auto result = android::KeyLayoutMap::load(filename, nullptr);
    if (!result.ok()) {
        *out_handle = nullptr;
        return -2; // Load failed
    }

    // Extract the shared_ptr and store it
    // We need to keep the shared_ptr alive, so we'll new it
    std::shared_ptr<android::KeyLayoutMap>* map_ptr = new std::shared_ptr<android::KeyLayoutMap>(*result);
    *out_handle = reinterpret_cast<KeyLayoutMapHandle>(map_ptr);
    return 0; // Success
}

int keylayoutmap_load_contents(const char* filename, const char* contents, KeyLayoutMapHandle* out_handle) {
    if (!filename || !contents || !out_handle) {
        return -1; // Invalid argument
    }

    auto result = android::KeyLayoutMap::loadContents(filename, contents);
    if (!result.ok()) {
        *out_handle = nullptr;
        return -2; // Load failed
    }

    std::shared_ptr<android::KeyLayoutMap>* map_ptr = new std::shared_ptr<android::KeyLayoutMap>(*result);
    *out_handle = reinterpret_cast<KeyLayoutMapHandle>(map_ptr);
    return 0; // Success
}

int keylayoutmap_map_key(KeyLayoutMapHandle handle, int32_t scan_code, int32_t usage_code,
                         int32_t* out_key_code, uint32_t* out_flags) {
    if (!handle || !out_key_code || !out_flags) {
        return -1; // Invalid argument
    }

    std::shared_ptr<android::KeyLayoutMap>* map_ptr = 
        reinterpret_cast<std::shared_ptr<android::KeyLayoutMap>*>(handle);
    
    if (!map_ptr || !*map_ptr) {
        return -1; // Invalid handle
    }

    android::status_t status = (*map_ptr)->mapKey(scan_code, usage_code, out_key_code, out_flags);
    return static_cast<int>(status);
}

int keylayoutmap_map_axis(KeyLayoutMapHandle handle, int32_t scan_code, AxisInfo* out_axis_info) {
    if (!handle || !out_axis_info) {
        return -1; // Invalid argument
    }

    std::shared_ptr<android::KeyLayoutMap>* map_ptr = 
        reinterpret_cast<std::shared_ptr<android::KeyLayoutMap>*>(handle);
    
    if (!map_ptr || !*map_ptr) {
        return -1; // Invalid handle
    }

    auto axis_opt = (*map_ptr)->mapAxis(scan_code);
    if (!axis_opt.has_value()) {
        return -2; // Axis not found
    }

    const android::AxisInfo& axis = axis_opt.value();
    out_axis_info->mode = static_cast<int32_t>(axis.mode);
    out_axis_info->axis = axis.axis;
    out_axis_info->highAxis = axis.highAxis;
    out_axis_info->splitValue = axis.splitValue;
    out_axis_info->flatOverride = axis.flatOverride;
    return 0; // Success
}

void keylayoutmap_destroy(KeyLayoutMapHandle handle) {
    if (!handle) {
        return;
    }

    std::shared_ptr<android::KeyLayoutMap>* map_ptr = 
        reinterpret_cast<std::shared_ptr<android::KeyLayoutMap>*>(handle);
    
    delete map_ptr;
}

} // extern "C"

