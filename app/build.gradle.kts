@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
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

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
            versionNameSuffix = "-ci.${versionProperties.getProperty("VERSION_NUM")}"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug") // Assuming debug signing for CI
        }
    }

    flavorDimensions.add("pro")
    productFlavors {
        create("free") {
            dimension = "pro"
        }
        create("pro") {
            dimension = "pro"
            val localPropertiesFile = rootProject.file("local.properties")
            val keyName = "REVENUECAT_API_KEY"
            if (localPropertiesFile.exists()) {
                val localProperties = Properties().apply {
                    FileInputStream(localPropertiesFile).use { fis -> load(fis) }
                }
                if (localProperties.containsKey(keyName)) {
                    buildConfigField("String", keyName, "${localProperties[keyName]}")
                }
            }
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        aidl = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
            output.outputFileName = "keymapper-${'$'}{variant.versionName}.apk"
        }
    }

}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    implementation(project(":common"))
    implementation(project(":base"))
    implementation(project(":api"))
    implementation(project(":data"))
    implementation(project(":system"))
    compileOnly(project(":systemstubs"))

    // TODO delete the unused libraries and plugins

    // kotlin stuff
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // random stuff
    implementation(libs.google.android.material)
    implementation(libs.kotson)
    implementation(libs.airbnb.epoxy)
    implementation(libs.airbnb.epoxy.databinding)
    debugImplementation(libs.androidx.ui.tooling)
    kapt(libs.airbnb.epoxy.processor)
    implementation(libs.jakewharton.timber)
    implementation(libs.net.lingala.zip4j)
    implementation(libs.anggrayudi.storage)
    implementation(libs.github.mflisar.dragselectrecyclerview)
    implementation(libs.google.flexbox)
    implementation(libs.lsposed.hiddenapibypass)
    "proImplementation"(libs.revenuecat.purchases)
    "proImplementation"(libs.airbnb.lottie.compose)
    implementation(libs.squareup.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.canopas.introshowcaseview)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)

    // splitties
    implementation(libs.bundles.splitties)

    // androidx
    implementation(libs.androidx.legacy.support.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.androidx.room.ktx)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.extensions) // Note: Deprecated
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.core.splashscreen)
    ksp(libs.androidx.room.compiler)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.android)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.google.accompanist.drawablepainter)
    implementation(libs.androidx.compose.ui.tooling)

//    debugImplementation(libs.squareup.leakcanary.android) // Keep commented

    // Dependencies for local unit tests
    testImplementation(libs.junit)
    testImplementation(libs.hamcrest.all)
    testImplementation(libs.androidx.junit.ktx) // androidx.test.ext:junit-ktx
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.params)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testDebugImplementation(libs.androidx.fragment.testing)

    // Dependencies for Android instrumented tests
    androidTestImplementation(libs.androidx.test.ext.junit) // androidx.test.ext:junit
    androidTestImplementation(libs.junit) // Repeated, fine
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.room.testing.legacy)
    androidTestImplementation(libs.mockito.android)
}
