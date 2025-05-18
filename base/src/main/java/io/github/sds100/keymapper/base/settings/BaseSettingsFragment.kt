package io.github.sds100.keymapper.base.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.base.utils.Inject
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.base.utils.ui.showPopups

abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    val viewModel by activityViewModels<SettingsViewModel> {
        Inject.settingsViewModel(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

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
