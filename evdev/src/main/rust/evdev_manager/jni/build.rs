fn main() {
    // This crate now uses pure JNI without C++ Binder layer.
    // No C++ compilation needed.

    // Just check that we're building for Android
    let target = std::env::var("TARGET").expect("TARGET environment variable not set");
    let is_android = target.contains("android");

    if !is_android {
        eprintln!(
            "Warning: Building for non-Android target '{}'. This crate is designed for Android.",
            target
        );
        eprintln!("Use Gradle for actual builds.");
    }
}
