package io.github.sds100.keymapper.mappings.keymaps

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentComposeBinding
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource

class CreateKeyMapShortcutFragment : Fragment() {

    private val viewModel by activityViewModels<CreateKeyMapShortcutViewModel> {
        Inject.createActionShortcutViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentComposeBinding.inflate(inflater, container, false).apply {
            composeView.apply {
                // Dispose of the Composition when the view's LifecycleOwner
                // is destroyed
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val listItems by viewModel.state.collectAsStateWithLifecycle()
                    KeyMapperTheme {
                        Text(text = stringResource(R.string.caption_create_keymap_shortcut))
                        Scaffold {
                            KeyMapListScreen(
                                listItems = listItems,
                                isSelectable = false,
                            )
                        }
                    }

                    BackHandler {
                        // TODO warning dialog
                    }
                }
            }
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, view)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnIntentResult.collectLatest { intent ->
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
        }
    }

    private fun showOnBackPressedWarning() {
        requireContext().alertDialog {
            titleResource = R.string.dialog_title_unsaved_changes
            messageResource = R.string.dialog_message_unsaved_changes

            positiveButton(R.string.pos_discard_changes) {
                requireActivity().finish()
            }

            negativeButton(R.string.neg_keep_editing) { it.cancel() }
            show()
        }
    }
}
