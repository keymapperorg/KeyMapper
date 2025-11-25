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
    let cpp_dir = manifest_dir.join("../../../cpp");

    println!("cargo:rerun-if-changed={}", cpp_dir.to_str().unwrap());

    // Find Android NDK sysroot
    let ndk_sysroot = find_ndk_sysroot(&manifest_dir);
    let sysroot_include = ndk_sysroot.join("usr/include");

    // Get target-specific library directory for linking
    let target_lib_dir = get_target_lib_dir(&ndk_sysroot, &target);

    // Build C++ files (AIDL-generated binder code and JNI manager)
    let mut cpp_builder = cc::Build::new();
    cpp_builder
        .cpp(true)
        .cpp_link_stdlib(None) // We'll link manually with static lib
        .file(cpp_dir.join("evdev_callback_jni_manager.cpp"))
        .file(cpp_dir.join("aidl/io/github/sds100/keymapper/evdev/IEvdevCallback.cpp"))
        .include(&cpp_dir)
        .include(&sysroot_include)
        .flag("-std=c++20")
        .flag("-Werror=format")
        .flag("-fdata-sections")
        .flag("-ffunction-sections")
        .flag("-fexceptions");

    if env::var("PROFILE").unwrap() == "release" {
        cpp_builder.flag("-O2").flag("-fvisibility=hidden");
    }

    cpp_builder.compile("evdev_manager_cpp");

    // Link against static C++ standard library
    println!("cargo:rustc-link-search=native={}", target_lib_dir.display());
    println!("cargo:rustc-link-lib=static=c++_static");
    println!("cargo:rustc-link-lib=static=c++abi");
    
    // Link against NDK Binder library (required for AIDL)
    println!("cargo:rustc-link-lib=dylib=binder_ndk");
}

fn find_ndk_sysroot(manifest_dir: &Path) -> PathBuf {
    let sdk_dir = get_sdk_dir(manifest_dir).expect("SDK directory not available");
    let ndk_version = "27.2.12479018";

    get_sysroot_for_version(&sdk_dir, ndk_version)
}

fn get_sdk_dir(manifest_dir: &Path) -> Option<String> {
    // 1. Read from local.properties file
    // Navigate from jni crate to project root (foss/)
    let local_properties = manifest_dir.join("../../../../../../local.properties");

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

fn get_target_lib_dir(sysroot: &Path, target: &str) -> PathBuf {
    // Map Rust target to NDK target triple
    let ndk_triple = if target.contains("aarch64") {
        "aarch64-linux-android"
    } else if target.contains("armv7") || target.contains("arm-linux") {
        "arm-linux-androideabi"
    } else if target.contains("x86_64") {
        "x86_64-linux-android"
    } else if target.contains("i686") {
        "i686-linux-android"
    } else {
        panic!("Unsupported Android target: {}", target)
    };

    // The static C++ libraries are in sysroot/usr/lib/{target}/
    sysroot.join("usr").join("lib").join(ndk_triple)
}
