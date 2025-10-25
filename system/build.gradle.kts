plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "io.github.sds100.keymapper.system"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
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
    implementation(project(":data"))
    implementation(project(":systemstubs"))
    implementation(project(":sysbridge"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.jakewharton.timber)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.net.lingala.zip4j)
    implementation(libs.lsposed.hiddenapibypass)
    implementation(libs.anggrayudi.storage)
    implementation(libs.androidx.core.ktx)
    implementation(libs.squareup.okhttp)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.github.topjohnwu.libsu)
}
