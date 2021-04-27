package io.github.sds100.keymapper.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentAboutBinding
import io.github.sds100.keymapper.util.FeedbackUtils
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 05/04/2020.
 */

class AboutFragment : BottomSheetDialogFragment() {

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentAboutBinding? = null
    val binding: FragmentAboutBinding
        get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentAboutBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

class AboutPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        findPreference<Preference>(str(R.string.key_pref_version))?.summary = Constants.VERSION

        findPreference<Preference>(str(R.string.key_pref_changelog))?.setOnPreferenceClickListener {
            val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                R.string.pref_title_changelog,
                R.string.url_changelog
            )
            findNavController().navigate(direction)

            true
        }

        findPreference<Preference>(str(R.string.key_pref_license))?.setOnPreferenceClickListener {
            val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                R.string.pref_title_license,
                R.string.url_license
            )
            findNavController().navigate(direction)

            true
        }

        findPreference<Preference>(str(R.string.key_pref_privacy_policy))?.setOnPreferenceClickListener {
            val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                R.string.pref_title_privacy_policy,
                R.string.url_privacy_policy
            )
            findNavController().navigate(direction)

            true
        }

        findPreference<Preference>(str(R.string.key_pref_credits))?.setOnPreferenceClickListener {
            val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                R.string.pref_title_credits,
                R.string.url_credits
            )
            findNavController().navigate(direction)

            true
        }

        findPreference<Preference>(str(R.string.key_pref_credits))?.setOnPreferenceClickListener {
            val direction = NavAppDirections.actionGlobalOnlineFileFragment(
                R.string.pref_title_credits,
                R.string.url_credits
            )
            findNavController().navigate(direction)

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

        findPreference<Preference>(str(R.string.key_pref_developer_email))?.setOnPreferenceClickListener {
            FeedbackUtils.emailDeveloper(requireContext())
            true
        }
    }
}