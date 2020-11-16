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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.viewmodel.KeyEventActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.ui.adapter.ChooseActionPagerAdapter
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.setCurrentDestinationLiveData
import io.github.sds100.keymapper.util.strArray

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseActionFragment : Fragment() {

    companion object {
        const val REQUEST_KEY = "request_choose_action"
        const val EXTRA_ACTION = "extra_action"
    }

    private lateinit var mPagerAdapter: ChooseActionPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResultListener(AppListFragment.REQUEST_KEY) {
            val packageName = it.getString(AppListFragment.EXTRA_PACKAGE_NAME)
            Action.appAction(packageName!!)
        }

        setResultListener(AppShortcutListFragment.REQUEST_KEY) {
            val name = it.getString(AppShortcutListFragment.EXTRA_NAME)
            val packageName = it.getString(AppShortcutListFragment.EXTRA_PACKAGE_NAME)
            val uri = it.getString(AppShortcutListFragment.EXTRA_URI)

            Action.appShortcutAction(name!!, packageName, uri!!)
        }

        setResultListener(KeyActionTypeFragment.REQUEST_KEY) {
            val keyCode = it.getInt(KeyActionTypeFragment.EXTRA_KEYCODE)

            Action.keyAction(keyCode)
        }

        setResultListener(KeyEventActionTypeFragment.REQUEST_KEY) {
            val keyCode = it.getInt(KeyEventActionTypeFragment.EXTRA_KEYCODE)
            val metaState = it.getInt(KeyEventActionTypeFragment.EXTRA_META_STATE)
            val deviceDescriptor = it.getString(KeyEventActionTypeFragment.EXTRA_DEVICE_DESCRIPTOR)

            Action.keyEventAction(keyCode, metaState, deviceDescriptor)
        }

        setResultListener(TextBlockActionTypeFragment.REQUEST_KEY) {
            val text = it.getString(TextBlockActionTypeFragment.EXTRA_TEXT_BLOCK)

            Action.textBlockAction(text!!)
        }

        setResultListener(UrlActionTypeFragment.REQUEST_KEY) {
            val url = it.getString(UrlActionTypeFragment.EXTRA_URL)

            Action.urlAction(url!!)
        }

        setResultListener(SystemActionListFragment.REQUEST_KEY) {
            val id = it.getString(SystemActionListFragment.EXTRA_SYSTEM_ACTION_ID)
            val optionData = it.getString(SystemActionListFragment.EXTRA_SYSTEM_ACTION_OPTION_DATA)

            Action.systemAction(requireContext(), id!!, optionData)
        }

        setResultListener(KeycodeListFragment.REQUEST_KEY) {
            val keyCode = it.getInt(KeycodeListFragment.EXTRA_KEYCODE)

            Action.keyCodeAction(keyCode)
        }

        setResultListener(TapCoordinateActionTypeFragment.REQUEST_KEY) {
            val x = it.getInt(TapCoordinateActionTypeFragment.EXTRA_X)
            val y = it.getInt(TapCoordinateActionTypeFragment.EXTRA_Y)
            val description = it.getString(TapCoordinateActionTypeFragment.EXTRA_DESCRIPTION)

            Action.tapCoordinateAction(x, y, description)
        }

        setFragmentResultListener(KeycodeListFragment.REQUEST_KEY) { _, result ->
            val keyEventViewModel by activityViewModels<KeyEventActionTypeViewModel> {
                InjectorUtils.provideKeyEventActionTypeViewModel(requireContext())
            }

            result.getInt(KeycodeListFragment.EXTRA_KEYCODE).let {
                keyEventViewModel.keyCode.value = it.toString()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentChooseActionBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            mPagerAdapter = ChooseActionPagerAdapter(this@ChooseActionFragment)
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
    private fun setResultListener(
        requestKey: String,
        createAction: (bundle: Bundle) -> Action
    ) {
        childFragmentManager.setFragmentResultListener(requestKey, this) { _, result ->
            val action = createAction(result)

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