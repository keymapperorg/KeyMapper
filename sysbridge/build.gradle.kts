plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
}

android {
    namespace = "io.github.sds100.keymapper.sysbridge"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    ndkVersion = "27.2.12479018"

    defaultConfig {
        // Must be API 29 so that the binder-ndk library can be found.
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                val aidlSrcDir = project.file("src/main/cpp/aidl")

                // -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON is required to get the app running on the Android 15. This is related to the new 16kB page size support.
                // -DANDROID_WEAK_API_DEFS=ON is required so the libevdev_jni file can run code depending on the SDK. https://developer.android.com/ndk/guides/using-newer-apis
                arguments(
                    "-DANDROID_STL=c++_static",
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DANDROID_WEAK_API_DEFS=ON",
                    "-Daidl_src_dir=${aidlSrcDir.absolutePath}",
                )
            }
        }
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        buildConfig = true
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
    implementation(project(":common"))

    implementation(libs.jakewharton.timber)

    implementation(libs.conscrypt.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.github.topjohnwu.libsu)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)

    // From Shizuku :manager module build.gradle file.
    implementation(libs.vvb2060.ndk.boringssl)
    implementation(libs.lsposed.hiddenapibypass.updated)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.zhanghai.appiconloader)
    implementation(libs.rikka.rikkax.core)
}
