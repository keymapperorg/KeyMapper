package io.github.sds100.keymapper.common

interface BuildConfigProvider {
    val minApi: Int
    val maxApi: Int
    val packageName: String
    val version: String
    val versionCode: Int
}