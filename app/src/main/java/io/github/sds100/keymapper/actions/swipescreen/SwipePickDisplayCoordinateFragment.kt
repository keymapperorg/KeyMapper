package io.github.sds100.keymapper.actions.swipescreen

import android.annotation.SuppressLint
import android.graphics.ImageDecoder
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.graphics.decodeBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.databinding.FragmentSwipePickCoordinatesBinding
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SwipePickDisplayCoordinateFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    private val args: SwipePickDisplayCoordinateFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }

    private val viewModel: SwipePickDisplayCoordinateViewModel by viewModels {
        Inject.swipeCoordinateActionTypeViewModel(requireContext())
    }

    private val screenshotLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(requireContext().contentResolver, it)
                    .decodeBitmap { _, _ -> }
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }

            val displaySize = Point().apply {
                val windowManager: WindowManager = requireContext().getSystemService()!!
                windowManager.defaultDisplay.getRealSize(this)
            }

            viewModel.selectedScreenshot(bitmap, displaySize)
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentSwipePickCoordinatesBinding? = null
    val binding: FragmentSwipePickCoordinatesBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args.result?.let {
            viewModel.loadResult(Json.decodeFromString(it))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentSwipePickCoordinatesBinding.inflate(inflater, container, false).apply {
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

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.bitmap.collectLatest { bitmap ->
                if (bitmap == null) {
                    binding.imageViewScreenshot.setImageDrawable(null)
                } else {
                    binding.imageViewScreenshot.setImageBitmap(bitmap)
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.imageViewScreenshot.pointCoordinates.collectLatest { point ->
                if (point != null) {
                    viewModel.onScreenshotTouch(
                        point.x.toFloat() / binding.imageViewScreenshot.width,
                        point.y.toFloat() / binding.imageViewScreenshot.height
                    )
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

        binding.setOnSelectScreenshotClick {
            screenshotLauncher.launch(FileUtils.MIME_TYPE_IMAGES)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}