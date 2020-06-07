package io.github.sds100.keymapper.data.model

import java.io.Serializable

/**
 * Created by sds100 on 30/03/2020.
 */
data class AppShortcutModel(val name: String, val packageName: String, val uri: String) : Serializable