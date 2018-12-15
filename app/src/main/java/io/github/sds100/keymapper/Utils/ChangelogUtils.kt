package io.github.sds100.keymapper.Utils

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Views.ProgressDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Created by sds100 on 10/12/2018.
 */

object ChangelogUtils {
    private const val CHANGELOG_URL = "https://raw.githubusercontent.com/sds100/KeyMapper/master/CHANGELOG.md"
    private const val CHANGELOG_FILE_NAME = "CHANGELOG.md"

    fun showChangelog(ctx: Context) {

        //show a progress dialog
        val progressDialog = ProgressDialog(ctx, R.string.dialog_message_downloading_changelog)

        progressDialog.show()

        doAsync {
            val changelogText = getChangelogText(ctx)

            uiThread {
                //dismiss the progress dialog once the changelog has been received
                progressDialog.dismiss()
                MarkdownUtils.showDialog(ctx, changelogText)
            }
        }
    }

    private fun getChangelogText(ctx: Context): String {
        val path = FileUtils.getPathToFileInAppData(ctx, CHANGELOG_FILE_NAME)
        NetworkUtils.downloadFile(ctx, CHANGELOG_URL, path)

        return FileUtils.getTextFromFile(ctx, CHANGELOG_FILE_NAME)
    }
}