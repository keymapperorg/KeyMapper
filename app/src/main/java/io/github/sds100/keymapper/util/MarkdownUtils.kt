package io.github.sds100.keymapper.util

import android.content.Context
import com.mukesh.MarkdownView
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton

/**
 * Created by sds100 on 15/12/2018.
 */

object MarkdownUtils {

    fun showDialog(ctx: Context, text: String) {

        val markdownView = MarkdownView(ctx)
        markdownView.setMarkDownText(text)

        //open links in the default browser/app rather than in the WebView inside the dialog
        markdownView.isOpenUrlInBrowser = true

        //show a dialog displaying the changelog in a textview
        ctx.alert {
            customView = markdownView
            okButton { dialog -> dialog.dismiss() }
        }.show()
    }
}