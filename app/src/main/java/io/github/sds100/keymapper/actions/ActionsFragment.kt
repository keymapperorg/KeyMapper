package io.github.sds100.keymapper.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.compose.rememberNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.composethemeadapter3.Mdc3Theme
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.databinding.FragmentComposeViewBinding

/**
 * Created by sds100 on 12/07/2022.
 */
@AndroidEntryPoint
class ActionsFragment : Fragment() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
    }

    private var _binding: FragmentComposeViewBinding? = null
    private val binding: FragmentComposeViewBinding
        get() = _binding!!

    private val args: ActionsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeViewBinding.inflate(inflater, container, false)
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                Mdc3Theme {
                    ActionsNavHost(
                        navHostController = rememberNavController(),
                        setResult = { result ->
                            setFragmentResult(args.requestKey, result)
                        },
                        startDestination = args.destination,
                        navigateBack = { findNavController().navigateUp() }
                    )
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}