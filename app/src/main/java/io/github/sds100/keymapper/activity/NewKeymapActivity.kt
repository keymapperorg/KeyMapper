package io.github.sds100.keymapper.activity

import androidx.lifecycle.ViewModelProviders
import io.github.sds100.keymapper.viewmodel.ConfigKeyMapViewModel
import io.github.sds100.keymapper.viewmodel.NewKeyMapViewModel

open class NewKeymapActivity : ConfigKeymapActivity() {

    override val viewModel: ConfigKeyMapViewModel by lazy {
        ViewModelProviders.of(this).get(NewKeyMapViewModel::class.java)
    }
}
