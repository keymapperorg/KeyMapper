package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding
import splitties.experimental.ExperimentalSplittiesApi
import splitties.snackbar.snackForever

/**
 * A placeholder fragment containing a simple view.
 */
@ExperimentalSplittiesApi
class KeymapListFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentKeymapListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.setOnNewKeymapClick {
            val direction = KeymapListFragmentDirections.actionHomeToNewKeymap()
            findNavController().navigate(direction)
        }

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_toggle_dark_theme -> {

                    lifecycleScope.launchWhenCreated {
                        AppPreferences().toggleDarkThemeMode()
                    }

                    true
                }

                else -> false
            }
        }

        return binding.root
    }
}