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

    // Build C++ files
    let mut c_builder = cc::Build::new();
    c_builder
        .cpp(true)
        .file(cpp_dir.join("evdev_callback_jni_manager.cpp"))
        .include(&cpp_dir)
        .flag("-Werror=format")
        .flag("-fdata-sections")
        .flag("-ffunction-sections");

    if env::var("PROFILE").unwrap() == "release" {
        c_builder.flag("-O2").flag("-fvisibility=hidden");
    }

    c_builder.compile("evdev_manager_cpp");
}
