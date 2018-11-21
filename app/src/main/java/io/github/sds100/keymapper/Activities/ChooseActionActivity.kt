package io.github.sds100.keymapper.Activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import io.github.sds100.keymapper.ActionType
import io.github.sds100.keymapper.ActionTypeFragments.*
import io.github.sds100.keymapper.Adapters.ActionTypeSpinnerAdapter
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.activity_choose_action.*
import kotlinx.android.synthetic.main.content_choose_action.*

class ChooseActionActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    //The fragments which will each be shown when their corresponding item in the spinner is pressed
    private val mAppActionTypeFragment = AppActionTypeFragment()
    private val mAppShortcutActionTypeFragment = AppShortcutActionTypeFragment()
    private val mKeycodeActionTypeFragment = KeycodeActionTypeFragment()
    private val mKeyActionTypeFragment = KeyActionTypeFragment()
    private val mTextActionTypeFragment = TextActionTypeFragment()
    private val mSystemActionTypeFragment = SystemActionTypeFragment()

    private lateinit var mSearchViewMenuItem: MenuItem

    private val mSearchView
        get() = mSearchViewMenuItem.actionView as SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_action)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        spinnerActionTypes.adapter = ActionTypeSpinnerAdapter(this)
        spinnerActionTypes.onItemSelectedListener = this
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (mKeyActionTypeFragment.isVisible) {
            mKeyActionTypeFragment.showKeyEventChip(event!!)
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_choose_action, menu)

        mSearchViewMenuItem = menu!!.findItem(R.id.action_search)
        mSearchView.queryHint = getString(R.string.action_search)

        //hide the action type spinner when the user opens the SearchView
        mSearchViewMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                spinnerActionTypes.visibility = View.GONE
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                spinnerActionTypes.visibility = View.VISIBLE
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
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

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val actionType = ActionTypeSpinnerAdapter.getActionTypeFromPosition(
                ctx = this,
                position = position
        )

        when (actionType) {
            ActionType.APP -> {
                changeSelectedActionTypeFragment(mAppActionTypeFragment)
                mSearchViewMenuItem.isVisible = true
            }

            ActionType.KEYCODE -> {
                changeSelectedActionTypeFragment(mKeycodeActionTypeFragment)
                mSearchViewMenuItem.isVisible = true
            }

            ActionType.APP_SHORTCUT -> {
                changeSelectedActionTypeFragment(mAppShortcutActionTypeFragment)
                mSearchViewMenuItem.isVisible = true
            }

            ActionType.KEY -> {
                changeSelectedActionTypeFragment(mKeyActionTypeFragment)
                mSearchViewMenuItem.isVisible = false
            }

            ActionType.SYSTEM_ACTION -> {
                changeSelectedActionTypeFragment(mSystemActionTypeFragment)
                mSearchViewMenuItem.isVisible = true
            }

            ActionType.TEXT_BLOCK -> {
                changeSelectedActionTypeFragment(mTextActionTypeFragment)
                mSearchViewMenuItem.isVisible = false
            }
        }
    }

    private fun changeSelectedActionTypeFragment(fragment: ActionTypeFragment) {
        supportFragmentManager.beginTransaction().replace(R.id.frameLayout, fragment).commit()

        if (fragment is FilterableActionTypeFragment) {
            mSearchView.setOnQueryTextListener(fragment)
        }
    }
}
