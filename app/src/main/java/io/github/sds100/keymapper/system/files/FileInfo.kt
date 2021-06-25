package io.github.sds100.keymapper.system.files

/**
 * Created by sds100 on 23/06/2021.
 */

data class FileInfo(
    /**
     * This includes the extension
     */
    val nameWithExtension: String,
) {
    val name: String
        get() = nameWithExtension.substringBeforeLast('.')

    val extension: String
        get() = nameWithExtension.substringAfterLast('.')

}