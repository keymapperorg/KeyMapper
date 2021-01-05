package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.viewmodel.IntentActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentIntentActionTypeBinding
import io.github.sds100.keymapper.databinding.ListItemIntentExtraBoolBinding
import io.github.sds100.keymapper.databinding.ListItemIntentExtraGenericBinding
import io.github.sds100.keymapper.intentExtraBool
import io.github.sds100.keymapper.intentExtraGeneric
import io.github.sds100.keymapper.util.BuildIntentExtraListItemModels
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.okButton

/**
 * Created by sds100 on 30/03/2020.
 */

class IntentActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_intent"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_URi = "extra_uri"

        private val EXTRA_TYPES = arrayOf(
            BoolExtraType(),
            IntArrayExtraType()
        )
    }

    private val mViewModel: IntentActionTypeViewModel by activityViewModels {
        InjectorUtils.provideIntentActionTypeViewModel()
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentIntentActionTypeBinding? = null
    val binding: FragmentIntentActionTypeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentIntentActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            setOnDoneClick {
                findNavController().navigateUp()
            }

            setOnAddExtraClick {
                requireContext().alertDialog {
                    val labels = EXTRA_TYPES.map { str(it.labelStringRes) }.toTypedArray()

                    setItems(labels) { _, position ->
                        mViewModel.addExtra(EXTRA_TYPES[position])
                    }

                    show()
                }
            }

            mViewModel.eventStream.observe(viewLifecycleOwner, { event ->
                when (event) {
                    is BuildIntentExtraListItemModels -> {
                        val models = event.extraModels.map { it.toListItemModel() }
                        mViewModel.setListItemModels(models)
                    }
                }
            })

            subscribeExtrasList()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun FragmentIntentActionTypeBinding.subscribeExtrasList() {
        mViewModel.extrasListItemModels.observe(viewLifecycleOwner, { state ->
            epoxyRecyclerViewExtras.withModels {

                val models = if (state is Data) {
                    state.data
                } else {
                    emptyList()
                }

                models.forEach {
                    bindExtra(it)
                }
            }
        })
    }

    private fun EpoxyController.bindExtra(model: IntentExtraListItemModel) {
        when (model) {
            is GenericIntentExtraListItemModel -> intentExtraGeneric {

                id(model.uid)

                model(model)

                onRemoveClick { _ ->
                    mViewModel.removeExtra(model.uid)
                }

                onShowExampleClick { _ ->
                    requireContext().alertDialog {
                        message = model.exampleString
                        okButton()

                        show()
                    }
                }

                onBind { model, view, _ ->
                    (view.dataBinding as ListItemIntentExtraGenericBinding).apply {
                        textInputLayoutExtraValue.editText?.doAfterTextChanged {
                            mViewModel.setExtraValue(model.model().uid, it.toString())
                        }

                        textInputLayoutExtraName.editText?.doAfterTextChanged {
                            mViewModel.setExtraName(model.model().uid, it.toString())
                        }
                    }
                }
            }

            is BoolIntentExtraListItemModel -> intentExtraBool {
                id(model.uid)

                model(model)

                onRemoveClick { _ ->
                    mViewModel.removeExtra(model.uid)
                }

                onBind { model, view, _ ->
                    (view.dataBinding as ListItemIntentExtraBoolBinding).apply {
                        radioButtonTrue.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) mViewModel.setExtraValue(model.model().uid, "true")
                        }

                        radioButtonFalse.setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) mViewModel.setExtraValue(model.model().uid, "false")
                        }
                    }
                }
            }
        }
    }

    private fun IntentExtraModel.toListItemModel(): IntentExtraListItemModel {
        return when (type) {
            is BoolExtraType -> BoolIntentExtraListItemModel(
                uid,
                name,
                parsedValue?.let { it as Boolean } ?: true,
                isValidValue
            )

            else -> {
                val inputType = when (type) {
                    is IntArrayExtraType -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_CLASS_TEXT
                    else -> InputType.TYPE_CLASS_TEXT
                }

                GenericIntentExtraListItemModel(
                    uid,
                    str(type.labelStringRes),
                    name,
                    value,
                    isValidValue,
                    str(type.exampleStringRes),
                    inputType
                )
            }
        }
    }
}