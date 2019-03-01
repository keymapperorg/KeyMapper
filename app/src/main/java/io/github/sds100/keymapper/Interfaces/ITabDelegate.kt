package io.github.sds100.keymapper.interfaces

import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.github.sds100.keymapper.CustomViewPager

/**
 * Created by sds100 on 29/11/2018.
 */

interface ITabDelegate {
    val tabLayout: TabLayout
    val viewPager: CustomViewPager
    val tabFragments: List<Fragment>
    val tabTitles: List<String>
}
