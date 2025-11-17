/// Error type for evdev operations
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EvdevErrorCode {
    NoSuchFileOrDirectory,
    IoError,
    NoSuchDevice,
    BadFileDescriptor,
    OutOfMemory,
    WouldBlock,
    PermissionDenied,
    InvalidArgument,
    Unknown(i32),
}

impl EvdevErrorCode {
    pub fn from_code(code: i32) -> Self {
        match -code {
            libc::ENOENT => Self::NoSuchFileOrDirectory,
            libc::EIO => Self::IoError,
            libc::EBADF => Self::BadFileDescriptor,
            libc::EAGAIN => Self::WouldBlock,
            libc::ENOMEM => Self::OutOfMemory,
            libc::EACCES => Self::PermissionDenied,
            libc::ENODEV => Self::NoSuchDevice,
            libc::EINVAL => Self::InvalidArgument,
            _ => Self::Unknown(code),
        }
    }

    pub fn to_code(self) -> i32 {
        -(match self {
            Self::NoSuchFileOrDirectory => libc::ENOENT,
            Self::IoError => libc::EIO,
            Self::BadFileDescriptor => libc::EBADF,
            Self::WouldBlock => libc::EAGAIN,
            Self::OutOfMemory => libc::ENOMEM,
            Self::PermissionDenied => libc::EACCES,
            Self::NoSuchDevice => libc::ENODEV,
            Self::InvalidArgument => libc::EINVAL,
            Self::Unknown(code) => return code,
        })
    }

    pub fn description(&self) -> &'static str {
        match self {
            Self::NoSuchFileOrDirectory => "No such file or directory (device not found)",
            Self::IoError => "Input/output error",
            Self::NoSuchDevice => "No such device",
            Self::BadFileDescriptor => "Bad file descriptor",
            Self::OutOfMemory => "Out of memory",
            Self::WouldBlock => "Resource temporarily unavailable",
            Self::PermissionDenied => "Permission denied",
            Self::InvalidArgument => "Invalid argument",
            Self::Unknown(_) => "Unknown error",
        }
    }
}

#[derive(Debug)]
pub struct EvdevError {
    kind: EvdevErrorCode,
    code: i32,
    message: String,
}

impl EvdevError {
    pub fn new(code: i32) -> Self {
        let kind = EvdevErrorCode::from_code(code);
        let message = if let EvdevErrorCode::Unknown(_) = kind {
            format!("evdev manager error: {}", code)
        } else {
            format!("evdev manager error: {} ({})", kind.description(), -code)
        };

        Self {
            kind,
            code,
            message,
        }
    }
    pub fn from_enum(error_code: EvdevErrorCode) -> Self {
        let message = format!(
            "evdev manager error: {} ({})",
            error_code.description(),
            -error_code.to_code()
        );

        Self {
            kind: error_code,
            code: error_code.to_code(),
            message,
        }
    }

    pub fn code(&self) -> i32 {
        self.code
    }

    pub fn kind(&self) -> EvdevErrorCode {
        self.kind
    }
}

impl std::fmt::Display for EvdevError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl std::error::Error for EvdevError {}

impl From<EvdevError> for std::io::Error {
    fn from(err: EvdevError) -> Self {
        std::io::Error::from_raw_os_error(-err.code)
    }
}

impl From<std::io::Error> for EvdevError {
    fn from(err: std::io::Error) -> Self {
        let code = err.raw_os_error().unwrap_or(-1);
        EvdevError::new(-code)
    }
}
