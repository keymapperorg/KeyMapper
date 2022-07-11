package io.github.sds100.keymapper.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.util.color
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MenuFragment : BottomSheetDialogFragment() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val viewModel: HomeMenuViewModel
        get() = homeViewModel.menuViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentMenuBinding? = null
    val binding: FragmentMenuBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentMenuBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.viewModel = viewModel

        viewModel.showPopups(this, binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.toggleMappingsButtonState.collectLatest { state ->
                val text: String
                val color: Int

                when (state) {
                    ToggleMappingsButtonState.PAUSED -> {
                        text = str(R.string.action_tap_to_resume_keymaps)
                        color = color(R.color.green, harmonize = true)
                    }
                    ToggleMappingsButtonState.RESUMED -> {
                        text = str(R.string.action_tap_to_pause_keymaps)
                        color = color(R.color.red, harmonize = true)
                    }
                    ToggleMappingsButtonState.SERVICE_DISABLED -> {
                        text = str(R.string.button_enable_accessibility_service)
                        color = color(R.color.red, harmonize = true)
                    }
                    ToggleMappingsButtonState.SERVICE_CRASHED -> {
                        text = str(R.string.button_restart_accessibility_service)
                        color = color(R.color.red, harmonize = true)
                    }
                    else -> return@collectLatest
                }

                binding.buttonToggleKeymaps.text = text
                binding.buttonToggleKeymaps.setBackgroundColor(color)

            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.dismiss.collectLatest {
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}