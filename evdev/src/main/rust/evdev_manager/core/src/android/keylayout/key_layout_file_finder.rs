use std::{
    fs::{self, File},
    io::ErrorKind,
    path::PathBuf,
};

/// Trait for finding key layout files.
/// This allows dependency injection for testing purposes.
pub trait KeyLayoutFileFinder: Send + Sync {
    /// Find a key layout file in the system by its name.
    fn find_system_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf>;

    /// Find a key layout file shipped with Key Mapper by its name.
    fn find_key_mapper_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf>;
}

/// Default implementation that uses the real file system.
/// This searches the standard Android key layout file locations.
pub struct AndroidKeyLayoutFileFinder;

impl KeyLayoutFileFinder for AndroidKeyLayoutFileFinder {
    fn find_system_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf> {
        // See https://source.android.com/docs/core/interaction/input/key-layout-files#location
        let path_prefixes = vec![
            "/odm/usr/".to_string(),
            "/vendor/usr/".to_string(),
            "/system/usr/".to_string(),
            "/data/system/devices/".to_string(),
        ];

        for prefix in &path_prefixes {
            let path = PathBuf::new()
                .join(prefix)
                .join("keylayout")
                .join(format!("{}.kl", name));

            match fs::metadata(&path) {
                Ok(metadata) if metadata.is_file() => {
                    if File::open(&path).is_ok() {
                        return Some(path);
                    }
                }
                Err(e) if e.kind() != ErrorKind::NotFound => {
                    debug!("Error accessing {:?}: {}", path, e);
                }
                _ => {}
            }
        }

        None
    }

    fn find_key_mapper_key_layout_file_by_name(&self, _name: &str) -> Option<PathBuf> {
        None
    }
}
