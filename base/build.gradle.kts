import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
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
    namespace = "io.github.sds100.keymapper.base"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    val versionProperties = Properties().apply {
        project.file("../app/version.properties").inputStream().use { load(it) }
    }

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(
            "Integer",
            "VERSION_CODE",
            versionProperties.getProperty("VERSION_CODE"),
        )
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
}

dependencies {
    implementation(project(":common"))
    implementation(project(":data"))
    implementation(project(":sysbridge"))
    implementation(project(":system"))
    implementation(project(":systemstubs"))

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
    implementation(libs.anggrayudi.storage)
    implementation(libs.github.mflisar.dragselectrecyclerview)
    implementation(libs.google.flexbox)
    implementation(libs.squareup.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.canopas.introshowcaseview)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.bundles.splitties)

    // androidx
    implementation(libs.androidx.legacy.support.core.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.extensions) // Note: Deprecated
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)

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

    // Tests

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
