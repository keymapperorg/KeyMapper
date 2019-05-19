package io.github.sds100.keymapper.delegate

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.FileUtils
import io.github.sds100.keymapper.util.NetworkUtils
import io.github.sds100.keymapper.view.ProgressDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.net.URL

/**
 * Created by sds100 on 26/12/2018.
 */

/**
 * Downloads a specified file from a url and allows the text from that file to be shown however you want.
 * It saves the file locally so it can be viewed while offline.
 */
interface ShowTextFromUrlDelegate {

    fun showTextFromUrl(ctx: Context, fileUrl: String, onShowText: (text: String) -> Unit) {
        //show a progress dialog
        val progressDialog = ProgressDialog(ctx, R.string.dialog_message_downloading)

        progressDialog.show()

        doAsync {
            val text = getText(ctx, fileUrl)

            uiThread {
                //dismiss the progress dialog once the changelog has been received
                progressDialog.dismiss()
                onShowText(text)
            }
        }
    }

    private fun getText(ctx: Context, fileUrl: String): String {
        val path = FileUtils.getPathToFileInAppData(ctx, extractFileName(fileUrl))
        NetworkUtils.downloadFile(ctx, fileUrl, path)

        return FileUtils.getTextFromAppFiles(ctx, extractFileName(fileUrl))
    }

    /**
     * Extracts the file name from the url
     */
    private fun extractFileName(fileUrl: String): String {
        return File(URL(fileUrl).path.toString()).name
    }
}