package io.github.sds100.keymapper.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import io.github.sds100.keymapper.R
import org.jetbrains.anko.selector

/**
 * Created by sds100 on 06/04/2019.
 */

object FlashlightUtils {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val LENS_LABEL_MAP = mapOf(
            CameraCharacteristics.LENS_FACING_FRONT to R.string.lens_front,
            CameraCharacteristics.LENS_FACING_BACK to R.string.lens_back
    )

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @StringRes
    fun getLensLabel(lens: Int) = LENS_LABEL_MAP.getValue(lens)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun showLensSelector(ctx: Context, onSelect: (lens: Int) -> Unit) {
        ctx.apply {
            val items = LENS_LABEL_MAP.map { ctx.str(it.value) }

            ctx.selector(str(R.string.dialog_title_choose_flash), items) { _, which ->

                onSelect(LENS_LABEL_MAP.toList()[which].first)
            }
        }
    }
}