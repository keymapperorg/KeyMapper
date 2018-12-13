package io.github.sds100.keymapper.Utils

import android.content.Context
import android.os.Build
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.mittsu.markedview.MarkedView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Views.ProgressDialog
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.okButton
import org.jetbrains.anko.uiThread
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

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
                val view = MarkedView(ctx)
                view.setMDText(changelogText)
                
                //dismiss the progress dialog once the changelog has been received
                progressDialog.dismiss()

                //show a dialog displaying the changelog in a textview
                ctx.alert {
                    customView = view
                    okButton { dialog -> dialog.dismiss() }
                }.show()

            }
        }
    }

    private fun getChangelogText(ctx: Context): String {
        if (NetworkUtils.isNetworkAvailable(ctx)) {
            downloadChangelogFromServer(ctx)
        }

        ctx.openFileInput(CHANGELOG_FILE_NAME).bufferedReader().use {
            return it.readText()
        }
    }

    private fun downloadChangelogFromServer(ctx: Context) {
        val directory = getChangelogDirectory(ctx)

        val inputStream = URL(CHANGELOG_URL).openStream()

        inputStream.use {
            //only available in Java 7 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(inputStream,
                        Paths.get("$directory/$CHANGELOG_FILE_NAME"), StandardCopyOption.REPLACE_EXISTING)
            } else {

                try {
                    val bufferedInputStream = BufferedInputStream(inputStream)
                    val fileOutputStream = FileOutputStream("$directory/$CHANGELOG_FILE_NAME")

                    bufferedInputStream.use {
                        fileOutputStream.use {
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int

                            while (true) {
                                bytesRead = bufferedInputStream.read(dataBuffer, 0, 1024)

                                //if at the end of the file, break out of the loop
                                if (bytesRead == -1) break

                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }

                } catch (e: IOException) {
                    Toast.makeText(ctx, "IO Exception when downloading changelog", LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getChangelogDirectory(ctx: Context): String {
        return ctx.filesDir.path
    }
}