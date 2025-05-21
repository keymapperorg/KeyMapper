package io.github.sds100.keymapper

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.utils.ui.showPopups
import io.github.sds100.keymapper.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : BaseMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        viewModel.showPopups(this, binding.coordinatorLayout)
    }
}
