use std::env;
use std::path::Path;

/// Types of input device configuration files
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InputDeviceConfigurationFileType {
    Configuration = 0, // .idc file
    KeyLayout = 1,     // .kl file
    KeyCharacterMap = 2, // .kcm file
}

impl InputDeviceConfigurationFileType {
    fn directory(&self) -> &'static str {
        match self {
            Self::Configuration => "idc/",
            Self::KeyLayout => "keylayout/",
            Self::KeyCharacterMap => "keychars/",
        }
    }

    fn extension(&self) -> &'static str {
        match self {
            Self::Configuration => ".idc",
            Self::KeyLayout => ".kl",
            Self::KeyCharacterMap => ".kcm",
        }
    }
}

/// Input device identifier information
#[derive(Debug, Clone)]
pub struct InputDeviceIdentifier {
    pub name: String,
    pub bus: u16,
    pub vendor: u16,
    pub product: u16,
    pub version: u16,
}

impl InputDeviceIdentifier {
    /// Get canonical name with all invalid characters replaced by underscores
    pub fn get_canonical_name(&self) -> String {
        self.name
            .chars()
            .map(|ch| {
                if ch.is_ascii_alphanumeric() || ch == '-' || ch == '_' {
                    ch
                } else {
                    '_'
                }
            })
            .collect()
    }
}

/// Append the relative path for an input device configuration file
fn append_input_device_configuration_file_relative_path(
    path: &mut String,
    name: &str,
    file_type: InputDeviceConfigurationFileType,
) {
    path.push_str(file_type.directory());
    path.push_str(name);
    path.push_str(file_type.extension());
}

/// Gets the path of an input device configuration file by name
/// Searches system repository and user repository
/// Returns None if not found
pub fn get_input_device_configuration_file_path_by_name(
    name: &str,
    file_type: InputDeviceConfigurationFileType,
) -> Option<String> {
    // Search system repository
    // Treblized input device config files will be located in multiple paths
    let mut path_prefixes = vec![
        "/product/usr/".to_string(),
        "/system_ext/usr/".to_string(),
        "/odm/usr/".to_string(),
        "/vendor/usr/".to_string(),
        "/system/usr/".to_string(),
    ];

    // ANDROID_ROOT may not be set on host
    if let Ok(android_root) = env::var("ANDROID_ROOT") {
        path_prefixes.push(format!("{}/usr/", android_root));
    }

    // Try each system path prefix
    for prefix in &path_prefixes {
        let mut path = prefix.clone();
        append_input_device_configuration_file_relative_path(&mut path, name, file_type);

        match std::fs::metadata(&path) {
            Ok(metadata) if metadata.is_file() => {
                // Check if readable
                if let Ok(_) = std::fs::File::open(&path) {
                    return Some(path);
                }
            }
            Err(e) if e.kind() != std::io::ErrorKind::NotFound => {
                // Error other than file not found - log but continue
                debug!("Error accessing {}: {}", path, e);
            }
            _ => {
                // File not found, continue searching
            }
        }
    }

    // Search user repository
    // TODO: Should only look here if not in safe mode
    if let Ok(android_data) = env::var("ANDROID_DATA") {
        let mut path = format!("{}/system/devices/", android_data);
        append_input_device_configuration_file_relative_path(&mut path, name, file_type);

        match std::fs::metadata(&path) {
            Ok(metadata) if metadata.is_file() => {
                if let Ok(_) = std::fs::File::open(&path) {
                    return Some(path);
                }
            }
            Err(e) if e.kind() != std::io::ErrorKind::NotFound => {
                warn!("Error accessing user config file {}: {}", path, e);
            }
            _ => {}
        }
    }

    None
}

/// Gets the path of an input device configuration file by device identifier
/// Tries multiple naming schemes based on vendor/product/version, then device name, then Generic
/// Returns None if not found
pub fn get_input_device_configuration_file_path_by_device_identifier(
    device_identifier: &InputDeviceIdentifier,
    file_type: InputDeviceConfigurationFileType,
    suffix: Option<&str>,
) -> Option<String> {
    let suffix = suffix.unwrap_or("");

    // Try vendor/product/version path first
    if device_identifier.vendor != 0 && device_identifier.product != 0 {
        if device_identifier.version != 0 {
            let version_name = format!(
                "Vendor_{:04x}_Product_{:04x}_Version_{:04x}{}",
                device_identifier.vendor,
                device_identifier.product,
                device_identifier.version,
                suffix
            );
            if let Some(path) =
                get_input_device_configuration_file_path_by_name(&version_name, file_type)
            {
                info!("Found key layout map by version path {}", path);
                return Some(path);
            }
        }

        // Try vendor/product
        let product_name = format!(
            "Vendor_{:04x}_Product_{:04x}{}",
            device_identifier.vendor, device_identifier.product, suffix
        );
        if let Some(path) = get_input_device_configuration_file_path_by_name(&product_name, file_type)
        {
            info!("Found key layout map by product path {}", path);
            return Some(path);
        }
    }

    // Try device name (canonical)
    let canonical_name = device_identifier.get_canonical_name();
    let name_with_suffix = format!("{}{}", canonical_name, suffix);
    if let Some(path) = get_input_device_configuration_file_path_by_name(&name_with_suffix, file_type)
    {
        info!("Found key layout map by name path {}", path);
        return Some(path);
    }

    // As a last resort, try Generic
    get_input_device_configuration_file_path_by_name("Generic", file_type)
}

