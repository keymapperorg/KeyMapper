package io.github.sds100.keymapper.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mukesh.MarkdownView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.delegate.ShowTextFromUrlDelegate
import io.github.sds100.keymapper.interfaces.OnLogChangedListener
import io.github.sds100.keymapper.util.Logger
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 23/03/2019.
 */

class LogFragment : Fragment(), ShowTextFromUrlDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Logger.registerOnLogChangedListener(object : OnLogChangedListener {
            override fun onLogChange() {
                updateText()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return MarkdownView(context!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateText()
    }

    override fun onDestroy() {
        super.onDestroy()

        Logger.unregisterOnLogChangedListener()
    }

    private fun updateText() {
        val text = Logger.read(context!!)

        (view as MarkdownView).let { markDownView ->
            markDownView.setMarkDownText(text)

            if (text.isEmpty()) {
                markDownView.setMarkDownText(context?.str(R.string.log_is_empty))
            }
        }
    }
}
