package io.github.sds100.keymapper.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.databinding.FragmentAppIntroSlideBinding
import io.github.sds100.keymapper.util.ui.showPopups

class FixAppKillingSlideFragment : Fragment() {

    companion object {
        const val KEY_SLIDE = "key_slide"
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentAppIntroSlideBinding? = null
    val binding: FragmentAppIntroSlideBinding
        get() = _binding!!

    private val viewModel by activityViewModels<FixAppKillingViewModel>()

    private val slide: String by lazy {
        requireArguments().getString(KEY_SLIDE)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentAppIntroSlideBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, binding)

        val model = viewModel.getSlide(slide)
        binding.model = model

        binding.setOnButton1ClickListener {
            if (model.buttonId1 != null) {
                viewModel.onButtonClick(model.buttonId1)
            }
        }

        binding.setOnButton2ClickListener {
            if (model.buttonId2 != null) {
                viewModel.onButtonClick(model.buttonId2)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
