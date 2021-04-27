package io.github.sds100.keymapper.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import io.github.sds100.keymapper.databinding.FragmentAppIntroSlideBinding
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest

class AppIntroScrollableFragment : Fragment() {

    companion object {
        const val KEY_SLIDE = "key_slide"
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentAppIntroSlideBinding? = null
    val binding: FragmentAppIntroSlideBinding
        get() = _binding!!

    private val appIntroViewModel by activityViewModels<AppIntroViewModel> {
        val slides = requireActivity().intent.getStringArrayExtra(AppIntroActivity.EXTRA_SLIDES)
            ?.map { AppIntroSlide.valueOf(it) }

        Inject.appIntroViewModel(requireContext(), slides!!)
    }

    private val slide: AppIntroSlide by lazy {
        val string = requireArguments().getString(KEY_SLIDE)!!
        AppIntroSlide.valueOf(string)
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

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.CREATED) {
            appIntroViewModel.getSlide(slide).collectLatest { model ->
                binding.model = model

                binding.setOnButton1ClickListener {

                    if (model.buttonId1 != null) {
                        appIntroViewModel.onButtonClick(model.buttonId1)
                    }
                }

                binding.setOnButton2ClickListener {
                    if (model.buttonId2 != null) {
                        appIntroViewModel.onButtonClick(model.buttonId2)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
