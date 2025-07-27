import kotlin.io.path.absolutePathString

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "io.github.sds100.keymapper.sysbridge"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        // Must be API 29 so that the binder-ndk library can be found.
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                // -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON is required to get the app running on the Android 15. This is related to the new 16kB page size support.
                // -DANDROID_WEAK_API_DEFS=ON is required so the libevdev_jni file can run code depending on the SDK. https://developer.android.com/ndk/guides/using-newer-apis
                arguments(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DANDROID_WEAK_API_DEFS=ON",
                )
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
            // This replaces extractNativeLibs option in the manifest. This is needed so the
            // libraries are extracted to a location on disk where the system bridge process
            // can access them. Start in Android 6.0, they are no longer extracted by default.
            useLegacyPackaging = true

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
    compileOnly(project(":common"))

    implementation(libs.jakewharton.timber)

    // TODO use version catalog
    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)

    // From Shizuku :manager module build.gradle file.
    implementation("io.github.vvb2060.ndk:boringssl:20250114")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")
}

tasks.named("preBuild") {
    dependsOn(generateLibEvDevEventNames)
    dependsOn(compileAidlNdk)
}

// The list of event names needs to be parsed from the input.h file in the NDK.
// input.h can be found in the Android/sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/linux/input.h
// folder on macOS.
val generateLibEvDevEventNames by tasks.registering(Exec::class) {
    dependsOn(compileAidlNdk)

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

// Task to compile AIDL files for NDK.
// Taken from https://github.com/lakinduboteju/AndroidNdkBinderExamples
val compileAidlNdk by tasks.registering(Exec::class) {
    group = "build"
    description = "Compiles AIDL files in src/main/aidl to NDK C++ headers and sources."

    val aidlSrcDir = project.file("src/main/aidl")
    // Find all .aidl files. Using fileTree ensures it's dynamic.
    val aidlFiles = project.fileTree(aidlSrcDir) {
        include("**/IEvdevCallback.aidl")
        include("**/InputDeviceIdentifier.aidl")
    }

    inputs.files(aidlFiles)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("aidlInputFiles")

    val cppOutDir = project.file("src/main/cpp/aidl")
    val cppHeaderOutDir = project.file("src/main/cpp")

    outputs.dir(cppOutDir).withPropertyName("cppOutputDir")
    outputs.dir(cppHeaderOutDir).withPropertyName("cppHeaderOutputDir")

    // Path to the aidl executable in the Android SDK
    val aidlToolPath =
        android.sdkDirectory.toPath()
            .resolve("build-tools")
            .resolve(android.buildToolsVersion)
            .resolve("aidl")
            .absolutePathString()
    val importSearchPath = aidlSrcDir.absolutePath

    // Ensure output directories exist before trying to write to them
    cppOutDir.mkdirs()
    cppHeaderOutDir.mkdirs()

    if (aidlFiles.isEmpty) {
        logger.info("No AIDL files found in $aidlSrcDir. Skipping compileAidlNdk task.")
        return@registering // Exit doLast if no files to process
    }

    for (aidlFile in aidlFiles) {
        logger.lifecycle("Compiling AIDL file (NDK): ${aidlFile.path}")

        commandLine(
            aidlToolPath,
            "--lang=ndk",
            "-o", cppOutDir.absolutePath,
            "-h", cppHeaderOutDir.absolutePath,
            "-I", importSearchPath,
            aidlFile.absolutePath
        )
    }

    logger.lifecycle("AIDL NDK compilation finished. Check outputs in $cppOutDir and $cppHeaderOutDir")
}