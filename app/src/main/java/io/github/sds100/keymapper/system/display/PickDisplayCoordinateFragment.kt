package io.github.sds100.keymapper.system.display

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.graphics.decodeBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.actions.PickDisplayCoordinateViewModel
import io.github.sds100.keymapper.databinding.FragmentPickCoordinateBinding
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 30/03/2020.
 */

class PickDisplayCoordinateFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_coordinate"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    private val viewModel: PickDisplayCoordinateViewModel by activityViewModels {
        Inject.tapCoordinateActionTypeViewModel(requireContext())
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

            /**
             * Do this on resume so the snack bar shared flow has a chance to be collected.
             */
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                viewModel.selectedScreenshot(bitmap, displaySize)
            }
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentPickCoordinateBinding? = null
    val binding: FragmentPickCoordinateBinding
        get() = _binding!!


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        FragmentPickCoordinateBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.bitmap.collectLatest {
                binding.imageViewScreenshot.setImageBitmap(it)
            }
        }

        binding.imageViewScreenshot.pointCoordinates.observe(viewLifecycleOwner, {
            viewModel.onScreenshotTouch(
                it.x.toFloat() / binding.imageViewScreenshot.width,
                it.y.toFloat() / binding.imageViewScreenshot.height
            )
        })

        viewModel.showPopups(this, binding)

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        EXTRA_X to it.x,
                        EXTRA_Y to it.y,
                        EXTRA_DESCRIPTION to it.description
                    )
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