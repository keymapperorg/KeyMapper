plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "io.github.sds100.keymapper.api"
    compileSdk =
        libs.versions.compile.sdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.min.sdk
                .get()
                .toInt()

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":base"))
    implementation(project(":system"))

    implementation(libs.jakewharton.timber)

    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
}
