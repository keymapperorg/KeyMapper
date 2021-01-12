package io.github.sds100.keymapper.ui.activity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.databinding.FragmentAppIntroSlideBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.fragment.AppIntroScrollableFragment
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.DexUtils.isDexSupported
import splitties.systemservices.powerManager
import splitties.toast.longToast

/**
 * Created by sds100 on 07/07/2019.
 */

class AppIntroActivity : AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        addSlide(NoteFromDeveloperSlide())
        addSlide(AccessibilityServiceSlide())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {

            addSlide(BatteryOptimisationSlide())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            addSlide(FingerprintGestureSupportSlide())
        }

        if (isDexSupported(this)) {
            addSlide(DexSlide())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {

            addSlide(DndAccessSlide())
        }

        addSlide(ContributingSlide())
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        AppPreferences.shownAppIntro = true
        startActivity(Intent(this, HomeActivity::class.java))

        finish()
    }
}

private class NoteFromDeveloperSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_note_from_the_developer_title)
            description = str(R.string.showcase_note_from_the_developer_description)
            imageDrawable = drawable(R.mipmap.ic_launcher_round)
            backgroundColor = color(R.color.red)
        }
    }
}

private class AccessibilityServiceSlide : AppIntroScrollableFragment() {
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MyAccessibilityService.ACTION_ON_START) {
                binding.apply {
                    if (AccessibilityUtils.isServiceEnabled(requireContext())) {
                        serviceEnabledLayout()
                    } else {
                        serviceDisabledLayout()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        IntentFilter().apply {
            addAction(MyAccessibilityService.ACTION_ON_START)

            requireContext().registerReceiver(broadcastReceiver, this)
        }
    }

    override fun onDestroyView() {
        requireContext().unregisterReceiver(broadcastReceiver)

        super.onDestroyView()
    }

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            if (AccessibilityUtils.isServiceEnabled(requireContext())) {
                serviceEnabledLayout()
            } else {
                serviceDisabledLayout()
            }
        }
    }

    private fun FragmentAppIntroSlideBinding.serviceDisabledLayout() {
        title = str(R.string.showcase_accessibility_service_title_disabled)
        description = str(R.string.showcase_accessibility_service_description_disabled)

        imageDrawable = drawable(R.drawable.ic_outline_error_outline_64)
        backgroundColor = color(R.color.purple)

        buttonText = str(R.string.enable)

        setOnButtonClickListener {
            AccessibilityUtils.enableService(requireContext())
        }
    }

    private fun FragmentAppIntroSlideBinding.serviceEnabledLayout() {
        title = str(R.string.showcase_accessibility_service_title_enabled)
        description = str(R.string.showcase_accessibility_service_description_enabled)

        imageDrawable = drawable(R.drawable.ic_baseline_check_64)
        backgroundColor = color(R.color.purple)

        buttonText = null
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private class BatteryOptimisationSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_disable_battery_optimisation_title)
            description = str(R.string.showcase_disable_battery_optimisation_message)

            imageDrawable = drawable(R.drawable.ic_battery_std_white_64dp)
            backgroundColor = color(R.color.blue)

            buttonText = str(R.string.showcase_disable_battery_optimisation_button)

            setOnButtonClickListener {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    longToast(R.string.error_battery_optimisation_activity_not_found)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private class FingerprintGestureSupportSlide : AppIntroScrollableFragment() {

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_fingerprint_gesture_support_title)

            imageDrawable = drawable(R.drawable.ic_baseline_fingerprint_64)
            backgroundColor = color(R.color.orange)

            ServiceLocator.fingerprintMapRepository(requireContext())
                .fingerprintGesturesAvailable.observe(viewLifecycleOwner, { available ->
                    when (available) {
                        true -> gesturesSupportedLayout()
                        false -> gesturesUnsupportedLayout()
                        null -> supportedUnknownLayout()
                    }
                })
        }
    }

    private fun FragmentAppIntroSlideBinding.gesturesSupportedLayout() {
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_supported)

        buttonText = null
    }

    private fun FragmentAppIntroSlideBinding.supportedUnknownLayout() {
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_supported_unknown)

        buttonText = str(R.string.showcase_fingerprint_gesture_support_button)

        setOnButtonClickListener {
            AccessibilityUtils.enableService(requireContext())
        }
    }

    private fun FragmentAppIntroSlideBinding.gesturesUnsupportedLayout() {
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_not_supported)

        buttonText = null
    }
}

private class DexSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_dex_mode_supported_title)
            description = str(R.string.showcase_dex_mode_supported_message)
            imageDrawable = drawable(R.drawable.ic_dock_white_64dp)
            backgroundColor = color(R.color.orange)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private class DndAccessSlide : AppIntroScrollableFragment() {

    private val requestAccessNotificationPolicy =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_dnd_access_title)
            description = str(R.string.showcase_dnd_access_description)

            imageDrawable = drawable(R.drawable.ic_outline_dnd_circle_outline_64)
            backgroundColor = color(R.color.red)

            buttonText = str(R.string.pos_grant)

            setOnButtonClickListener {
                PermissionUtils.requestAccessNotificationPolicy(requestAccessNotificationPolicy)
            }
        }
    }
}

private class ContributingSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_contributing_title)
            description = str(R.string.showcase_contributing_description)
            imageDrawable = drawable(R.drawable.ic_outline_feedback_64)
            backgroundColor = color(R.color.green)
        }
    }
}