package io.github.sds100.keymapper.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomappbar.BottomAppBar
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.showPopups

/**
 * Created by sds100 on 20/07/2021.
 */
@AndroidEntryPoint
abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    val viewModel: SettingsViewModel by viewModels()

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