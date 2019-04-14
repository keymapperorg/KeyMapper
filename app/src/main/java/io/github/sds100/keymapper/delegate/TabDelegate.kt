package io.github.sds100.keymapper.delegate

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import io.github.sds100.keymapper.interfaces.ITabDelegate

/**
 * Created by sds100 on 29/11/2018.
 */

class TabDelegate(supportFragmentManager: FragmentManager,
                  iTabDelegate: ITabDelegate,
                  onTabSelectedListener: TabLayout.OnTabSelectedListener,
                  private val mOffScreenLimit: Int = 3
) : ITabDelegate by iTabDelegate, TabLayout.OnTabSelectedListener by onTabSelectedListener {

    private val mFragmentPagerAdapter =
            object : FragmentStatePagerAdapter(supportFragmentManager) {
                override fun getItem(position: Int) = tabFragments[position]
                override fun getPageTitle(position: Int) = tabTitles[position]
                override fun getCount() = tabFragments.size
            }

    fun configureTabs() {
        if (tabTitles.size != tabFragments.size) {
            throw Exception("Not every fragment has been assigned a tab descriptionRes!")
        }

        //improves performance when switching tabs since the fragment's onViewCreated isn't called
        viewPager.offscreenPageLimit = mOffScreenLimit
        viewPager.adapter = mFragmentPagerAdapter

        tabLayout.setupWithViewPager(viewPager)
    }
}