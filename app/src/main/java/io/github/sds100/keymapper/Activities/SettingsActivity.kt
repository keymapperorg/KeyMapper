package io.github.sds100.keymapper.Activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.transaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 01/10/2018.
 */

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //show back button
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.transaction {
                add(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            /* when the back button in the toolbar is pressed, call onBackPressed so it acts like
            the hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

        private val mShowNotificationPreference by lazy {
            findPreference(getString(R.string.key_pref_show_notification)) as SwitchPreference
        }

        private val mShowNotificationOnBootPreference by lazy {
            findPreference(getString(R.string.key_pref_show_notification_on_boot))
                    as SwitchPreference
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)

            mShowNotificationPreference.onPreferenceChangeListener = this

            /*only allow the user to toggle whether the notification shows on boot if they want
            * to see the notification at all. */
            mShowNotificationOnBootPreference.isEnabled = mShowNotificationPreference.isChecked
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            when (preference) {
                mShowNotificationPreference -> {
                    /* if the user doesn't want to see the notification, don't allow them
                     * to toggle whether it is shown on boot on and off */
                    mShowNotificationOnBootPreference.isEnabled = newValue as Boolean
                }
            }

            return true
        }
    }
}