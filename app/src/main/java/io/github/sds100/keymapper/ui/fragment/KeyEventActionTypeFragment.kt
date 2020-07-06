package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.viewmodel.KeyEventActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentKeyeventActionTypeBinding
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyEventActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_key_event"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_META_STATE = "extra_meta_state"
    }

    private val mViewModel: KeyEventActionTypeViewModel by activityViewModels {
        InjectorUtils.provideKeyEventActionTypeViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeyeventActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            mViewModel.clearKeyEvent()

            viewModel = mViewModel

            setOnDoneClick {
                setFragmentResult(REQUEST_KEY,
                    bundleOf(
                        EXTRA_KEYCODE to mViewModel.keyCode.value?.toInt(),
                        EXTRA_META_STATE to mViewModel.metaState.value)
                )

                findNavController().navigateUp()
            }

            mViewModel.chooseKeycode.observe(viewLifecycleOwner, EventObserver {
                val direction = ChooseActionFragmentDirections.actionChooseActionFragmentToKeycodeListFragment()
                findNavController().navigate(direction)
            })

            mViewModel.modifierKeyModels.observe(viewLifecycleOwner) { models ->
                epoxyRecyclerViewModifiers.withModels {
                    models.forEach {
                        checkbox {
                            id(it.id)
                            primaryText(str(it.label))
                            isSelected(it.isChecked)

                            onClick { view ->
                                mViewModel.setModifierKey(it.id.toInt(), (view as CheckBox).isChecked)
                            }
                        }
                    }
                }
            }

            return this.root
        }
    }
}