package io.github.sds100.keymapper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mukesh.MarkdownView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.delegate.ShowTextFromUrlDelegate

/**
 * Created by sds100 on 23/03/2019.
 */

class HelpFragment : Fragment(), ShowTextFromUrlDelegate {
    private val mHelpMarkdownUrl by lazy { getString(R.string.url_help) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return MarkdownView(context!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showTextFromUrl(context!!, mHelpMarkdownUrl) { text ->
            (view as MarkdownView).setMarkDownText(text)
        }
    }
}