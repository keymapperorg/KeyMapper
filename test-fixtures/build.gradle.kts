plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
}

// Fakes and test utilities shared between the unit tests of other modules.
android {
    namespace = "io.github.sds100.keymapper.testfixtures"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
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
    implementation(project(":base"))
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":system"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.junit)
    implementation(libs.anggrayudi.storage)
    implementation(libs.jakewharton.timber)
}
