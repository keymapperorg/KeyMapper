package io.github.sds100.keymapper.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.*
import io.github.sds100.keymapper.databinding.FragmentChooseActionBinding
import io.github.sds100.keymapper.system.apps.*
import io.github.sds100.keymapper.system.display.PickDisplayCoordinateFragment
import io.github.sds100.keymapper.system.intents.ConfigIntentFragment
import io.github.sds100.keymapper.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.system.intents.ConfigIntentViewModel
import io.github.sds100.keymapper.system.keyevents.*
import io.github.sds100.keymapper.system.phone.ChoosePhoneNumberFragment
import io.github.sds100.keymapper.system.url.ChooseUrlFragment
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.TextBlockActionTypeFragment
import io.github.sds100.keymapper.util.ui.setCurrentDestinationLiveData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseActionFragment : Fragment() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
    }

    private val viewModel by activityViewModels<ChooseActionViewModel> { ChooseActionViewModel.Factory() }

    private val mArgs by navArgs<ChooseActionFragmentArgs>()

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentChooseActionBinding? = null
    val binding: FragmentChooseActionBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        createActionOnResult(ChooseAppFragment.REQUEST_KEY) {
            val packageName = it.getString(ChooseAppFragment.EXTRA_PACKAGE_NAME)
            OpenAppAction(packageName!!)
        }

        createActionOnResult(ChooseAppShortcutFragment.REQUEST_KEY) {
            val result =
                it.getJsonSerializable<ChooseAppShortcutResult>(ChooseAppShortcutFragment.EXTRA_RESULT)

            OpenAppShortcutAction(
                result!!.packageName,
                result.shortcutName,
                result.uri
            )
        }

        createActionOnResult(ChooseKeyFragment.REQUEST_KEY) {
            val keyCode = it.getInt(ChooseKeyFragment.EXTRA_KEYCODE)

            KeyEventAction(keyCode)
        }

        createActionOnResult(ConfigKeyEventFragment.REQUEST_KEY) { bundle ->
            val result =
                bundle.getJsonSerializable<ConfigKeyEventResult>(ConfigKeyEventFragment.EXTRA_RESULT)

            result!!

            val device = if (result.device!=null){
                KeyEventAction.Device(result.device.descriptor, result.device.name)
            }else{
                null
            }

            KeyEventAction(
                result.keyCode,
                result.metaState,
                result.useShell,
                device
            )
        }

        createActionOnResult(TextBlockActionTypeFragment.REQUEST_KEY) {
            val text = it.getString(TextBlockActionTypeFragment.EXTRA_TEXT_BLOCK)

            TextAction(text!!)
        }

        createActionOnResult(ChooseUrlFragment.REQUEST_KEY) {
            val url = it.getString(ChooseUrlFragment.EXTRA_URL)

            UrlAction(url!!)
        }

        createActionOnResult(SystemActionListFragment.REQUEST_KEY) {
            it.getJsonSerializable<SystemAction>(SystemActionListFragment.EXTRA_SYSTEM_ACTION)!!
        }

        createActionOnResult(KeyCodeListFragment.REQUEST_KEY) {
            val keyCode = it.getInt(KeyCodeListFragment.EXTRA_KEYCODE)

            KeyEventAction(keyCode)
        }

        createActionOnResult(PickDisplayCoordinateFragment.REQUEST_KEY) {
            val x = it.getInt(PickDisplayCoordinateFragment.EXTRA_X)
            val y = it.getInt(PickDisplayCoordinateFragment.EXTRA_Y)
            val description = it.getString(PickDisplayCoordinateFragment.EXTRA_DESCRIPTION)

            TapCoordinateAction(x, y, description)
        }

        createActionOnResult(ConfigIntentFragment.REQUEST_KEY) { bundle ->
            val json = bundle.getString(ConfigIntentFragment.EXTRA_RESULT)!!
            val result: ConfigIntentResult = Json.decodeFromString(json)

            IntentAction(result.description, result.target, result.uri)
        }

        createActionOnResult(ChoosePhoneNumberFragment.REQUEST_KEY) {
            val number = it.getString(ChoosePhoneNumberFragment.EXTRA_PHONE_NUMBER)

            PhoneCallAction(number!!)
        }

        setFragmentResultListener(KeyCodeListFragment.REQUEST_KEY) { _, result ->
            val keyEventViewModel by activityViewModels<ConfigKeyEventViewModel> {
                Inject.configKeyEventViewModel(requireContext())
            }

            result.getInt(KeyCodeListFragment.EXTRA_KEYCODE).let {
                keyEventViewModel.setKeyCode(it)
            }
        }

        setFragmentResultListener(ChooseActivityFragment.REQUEST_KEY) { _, result ->
            val viewModel by activityViewModels<ConfigIntentViewModel> {
                Inject.configIntentViewModel(requireContext())
            }

            result.getJsonSerializable<ActivityInfo>(ChooseActivityFragment.EXTRA_ACTIVITY_INFO).let {
                viewModel.setActivity(it ?: return@let)
            }
        }

        FragmentChooseActionBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val pagerAdapter = ChooseActionPagerAdapter(this@ChooseActionFragment)
            viewPager.adapter = pagerAdapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = str(pagerAdapter.tabFragmentCreators[position].tabTitle)
            }.attach()

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            viewModel.currentTabPosition.let {
                if (it > pagerAdapter.itemCount - 1) return@let

                viewPager.setCurrentItem(it, false)
            }

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    viewModel.currentTabPosition = position
                }
            })

            subscribeSearchView(pagerAdapter)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @Suppress("UNCHECKED_CAST")
    private fun createActionOnResult(
        requestKey: String,
        createAction: (bundle: Bundle) -> ActionData
    ) {
        childFragmentManager.setFragmentResultListener(
            requestKey,
            viewLifecycleOwner
        ) { _, result ->
            val action = createAction(result)

            setFragmentResult(
                this.mArgs.chooseActionRequestKey,
                Bundle().apply { putJsonSerializable(EXTRA_ACTION, action) }
            )
        }
    }

    private fun FragmentChooseActionBinding.subscribeSearchView(
        pagerAdapter: ChooseActionPagerAdapter
    ) {
        val searchViewMenuItem = appBar.menu.findItem(R.id.action_search)
        val searchView = searchViewMenuItem.actionView as SearchView

        searchViewMenuItem.isVisible =
            pagerAdapter.tabFragmentCreators[viewPager.currentItem].searchStateKey != null

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                searchViewMenuItem.isVisible =
                    pagerAdapter.tabFragmentCreators[position].searchStateKey != null
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

                pagerAdapter.tabFragmentCreators[viewPager.currentItem].searchStateKey?.let {
                    findNavController().setCurrentDestinationLiveData(it, newText)
                }

                return false
            }

            override fun onQueryTextSubmit(query: String?) = onQueryTextChange(query)
        })
    }
}