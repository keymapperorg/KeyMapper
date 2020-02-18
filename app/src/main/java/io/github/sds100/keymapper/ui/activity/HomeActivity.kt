package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.ActivityHomeBinding

/**
 * Created by sds100 on 19/02/2020.
 */

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
    }
}