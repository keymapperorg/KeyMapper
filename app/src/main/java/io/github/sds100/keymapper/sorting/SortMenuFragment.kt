package io.github.sds100.keymapper.sorting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.sorting.ui.SortBottomSheetContent
import io.github.sds100.keymapper.util.Inject

class SortMenuFragment : BottomSheetDialogFragment() {

    private val sortViewModel: SortViewModel by activityViewModels {
        Inject.sortViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                KeyMapperTheme {
                    Surface {
                        SortBottomSheetContent(
                            onExit = ::dismiss,
                            viewModel = sortViewModel,
                        )
                    }
                }
            }
        }
    }
}
