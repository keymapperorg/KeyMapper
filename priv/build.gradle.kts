plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "io.github.sds100.keymapper.priv"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                // -DANDROID_STL=none is required by Rikka's library: https://github.com/RikkaW/libcxx-prefab
                // -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON is required to get the app running on the Android 15. This is related to the new 16kB page size support.
                arguments("-DANDROID_STL=none", "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        aidl = true
        prefab = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false

            // This is required on Android 15. Otherwise a java.lang.UnsatisfiedLinkError: dlopen failed: empty/missing DT_HASH/DT_GNU_HASH error is thrown.
            keepDebugSymbols.add("**/*.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    compileOnly(project(":systemstubs"))

    implementation(libs.jakewharton.timber)

    // TODO use version catalog
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    // From Shizuku :manager module build.gradle file.
    implementation("io.github.vvb2060.ndk:boringssl:20250114")
    implementation("dev.rikka.ndk.thirdparty:cxx:1.2.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")
}

tasks.named("preBuild") {
    dependsOn(generateLibEvDevEventNames)
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
        throw GradleException("Expected exactly one prebuilt toolchain directory in $prebuiltDir, found ${hostDirs.size}")
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