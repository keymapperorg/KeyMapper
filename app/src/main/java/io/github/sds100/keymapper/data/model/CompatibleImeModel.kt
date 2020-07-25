package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 24/07/20.
 */
data class CompatibleImeListItemModel(
    val packageName: String,
    val imeName: String,
    val description: String,
    val errorMessage: String? = null,
    val playStoreLink: String? = null,
    val fdroidLink: String? = null,
    val githubLink: String? = null,
    val xdaLink: String? = null
) {
    val isSupported: Boolean
        get() = errorMessage == null
}