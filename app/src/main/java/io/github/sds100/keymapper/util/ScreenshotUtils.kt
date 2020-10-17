package io.github.sds100.keymapper.util

import android.os.Environment

/**
 * Created by sds100 on 18/06/2020.
 */
object ScreenshotUtils {
    fun takeScreenshotRoot() {
        val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val screenshotsFolder = "$picturesFolder/Screenshots"
        val fileDate = FileUtils.createFileDate()

        RootUtils.executeRootCommand("mkdir -p $screenshotsFolder; screencap -p $screenshotsFolder/Screenshot_$fileDate.png")
    }
}