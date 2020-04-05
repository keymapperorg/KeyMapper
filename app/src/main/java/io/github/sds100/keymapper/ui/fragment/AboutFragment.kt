package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentAboutBinding
import io.github.sds100.keymapper.util.FeedbackUtils

/**
 * Created by sds100 on 05/04/2020.
 */

class AboutFragment : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentAboutBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            return this.root
        }
    }
}

class AboutPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        findPreference<Preference>(getString(R.string.key_pref_version))?.summary = Constants.VERSION

        findPreference<Preference>(getString(R.string.key_pref_developer_email))?.setOnPreferenceClickListener {
            FeedbackUtils.sendFeedback()
            true
        }
    }
}