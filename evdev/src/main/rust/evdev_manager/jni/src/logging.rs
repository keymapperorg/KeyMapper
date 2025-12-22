use jni::objects::{GlobalRef, JValue};
use jni::JavaVM;
use libc::{c_char, c_int};
use log::LevelFilter;
use std::ffi::CString;
use std::str::FromStr;
use std::sync::{Arc, OnceLock};

/// Holds the JVM and SystemBridge reference for logging callbacks
static KEY_MAPPER_LOGGER: OnceLock<KeyMapperLogger> = OnceLock::new();

#[link(name = "log")]
extern "C" {
    pub fn __android_log_write(prio: c_int, tag: *const c_char, text: *const c_char) -> c_int;
}

/// Custom logger that forwards log messages to Kotlin via JNI
pub struct KeyMapperLogger {
    jvm: Arc<JavaVM>,
    system_bridge: GlobalRef,
    tag: CString,
}

impl KeyMapperLogger {
    pub fn init(jvm: Arc<JavaVM>, system_bridge: GlobalRef, tag: CString) {
        let logger = Self {
            jvm,
            system_bridge,
            tag,
        };

        if KEY_MAPPER_LOGGER.set(logger).is_err() {
            panic!("Log callback holder already initialized");
        }

        // Set up the custom JNI logger
        // Note: log::set_logger can only be called once per process
        if log::set_logger(KEY_MAPPER_LOGGER.get().unwrap()).is_err() {
            panic!("Failed to set logger");
        }

        // Set default log level: Info for production builds, Debug for debug builds
        let log_level = if cfg!(debug_assertions) {
            LevelFilter::Debug
        } else {
            LevelFilter::Info
        };

        log::set_max_level(log_level);
    }

    pub fn set_level(level: AndroidLogLevel) {
        log::set_max_level(level.into());
    }

    pub fn set_log_panic_hook() {
        std::panic::set_hook(Box::new(|panic_info| {
            let mut message = String::from("PANIC in Rust code!");

            if let Some(location) = panic_info.location() {
                message.push_str(&format!(
                    " at {}:{}:{}",
                    location.file(),
                    location.line(),
                    location.column()
                ));
            } else {
                message.push_str(" at unknown location");
            }

            if let Some(payload) = panic_info.payload().downcast_ref::<&str>() {
                message.push_str(&format!(" - {}", payload));
            } else if let Some(payload) = panic_info.payload().downcast_ref::<String>() {
                message.push_str(&format!(" - {}", payload));
            } else {
                message.push_str(" - unknown payload");
            }

            // Also log via the standard logger for logcat
            error!("{}", message);
        }));
    }

    /// Send a log message to Java via JNI
    fn send_log_to_java(&self, level: i32, message: &str) {
        let mut env = self
            .jvm
            .attach_current_thread_permanently()
            .expect("Failed to attach to JVM thread");

        if let Ok(msg) = env.new_string(message) {
            let _ = env.call_method(
                &self.system_bridge,
                "onLogMessage",
                "(ILjava/lang/String;)V",
                &[JValue::Int(level), JValue::Object(&msg.into())],
            );
        }
    }
}

// Safety: JavaVM and GlobalRef are thread-safe
unsafe impl Send for KeyMapperLogger {}
unsafe impl Sync for KeyMapperLogger {}

impl log::Log for KeyMapperLogger {
    fn enabled(&self, metadata: &log::Metadata) -> bool {
        metadata.level() <= log::max_level()
    }

    fn log(&self, record: &log::Record) {
        if !self.enabled(record.metadata()) {
            return;
        }

        let msg_level: AndroidLogLevel = match record.level() {
            log::Level::Error => AndroidLogLevel::Error,
            log::Level::Warn => AndroidLogLevel::Warn,
            log::Level::Info => AndroidLogLevel::Info,
            log::Level::Debug => AndroidLogLevel::Debug,
            log::Level::Trace => AndroidLogLevel::Verbose,
        };

        let message = format!("{}", record.args());
        let c_message = CString::from_str(&message).unwrap();

        // This is taken from the android_log crate. https://crates.io/crates/android_log
        unsafe {
            __android_log_write(msg_level as c_int, self.tag.as_ptr(), c_message.as_ptr());
        }

        self.send_log_to_java(msg_level as i32, &message);
    }

    fn flush(&self) {}
}

#[derive(Clone, Copy, PartialEq)]
pub enum AndroidLogLevel {
    Unknown = 0,
    Default,
    Verbose,
    Debug,
    Info,
    Warn,
    Error,
    Fatal,
    Silent,
}

impl From<AndroidLogLevel> for log::LevelFilter {
    fn from(level: AndroidLogLevel) -> Self {
        match level {
            AndroidLogLevel::Verbose => log::LevelFilter::Trace,
            AndroidLogLevel::Debug => log::LevelFilter::Debug,
            AndroidLogLevel::Info => log::LevelFilter::Info,
            AndroidLogLevel::Warn => log::LevelFilter::Warn,
            AndroidLogLevel::Error => log::LevelFilter::Error,
            _ => log::LevelFilter::Off,
        }
    }
}

impl From<log::Level> for AndroidLogLevel {
    fn from(level: log::Level) -> Self {
        match level {
            log::Level::Trace => AndroidLogLevel::Verbose,
            log::Level::Debug => AndroidLogLevel::Debug,
            log::Level::Info => AndroidLogLevel::Info,
            log::Level::Warn => AndroidLogLevel::Warn,
            log::Level::Error => AndroidLogLevel::Error,
        }
    }
}

impl From<i32> for AndroidLogLevel {
    fn from(value: i32) -> Self {
        match value {
            0 => AndroidLogLevel::Unknown,
            1 => AndroidLogLevel::Default,
            2 => AndroidLogLevel::Verbose,
            3 => AndroidLogLevel::Debug,
            4 => AndroidLogLevel::Info,
            5 => AndroidLogLevel::Warn,
            6 => AndroidLogLevel::Error,
            7 => AndroidLogLevel::Fatal,
            8 => AndroidLogLevel::Silent,
            _ => AndroidLogLevel::Debug,
        }
    }
}
