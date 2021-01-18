package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.databinding.FragmentAppIntroSlideBinding
import io.github.sds100.keymapper.util.ViewLoading
import io.github.sds100.keymapper.util.ViewPopulated

abstract class AppIntroScrollableFragment : Fragment() {

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentAppIntroSlideBinding? = null
    val binding: FragmentAppIntroSlideBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentAppIntroSlideBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewState = ViewLoading()

        onBind(binding)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    fun viewLoaded() {
        binding.viewState = ViewPopulated()
    }

    abstract fun onBind(binding: FragmentAppIntroSlideBinding)
}
