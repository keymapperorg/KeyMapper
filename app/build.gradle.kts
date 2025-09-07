@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jlleitschuh.gradle.ktlint)
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "io.github.sds100.keymapper"
    compileSdk = libs.versions.compile.sdk.get().toInt()
    buildToolsVersion = libs.versions.build.tools.get()

    val versionProperties = Properties().apply {
        project.file("version.properties").inputStream().use { load(it) }
    }

    defaultConfig {
        applicationId = "io.github.sds100.keymapper"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()

        versionCode = versionProperties.getProperty("VERSION_CODE").toInt()
        versionName = versionProperties.getProperty("VERSION_NAME")
        multiDexEnabled = true

        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "keymapper"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            versionNameSuffix = "-foss"
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-foss-debug"
        }

        create("debug_release") {
            initWith(getByName("debug"))
            applicationIdSuffix = "" // Reset from debug
            matchingFallbacks.add("debug")
        }

        create("ci") {
            isMinifyEnabled = true
            // shrinkResources is now part of isMinifyEnabled in newer AGP,
            // but let's keep explicit if an older AGP interpretation is in mind.
            // If build fails, this might need adjustment.
            // For AGP 8.x, shrinkResources is controlled by isMinifyEnabled.
            // Explicitly setting it might be deprecated or have no effect.
            // I'll assume it implies full R8 shrinkage.
            // shrinkResources = true // This property might not exist directly here in KTS for AGP 8+
            // Instead, you rely on isMinifyEnabled and Proguard rules.

            matchingFallbacks.add("debug")
            applicationIdSuffix = ".ci"
            versionNameSuffix = "-foss-ci.${versionProperties.getProperty("VERSION_NUM")}"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug") // Assuming debug signing for CI
        }
    }

    buildFeatures {
        dataBinding = true
        aidl = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    kapt {
        correctErrorTypes = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
            resources.srcDirs("src/test/resources")
        }
        getByName("test") {
            java.srcDirs("src/pro/test/java")
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "keymapper-$versionName.apk"
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    implementation(project(":common"))
    implementation(project(":base"))
    implementation(project(":api"))
    implementation(project(":data"))
    implementation(project(":sysbridge"))
    implementation(project(":system"))
    compileOnly(project(":systemstubs"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Other
    implementation(libs.jakewharton.timber)

    // Androidx
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.appcompat)
    implementation(libs.bundles.androidx.navigation)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.android)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.google.accompanist.drawablepainter)
    implementation(libs.androidx.compose.ui.tooling)

    // Dagger
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
