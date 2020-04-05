package io.github.sds100.keymapper.ui

import android.Manifest
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding
import io.github.sds100.keymapper.service.KeyMapperImeService
import io.github.sds100.keymapper.status
import io.github.sds100.keymapper.util.AccessibilityUtils
import io.github.sds100.keymapper.util.KeyboardUtils
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.StatusLayoutState
import io.github.sds100.keymapper.util.result.ImeServiceDisabled
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr

/**
 * Created by sds100 on 05/04/2020.
 */

@ExperimentalSplittiesApi
class ExpandableHeaderDelegate(
    private val requireActivity: () -> FragmentActivity,
    private val mLifecycleOwner: LifecycleOwner,
    binding: FragmentKeymapListBinding) {

    //status layout states
    private val mAccessibilityServiceState = MutableLiveData(StatusLayoutState.ERROR)
    private val mSecureSettingsState = MutableLiveData(StatusLayoutState.ERROR)
    private val mImeServiceState = MutableLiveData(StatusLayoutState.ERROR)
    private val mDndState = MutableLiveData(StatusLayoutState.ERROR)
    private val mCollapsedLayoutState = MutableLiveData(StatusLayoutState.ERROR)

    private val mExpanded = MutableLiveData(false)

    init {
        binding.apply {
            expanded = mExpanded

            buttonContract.setOnClickListener {
                mExpanded.value = false
            }

            layoutContracted.setOnClickListener {
                mExpanded.value = true
            }

            mExpanded.observe(mLifecycleOwner) {
                expandableLayout.isExpanded = it
            }

            setCollapsedState()
            populateExpandedStateList()
            observeStateChanges()
            updateStatusLayouts(binding)
        }
    }

    fun updateStatusLayouts(binding: FragmentKeymapListBinding) {
        if (AccessibilityUtils.isServiceEnabled(requireActivity())) {
            mAccessibilityServiceState.value = StatusLayoutState.POSITIVE

        } else {
            mAccessibilityServiceState.value = StatusLayoutState.ERROR
        }

        if (PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            mSecureSettingsState.value = StatusLayoutState.POSITIVE
        } else {
            mSecureSettingsState.value = StatusLayoutState.WARN
        }

        if (KeyMapperImeService.isServiceEnabled()) {
            mImeServiceState.value = StatusLayoutState.POSITIVE

        } else if (binding.viewModel?.keymapModelList?.value?.any { keymap ->
                keymap.actionList.any { it.error is ImeServiceDisabled }
            } == true) {

            mImeServiceState.value = StatusLayoutState.ERROR
        } else {
            mImeServiceState.value = StatusLayoutState.WARN
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PermissionUtils.isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY)) {
                mDndState.value = StatusLayoutState.POSITIVE
            } else {
                mDndState.value = StatusLayoutState.WARN
            }
        }
    }

    @ExperimentalSplittiesApi
    private fun FragmentKeymapListBinding.setCollapsedState() {

        collapsedLayoutPositiveState = appStr(R.string.everything_looks_good)
        collapsedLayoutWarningState = appStr(R.string.warning_the_app_will_work)
        collapsedLayoutErrorState = appStr(R.string.error_the_app_wont_work)
        collapsedLayoutState = mCollapsedLayoutState.value
    }

    private fun FragmentKeymapListBinding.populateExpandedStateList() {
        epoxyRecyclerViewStatusLayout.withModels {
            status {
                id("accessibility_service")
                positiveText(appStr(R.string.error_accessibility_service_enabled))
                errorText(appStr(R.string.error_accessibility_service_disabled))
                state(mAccessibilityServiceState.value)

                onFixClickListener { _ ->
                    AccessibilityUtils.enableService(requireActivity())
                }
            }

            status {
                id("secure_settings")
                positiveText(appStr(R.string.fixed_need_write_secure_settings_permission))
                warnText(appStr(R.string.warning_need_write_secure_settings_permission))
                state(mSecureSettingsState.value)

                onFixClickListener { _ ->
                    PermissionUtils.requestPermission(requireActivity(), Manifest.permission.WRITE_SECURE_SETTINGS) {
                        updateStatusLayouts(this@populateExpandedStateList)
                    }
                }
            }

            status {
                id("ime_service")
                positiveText(appStr(R.string.error_ime_service_enabled))
                warnText(appStr(R.string.error_ime_service_disabled_status_layout))
                errorText(appStr(R.string.error_ime_service_disabled))
                state(mImeServiceState.value)

                onFixClickListener { _ ->
                    KeyboardUtils.openImeSettings()
                }
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                status {
                    id("dnd_access")
                    positiveText(appStr(R.string.dnd_access_granted))
                    warnText(appStr(R.string.error_dnd_access_not_granted))
                    state(mDndState.value)

                    onFixClickListener { _ ->
                        PermissionUtils.requestPermission(requireActivity(),
                            Manifest.permission.ACCESS_NOTIFICATION_POLICY) {
                            updateStatusLayouts(this@populateExpandedStateList)
                        }
                    }
                }
            }
        }
    }

    private fun FragmentKeymapListBinding.observeStateChanges() {
        val states = listOf(
            mAccessibilityServiceState,
            mSecureSettingsState,
            mImeServiceState,
            mDndState
        )

        states.forEach { state ->
            state.observe(mLifecycleOwner) {
                epoxyRecyclerViewStatusLayout.requestModelBuild()

                when {
                    states.all { it.value == StatusLayoutState.POSITIVE } -> {
                        mExpanded.value = false
                        mCollapsedLayoutState.value = StatusLayoutState.POSITIVE
                    }

                    states.any { it.value == StatusLayoutState.ERROR } -> {
                        mExpanded.value = true
                        mCollapsedLayoutState.value = StatusLayoutState.ERROR
                    }

                    states.any { it.value == StatusLayoutState.WARN } -> {
                        mExpanded.value = false
                        mCollapsedLayoutState.value = StatusLayoutState.WARN
                    }
                }
            }
        }

        mCollapsedLayoutState.observe(mLifecycleOwner) {
            collapsedLayoutState = it
        }
    }
}