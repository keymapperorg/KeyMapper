package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.model.AppShortcutModel
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.ui.adapter.ChooseActionPagerAdapter
import io.github.sds100.keymapper.util.observeLiveData
import io.github.sds100.keymapper.util.setCurrentDestinationLiveData
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

            onModelSelected<AppListItemModel>(AppListFragment.SAVED_STATE_KEY) {
                Action.appAction(it.packageName)
            }

            onModelSelected<AppShortcutModel>(AppShortcutListFragment.SAVED_STATE_KEY) {
                Action.appShortcutAction(it)
            }

            onModelSelected<Int>(KeyActionTypeFragment.SAVED_STATE_KEY){
                Action.keyAction(it)
            }

            subscribeSearchView()

            return this.root
        }
    }

    private fun <T> onModelSelected(key: String, createAction: (model: T) -> Action) = findNavController().apply {

        currentBackStackEntry?.observeLiveData<T>(viewLifecycleOwner, key) {
            val action = createAction(it)

            previousBackStackEntry?.setLiveData(SAVED_STATE_KEY, action)
            navigateUp()
        }
    }

    private fun FragmentChooseActionBinding.subscribeSearchView() {
        val searchViewMenuItem = appBar.menu.findItem(R.id.action_search)
        val searchView = searchViewMenuItem.actionView as SearchView

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                searchViewMenuItem.isVisible = mPagerAdapter.getSearchStateKey(position) != null
            }
        })

        searchViewMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                //don't allow the user to change the tab when searching
                viewPager.isUserInputEnabled = false
                tabLayout.isVisible = false

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                viewPager.isUserInputEnabled = true
                tabLayout.isVisible = true

                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?): Boolean {

                mPagerAdapter.getSearchStateKey(viewPager.currentItem)?.let { searchStateKey ->
                    findNavController().setCurrentDestinationLiveData(searchStateKey, newText)
                }

                return false
            }

            override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
        })
    }
}