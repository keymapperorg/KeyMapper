package io.github.sds100.keymapper.actions.sound

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.databinding.FragmentChooseSoundFileBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 22/06/2021.
 */

class ChooseSoundFileFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_sound_file"
        const val EXTRA_RESULT = "extra_sound_file_result"
    }

    private val viewModel: ChooseSoundFileViewModel by viewModels {
        Inject.soundFileActionTypeViewModel(requireContext())
    }

    private val chooseSoundFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            viewModel.onChooseNewSoundFile(it.toString())
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentChooseSoundFileBinding? = null
    val binding: FragmentChooseSoundFileBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        FragmentChooseSoundFileBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        viewModel.showPopups(this, binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.chooseSoundFile.collectLatest {
                chooseSoundFileLauncher.launch(FileUtils.MIME_TYPE_AUDIO)
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest { result ->
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(EXTRA_RESULT to Json.encodeToString(result))
                )
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.soundFileListItems.collectLatest { listItems ->
                binding.epoxyRecyclerView.withModels {
                    listItems.forEach { soundFile ->
                        simple {
                            id(soundFile.uid)
                            primaryText(soundFile.name)

                            onClick { _ ->
                                viewModel.onFileListItemClick(soundFile)
                            }
                        }
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