#![allow(bad_style)]
#![allow(dead_code)]
#![allow(improper_ctypes)]

pub use libc::timeval;
use libc::{c_char, c_int, c_uint, c_ushort, c_void, size_t};

pub type __enum_ty = c_int;
pub type libevdev_read_flag = __enum_ty;
pub type libevdev_log_priority = __enum_ty;
pub type libevdev_grab_mode = __enum_ty;
pub type libevdev_read_status = __enum_ty;
pub type libevdev_led_value = __enum_ty;
pub type libevdev_uinput_open_mode = __enum_ty;

pub const LIBEVDEV_READ_FLAG_SYNC: libevdev_read_flag = 1;
pub const LIBEVDEV_READ_FLAG_NORMAL: libevdev_read_flag = 2;
pub const LIBEVDEV_READ_FLAG_FORCE_SYNC: libevdev_read_flag = 4;
pub const LIBEVDEV_READ_FLAG_BLOCKING: libevdev_read_flag = 8;

pub const LIBEVDEV_LOG_ERROR: libevdev_log_priority = 10;
pub const LIBEVDEV_LOG_INFO: libevdev_log_priority = 20;
pub const LIBEVDEV_LOG_DEBUG: libevdev_log_priority = 30;

pub const LIBEVDEV_GRAB: libevdev_grab_mode = 3;
pub const LIBEVDEV_UNGRAB: libevdev_grab_mode = 4;

pub const LIBEVDEV_READ_STATUS_SUCCESS: libevdev_read_status = 0;
pub const LIBEVDEV_READ_STATUS_SYNC: libevdev_read_status = 1;

pub const LIBEVDEV_LED_ON: libevdev_led_value = 3;
pub const LIBEVDEV_LED_OFF: libevdev_led_value = 4;

pub const LIBEVDEV_UINPUT_OPEN_MANAGED: libevdev_uinput_open_mode = -2;

pub enum libevdev {}
pub enum libevdev_uinput {}

#[repr(C)]
pub struct va_list {
    // TODO
}

#[repr(C)]
#[derive(Copy, Clone)]
pub struct input_event {
    pub time: timeval,
    pub type_: c_ushort,
    pub code: c_ushort,
    pub value: c_int,
}

#[repr(C)]
#[derive(Debug, Copy, Clone)]
pub struct input_absinfo {
    pub value: c_int,
    pub minimum: c_int,
    pub maximum: c_int,
    pub fuzz: c_int,
    pub flat: c_int,
    pub resolution: c_int,
}

type libevdev_log_func_t = extern "C" fn(
    *const libevdev,
    *mut c_void,
    *const c_char,
    c_int,
    *const c_char,
    *const c_char,
    va_list,
);

type libevdev_device_log_func_t = extern "C" fn(
    *const libevdev,
    c_int,
    *mut c_void,
    *const c_char,
    c_int,
    *const c_char,
    *const c_char,
    va_list,
);

extern "C" {
    pub fn libevdev_new() -> *mut libevdev;
    pub fn libevdev_new_from_fd(fd: c_int, ctx: *mut *mut libevdev) -> c_int;
    pub fn libevdev_free(ctx: *mut libevdev);
    pub fn libevdev_set_log_function(logfunc: libevdev_log_func_t, data: *mut c_void);
    pub fn libevdev_set_log_priority(priority: libevdev_log_priority);
    pub fn libevdev_get_log_priority() -> libevdev_log_priority;
    pub fn libevdev_set_device_log_function(
        ctx: *mut libevdev,
        logfunc: libevdev_device_log_func_t,
        priority: libevdev_log_priority,
        data: *mut c_void,
    );
    pub fn libevdev_grab(ctx: *mut libevdev, grab: libevdev_grab_mode) -> c_int;
    pub fn libevdev_set_fd(ctx: *mut libevdev, fd: c_int) -> c_int;
    pub fn libevdev_change_fd(ctx: *mut libevdev, fd: c_int) -> c_int;
    pub fn libevdev_get_fd(ctx: *mut libevdev) -> c_int;
    pub fn libevdev_next_event(ctx: *mut libevdev, flags: c_uint, ev: *mut input_event) -> c_int;
    pub fn libevdev_has_event_pending(ctx: *mut libevdev) -> c_int;
    pub fn libevdev_get_name(ctx: *const libevdev) -> *const c_char;
    pub fn libevdev_set_name(ctx: *mut libevdev, name: *const c_char);
    pub fn libevdev_get_phys(ctx: *const libevdev) -> *const c_char;
    pub fn libevdev_set_phys(ctx: *mut libevdev, phys: *const c_char);
    pub fn libevdev_get_uniq(ctx: *const libevdev) -> *const c_char;
    pub fn libevdev_set_uniq(ctx: *mut libevdev, uniq: *const c_char);
    pub fn libevdev_get_id_product(ctx: *const libevdev) -> c_int;
    pub fn libevdev_set_id_product(ctx: *mut libevdev, product_id: c_int);
    pub fn libevdev_get_id_vendor(ctx: *const libevdev) -> c_int;
    pub fn libevdev_set_id_vendor(ctx: *mut libevdev, vendor_id: c_int);
    pub fn libevdev_get_id_bustype(ctx: *const libevdev) -> c_int;
    pub fn libevdev_set_id_bustype(ctx: *mut libevdev, bustype: c_int);
    pub fn libevdev_get_id_version(ctx: *const libevdev) -> c_int;
    pub fn libevdev_set_id_version(ctx: *mut libevdev, version: c_int);
    pub fn libevdev_get_driver_version(ctx: *const libevdev) -> c_int;
    pub fn libevdev_has_property(ctx: *const libevdev, prop: c_uint) -> c_int;
    pub fn libevdev_enable_property(ctx: *mut libevdev, prop: c_uint) -> c_int;
    pub fn libevdev_disable_property(ctx: *mut libevdev, prop: c_uint) -> c_int;
    pub fn libevdev_has_event_type(ctx: *const libevdev, type_: c_uint) -> c_int;
    pub fn libevdev_has_event_code(ctx: *const libevdev, type_: c_uint, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_minimum(ctx: *const libevdev, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_maximum(ctx: *const libevdev, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_fuzz(ctx: *const libevdev, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_flat(ctx: *const libevdev, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_resolution(ctx: *const libevdev, code: c_uint) -> c_int;
    pub fn libevdev_get_abs_info(ctx: *const libevdev, code: c_uint) -> *const input_absinfo;
    pub fn libevdev_get_event_value(ctx: *const libevdev, type_: c_uint, code: c_uint) -> c_int;
    pub fn libevdev_set_event_value(
        ctx: *mut libevdev,
        type_: c_uint,
        code: c_uint,
        value: c_int,
    ) -> c_int;
    pub fn libevdev_fetch_event_value(
        ctx: *const libevdev,
        type_: c_uint,
        code: c_uint,
        value: *mut c_int,
    ) -> c_int;
    pub fn libevdev_get_slot_value(ctx: *const libevdev, slot: c_uint, code: c_uint) -> c_int;
    pub fn libevdev_set_slot_value(
        ctx: *mut libevdev,
        slot: c_uint,
        code: c_uint,
        value: c_int,
    ) -> c_int;
    pub fn libevdev_fetch_slot_value(
        ctx: *const libevdev,
        slot: c_uint,
        code: c_uint,
        value: *mut c_int,
    ) -> c_int;
    pub fn libevdev_get_num_slots(ctx: *const libevdev) -> c_int;
    pub fn libevdev_get_current_slot(ctx: *const libevdev) -> c_int;
    pub fn libevdev_set_abs_minimum(ctx: *mut libevdev, code: c_uint, min: c_int);
    pub fn libevdev_set_abs_maximum(ctx: *mut libevdev, code: c_uint, max: c_int);
    pub fn libevdev_set_abs_fuzz(ctx: *mut libevdev, code: c_uint, fuzz: c_int);
    pub fn libevdev_set_abs_flat(ctx: *mut libevdev, code: c_uint, flat: c_int);
    pub fn libevdev_set_abs_resolution(ctx: *mut libevdev, code: c_uint, resolution: c_int);
    pub fn libevdev_set_abs_info(ctx: *mut libevdev, code: c_uint, abs: *const input_absinfo);
    pub fn libevdev_enable_event_type(ctx: *mut libevdev, type_: c_uint) -> c_int;
    pub fn libevdev_disable_event_type(ctx: *mut libevdev, type_: c_uint) -> c_int;
    pub fn libevdev_enable_event_code(
        ctx: *mut libevdev,
        type_: c_uint,
        code: c_uint,
        data: *const c_void,
    ) -> c_int;
    pub fn libevdev_disable_event_code(ctx: *mut libevdev, type_: c_uint, code: c_uint) -> c_int;
    pub fn libevdev_kernel_set_abs_info(
        ctx: *mut libevdev,
        code: c_uint,
        abs: *const input_absinfo,
    ) -> c_int;
    pub fn libevdev_kernel_set_led_value(
        ctx: *mut libevdev,
        code: c_uint,
        value: libevdev_led_value,
    ) -> c_int;
    pub fn libevdev_kernel_set_led_values(ctx: *mut libevdev, ...) -> c_int;
    pub fn libevdev_set_clock_id(ctx: *mut libevdev, clockid: c_int) -> c_int;
    pub fn libevdev_event_is_type(ev: *const input_event, type_: c_uint) -> c_int;
    pub fn libevdev_event_is_code(ev: *const input_event, type_: c_uint, code: c_uint) -> c_int;
    pub fn libevdev_event_type_get_name(type_: c_uint) -> *const c_char;
    pub fn libevdev_event_code_get_name(type_: c_uint, code: c_uint) -> *const c_char;
    pub fn libevdev_property_get_name(prop: c_uint) -> *const c_char;
    pub fn libevdev_event_type_get_max(type_: c_uint) -> c_int;
    pub fn libevdev_event_type_from_name(name: *const c_char) -> c_int;
    pub fn libevdev_event_type_from_name_n(name: *const c_char, len: size_t) -> c_int;
    pub fn libevdev_event_code_from_name(type_: c_uint, name: *const c_char) -> c_int;
    pub fn libevdev_event_code_from_name_n(
        type_: c_uint,
        name: *const c_char,
        len: size_t,
    ) -> c_int;
    pub fn libevdev_property_from_name(name: *const c_char) -> c_int;
    pub fn libevdev_property_from_name_n(name: *const c_char, len: size_t) -> c_int;
    pub fn libevdev_get_repeat(
        ctx: *const libevdev,
        delay: *mut c_int,
        period: *mut c_int,
    ) -> c_int;
    pub fn libevdev_uinput_create_from_device(
        ctx: *const libevdev,
        uinput_fd: c_int,
        uinput_dev: *mut *mut libevdev_uinput,
    ) -> c_int;
    pub fn libevdev_uinput_destroy(uinput_dev: *mut libevdev_uinput);
    pub fn libevdev_uinput_get_devnode(uinput_dev: *mut libevdev_uinput) -> *const c_char;
    pub fn libevdev_uinput_get_fd(uinput_dev: *const libevdev_uinput) -> c_int;
    pub fn libevdev_uinput_get_syspath(uinput_dev: *mut libevdev_uinput) -> *const c_char;
    pub fn libevdev_uinput_write_event(
        uinput_dev: *const libevdev_uinput,
        type_: c_uint,
        code: c_uint,
        value: c_int,
    ) -> c_int;
}
