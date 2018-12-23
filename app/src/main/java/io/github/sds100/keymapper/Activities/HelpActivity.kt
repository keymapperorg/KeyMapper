package io.github.sds100.keymapper.Activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by sds100 on 19/12/2018.
 */

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setSupportActionBar(toolbar)
    }
}