package io.github.sds100.keymapper.mappings.assistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.databinding.FragmentComposeViewBinding

class AssistantTriggerFragment : Fragment() {
    private var _binding: FragmentComposeViewBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeViewBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Text("I am free build variant.")
                }
            }
        }

        return view
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}