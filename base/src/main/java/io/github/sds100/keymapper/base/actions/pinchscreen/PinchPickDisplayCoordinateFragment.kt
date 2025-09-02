package io.github.sds100.keymapper.base.actions.pinchscreen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.databinding.FragmentPinchPickCoordinatesBinding
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.str
import io.github.sds100.keymapper.system.files.FileUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class PinchPickDisplayCoordinateFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    private val args: PinchPickDisplayCoordinateFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }
    private var pinchTypesDisplayValues = mutableListOf<String>()

    private val viewModel: PinchPickDisplayCoordinateViewModel by viewModels()

    private val screenshotLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult

            val bitmap: Bitmap? =
                FileUtils.decodeBitmapFromUri(requireContext().contentResolver, uri)

            bitmap ?: return@registerForActivityResult

            val displaySize = Point().apply {
                @Suppress("DEPRECATION")
                ContextCompat.getDisplayOrDefault(requireContext()).getRealSize(this)
            }

            viewModel.selectedScreenshot(bitmap, displaySize)
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentPinchPickCoordinatesBinding? = null
    val binding: FragmentPinchPickCoordinatesBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pinchTypesDisplayValues = arrayOf(
            str(R.string.hint_coordinate_type_pinch_in),
            str(R.string.hint_coordinate_type_pinch_out),
        ).toMutableList()

        args.result?.let {
            viewModel.loadResult(Json.decodeFromString(it))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentPinchPickCoordinatesBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
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

        binding.viewModel = viewModel
        binding.pinchTypeSpinnerAdapter =
            ArrayAdapter(
                this.requireActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                pinchTypesDisplayValues,
            )

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
                        point.y.toFloat() / binding.imageViewScreenshot.height,
                    )
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest { result ->
                setFragmentResult(
                    requestKey,
                    bundleOf(EXTRA_RESULT to Json.encodeToString(result)),
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
