package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.graphics.Color
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ActionUiHelper
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.mappings.keymaps.DisplayKeyMapUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SliderListItem
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.ui.SliderModel
import io.github.sds100.keymapper.util.ui.SliderStepSizes
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 29/11/20.
 */
class ConfigTriggerOptionsViewModel(
    private val coroutineScope: CoroutineScope,
    private val config: ConfigKeyMapUseCase,
    private val displayUseCase: DisplayKeyMapUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val ID_LONG_PRESS_DELAY = "long_press_delay"
        private const val ID_DOUBLE_PRESS_DELAY = "double_press_delay"
        private const val ID_SEQUENCE_TRIGGER_TIMEOUT = "sequence_trigger_timeout"
        private const val ID_VIBRATE_DURATION = "vibrate_duration"
        private const val ID_VIBRATE = "vibrate"
        private const val ID_LONG_PRESS_DOUBLE_VIBRATION = "long_press_double_vibration"
        private const val ID_SCREEN_OFF_TRIGGER = "screen_off_trigger"
        private const val ID_TRIGGER_FROM_OTHER_APPS = "trigger_from_other_apps"
        private const val ID_SHOW_TOAST = "show_toast"
    }

    private val actionUiHelper = ActionUiHelper(displayUseCase, resourceProvider)
    private val _state by lazy { MutableStateFlow(buildUiState(State.Loading)) }
    val state by lazy { _state.asStateFlow() }

    init {
        coroutineScope.launch {
            config.keyMap.collectLatest { state ->
                _state.value = withContext(Dispatchers.Default) {
                    buildUiState(state)
                }
            }
        }
    }

    fun setSliderValue(id: String, value: Defaultable<Int>) {
        when (id) {
            ID_VIBRATE_DURATION -> config.setVibrationDuration(value)
            ID_LONG_PRESS_DELAY -> config.setLongPressDelay(value)
            ID_DOUBLE_PRESS_DELAY -> config.setDoublePressDelay(value)
            ID_SEQUENCE_TRIGGER_TIMEOUT -> config.setSequenceTriggerTimeout(value)
        }
    }

    fun setCheckboxValue(id: String, value: Boolean) {
        when (id) {
            ID_VIBRATE -> config.setVibrateEnabled(value)
            ID_TRIGGER_FROM_OTHER_APPS -> config.setTriggerFromOtherAppsEnabled(value)
            ID_LONG_PRESS_DOUBLE_VIBRATION -> config.setLongPressDoubleVibrationEnabled(value)
            ID_SHOW_TOAST -> config.setShowToastEnabled(value)
            ID_SCREEN_OFF_TRIGGER -> config.setTriggerWhenScreenOff(value)
        }
    }

    fun createLauncherShortcut() {
        coroutineScope.launch {
            val mapping = config.keyMap.firstOrNull()?.dataOrNull() ?: return@launch
            val keyMapUid = mapping.uid

            val key = "create_launcher_shortcut"
            val defaultShortcutName: String
            val icon: Drawable?

            if (mapping.actionList.size == 1) {
                val action = mapping.actionList.first().data
                defaultShortcutName = actionUiHelper.getTitle(
                    action,
                    showDeviceDescriptors = false,
                )

                val iconInfo = actionUiHelper.getDrawableIcon(action)

                if (iconInfo == null) {
                    icon = null
                } else {
                    when (iconInfo.tintType) {
                        // Always set the icon as black if it needs to be on surface because the
                        // background is white. Also, getting the colorOnSurface attribute
                        // from the application context doesn't seem to work correctly.
                        TintType.OnSurface -> iconInfo.drawable.setTint(Color.BLACK)
                        is TintType.Color -> iconInfo.drawable.setTint(iconInfo.tintType.color)
                        else -> {}
                    }

                    icon = iconInfo.drawable
                }
            } else {
                defaultShortcutName = ""
                icon = null
            }

            val shortcutName = showPopup(
                key,
                PopupUi.Text(
                    getString(R.string.hint_shortcut_name),
                    allowEmpty = false,
                    text = defaultShortcutName,
                ),
            ) ?: return@launch

            val result = createKeyMapShortcut.pinShortcut(keyMapUid, shortcutName, icon)

            result.onFailure { error ->
                val snackBar = PopupUi.SnackBar(
                    message = error.getFullMessage(this@ConfigTriggerOptionsViewModel),
                )

                showPopup("create_shortcut_result", snackBar)
            }
        }
    }

    private fun buildUiState(configState: State<KeyMap>): State<List<ListItem>> = configState.mapData { keyMap ->
        sequence {
            val trigger = keyMap.trigger
            val keyMapUid = keyMap.uid

            yield(
                TriggerFromOtherAppsListItem(
                    id = ID_TRIGGER_FROM_OTHER_APPS,
                    isEnabled = trigger.triggerFromOtherApps,
                    keyMapUid = keyMapUid,
                    label = getString(R.string.flag_trigger_from_other_apps),
                    isCreateLauncherShortcutButtonEnabled = createKeyMapShortcut.isSupported,
                ),
            )

            yield(
                CheckBoxListItem(
                    id = ID_SHOW_TOAST,
                    isChecked = trigger.showToast,
                    label = getString(R.string.flag_show_toast),
                ),
            )

            if (trigger.isDetectingWhenScreenOffAllowed()) {
                yield(
                    CheckBoxListItem(
                        id = ID_SCREEN_OFF_TRIGGER,
                        isChecked = trigger.screenOffTrigger,
                        label = getString(R.string.flag_detect_triggers_screen_off),
                    ),
                )
            }

            if (trigger.isVibrateAllowed()) {
                yield(
                    CheckBoxListItem(
                        id = ID_VIBRATE,
                        isChecked = trigger.vibrate,
                        label = getString(R.string.flag_vibrate),
                    ),
                )
            }

            if (trigger.isLongPressDoubleVibrationAllowed()) {
                yield(
                    CheckBoxListItem(
                        id = ID_LONG_PRESS_DOUBLE_VIBRATION,
                        isChecked = trigger.longPressDoubleVibration,
                        label = getString(R.string.flag_long_press_double_vibration),
                    ),
                )
            }

            if (trigger.isChangingVibrationDurationAllowed()) {
                yield(
                    SliderListItem(
                        id = ID_VIBRATE_DURATION,
                        label = getString(R.string.extra_label_vibration_duration),
                        SliderModel(
                            value = Defaultable.create(trigger.vibrateDuration),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.VIBRATION_DURATION,
                            max = SliderMaximums.VIBRATION_DURATION,
                            stepSize = SliderStepSizes.VIBRATION_DURATION,
                        ),
                    ),
                )
            }

            if (trigger.isChangingLongPressDelayAllowed()) {
                yield(
                    SliderListItem(
                        id = ID_LONG_PRESS_DELAY,
                        label = getString(R.string.extra_label_long_press_delay_timeout),
                        SliderModel(
                            value = Defaultable.create(trigger.longPressDelay),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.TRIGGER_LONG_PRESS_DELAY,
                            max = SliderMaximums.TRIGGER_LONG_PRESS_DELAY,
                            stepSize = SliderStepSizes.TRIGGER_LONG_PRESS_DELAY,
                        ),
                    ),
                )
            }

            if (trigger.isChangingDoublePressDelayAllowed()) {
                yield(
                    SliderListItem(
                        id = ID_DOUBLE_PRESS_DELAY,
                        label = getString(R.string.extra_label_double_press_delay_timeout),
                        SliderModel(
                            value = Defaultable.create(trigger.doublePressDelay),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.TRIGGER_DOUBLE_PRESS_DELAY,
                            max = SliderMaximums.TRIGGER_DOUBLE_PRESS_DELAY,
                            stepSize = SliderStepSizes.TRIGGER_DOUBLE_PRESS_DELAY,
                        ),
                    ),
                )
            }

            if (trigger.isChangingSequenceTriggerTimeoutAllowed()) {
                yield(
                    SliderListItem(
                        id = ID_SEQUENCE_TRIGGER_TIMEOUT,
                        label = getString(R.string.extra_label_sequence_trigger_timeout),
                        SliderModel(
                            value = Defaultable.create(trigger.sequenceTriggerTimeout),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
                            max = SliderMaximums.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
                            stepSize = SliderStepSizes.TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
                        ),
                    ),
                )
            }
        }.toList()
    }
}
