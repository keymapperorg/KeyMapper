plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.mozilla.rust.android)
}

android {
    namespace = "io.github.sds100.keymapper.evdev"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    // IMPORTANT! If you change this, also change it in build.rs for evdev crate and any CI workflows. This must
    // match one of the NDK versions installed in the GitHub actions runner. E.g look here for Ubuntu 24.04
    // https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md
    ndkVersion = "27.3.13750724"

    defaultConfig {
        // System bridge originally only supported Android 10+ because that was the min sdk
        // for binder-ndk library. Even though that is no longer used, it is extra effort to support
        // the changes to the internal Android APIs on Android 8 and 9.
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        // Disable because a Java implementation of IEvdevCallback is not required in this module
        aidl = false
    }

    packaging {
        jniLibs {
            // This replaces extractNativeLibs option in the manifest. This is needed so the
            // libraries are extracted to a location on disk where the system bridge process
            // can access them. Start in Android 6.0, they are no longer extracted by default.
            useLegacyPackaging = true

            // This is required on Android 15. Otherwise a java.lang.UnsatisfiedLinkError: dlopen failed: empty/missing DT_HASH/DT_GNU_HASH error is thrown.
            keepDebugSymbols.add("**/*.so")
        }
    }
}

cargo {
    val isDebug = gradle.startParameter.taskNames.any { it.lowercase().contains("debug") }

    module = "src/main/rust/evdev_manager"
    libname = "evdev_manager"
    targets = if (isDebug) {
        // Only building for one target saves some time when developing
        listOf("arm64")
    } else {
        listOf("arm", "arm64", "x86", "x86_64")
    }

    // Can not do this with buildType configurations.
    // See https://github.com/mozilla/rust-android-gradle/issues/38
    profile = if (isDebug) {
        "debug"
    } else {
        "release"
    }
}

dependencies {
}

// The list of event names needs to be parsed from the input.h file in the NDK.
// input.h can be found in the Android/sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/linux/input.h
// folder on macOS.
val generateLibEvDevEventNames by tasks.registering(Exec::class) {
    group = "build"
    description = "Generates event names header from input.h"

    val prebuiltDir = File(android.ndkDirectory, "toolchains/llvm/prebuilt")

    // The "darwin-x86_64" part of the path is different on each operating system but it seems like
    // the SDK Manager only downloads the NDK specific to the local operating system. So, just
    // go into the only directory that the "prebuilt" directory contains.
    val hostDirs = prebuiltDir.listFiles { file -> file.isDirectory }
        ?: throw GradleException("No prebuilt toolchain directories found in $prebuiltDir")

    if (hostDirs.size != 1) {
        throw GradleException(
            "Expected exactly one prebuilt toolchain directory in $prebuiltDir, found ${hostDirs.size}",
        )
    }
    val toolchainDir = hostDirs[0].absolutePath

    val inputHeader = "$toolchainDir/sysroot/usr/include/linux/input.h"
    val inputEventCodesHeader = "$toolchainDir/sysroot/usr/include/linux/input-event-codes.h"
    val outputHeader = "$projectDir/src/main/cpp/libevdev/event-names.h"
    val pythonScript = "$projectDir/src/main/cpp/libevdev/make-event-names.py"

    commandLine("python3", pythonScript, inputHeader, inputEventCodesHeader)

    standardOutput = File(outputHeader).outputStream()

    inputs.file(pythonScript)
    inputs.file(inputHeader)
    inputs.file(inputEventCodesHeader)
    outputs.file(outputHeader)
}

// Note: NDK AIDL compilation is no longer needed since we're using pure JNI
// instead of C++ Binder layer. The Kotlin side still uses IEvdevCallback AIDL,
// but that's handled by Android's standard AIDL processing.

tasks.named("preBuild") {
    dependsOn(generateLibEvDevEventNames)
}

// Ensure event names are generated before cargo build runs
afterEvaluate {
    tasks.matching { it.name.contains("cargoBuild") }.configureEach {
        dependsOn(generateLibEvDevEventNames)
    }
}

// Must come after all tasks above, otherwise gradle syncing fails.
//
// Run cargo build when the files change.
// See https://github.com/mozilla/rust-android-gradle/issues/166
tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        outputs.upToDateWhen { false }

        dependsOn("cargoBuild")
    }
}
