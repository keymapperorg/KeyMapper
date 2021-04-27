package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.util.Result
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 13/04/2021.
 */
interface FileAdapter {
    fun openOutputStream(uriString: String): Result<OutputStream>
    fun openInputStream(uriString: String): Result<InputStream>

    fun getPicturesFolder(): File
}