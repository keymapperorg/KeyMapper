package io.github.sds100.keymapper.system.apps

import kotlinx.serialization.Serializable

@Serializable
data class ActivityInfo(val activityName: String, val packageName: String)
