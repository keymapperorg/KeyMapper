package io.github.sds100.keymapper.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.collectLatest

class MenuFragment : BottomSheetDialogFragment() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    private val viewModel: HomeMenuViewModel
        get() = homeViewModel.menuViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentMenuBinding? = null
    val binding: FragmentMenuBinding
        get() = _binding!!

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

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
            viewModel.openSettings.collectLatest {
                findNavController().navigate(NavAppDirections.actionGlobalSettingsFragment())
            }
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
            viewModel.openAbout.collectLatest {
                findNavController().navigate(NavAppDirections.actionGlobalAboutFragment())
            }
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
            viewModel.emailDeveloper.collectLatest {
                FeedbackUtils.emailDeveloper(requireContext())
            }
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
            viewModel.openUrl.collectLatest {
                UrlUtils.openUrl(requireContext(), it)
            }
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
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