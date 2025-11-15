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
    let android_dir = cpp_dir.join("android");
    let aidl_dir = cpp_dir.join("aidl");

    // Build C files from libevdev
    let mut c_builder = cc::Build::new();
    c_builder
        .file(libevdev_dir.join("libevdev.c"))
        .file(libevdev_dir.join("libevdev-names.c"))
        .file(libevdev_dir.join("libevdev-uinput.c"))
        .include(&cpp_dir)
        .include(&libevdev_dir)
        .flag("-Werror=format")
        .flag("-fdata-sections")
        .flag("-ffunction-sections");

    if env::var("PROFILE").unwrap() == "release" {
        c_builder.flag("-O2").flag("-fvisibility=hidden");
    }

    c_builder.compile("evdev_c");

    // Build C++ files
    let mut cpp_builder = cc::Build::new();
    cpp_builder
        .cpp(true)
        .std("c++20")
        // libevdev JNI wrapper
        .file(cpp_dir.join("libevdev_jni.cpp"))
        // Android input framework files
        .file(android_dir.join("input/KeyLayoutMap.cpp"))
        .file(android_dir.join("input/InputEventLabels.cpp"))
        .file(android_dir.join("input/InputDevice.cpp"))
        .file(android_dir.join("input/Input.cpp"))
        // C interface wrapper for KeyLayoutMap
        .file(cpp_dir.join("wrappers/keylayoutmap_c.cpp"))
        // JNI manager for IEvdevCallback so don't need to use a Rust binder library
        .file(cpp_dir.join("evdev_callback_jni_manager.cpp"))
        // Android base library files
        .file(android_dir.join("libbase/result.cpp"))
        .file(android_dir.join("libbase/stringprintf.cpp"))
        // Android utils files
        .file(android_dir.join("utils/Tokenizer.cpp"))
        .file(android_dir.join("utils/String16.cpp"))
        .file(android_dir.join("utils/String8.cpp"))
        .file(android_dir.join("utils/SharedBuffer.cpp"))
        .file(android_dir.join("utils/FileMap.cpp"))
        .file(android_dir.join("utils/Unicode.cpp"))
        // AIDL generated file
        .file(aidl_dir.join("io/github/sds100/keymapper/evdev/IEvdevCallback.cpp"))
        // Include directories
        .include(&cpp_dir)
        .include(&cpp_dir.join("wrappers"))
        .include(&android_dir)
        .include(&android_dir.join("input"))
        .include(&android_dir.join("libbase"))
        .include(&android_dir.join("utils"))
        .include(&libevdev_dir)
        // Compiler flags matching CMakeLists.txt
        .flag("-Werror=format")
        .flag("-fdata-sections")
        .flag("-ffunction-sections")
        .flag("-fno-exceptions")
        .flag("-fno-rtti")
        .flag("-fno-threadsafe-statics");

    if env::var("PROFILE").unwrap() == "release" {
        cpp_builder
            .flag("-O2")
            .flag("-fvisibility=hidden")
            .flag("-fvisibility-inlines-hidden");
    }

    cpp_builder.compile("evdev_cpp");

    // Link against Android libraries
    println!("cargo:rustc-link-lib=android");
    println!("cargo:rustc-link-lib=log");
    println!("cargo:rustc-link-lib=binder_ndk");

    // Generate Rust bindings from headers
    // We generate C and C++ bindings separately because:
    // 1. C headers (libevdev) need C mode to allow implicit void* conversions
    // 2. C++ headers (KeyLayoutMap.h) need C++ mode to find cstdint
    let evdev_headers_path = libevdev_dir.clone();

    // Common bindgen configuration
    let common_allow_attributes = vec![
        "#![allow(clippy::all)]",
        "#![allow(non_camel_case_types)]",
        "#![allow(non_snake_case)]",
        "#![allow(non_upper_case_globals)]",
        "#![allow(dead_code)]",
        "#![allow(rustdoc::broken_intra_doc_links)]",
        "#![allow(rustdoc::private_intra_doc_links)]",
        "#![allow(arithmetic_overflow)]", // Needed for bindgen-generated array size calculations
    ];

    // Generate C bindings (libevdev headers) in C mode
    let mut bindgen_builder =
        bindgen::Builder::default().formatter(bindgen::Formatter::Prettyplease);

    for attr in &common_allow_attributes {
        bindgen_builder = bindgen_builder.raw_line(*attr);
    }

    bindgen_builder = bindgen_builder
        .header(evdev_headers_path.join("libevdev.h").display().to_string())
        .header(
            evdev_headers_path
                .join("libevdev-int.h")
                .display()
                .to_string(),
        )
        .header(
            evdev_headers_path
                .join("libevdev-uinput.h")
                .display()
                .to_string(),
        )
        .header(
            evdev_headers_path
                .join("libevdev-uinput-int.h")
                .display()
                .to_string(),
        )
        .header(cpp_dir.join("evdev_callback_jni_manager.h").display().to_string())
        .header(cpp_dir.join("wrappers/keylayoutmap_c.h").display().to_string())
        // Generate a proper Rust enum for EvdevCallbackError
        .rustified_enum("EvdevCallbackError")
        .clang_arg(format!("--sysroot={}", ndk_sysroot.display()))
        .clang_arg(format!("-I{}", sysroot_include.display()));

    // Add architecture-specific includes if needed
    let arch_include = get_arch_include_path(&target, &ndk_sysroot);
    if arch_include.exists() {
        bindgen_builder = bindgen_builder.clang_arg(format!("-I{}", arch_include.display()));
    }

    let bindings = bindgen_builder
        .generate()
        .expect("Unable to generate C bindings");

    let out_path = manifest_dir.join("src");

    bindings
        .write_to_file(out_path.join("bindings.rs"))
        .expect("Couldn't write bindings!");
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

fn get_arch_include_path(target: &str, sysroot: &Path) -> PathBuf {
    let arch = if target.contains("aarch64") {
        "aarch64-linux-android"
    } else if target.contains("armv7") {
        "arm-linux-androideabi"
    } else if target.contains("i686") {
        "i686-linux-android"
    } else if target.contains("x86_64") {
        "x86_64-linux-android"
    } else {
        return sysroot.join("usr").join("include").join("nonexistent");
    };

    sysroot.join("usr").join("include").join(arch)
}
