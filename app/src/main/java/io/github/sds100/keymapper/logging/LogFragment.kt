package io.github.sds100.keymapper.logging

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentLogBinding
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.styledColor
import io.github.sds100.keymapper.util.ui.SpannableUtils
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*

/**
 * Created by sds100 on 13/05/2021.
 */
class LogFragment : Fragment() {

    private val viewModel by viewModels<LogViewModel> {
        Inject.logViewModel(requireContext())
    }

    private val saveLogToFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.onPickFileToSaveTo(it.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
        }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentLogBinding? = null
    val binding: FragmentLogBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        FragmentLogBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, binding)

        binding.appBar.setOnMenuItemClickListener { menuItem ->
            viewModel.onMenuItemClick(menuItem.itemId)
            true
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.pickFileToSaveTo.collectLatest {
                saveLogToFileLauncher.launch(LogUtils.createLogFileName())
            }
        }

        addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.listItems.onEach { state ->
                when (state) {
                    is State.Data -> {
                        if (state.data.isEmpty()) {
                            binding.progressBar.isVisible = false
                            binding.scrollView.isVisible = false
                            binding.emptyListPlaceHolder.isVisible = true

                            binding.textViewLog.text = buildLogSpannableString(state.data)
                        } else {
                            binding.progressBar.isVisible = true
                            binding.emptyListPlaceHolder.isVisible = false
                            binding.textViewLog.text = buildLogSpannableString(state.data)
                            binding.progressBar.isVisible = false
                            binding.scrollView.isVisible = true

                            val scrollView = binding.scrollView

                            val scrollToBottom: Boolean =
                                scrollView.getChildAt(0).bottom == scrollView.height + scrollView.scrollY

                            if (scrollToBottom) {
                                scrollView.doOnNextLayout {
                                    scrollView.scrollTo(0, binding.textViewLog.measuredHeight)
                                }
                            }
                        }
                    }

                    is State.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.scrollView.isVisible = false
                        binding.emptyListPlaceHolder.isVisible = false
                    }
                }
            }.launchIn(this)
        }
    }

    private fun buildLogSpannableString(listItems: List<LogEntryListItem>): SpannableStringBuilder {
        val stringBuilder = SpannableStringBuilder()

        val errorColor = requireContext().styledColor(R.attr.colorError)

        listItems.joinTo(stringBuilder, "\n") { logEntry ->
            val useErrorTextColor = logEntry.textTint == TintType.ERROR

            val string = "${logEntry.time} ${logEntry.message}"

            if (useErrorTextColor) {
                SpannableUtils.color(errorColor, string)
            } else {
                string
            }
        }

        return stringBuilder
    }
}