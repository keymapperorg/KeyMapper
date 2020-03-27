package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.ui.adapter.ChooseActionPagerAdapter
import io.github.sds100.keymapper.util.observeLiveData
import io.github.sds100.keymapper.util.setLiveData
import splitties.resources.strArray

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseActionFragment : Fragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_choose_action"
    }

    private val mPagerAdapter: ChooseActionPagerAdapter by lazy { ChooseActionPagerAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentChooseActionBinding.inflate(inflater, container, false).apply {
            viewPager.adapter = mPagerAdapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.choose_action_tab_titles)[position]
            }.attach()

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            findNavController().apply {
                currentBackStackEntry?.observeLiveData<AppListItemModel>(
                    viewLifecycleOwner,
                    AppListFragment.SAVED_STATE_KEY
                ) {
                    previousBackStackEntry?.setLiveData(SAVED_STATE_KEY, Action.appAction(it.packageName))
                    navigateUp()
                }
            }

            return this.root
        }
    }
}