/*
 * C interface wrapper for KeyLayoutMap
 * This provides a simple C API that can be easily bound from Rust
 */

#ifndef KEYLAYOUTMAP_C_H
#define KEYLAYOUTMAP_C_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

// Opaque handle to KeyLayoutMap
typedef void* KeyLayoutMapHandle;

// AxisInfo structure (matches android::AxisInfo)
typedef struct {
    int32_t mode;        // 0 = MODE_NORMAL, 1 = MODE_INVERT, 2 = MODE_SPLIT
    int32_t axis;
    int32_t highAxis;
    int32_t splitValue;
    int32_t flatOverride;
} AxisInfo;

// Load KeyLayoutMap from file
// Returns 0 on success, non-zero error code on failure
// On success, *out_handle will be set to a valid handle (must be freed with keylayoutmap_destroy)
// On failure, *out_handle will be NULL
int keylayoutmap_load(const char* filename, KeyLayoutMapHandle* out_handle);

// Load KeyLayoutMap from file contents
// Returns 0 on success, non-zero error code on failure
int keylayoutmap_load_contents(const char* filename, const char* contents, KeyLayoutMapHandle* out_handle);

// Map a key (scan code or usage code) to Android key code
// Returns 0 on success, non-zero error code on failure
// On success, *out_key_code and *out_flags will be set
int keylayoutmap_map_key(KeyLayoutMapHandle handle, int32_t scan_code, int32_t usage_code,
                         int32_t* out_key_code, uint32_t* out_flags);

// Map an axis (scan code) to AxisInfo
// Returns 0 if axis was found, non-zero if not found
// On success, *out_axis_info will be set
int keylayoutmap_map_axis(KeyLayoutMapHandle handle, int32_t scan_code, AxisInfo* out_axis_info);

// Destroy a KeyLayoutMap handle
// Safe to call with NULL handle
void keylayoutmap_destroy(KeyLayoutMapHandle handle);

#ifdef __cplusplus
}
#endif

#endif // KEYLAYOUTMAP_C_H


