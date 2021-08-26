package io.github.sds100.keymapper.actions.selectscreenshot

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.graphics.decodeBitmap
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.github.sds100.keymapper.actions.tapscreen.PickDisplayCoordinateViewModel
import io.github.sds100.keymapper.databinding.FragmentPickCoordinateBinding
import io.github.sds100.keymapper.databinding.FragmentSelectscreenshotBinding
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.ui.showPopups

class SelectScreenshotFragment : Fragment() {
    private val viewModel: SelectScreenshotViewModel by viewModels({requireParentFragment()})

    private val screenshotLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(requireContext().contentResolver, it)
                        .decodeBitmap { _, _ -> }
            } else {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }

            viewModel.selectedScreenshot(bitmap)
        }

    private var _binding: FragmentSelectscreenshotBinding? = null
    val binding: FragmentSelectscreenshotBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentSelectscreenshotBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, binding)

        viewModel.displaySize = Point().apply {
            val windowManager: WindowManager = requireContext().getSystemService()!!
            windowManager.defaultDisplay.getRealSize(this) }

        viewModel.blankScreen.shape = GradientDrawable.RECTANGLE
        viewModel.blankScreen.setStroke(
                ((3f * requireContext().resources.displayMetrics.density) + 0.5).toInt(),
                ColorStateList.valueOf(Color.BLACK))
        viewModel.blankScreen.setSize(viewModel.displaySize.x, viewModel.displaySize.y)

        binding.setOnSelectScreenshotClick {
            screenshotLauncher.launch(FileUtils.MIME_TYPE_IMAGES)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}