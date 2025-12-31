package io.github.sds100.keymapper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.databinding.ActivityMainBinding
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.showDialogs
import javax.inject.Inject

// Import du service ADB
import com.tpn.adbautoenable.AdbConfigService

@AndroidEntryPoint
class MainActivity : BaseMainActivity() {

    @Inject
    lateinit var dialogProvider: DialogProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. DÉMARRAGE DU SERVICE ADB
        startAdbEngine()

        // 2. INTERFACE KEYMAPPER
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val navController = binding.container.getFragment<NavHostFragment>().navController
        val fragmentNavigator = navController.navigatorProvider.getNavigator(FragmentNavigator::class.java)

        val homeDest = fragmentNavigator.createDestination().apply {
            id = R.id.home_fragment
            setClassName(MainFragment::class.java.name)
        }

        navController.graph = navController.navInflater.inflate(R.navigation.nav_base_app).apply {
            addDestination(homeDest)
            setStartDestination(R.id.home_fragment)
        }

        dialogProvider.showDialogs(this, binding.coordinatorLayout)
    }

    private fun startAdbEngine() {
        try {
            val serviceIntent = Intent(this, AdbConfigService::class.java)
            serviceIntent.putExtra("boot_config", false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d("ADB_FUSION", "Moteur ADB lancé en tâche de fond.")
        } catch (e: Exception) {
            Log.e("ADB_FUSION", "Erreur Service: ${e.message}")
        }
    }
}
