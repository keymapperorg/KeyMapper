package io.github.sds100.keymapper.activity

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.github.sds100.keymapper.CustomViewPager
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.activity.ConfigKeymapActivity.Companion.EXTRA_KEY_EVENT
import io.github.sds100.keymapper.delegate.TabDelegate
import io.github.sds100.keymapper.fragment.*
import io.github.sds100.keymapper.fragment.KeyActionTypeFragment.Companion.ACTION_ON_KEY_EVENT
import io.github.sds100.keymapper.interfaces.ITabDelegate
import io.github.sds100.keymapper.util.SystemActionUtils
import io.github.sds100.keymapper.util.str
import kotlinx.android.synthetic.main.activity_choose_action.*

class ChooseActionActivity : AppCompatActivity(), ITabDelegate, TabLayout.OnTabSelectedListener {
    override val tabLayout: TabLayout
        get() = findViewById(R.id.tabLayout)

    override val viewPager: CustomViewPager
        get() = findViewById(R.id.viewPager)

    override lateinit var tabFragments: List<Fragment>

    override val tabTitles by lazy {
        sequence {
            yieldAll(listOf(
                str(R.string.action_type_title_application),
                str(R.string.action_type_title_application_shortcut),
                str(R.string.action_type_title_keycode),
                str(R.string.action_type_title_key),
                str(R.string.action_type_title_text_block),
                str(R.string.action_type_title_system_action)
            ))

            if (!mAreAllActionsSupported) {
                yield(str(R.string.tab_unsupported_actions))
            }
        }.toList()
    }

    private val mTabDelegate = TabDelegate(
        supportFragmentManager,
        iTabDelegate = this,
        onTabSelectedListener = this,
        mOffScreenLimit = 7)

    private lateinit var mSearchViewMenuItem: MenuItem

    private val mSearchView
        get() = mSearchViewMenuItem.actionView as SearchView

    private val mAreAllActionsSupported by lazy {
        SystemActionUtils.areAllActionsSupported(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_action)
        setSupportActionBar(toolbar)

        //show the back button in the toolbar
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        /* when the activity is destroyed, the FragmentStatePagerAdapter doesn't destroy the old fragments so the
        * fragments being shown will not be the ones created again, which is why they have to be retrieved from the
        * fragment manager. */
        if (savedInstanceState == null) {
            tabFragments = sequence {
                yieldAll(listOf(
                    AppActionTypeFragment(),
                    AppShortcutActionTypeFragment(),
                    KeycodeActionTypeFragment(),
                    KeyActionTypeFragment(),
                    TextActionTypeFragment(),
                    SystemActionFragment()
                ))

                if (!mAreAllActionsSupported) {
                    yield(UnsupportedActionsFragment())
                }
            }.toList()

        } else {
            val oldActionTypeFragments = supportFragmentManager.fragments.filter {
                it is ActionTypeFragment || it is UnsupportedActionsFragment
            }

            @Suppress("UNCHECKED_CAST")
            if (oldActionTypeFragments.all { it != null }) {
                tabFragments = oldActionTypeFragments as List<ActionTypeFragment>
            }
        }

        mTabDelegate.configureTabs()
        //the OnTabSelectedListener has been set in onCreateOptionsMenu
    }

    override fun onDestroy() {
        super.onDestroy()

        tabLayout.removeOnTabSelectedListener(this)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        //tell KeyActionTypeFragment to show a new key event chip
        val intent = Intent(ACTION_ON_KEY_EVENT)
        intent.putExtra(EXTRA_KEY_EVENT, event)
        sendBroadcast(intent)

        return super.onKeyUp(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_choose_action, menu)

        mSearchViewMenuItem = menu!!.findItem(R.id.action_search)
        mSearchView.queryHint = getString(R.string.action_search)

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

        //The first fragment shown needs to be initially attached to the SearchView otherwise it won't be
        if (tabFragments[tabLayout.selectedTabPosition] is FilterableActionTypeFragment) {
            mSearchView.setOnQueryTextListener(tabFragments[tabLayout.selectedTabPosition]
                as FilterableActionTypeFragment)
        }

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

        val isFilterableFragment = fragment is FilterableActionTypeFragment

        mSearchViewMenuItem.isVisible = isFilterableFragment

        if (isFilterableFragment) {
            mSearchView.setOnQueryTextListener(fragment as FilterableActionTypeFragment)
        }
    }

    override fun onTabReselected(p0: TabLayout.Tab?) {}
    override fun onTabUnselected(p0: TabLayout.Tab?) {}
}
