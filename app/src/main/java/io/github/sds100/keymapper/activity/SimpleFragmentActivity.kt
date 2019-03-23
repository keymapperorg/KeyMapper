package io.github.sds100.keymapper.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction

/**
 * Created by sds100 on 01/10/2018.
 */

abstract class SimpleFragmentActivity : AppCompatActivity() {

    abstract val fragmentToShow: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //show back button
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.transaction {
                add(android.R.id.content, fragmentToShow)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            /* when the back button in the toolbar is pressed, call onBackPressed so it acts like
            the hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}