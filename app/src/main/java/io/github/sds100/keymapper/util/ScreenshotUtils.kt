package io.github.sds100.keymapper.util

import android.os.Environment
import io.github.sds100.keymapper.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import splitties.toast.toast

/**
 * Created by sds100 on 18/06/2020.
 */
object ScreenshotUtils {
    fun takeScreenshotRoot() {
        val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenshotsFolder = "$picturesFolder/Screenshots"
        val fileDate = FileUtils.createFileDate()

        GlobalScope.launch {
            RootUtils.executeRootCommand(
                "mkdir -p $screenshotsFolder; screencap -p $screenshotsFolder/Screenshot_$fileDate.png",
                waitFor = true)

            launch(Dispatchers.Main) {
                toast(R.string.toast_screenshot_taken)
            }
        }
    }
}