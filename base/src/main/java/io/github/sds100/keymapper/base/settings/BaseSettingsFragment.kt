package io.github.sds100.keymapper.base.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.bottomappbar.BottomAppBar
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.system.url.UrlUtils

@AndroidEntryPoint
abstract class BaseSettingsFragment : PreferenceFragmentCompat() {

    protected val viewModel: SettingsViewModel by viewModels()

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
    }

    private fun onBackPressed() {
        findNavController().navigateUp()
    }
}
