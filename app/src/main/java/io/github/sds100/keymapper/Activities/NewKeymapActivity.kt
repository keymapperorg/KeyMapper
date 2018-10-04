package io.github.sds100.keymapper.Activities

import androidx.lifecycle.ViewModelProviders
import io.github.sds100.keymapper.ViewModels.ConfigKeyMapViewModel
import io.github.sds100.keymapper.ViewModels.NewKeyMapViewModel

open class NewKeymapActivity : ConfigKeymapActivity() {

    override val viewModel: ConfigKeyMapViewModel by lazy {
        ViewModelProviders.of(this).get(NewKeyMapViewModel::class.java)
    }
}
