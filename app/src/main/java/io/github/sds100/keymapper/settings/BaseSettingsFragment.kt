package io.github.sds100.keymapper.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.showPopups

/**
 * Created by sds100 on 20/07/2021.
 */
abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    val viewModel by activityViewModels<SettingsViewModel> {
        Inject.settingsViewModel(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onBackPressed()
        }

        view.findViewById<BottomAppBar>(R.id.appBar).apply {
            replaceMenu(R.menu.menu_settings)

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

        viewModel.showPopups(this, view)
    }

    private fun onBackPressed() {
        findNavController().navigateUp()
    }
}