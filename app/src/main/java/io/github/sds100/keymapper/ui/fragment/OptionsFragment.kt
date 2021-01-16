package io.github.sds100.keymapper.ui.fragment

import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.OptionsListModel
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 15/01/21.
 */
abstract class OptionsFragment<O : BaseOptions<*>>
    : DefaultRecyclerViewFragment<OptionsListModel>() {
    abstract val optionsViewModel: BaseOptionsViewModel<O>
    abstract val controller: OptionsController

    override val modelState: IModelState<OptionsListModel>
        get() = optionsViewModel

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        super.subscribeUi(binding)

        binding.epoxyRecyclerView.adapter = controller.adapter
    }

    override fun populateList(binding: FragmentRecyclerviewBinding, model: OptionsListModel?) {
        controller.optionsListModel = model ?: OptionsListModel.EMPTY
    }
}