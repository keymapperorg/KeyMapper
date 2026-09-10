package io.github.sds100.keymapper.common

interface BuildConfigProvider {
    val minApi: Int
    val maxApi: Int
    val packageName: String
    val version: String
    val versionCode: Int
    val sdkInt: Int

    /**
     * Whether this build is allowed to request MANAGE_EXTERNAL_STORAGE ("All files
     * access"). Google Play restricts this permission to file-manager-type apps, so
     * it's only requestable in the FOSS/F-Droid build — Android TV users predominantly
     * sideload that build rather than install from Play. This only gates whether the
     * *request* UI is ever shown; whether the permission is actually granted is a
     * separate runtime check (`Permission.MANAGE_EXTERNAL_STORAGE`).
     */
    val canRequestAllFilesAccess: Boolean
}
