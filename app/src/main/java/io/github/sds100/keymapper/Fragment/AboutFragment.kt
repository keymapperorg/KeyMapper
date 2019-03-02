package io.github.sds100.keymapper.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R


/**
 * Created by sds100 on 10/12/2018.
 */

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        findPreference(getString(R.string.key_pref_version)).summary = Constants.VERSION

        findPreference(getString(R.string.key_pref_developer_email)).setOnPreferenceClickListener {

            val emailIntent = Intent(Intent.ACTION_SENDTO)

            emailIntent.data = Uri.parse("mailto:${getString(R.string.developer_email)}" +
                    "?subject=${getString(R.string.email_subject)}")

            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

            startActivity(emailIntent)

            true
        }
    }
}