// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Must come before the others for the jnilibs folder to be automatically detected
    alias(libs.plugins.mozilla.rust.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.androidx.navigation.safeargs.kotlin) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jlleitschuh.gradle.ktlint) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dagger.hilt.android) apply false
}
