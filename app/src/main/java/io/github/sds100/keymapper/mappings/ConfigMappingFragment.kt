package io.github.sds100.keymapper.mappings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentConfigMappingBinding
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.GenericFragmentPagerAdapter
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.viewLifecycleScope
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.alertdialog.material.materialAlertDialog

/**
 * Created by sds100 on 17/01/21.
 */
abstract class ConfigMappingFragment : Fragment() {

    abstract val viewModel: ConfigMappingViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentConfigMappingBinding? = null
    val binding: FragmentConfigMappingBinding
        get() = _binding!!

    private var onBackPressedDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.configActionsViewModel.setupNavigation(this)
        viewModel.editActionViewModel.setupNavigation(this)
        viewModel.configConstraintsViewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        FragmentConfigMappingBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@ConfigMappingFragment.viewModel
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.editActionViewModel.showPopups(this, binding)

        val fragmentInfoList = getFragmentInfoList()

        binding.viewPager.adapter = GenericFragmentPagerAdapter(
            this,
            fragmentInfoList.map { it.first.toLong() to it.second.instantiate }
        )

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val tabTitleRes = fragmentInfoList[position].second.header

            tab.text = tabTitleRes?.let { str(it) }
        }.attach()

        viewModel.configActionsViewModel.showPopups(this, binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.invalidateHelpMenuItemVisibility(
                fragmentInfoList,
                binding.viewPager.currentItem
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

        //prevents leaking window if configuration change when the dialog is showing
        onBackPressedDialog?.dismiss()
        onBackPressedDialog = null
        super.onDestroyView()
    }

    private fun FragmentConfigMappingBinding.invalidateHelpMenuItemVisibility(
        fragmentInfoList: List<Pair<Int, FragmentInfo>>,
        position: Int
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
                titleResource = R.string.dialog_title_discard_changes
                messageResource = R.string.dialog_message_discard_changes

                positiveButton(R.string.pos_confirm) {
                    findNavController().navigateUp()
                }

                negativeButton(R.string.neg_cancel) { it.cancel() }
            }

            onBackPressedDialog?.show()
        }
    }

    abstract fun getFragmentInfoList(): List<Pair<Int, FragmentInfo>>
}