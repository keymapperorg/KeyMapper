plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
    alias(libs.plugins.mozilla.rust.android)
}

android {
    namespace = "io.github.sds100.keymapper.keymapengine"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    // Keep in sync with the evdev module and CI workflows.
    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

// Specify path to rustc and cargo in local.properties, see the evdev module.
cargo {
    val isDebug = gradle.startParameter.taskNames.any { it.lowercase().contains("debug") }

    module = "src/main/rust/keymap_engine"
    libname = "keymap_engine"
    targets = if (isDebug) {
        // Only building for one target saves some time when developing
        listOf("arm64")
    } else {
        listOf("arm", "arm64", "x86", "x86_64")
    }

    // See https://github.com/mozilla/rust-android-gradle/issues/38
    profile = if (isDebug) {
        "debug"
    } else {
        "release"
    }
}

// Must come after all tasks above, otherwise gradle syncing fails.
// Run cargo build when the files change.
// See https://github.com/mozilla/rust-android-gradle/issues/166
tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        outputs.upToDateWhen { false }

        dependsOn("cargoBuild")
    }
}
