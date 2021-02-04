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
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.databinding.FragmentAppIntroSlideBinding
import io.github.sds100.keymapper.globalPreferences
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

        addSlide(ShizukuSlide())

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
            && !PermissionUtils.isPermissionGranted(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {

            addSlide(DndAccessSlide())
        }

        addSlide(ContributingSlide())
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        globalPreferences.set(Keys.shownAppIntro, true)
        globalPreferences.set(Keys.approvedFingerprintFeaturePrompt, true)

        startActivity(Intent(this, HomeActivity::class.java))

        finish()
    }
}

class NoteFromDeveloperSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_note_from_the_developer_title)
            description = str(R.string.showcase_note_from_the_developer_description)
            imageDrawable = drawable(R.mipmap.ic_launcher_round)
            backgroundColor = color(R.color.red)
        }

        viewLoaded()
    }
}

class ShizukuSlide : AppIntroScrollableFragment() {
    companion object {
        const val REQ_SHIZUKU_PERMISSION = 1000
    }

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = getString(R.string.showcase_shizuku_title)
            description = getString(R.string.showcase_shizuku_message)

            buttonText1 = getString(R.string.showcase_shizuku_grant)
            buttonText2 = getString(R.string.showcase_shizuku_install)

            imageDrawable = drawable(R.drawable.ic_outline_error_outline_64)
            backgroundColor = color(R.color.orange)

            setOnButton1ClickListener {
                if (!PermissionUtils.hasShizukuPermission(requireContext())) {
                    PermissionUtils.requestShizukuPermission(requireContext(), REQ_SHIZUKU_PERMISSION)
                }
            }

            setOnButton2ClickListener {
                UrlUtils.openUrl(requireContext(), "https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
            }
        }

        viewLoaded()
    }
}

class AccessibilityServiceSlide : AppIntroScrollableFragment() {
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

        viewLoaded()
    }

    private fun FragmentAppIntroSlideBinding.serviceDisabledLayout() {
        title = str(R.string.showcase_accessibility_service_title_disabled)
        description = str(R.string.showcase_accessibility_service_description_disabled)

        imageDrawable = drawable(R.drawable.ic_outline_error_outline_64)
        backgroundColor = color(R.color.purple)

        buttonText1 = str(R.string.enable)

        setOnButton1ClickListener {
            AccessibilityUtils.enableService(requireContext())
        }
    }

    private fun FragmentAppIntroSlideBinding.serviceEnabledLayout() {
        title = str(R.string.showcase_accessibility_service_title_enabled)
        description = str(R.string.showcase_accessibility_service_description_enabled)

        imageDrawable = drawable(R.drawable.ic_baseline_check_64)
        backgroundColor = color(R.color.purple)

        buttonText1 = null
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class BatteryOptimisationSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_disable_battery_optimisation_title)

            imageDrawable = drawable(R.drawable.ic_battery_std_white_64dp)
            backgroundColor = color(R.color.blue)

            invalidate()
        }

        viewLoaded()
    }

    override fun onResume() {
        super.onResume()

        viewLifecycleScope.launchWhenResumed {
            invalidate()
        }
    }

    private fun invalidate() {
        if (powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {
            binding.offLayout()
        } else {
            binding.onLayout()
        }
    }

    private fun FragmentAppIntroSlideBinding.offLayout() {
        description = str(R.string.showcase_disable_battery_optimisation_message_good)
        buttonText1 = str(R.string.showcase_disable_battery_optimisation_button_dont_kill_my_app)

        setOnButton1ClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_dont_kill_my_app))
        }

        buttonText2 = null
    }

    private fun FragmentAppIntroSlideBinding.onLayout() {
        description = str(R.string.showcase_disable_battery_optimisation_message_bad)
        buttonText1 = str(R.string.showcase_disable_battery_optimisation_button_turn_off)
        buttonText2 = str(R.string.showcase_disable_battery_optimisation_button_dont_kill_my_app)

        setOnButton1ClickListener {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                longToast(R.string.error_battery_optimisation_activity_not_found)
            }
        }

        setOnButton2ClickListener {
            UrlUtils.openUrl(requireContext(), str(R.string.url_dont_kill_my_app))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class FingerprintGestureSupportSlide : AppIntroScrollableFragment() {

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            backgroundColor = color(R.color.orange)

            ServiceLocator.fingerprintMapRepository(requireContext()).fingerprintGesturesAvailable
                .collectWhenResumed(viewLifecycleOwner) { available ->
                    when (available) {
                        true -> gesturesSupportedLayout()
                        false -> gesturesUnsupportedLayout()
                        null -> supportedUnknownLayout()
                    }

                    viewLoaded()
                }
        }
    }

    private fun FragmentAppIntroSlideBinding.gesturesSupportedLayout() {
        title =
            str(R.string.showcase_fingerprint_gesture_support_title_supported)
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_supported)
        imageDrawable = drawable(R.drawable.ic_baseline_check_64)

        buttonText1 = null
    }

    private fun FragmentAppIntroSlideBinding.supportedUnknownLayout() {
        title =
            str(R.string.showcase_fingerprint_gesture_support_title_supported_unknown)
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_supported_unknown)
        imageDrawable = drawable(R.drawable.ic_baseline_fingerprint_64)

        buttonText1 = str(R.string.showcase_fingerprint_gesture_support_button)

        setOnButton1ClickListener {
            AccessibilityUtils.enableService(requireContext())
        }
    }

    private fun FragmentAppIntroSlideBinding.gesturesUnsupportedLayout() {
        title =
            str(R.string.showcase_fingerprint_gesture_support_title_not_supported)
        description =
            str(R.string.showcase_fingerprint_gesture_support_message_not_supported)
        imageDrawable = drawable(R.drawable.ic_baseline_cross_64)

        buttonText1 = null
    }
}

class DexSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_dex_mode_supported_title)
            description = str(R.string.showcase_dex_mode_supported_message)
            imageDrawable = drawable(R.drawable.ic_dock_white_64dp)
            backgroundColor = color(R.color.orange)
        }

        viewLoaded()
    }
}

@RequiresApi(Build.VERSION_CODES.M)
class DndAccessSlide : AppIntroScrollableFragment() {

    private val requestAccessNotificationPolicy =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.backgroundColor = color(R.color.red)

        invalidate()

        viewLoaded()
    }

    override fun onResume() {
        super.onResume()

        invalidate()
    }

    private fun invalidate() {
        if (PermissionUtils.isPermissionGranted(
                requireContext(),
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            )) {
            binding.enabledLayout()
        } else {
            binding.disabledLayout()
        }
    }

    private fun FragmentAppIntroSlideBinding.disabledLayout() {

        title = str(R.string.showcase_dnd_access_title_disabled)
        description = str(R.string.showcase_dnd_access_description_disabled)
        buttonText1 = str(R.string.pos_grant)
        imageDrawable = drawable(R.drawable.ic_outline_dnd_circle_outline_64)

        setOnButton1ClickListener {
            PermissionUtils.requestAccessNotificationPolicy(requestAccessNotificationPolicy)
        }
    }

    private fun FragmentAppIntroSlideBinding.enabledLayout() {

        title = str(R.string.showcase_dnd_access_title_enabled)
        description = str(R.string.showcase_dnd_access_description_enabled)
        buttonText1 = null
        imageDrawable = drawable(R.drawable.ic_baseline_check_64)
    }
}

class ContributingSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_contributing_title)
            description = str(R.string.showcase_contributing_description)
            imageDrawable = drawable(R.drawable.ic_outline_feedback_64)
            backgroundColor = color(R.color.green)
        }

        viewLoaded()
    }
}