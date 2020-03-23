package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.ui.adapter.ChooseActionPagerAdapter
import splitties.resources.strArray

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseActionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentChooseActionBinding.inflate(inflater, container, false).apply {
            viewPager.adapter = ChooseActionPagerAdapter(this@ChooseActionFragment)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.choose_action_tab_titles)[position]
            }.attach()

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            return this.root
        }
    }
}