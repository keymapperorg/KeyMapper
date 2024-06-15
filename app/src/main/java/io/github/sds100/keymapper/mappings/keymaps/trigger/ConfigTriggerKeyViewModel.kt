package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.DefaultOptionsUiState
import io.github.sds100.keymapper.mappings.OptionsViewModel
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapUseCase
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.RadioButtonTripleListItem
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Created by sds100 on 12/04/2021.
 */
class ConfigTriggerKeyViewModel(
    coroutineScope: CoroutineScope,
    val config: ConfigKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : OptionsViewModel,
    ResourceProvider by resourceProvider {
    companion object {
        private const val ID_DONT_CONSUME_KEY_EVENT = "consume_key_event"
        private const val ID_SHORT_PRESS = "short_press"
        private const val ID_LONG_PRESS = "long_press"
        private const val ID_DOUBLE_PRESS = "double_press"
    }

    private val triggerKeyUid = MutableStateFlow<String?>(null)

    override val state = combine(config.mapping, triggerKeyUid) { mapping, keyUid ->
        when {
            mapping is State.Data -> {
                val key = mapping.data.trigger.keys.find { it.uid == keyUid }
                    ?: return@combine DefaultOptionsUiState(showProgressBar = true)

                DefaultOptionsUiState(
                    showProgressBar = false,
                    listItems = createListItems(mapping.data.trigger.mode, key),
                )
            }

            else -> DefaultOptionsUiState(showProgressBar = true)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            DefaultOptionsUiState(showProgressBar = true),
        )

    override fun setRadioButtonValue(id: String, value: Boolean) {
        val keyUid = triggerKeyUid.value ?: return

        when (id) {
            ID_SHORT_PRESS -> config.setTriggerKeyClickType(keyUid, ClickType.SHORT_PRESS)
            ID_LONG_PRESS -> config.setTriggerKeyClickType(keyUid, ClickType.LONG_PRESS)
            ID_DOUBLE_PRESS -> config.setTriggerKeyClickType(keyUid, ClickType.DOUBLE_PRESS)
        }
    }

    override fun setSliderValue(id: String, value: Defaultable<Int>) {
    }

    override fun setCheckboxValue(id: String, value: Boolean) {
        val keyUid = triggerKeyUid.value ?: return

        when (id) {
            ID_DONT_CONSUME_KEY_EVENT -> config.setTriggerKeyConsumeKeyEvent(keyUid, !value)
        }
    }

    fun setTriggerKeyToConfigure(uid: String) {
        triggerKeyUid.value = uid
    }

    private fun createListItems(triggerMode: TriggerMode, key: TriggerKey): List<ListItem> =
        sequence {
            yield(
                CheckBoxListItem(
                    id = ID_DONT_CONSUME_KEY_EVENT,
                    isChecked = !key.consumeKeyEvent,
                    label = getString(R.string.flag_dont_override_default_action),
                ),
            )

            if (triggerMode is TriggerMode.Sequence) {
                yield(
                    RadioButtonTripleListItem(
                        id = "click_type",
                        header = getString(R.string.header_click_type),

                        leftButtonId = ID_SHORT_PRESS,
                        leftButtonText = getString(R.string.clicktype_short_press),
                        leftButtonChecked = key.clickType == ClickType.SHORT_PRESS,

                        centerButtonId = ID_LONG_PRESS,
                        centerButtonText = getString(R.string.clicktype_long_press),
                        centerButtonChecked = key.clickType == ClickType.LONG_PRESS,

                        rightButtonId = ID_DOUBLE_PRESS,
                        rightButtonText = getString(R.string.clicktype_double_press),
                        rightButtonChecked = key.clickType == ClickType.DOUBLE_PRESS,
                    ),
                )
            }
        }.toList()
}
