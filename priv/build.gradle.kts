plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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

    buildFeatures {
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

    implementation("org.conscrypt:conscrypt-android:2.5.3")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // From Shizuku :manager module build.gradle file.
    implementation("io.github.vvb2060.ndk:boringssl:20250114")
    implementation("dev.rikka.ndk.thirdparty:cxx:1.2.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("dev.rikka.rikkax.core:core-ktx:1.4.1")
}