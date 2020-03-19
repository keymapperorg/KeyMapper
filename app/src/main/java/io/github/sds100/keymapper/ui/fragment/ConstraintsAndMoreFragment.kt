package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConstraintsAndMoreBinding
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 19/03/2020.
 */
class ConstraintsAndMoreFragment : Fragment() {
    private val mConfigKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_app)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentConstraintsAndMoreBinding>(
            inflater,
            R.layout.fragment_constraints_and_more,
            container,
            false
        )

        binding.apply {
            viewModel = mConfigKeymapViewModel
        }

        return binding.root
    }
}