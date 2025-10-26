package io.github.sds100.keymapper.base.actions.sound

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
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
import io.github.sds100.keymapper.base.databinding.FragmentChooseSoundFileBinding
import io.github.sds100.keymapper.base.simple
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.system.files.FileUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class ChooseSoundFileFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_sound_file_result"
    }

    private val args: ChooseSoundFileFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }

    private val viewModel: ChooseSoundFileViewModel by viewModels()

    private val chooseSoundFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) {
            it ?: return@registerForActivityResult

            viewModel.onChooseNewSoundFile(it.toString())
        }

    private val chooseRingtoneLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.data != null && result.resultCode == Activity.RESULT_OK) {
                val uri = IntentCompat.getParcelableExtra(
                    result.data!!,
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                ) ?: return@registerForActivityResult

                viewModel.onChooseRingtone(uri.toString())
            }
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentChooseSoundFileBinding? = null
    val binding: FragmentChooseSoundFileBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentChooseSoundFileBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime(),
                )
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.viewModel = viewModel

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.chooseSoundFile.collectLatest {
                chooseSoundFileLauncher.launch(FileUtils.MIME_TYPE_AUDIO)
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.chooseSystemRingtone.collectLatest {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    // Allow notification, alarms, and ringtones.
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                }
                chooseRingtoneLauncher.launch(intent)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.filterNotNull().collect { result ->
                setFragmentResult(
                    requestKey,
                    bundleOf(EXTRA_RESULT to Json.encodeToString(result)),
                )
                findNavController().navigateUp()
                viewModel.returnResult.update { null }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.soundFileListItems.collectLatest { listItems ->
                binding.epoxyRecyclerView.withModels {
                    listItems.forEach { listItem ->
                        simple {
                            id(listItem.id)
                            model(listItem)

                            onClickListener { _ ->
                                viewModel.onFileListItemClick(listItem.id)
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
