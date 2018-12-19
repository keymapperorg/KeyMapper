package io.github.sds100.keymapper.Views

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.AttrUtils
import io.github.sds100.keymapper.Utils.FileUtils
import io.github.sds100.keymapper.Utils.MarkdownUtils
import io.github.sds100.keymapper.Utils.NetworkUtils
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.net.URL

/**
 * Created by sds100 on 15/12/2018.
 */

/**
 * Downloads a specified file from a url then displays it in a MarkdownView. It saves the file locally
 * so the user can view it while offline.
 */

class MarkdownDialogPreference(context: Context?, attrs: AttributeSet?) : Preference(context, attrs) {

    private val mFileUrl = AttrUtils.getCustomStringAttrValue(
            context!!,
            attrs!!,
            R.styleable.MarkdownDialogPreference,
            R.styleable.MarkdownDialogPreference_fileUrl
    )!!

    override fun onClick() {
        super.onClick()

        //show a progress dialog
        val progressDialog = ProgressDialog(context!!, R.string.dialog_message_downloading)

        progressDialog.show()

        doAsync {
            val text = getText(context)

            uiThread {
                //dismiss the progress dialog once the changelog has been received
                progressDialog.dismiss()
                MarkdownUtils.showDialog(context!!, text)
            }
        }
    }

    private fun getText(ctx: Context): String {
        val path = FileUtils.getPathToFileInAppData(ctx, extractFileName())
        NetworkUtils.downloadFile(ctx, mFileUrl, path)

        return FileUtils.getTextFromFile(ctx, extractFileName())
    }

    /**
     * Extracts the file name from the url
     */
    private fun extractFileName(): String {
        return File(URL(mFileUrl).path.toString()).name
    }
}