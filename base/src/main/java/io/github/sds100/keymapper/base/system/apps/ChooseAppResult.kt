package io.github.sds100.keymapper.base.system.apps

import kotlinx.serialization.Serializable

@Serializable
data class ChooseAppResult(val packageName: String, val appName: String)
