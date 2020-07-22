package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.model.AppShortcutModel
import io.github.sds100.keymapper.data.model.SelectedSystemActionModel
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.ui.adapter.ChooseActionPagerAdapter
import io.github.sds100.keymapper.util.setCurrentDestinationLiveData
import io.github.sds100.keymapper.util.strArray
import java.io.Serializable

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseActionFragment : Fragment() {

    companion object {
        const val REQUEST_KEY = "request_choose_action"
        const val EXTRA_ACTION = "extra_action"
    }

    private val mPagerAdapter: ChooseActionPagerAdapter by lazy { ChooseActionPagerAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResultListener<AppListItemModel>(AppListFragment.REQUEST_KEY, AppListFragment.EXTRA_APP_MODEL) {
            Action.appAction(it.packageName)
        }

        setResultListener<AppShortcutModel>(AppShortcutListFragment.REQUEST_KEY,
            AppShortcutListFragment.EXTRA_APP_SHORTCUT) {
            Action.appShortcutAction(it)
        }

        setResultListener<Int>(KeyActionTypeFragment.REQUEST_KEY, KeyActionTypeFragment.EXTRA_KEYCODE) {
            Action.keyAction(it)
        }

        setResultListener<Int>(KeycodeListFragment.REQUEST_KEY, KeycodeListFragment.EXTRA_KEYCODE) {
            Action.keycodeAction(it)
        }

        setResultListener<String>(TextBlockActionTypeFragment.REQUEST_KEY,
            TextBlockActionTypeFragment.EXTRA_TEXT_BLOCk) {
            Action.textBlockAction(it)
        }

        setResultListener<String>(UrlActionTypeFragment.REQUEST_KEY, UrlActionTypeFragment.EXTRA_URL) {
            Action.urlAction(it)
        }

        setResultListener<SelectedSystemActionModel>(
            SystemActionListFragment.REQUEST_KEY,
            SystemActionListFragment.EXTRA_SYSTEM_ACTION
        ) {
            Action.systemAction(requireContext(), it)
        }
    }

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

            subscribeSearchView()

            return this.root
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Serializable> setResultListener(
        requestKey: String,
        extraKey: String,
        createAction: (model: T) -> Action
    ) {
        childFragmentManager.setFragmentResultListener(requestKey, this) { _, result ->
            val model = result.get(extraKey) as T

            val action = createAction(model)

            setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_ACTION to action))
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