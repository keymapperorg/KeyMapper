package io.github.sds100.keymapper.activity

import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.fragment.LogFragment
import io.github.sds100.keymapper.util.FeedbackUtils
import io.github.sds100.keymapper.util.Logger

/**
 * Created by sds100 on 11/05/2019.
 */

class LogActivity : SimpleFragmentActivity() {
    override val fragmentToShow: Fragment
        get() = LogFragment()


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_log, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_delete_log -> Logger.deleteLog(this)

            R.id.action_send_log -> {
                FeedbackUtils.sendFeedback(this, "\n\nHere is a log:\n${Logger.getLogText(this)}")
            }
        }

        return super.onOptionsItemSelected(item)
    }
}