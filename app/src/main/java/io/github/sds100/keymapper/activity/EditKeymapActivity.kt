package io.github.sds100.keymapper.activity

import androidx.lifecycle.ViewModelProviders
import io.github.sds100.keymapper.viewmodel.EditKeyMapViewModel

open class EditKeymapActivity : ConfigKeymapActivity() {

    companion object {
        const val EXTRA_KEYMAP_ID = "extra_id"
    }

    override val viewModel: EditKeyMapViewModel by lazy {
        val id = intent.getLongExtra(EXTRA_KEYMAP_ID, 0)
        ViewModelProviders.of(this, EditKeyMapViewModel.Factory(id, application)).get(EditKeyMapViewModel::class.java)
    }
}
