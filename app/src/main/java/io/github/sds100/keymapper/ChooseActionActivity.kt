package io.github.sds100.keymapper

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_choose_action.*
import kotlinx.android.synthetic.main.content_choose_action.*

class ChooseActionActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val mAppAdapter by lazy { AppListAdapter.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_action)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        spinnerActionTypes.adapter = ActionTypeSpinnerAdapter(this)
        spinnerActionTypes.onItemSelectedListener = this

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
        /*when the back button in the toolbar is pressed, call onBackPressed so it acts like the
        hardware back button */
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }
}
