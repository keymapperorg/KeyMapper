package io.github.sds100.keymapper.actions.swipegesture

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.actions.selectscreenshot.SelectScreenshotViewModel
import io.github.sds100.keymapper.databinding.FragmentPickSwipegestureBinding
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 30/03/2020.
 */

class PickSwipeFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    private val args: PickSwipeFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }

    private val viewModel: PickSwipeViewModel by viewModels {
        Inject.pickSwipeViewModel(requireContext())
    }

    private lateinit var screenshotViewModel: SelectScreenshotViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentPickSwipegestureBinding? = null
    val binding: FragmentPickSwipegestureBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        screenshotViewModel = ViewModelProvider(this,
            Inject.selectScreenshotViewModel(requireContext()))
            .get(SelectScreenshotViewModel::class.java)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        FragmentPickSwipegestureBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        viewModel.showPopups(this, binding)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.imageViewScreenshot.setImageDrawable(screenshotViewModel.blankScreen)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            screenshotViewModel.bitmap.collectLatest { bitmap ->
                if (bitmap != null) {
                    binding.imageViewScreenshot.setImageBitmap(bitmap)
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.imageViewScreenshot.finishedPath.collectLatest { path ->
                if (path != null) {
                    viewModel.updatePath(path, screenshotViewModel.displaySize,
                        binding.imageViewScreenshot.width, binding.imageViewScreenshot.height)
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest { result ->
                setFragmentResult(
                    requestKey,
                    bundleOf(EXTRA_RESULT to Json.encodeToString(result))
                )

                findNavController().navigateUp()
            }
        }

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}