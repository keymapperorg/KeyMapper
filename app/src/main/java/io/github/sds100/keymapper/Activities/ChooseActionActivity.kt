package io.github.sds100.keymapper.Activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.google.android.material.tabs.TabLayout
import io.github.sds100.keymapper.ActionTypeFragments.*
import io.github.sds100.keymapper.CustomViewPager
import io.github.sds100.keymapper.Delegates.ITabDelegate
import io.github.sds100.keymapper.Delegates.TabDelegate
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.activity_choose_action.*

class ChooseActionActivity : AppCompatActivity(), ITabDelegate, TabLayout.OnTabSelectedListener {
    override val tabLayout: TabLayout
        get() = findViewById(R.id.tabLayout)

    override val viewPager: CustomViewPager
        get() = findViewById(R.id.viewPager)

    override val tabFragments
        get() = listOf(
                mAppActionTypeFragment,
                mAppShortcutActionTypeFragment,
                mKeycodeActionTypeFragment,
                mKeyActionTypeFragment,
                mSystemActionTypeFragment,
                mTextActionTypeFragment
        )

    override val tabTitles by lazy {
        listOf(
                getString(R.string.action_type_title_application),
                getString(R.string.action_type_title_application_shortcut),
                getString(R.string.action_type_title_keycode),
                getString(R.string.action_type_title_key),
                getString(R.string.action_type_title_system_action),
                getString(R.string.action_type_title_text_block)
        )
    }

    private val mTabDelegate = TabDelegate(
            supportFragmentManager,
            iTabDelegate = this,
            onTabSelectedListener = this,
            mOffScreenLimit = 6)

    //The fragments which will each be shown when their corresponding item in the spinner is pressed
    private val mAppActionTypeFragment = AppActionTypeFragment()
    private val mAppShortcutActionTypeFragment = AppShortcutActionTypeFragment()
    private val mKeycodeActionTypeFragment = KeycodeActionTypeFragment()
    private val mKeyActionTypeFragment = KeyActionTypeFragment()
    private val mTextActionTypeFragment = TextActionTypeFragment()
    private val mSystemActionTypeFragment = SystemActionFragment()

    private lateinit var mSearchViewMenuItem: MenuItem
    private lateinit var mShowHiddenSystemActionsMenuItem: MenuItem

    private val mSearchView
        get() = mSearchViewMenuItem.actionView as SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_action)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mTabDelegate.configureTabs()
        //the OnTabSelectedListener has been set in onCreateOptionsMenu
    }

    override fun onDestroy() {
        super.onDestroy()

        tabLayout.removeOnTabSelectedListener(this)
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

        mShowHiddenSystemActionsMenuItem = menu.findItem(R.id.action_show_hidden_system_actions)

        //hide the tabs when the user opens the SearchView
        mSearchViewMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                tabLayout.visibility = View.GONE
                viewPager.isPagingEnabled = false
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                tabLayout.visibility = View.VISIBLE
                viewPager.isPagingEnabled = true
                return true
            }
        })

        //set AFTER the menu items have been initialised to avoid not-initialised error
        tabLayout.addOnTabSelectedListener(this)

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


    override fun onTabSelected(tab: TabLayout.Tab) {
        val fragment = tabFragments[tab.position]

        mShowHiddenSystemActionsMenuItem.isVisible = fragment is SystemActionFragment

        val isFilterableFragment = fragment is FilterableActionTypeFragment

        mSearchViewMenuItem.isVisible = isFilterableFragment

        if (isFilterableFragment) {
            mSearchView.setOnQueryTextListener(fragment as FilterableActionTypeFragment)
        }
    }

    override fun onTabReselected(p0: TabLayout.Tab?) {}
    override fun onTabUnselected(p0: TabLayout.Tab?) {}
}
