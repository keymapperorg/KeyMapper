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
import splitties.systemservices.powerManager
import splitties.toast.longToast

/**
 * Created by sds100 on 07/07/2019.
 */

class AppIntroActivity : AppIntro2() {

    companion object {
        const val EXTRA_SLIDES = "${Constants.PACKAGE_NAME}.EXTRA_SLIDES"

        const val SLIDE_NOTE_FROM_DEVELOPER = "slide_note_from_developer"
        const val SLIDE_ACCESSIBILITY_SERVICE = "slide_accessibility_service"
        const val SLIDE_BATTERY_OPTIMISATION = "slide_battery_optimisation"
        const val SLIDE_FINGERPRINT_GESTURE_SUPPORT = "slide_fingerprint_gesture_support"
        const val SLIDE_DEX = "slide_dex"
        const val SLIDE_DO_NOT_DISTURB = "slide_dnd"
        const val SLIDE_CONTRIBUTING = "slide_contributing"
        const val SLIDE_SETUP_CHOSEN_DEVICES_IN_SETTINGS_AGAIN =
            "slide_setup_chosen_devices_in_settings_again"
    }

    private val slidesToShowIfValid by lazy {
        intent.getStringArrayExtra(EXTRA_SLIDES) ?: emptyArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        val slidesThatAreAdded = mutableListOf<String>()

        slidesToShowIfValid.forEach {
            if (addSlideIfValid(it)) {
                slidesThatAreAdded.add(it)
            }
        }

        if (slidesThatAreAdded.isEmpty()) {
            onDonePressed(null)
        }
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        globalPreferences.set(Keys.shownAppIntro, true)

        if (slidesToShowIfValid.contains(SLIDE_FINGERPRINT_GESTURE_SUPPORT)) {
            globalPreferences.set(Keys.approvedFingerprintFeaturePrompt, true)
        }

        if (slidesToShowIfValid.contains(SLIDE_SETUP_CHOSEN_DEVICES_IN_SETTINGS_AGAIN)) {
            globalPreferences.set(Keys.approvedSetupChosenDevicesAgain, true)
        }

        startActivity(Intent(this, HomeActivity::class.java))

        finish()
    }

    /**
     * @return whether the slide was added
     */
    private fun addSlideIfValid(slide: String): Boolean {
        when (slide) {
            SLIDE_NOTE_FROM_DEVELOPER -> {
                addSlide(NoteFromDeveloperSlide())
                return true
            }
            SLIDE_ACCESSIBILITY_SERVICE -> {
                addSlide(AccessibilityServiceSlide())
                return true
            }
            SLIDE_BATTERY_OPTIMISATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && !powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)
                ) {
                    addSlide(BatteryOptimisationSlide())
                    return true
                }
            }

            SLIDE_FINGERPRINT_GESTURE_SUPPORT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            ) {
                addSlide(FingerprintGestureSupportSlide())
                return true
            }

            SLIDE_DEX -> if (DexUtils.isDexSupported(this)) {
                addSlide(DexSlide())
                return true
            }

            SLIDE_DO_NOT_DISTURB -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !PermissionUtils.isPermissionGranted(
                    this,
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY
                )
            ) {

                addSlide(DndAccessSlide())
                return true
            }

            SLIDE_CONTRIBUTING -> {
                addSlide(ContributingSlide())
                return true
            }

            SLIDE_SETUP_CHOSEN_DEVICES_IN_SETTINGS_AGAIN ->
                if (globalPreferences.getFlow(Keys.bluetoothDevicesThatShowImePicker)
                        .firstBlocking()?.isNotEmpty() == true
                    || globalPreferences.getFlow(Keys.bluetoothDevicesThatToggleKeyboard)
                        .firstBlocking()?.isNotEmpty() == true
                ) {
                    addSlide(SetupChosenDevicesAgainSlide())
                    return true
                }
        }

        return false
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
            )
        ) {
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

class SetupChosenDevicesAgainSlide : AppIntroScrollableFragment() {
    override fun onBind(binding: FragmentAppIntroSlideBinding) {
        binding.apply {
            title = str(R.string.showcase_setup_chosen_devices_again_title)
            description = str(R.string.showcase_setup_chosen_devices_again_message)
            imageDrawable = drawable(R.drawable.ic_baseline_devices_other_64)
            backgroundColor = color(R.color.blue)
        }

        viewLoaded()
    }
}