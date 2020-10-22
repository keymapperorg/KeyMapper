package io.github.sds100.keymapper.ui.fragment

import android.annotation.SuppressLint
import android.graphics.ImageDecoder
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.decodeBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.TapCoordinateActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentTapCoordinateActionTypeBinding
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch
import splitties.systemservices.windowManager
import splitties.toast.toast

/**
 * Created by sds100 on 30/03/2020.
 */

class TapCoordinateActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_coordinate"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
        const val EXTRA_DESCRIPTION = "extra_description"
    }

    private val mViewModel: TapCoordinateActionTypeViewModel by activityViewModels {
        InjectorUtils.provideTapCoordinateActionTypeViewModel()
    }

    private val mScreenshotLauncher by lazy {
        requireActivity().registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(requireContext().contentResolver, it).decodeBitmap { _, _ -> }
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }

            val displaySize = Point().apply {
                windowManager.defaultDisplay.getRealSize(this)
            }

            mViewModel.selectedScreenshot(bitmap, displaySize)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        FragmentTapCoordinateActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            mViewModel.bitmap.observe(viewLifecycleOwner, {
                imageViewScreenshot.setImageBitmap(it)
            })

            mViewModel.selectScreenshotEvent.observe(viewLifecycleOwner, EventObserver {
                mScreenshotLauncher.launch(FileUtils.MIME_TYPE_IMAGES)
            })

            mViewModel.incorrectScreenshotResolutionEvent.observe(viewLifecycleOwner, EventObserver {
                toast(R.string.toast_incorrect_screenshot_resolution)
            })

            imageViewScreenshot.pointCoordinates.observe(viewLifecycleOwner, {
                mViewModel.onScreenshotTouch(
                    it.x.toFloat() / imageViewScreenshot.width,
                    it.y.toFloat() / imageViewScreenshot.height
                )
            })

            setOnDoneClick {
                lifecycleScope.launch {
                    val description = requireActivity().editTextStringAlertDialog(
                        str(R.string.hint_tap_coordinate_title),
                        allowEmpty = true
                    )

                    setFragmentResult(REQUEST_KEY,
                        bundleOf(
                            EXTRA_X to mViewModel.x.value,
                            EXTRA_Y to mViewModel.y.value,
                            EXTRA_DESCRIPTION to description
                        ))

                    findNavController().navigateUp()
                }
            }

            return this.root
        }
    }
}