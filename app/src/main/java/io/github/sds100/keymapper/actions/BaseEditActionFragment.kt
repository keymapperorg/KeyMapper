package io.github.sds100.keymapper.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.sds100.keymapper.databinding.FragmentEditActionBinding
import io.github.sds100.keymapper.mappings.Mapping
import io.github.sds100.keymapper.mappings.OptionsBottomSheetFragment

/**
 * Created by sds100 on 26/07/2021.
 */
abstract class BaseEditActionFragment<M : Mapping<A>, A : Action> :
    OptionsBottomSheetFragment<FragmentEditActionBinding>() {

    abstract override val viewModel: EditActionViewModel<M, A>

    override fun bind(inflater: LayoutInflater, container: ViewGroup?): FragmentEditActionBinding {
        return FragmentEditActionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        binding.buttonEditAction.setOnClickListener {
            viewModel.onEditActionClick()
            dismiss()
        }

        binding.buttonReplaceAction.setOnClickListener {
            viewModel.onReplaceActionClick()
            dismiss()
        }
    }

    override fun getRecyclerView(binding: FragmentEditActionBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentEditActionBinding) = binding.progressBar
    override fun getDoneButton(binding: FragmentEditActionBinding) = binding.buttonDone
    override fun getHelpButton(binding: FragmentEditActionBinding) = binding.buttonHelp
}