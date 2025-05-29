package io.github.sds100.keymapper

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.utils.ui.showPopups
import io.github.sds100.keymapper.base.databinding.ActivityMainBinding
import io.github.sds100.keymapper.base.R

@AndroidEntryPoint
class MainActivity : BaseMainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val navController = binding.container.getFragment<NavHostFragment>().navController
        val fragmentNavigator =
            navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)

        val homeDest = fragmentNavigator.createDestination().apply {
            id = R.id.home_fragment
            setClassName(MainFragment::class.java.name)
        }

        navController.graph = navController.navInflater.inflate(R.navigation.nav_base_app).apply {
            addDestination(homeDest)
            setStartDestination(R.id.home_fragment)
        }

        viewModel.showPopups(this, binding.coordinatorLayout)
    }
}
