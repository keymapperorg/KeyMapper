package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding

/**
 * A placeholder fragment containing a simple view.
 */
class KeyMapListFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentKeymapListBinding.inflate(inflater, container, false)

        return binding.root
    }
}