use std::env;
use std::path::{Path, PathBuf};

fn main() {
    let manifest_dir: PathBuf = PathBuf::from(env!("CARGO_MANIFEST_DIR"));

    // Map Rust target architecture to Android ABI directory
    let target = env::var("TARGET").expect("TARGET environment variable not set");

    // This crate only supports Android targets for actual builds.
    // For cargo check on host systems, we'll skip C/C++ compilation but still
    // generate bindings to allow type checking.
    let is_android = target.contains("android");

    if !is_android {
        eprintln!(
            "Warning: Building for non-Android target '{}'. Skipping C/C++ compilation.",
            target
        );
        eprintln!("This crate is designed for Android. Use Gradle for actual builds.");
        // Skip all compilation but succeed to allow cargo check to work
        return;
    }

    // Path to C/C++ source files
    let cpp_dir = manifest_dir.join("../../cpp");

    println!("cargo:rerun-if-changed={}", cpp_dir.to_str().unwrap());

    // Find Android NDK sysroot for bindgen
    let ndk_sysroot = find_ndk_sysroot(&manifest_dir);
    let sysroot_include = ndk_sysroot.join("usr/include");

    let libevdev_dir = cpp_dir.join("libevdev");

    // Build C files from libevdev
    let mut c_builder = cc::Build::new();
    c_builder
        .file(libevdev_dir.join("libevdev.c"))
        .file(libevdev_dir.join("libevdev-names.c"))
        .file(libevdev_dir.join("libevdev-uinput.c"))
        .include(&libevdev_dir)
        .include(sysroot_include.join("linux/input-event-codes.h"))
        .flag("-Werror=format")
        .flag("-fdata-sections")
        .flag("-ffunction-sections");

    if env::var("PROFILE").unwrap() == "release" {
        c_builder.flag("-O2").flag("-fvisibility=hidden");
    }

    c_builder.compile("evdev_c");
}

fn find_ndk_sysroot(manifest_dir: &Path) -> PathBuf {
    let sdk_dir = get_sdk_dir(manifest_dir).expect("SDK directory not available");
    let ndk_version = "27.2.12479018";

    get_sysroot_for_version(&sdk_dir, &ndk_version)
}

fn get_sdk_dir(manifest_dir: &Path) -> Option<String> {
    // 1. Read from local.properties file
    // Navigate from evdev crate to project root
    let local_properties = manifest_dir.join("../../../../../local.properties");

    if let Ok(contents) = std::fs::read_to_string(&local_properties) {
        for line in contents.lines() {
            // Skip comments and empty lines
            let line = line.trim();
            if line.is_empty() || line.starts_with('#') {
                continue;
            }

            // Look for sdk.dir=value or android.sdk.dir=value
            if let Some(stripped) = line.strip_prefix("sdk.dir=") {
                let sdk_path = stripped.trim();
                if !sdk_path.is_empty() {
                    return Some(sdk_path.to_string());
                }
            }
            if let Some(stripped) = line.strip_prefix("android.sdk.dir=") {
                let sdk_path = stripped.trim();
                if !sdk_path.is_empty() {
                    return Some(sdk_path.to_string());
                }
            }
        }
    }

    // 2. Check environment variables
    if let Ok(sdk_dir) = env::var("ANDROID_SDK_ROOT") {
        return Some(sdk_dir);
    }

    if let Ok(sdk_dir) = env::var("ANDROID_HOME") {
        return Some(sdk_dir);
    }

    None
}

fn get_sysroot_for_version(sdk_dir: &str, version: &str) -> PathBuf {
    // Detect host platform
    let host = if cfg!(target_os = "macos") {
        "darwin-x86_64"
    } else if cfg!(target_os = "linux") {
        "linux-x86_64"
    } else if cfg!(target_os = "windows") {
        "windows-x86_64"
    } else {
        panic!("Unsupported host platform for NDK")
    };

    let sysroot = PathBuf::from(sdk_dir)
        .join("ndk")
        .join(version)
        .join("toolchains")
        .join("llvm")
        .join("prebuilt")
        .join(host)
        .join("sysroot");

    if !sysroot.exists() {
        panic!(
            "Could not find Android NDK sysroot for version {} at {}. Please ensure NDK {} is installed in {}/ndk/",
            version, sysroot.display(), version, sdk_dir
        );
    }

    sysroot
}
