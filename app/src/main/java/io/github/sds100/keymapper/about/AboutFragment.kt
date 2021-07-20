package io.github.sds100.keymapper.about

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 05/04/2020.
 */

class AboutFragment : PreferenceFragmentCompat() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onBackPressed()
        }

        view.findViewById<BottomAppBar>(R.id.appBar).apply {
            setNavigationOnClickListener {
                onBackPressed()
            }

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_help -> {
                        UrlUtils.openUrl(requireContext(), str(R.string.url_settings_guide))
                        true
                    }

                    else -> false
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        findPreference<Preference>(str(R.string.key_pref_version))?.summary = Constants.VERSION

        findPreference<Preference>(str(R.string.key_pref_changelog))?.setOnPreferenceClickListener {

            UrlUtils.launchCustomTab(requireContext(), str(R.string.url_changelog))

            true
        }

        findPreference<Preference>(str(R.string.key_pref_license))?.setOnPreferenceClickListener {
            UrlUtils.launchCustomTab(requireContext(), str(R.string.url_license))

            true
        }

        findPreference<Preference>(str(R.string.key_pref_privacy_policy))?.setOnPreferenceClickListener {
            UrlUtils.launchCustomTab(requireContext(), str(R.string.url_privacy_policy))

            true
        }

        findPreference<Preference>(str(R.string.key_pref_credits))?.setOnPreferenceClickListener {
            UrlUtils.launchCustomTab(requireContext(), str(R.string.url_credits))

            true
        }

        findPreference<Preference>(str(R.string.key_pref_xda_thread))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_xda_thread))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_discord))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_discord_server_invite))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_rate_review))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_play_store_listing))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_developer_github))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_developer_github))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_source_code))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_source_code))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_translate))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_translate))
            true
        }

        findPreference<Preference>(str(R.string.key_pref_youtube_channel))?.setOnPreferenceClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_youtube_channel))
            true
        }
    }

    private fun onBackPressed() {
        findNavController().navigateUp()
    }
}