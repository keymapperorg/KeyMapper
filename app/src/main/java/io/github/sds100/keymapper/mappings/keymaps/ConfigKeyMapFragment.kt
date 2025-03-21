package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ConfigActionsFragment
import io.github.sds100.keymapper.constraints.ChooseConstraintFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.databinding.FragmentConfigKeyMapBinding
import io.github.sds100.keymapper.mappings.keymaps.trigger.ConfigTriggerOptionsFragment
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerFragment
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.GenericFragmentPagerAdapter
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.intArray
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.FourFragments
import io.github.sds100.keymapper.util.ui.TwoFragments
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.viewLifecycleScope
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.alertdialog.material.materialAlertDialog

class ConfigKeyMapFragment : Fragment() {

    private val args by navArgs<ConfigKeyMapFragmentArgs>()

    private val viewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        Inject.configKeyMapViewModel(requireContext())
    }

    private var _binding: FragmentConfigKeyMapBinding? = null
    val binding: FragmentConfigKeyMapBinding
        get() = _binding!!

    private var onBackPressedDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // only load the keymap if opening this fragment for the first time
        if (savedInstanceState == null) {
            args.keymapUid.let {
                if (it == null) {
                    viewModel.loadNewKeymap(args.newFloatingButtonTriggerKey)
                } else {
                    viewModel.loadKeyMap(it)
                }
            }

            if (args.showAdvancedTriggers) {
                viewModel.configTriggerViewModel.showAdvancedTriggersBottomSheet = true
            }
        }

        setFragmentResultListener(ConfigConstraintsFragment.CHOOSE_CONSTRAINT_REQUEST_KEY) { _, result ->
            result.getJsonSerializable<Constraint>(ChooseConstraintFragment.EXTRA_CONSTRAINT)?.let {
                viewModel.configConstraintsViewModel.onChosenNewConstraint(it)
            }
        }

        viewModel.configTriggerViewModel.setupNavigation(this)
        viewModel.configActionsViewModel.setupNavigation(this)
        viewModel.configConstraintsViewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentConfigKeyMapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@ConfigKeyMapFragment.viewModel
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val fragmentInfoList = getFragmentInfoList()

        binding.viewPager.adapter = GenericFragmentPagerAdapter(
            this,
            fragmentInfoList.map { it.first.toLong() to it.second.instantiate },
        )

        // Don't show the swipe animations for reaching the end of the pager
        // if there is only one page.
        binding.viewPager.isUserInputEnabled = fragmentInfoList.size > 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val tabTitleRes = fragmentInfoList[position].second.header

            tab.text = tabTitleRes?.let { str(it) }
        }.attach()

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.invalidateHelpMenuItemVisibility(
                fragmentInfoList,
                binding.viewPager.currentItem,
            )
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                binding.invalidateHelpMenuItemVisibility(fragmentInfoList, position)
            }
        })

        binding.tabLayout.isVisible = binding.tabLayout.tabCount > 1

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showOnBackPressedWarning()
        }

        binding.appBar.setNavigationOnClickListener {
            showOnBackPressedWarning()
        }

        binding.appBar.menu.findItem(R.id.action_help).isVisible =
            fragmentInfoList[binding.viewPager.currentItem].second.supportUrl != null

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_save -> {
                    viewModel.save()
                    findNavController().navigateUp()

                    true
                }

                R.id.action_help -> {
                    fragmentInfoList[binding.viewPager.currentItem].second.supportUrl?.let { url ->
                        UrlUtils.openUrl(requireContext(), str(url))
                    }

                    true
                }

                else -> false
            }
        }

        viewModel.configActionsViewModel.showPopups(this, binding)
        viewModel.configTriggerViewModel.showPopups(this, binding)
        viewModel.configTriggerViewModel.optionsViewModel.showPopups(this, binding)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        viewModel.restoreState(savedInstanceState)
    }

    override fun onDestroyView() {
        _binding = null

        // prevents leaking window if configuration change when the dialog is showing
        onBackPressedDialog?.dismiss()
        onBackPressedDialog = null
        super.onDestroyView()
    }

    private fun getFragmentInfoList() = intArray(R.array.config_keymap_fragments).map {
        when (it) {
            int(R.integer.fragment_id_trigger) -> it to TriggerFragment.Info()
            int(R.integer.fragment_id_trigger_options) -> it to ConfigTriggerOptionsFragment.Info()
            int(R.integer.fragment_id_constraint_list) -> it to ConfigKeyMapConstraintsFragment.Info()
            int(R.integer.fragment_id_action_list) -> it to ConfigActionsFragment.Info()

            int(R.integer.fragment_id_constraints_and_options) ->
                it to FragmentInfo(R.string.tab_constraints_and_more) {
                    ConstraintsAndOptionsFragment()
                }

            int(R.integer.fragment_id_trigger_and_action_list) ->
                it to FragmentInfo(R.string.tab_trigger_and_actions) { TriggerAndActionsFragment() }

            int(R.integer.fragment_id_config_keymap_all) ->
                it to FragmentInfo { AllFragments() }

            else -> throw Exception("Don't know how to create FragmentInfo for this fragment $it")
        }
    }

    private fun FragmentConfigKeyMapBinding.invalidateHelpMenuItemVisibility(
        fragmentInfoList: List<Pair<Int, FragmentInfo>>,
        position: Int,
    ) {
        val visible = fragmentInfoList[position].second.supportUrl != null

        appBar.menu.findItem(R.id.action_help).apply {
            isEnabled = visible
            isVisible = visible
        }
    }

    private fun showOnBackPressedWarning() {
        viewLifecycleScope.launchWhenResumed {
            onBackPressedDialog = requireContext().materialAlertDialog {
                titleResource = R.string.dialog_title_unsaved_changes
                messageResource = R.string.dialog_message_unsaved_changes

                positiveButton(R.string.pos_discard_changes) {
                    findNavController().navigateUp()
                }

                negativeButton(R.string.neg_keep_editing) { it.cancel() }
            }

            onBackPressedDialog?.show()
        }
    }

    class TriggerAndActionsFragment :
        TwoFragments(
            TriggerFragment.Info(),
            ConfigActionsFragment.Info(),
        )

    class ConstraintsAndOptionsFragment :
        TwoFragments(
            ConfigTriggerOptionsFragment.Info(),
            ConfigKeyMapConstraintsFragment.Info(),
        )

    class AllFragments :
        FourFragments(
            TriggerFragment.Info(),
            ConfigTriggerOptionsFragment.Info(),
            ConfigActionsFragment.Info(),
            ConfigKeyMapConstraintsFragment.Info(),
        )
}
