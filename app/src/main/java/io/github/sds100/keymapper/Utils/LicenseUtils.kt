package io.github.sds100.keymapper.Utils

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Views.ProgressDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 10/12/2018.
 */

object LicenseUtils {
    private const val LICENSE_URL = "https://raw.githubusercontent.com/sds100/KeyMapper/master/LICENSE.md"
    private const val LICENSE_FILE_NAME = "LICENSE.md"

    fun showLicense(ctx: Context) {

        //show a progress dialog
        val progressDialog = ProgressDialog(ctx, R.string.dialog_message_downloading_license)

        progressDialog.show()

        doAsync {
            val changelogText = getLicenseText(ctx)

            uiThread {
                //dismiss the progress dialog once the changelog has been received
                progressDialog.dismiss()
                MarkdownUtils.showDialog(ctx, changelogText)
            }
        }
    }

    private fun getLicenseText(ctx: Context): String {
        val path = FileUtils.getPathToFileInAppData(ctx, LICENSE_FILE_NAME)
        NetworkUtils.downloadFile(ctx, LICENSE_URL, path)

        return FileUtils.getTextFromFile(ctx, LICENSE_FILE_NAME)
    }
}