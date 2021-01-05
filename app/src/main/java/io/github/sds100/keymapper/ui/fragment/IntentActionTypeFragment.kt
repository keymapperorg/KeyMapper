package io.github.sds100.keymapper.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.R
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
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton

/**
 * Created by sds100 on 30/03/2020.
 */

class IntentActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_intent"
        const val EXTRA_DESCRIPTION = "extra_intent_description"
        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_URI = "extra_uri"

        private val EXTRA_TYPES = arrayOf(
            BoolExtraType(),
            IntArrayExtraType(),
            StringExtraType()
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
                val intent = Intent().apply {
                    if (mViewModel.action.value?.isNotEmpty() == true) {
                        this.action = mViewModel.action.value
                    }

                    mViewModel.categoriesList.value?.forEach {
                        this.addCategory(it)
                    }

                    if (mViewModel.data.value?.isNotEmpty() == true) {
                        this.data = mViewModel.data.value?.toUri()
                    }

                    if (mViewModel.targetPackage.value?.isNotEmpty() == true) {
                        this.`package` = mViewModel.targetPackage.value

                        if (mViewModel.targetClass.value?.isNotEmpty() == true) {
                            this.setClassName(
                                mViewModel.targetPackage.value!!,
                                mViewModel.targetClass.value!!
                            )
                        }
                    }

                    mViewModel.extras.value?.forEach { model ->
                        if (model.name.isEmpty()) return@forEach
                        if (model.parsedValue == null) return@forEach

                        when (model.type) {
                            is BoolExtraType ->
                                putExtra(model.name, model.parsedValue as Boolean)

                            is IntArrayExtraType ->
                                putExtra(model.name, model.parsedValue as IntArray)

                            is StringExtraType ->
                                putExtra(model.name, model.parsedValue as String)
                        }
                    }
                }

                val uri = intent.toUri(0)

                setFragmentResult(REQUEST_KEY,
                    bundleOf(
                        EXTRA_DESCRIPTION to mViewModel.description.value,
                        EXTRA_TARGET to mViewModel.getTarget().toString(),
                        EXTRA_URI to uri
                    )
                )

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

            setOnShowCategoriesExampleClick {
                requireContext().alertDialog {
                    messageResource = R.string.intent_categories_example
                    okButton()
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