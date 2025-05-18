package io.github.sds100.keymapper.base.system.intents

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.system.intents.BoolArrayExtraType
import io.github.sds100.keymapper.system.intents.BoolExtraType
import io.github.sds100.keymapper.system.intents.BoolIntentExtraListItem
import io.github.sds100.keymapper.system.intents.ByteArrayExtraType
import io.github.sds100.keymapper.system.intents.ByteExtraType
import io.github.sds100.keymapper.system.intents.CharArrayExtraType
import io.github.sds100.keymapper.system.intents.CharExtraType
import io.github.sds100.keymapper.system.intents.DoubleArrayExtraType
import io.github.sds100.keymapper.system.intents.DoubleExtraType
import io.github.sds100.keymapper.system.intents.FloatArrayExtraType
import io.github.sds100.keymapper.system.intents.FloatExtraType
import io.github.sds100.keymapper.system.intents.GenericIntentExtraListItem
import io.github.sds100.keymapper.system.intents.IntArrayExtraType
import io.github.sds100.keymapper.system.intents.IntExtraType
import io.github.sds100.keymapper.system.intents.IntentExtraListItem
import io.github.sds100.keymapper.system.intents.IntentExtraModel
import io.github.sds100.keymapper.system.intents.IntentTarget
import io.github.sds100.keymapper.system.intents.LongArrayExtraType
import io.github.sds100.keymapper.system.intents.LongExtraType
import io.github.sds100.keymapper.system.intents.ShortArrayExtraType
import io.github.sds100.keymapper.system.intents.ShortExtraType
import io.github.sds100.keymapper.system.intents.StringArrayExtraType
import io.github.sds100.keymapper.system.intents.StringExtraType
import io.github.sds100.keymapper.base.utils.DialogResponse
import io.github.sds100.keymapper.base.utils.MultiChoiceItem
import io.github.sds100.keymapper.base.utils.NavDestination
import io.github.sds100.keymapper.base.utils.NavigationViewModel
import io.github.sds100.keymapper.base.utils.NavigationViewModelImpl
import io.github.sds100.keymapper.base.utils.PopupUi
import io.github.sds100.keymapper.base.utils.PopupViewModel
import io.github.sds100.keymapper.base.utils.PopupViewModelImpl
import io.github.sds100.keymapper.base.utils.ResourceProvider
import io.github.sds100.keymapper.base.utils.navigate
import io.github.sds100.keymapper.base.utils.showPopup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.withFlag
import javax.inject.Inject

@HiltViewModel
class ConfigIntentViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    companion object {
        private val EXTRA_TYPES = arrayOf(
            BoolExtraType,
            BoolArrayExtraType(),
            IntExtraType(),
            IntArrayExtraType(),
            StringExtraType(),
            StringArrayExtraType(),
            LongExtraType(),
            LongArrayExtraType(),
            ByteExtraType(),
            ByteArrayExtraType(),
            DoubleExtraType(),
            DoubleArrayExtraType(),
            CharExtraType(),
            CharArrayExtraType(),
            FloatExtraType(),
            FloatArrayExtraType(),
            ShortExtraType(),
            ShortArrayExtraType(),
        )

        val availableIntentFlags: List<Pair<Int, String>> =
            sequence {
                yield(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT to "FLAG_ACTIVITY_BROUGHT_TO_FRONT")
                yield(Intent.FLAG_ACTIVITY_CLEAR_TASK to "FLAG_ACTIVITY_CLEAR_TASK")
                yield(Intent.FLAG_ACTIVITY_CLEAR_TOP to "FLAG_ACTIVITY_CLEAR_TOP")

                yield(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET to "FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET")

                yield(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS to "FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS")
                yield(Intent.FLAG_ACTIVITY_FORWARD_RESULT to "FLAG_ACTIVITY_FORWARD_RESULT")
                yield(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY to "FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    yield(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT to "FLAG_ACTIVITY_LAUNCH_ADJACENT")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    yield(Intent.FLAG_ACTIVITY_MATCH_EXTERNAL to "FLAG_ACTIVITY_MATCH_EXTERNAL")
                }

                yield(Intent.FLAG_ACTIVITY_MULTIPLE_TASK to "FLAG_ACTIVITY_MULTIPLE_TASK")

                yield(Intent.FLAG_ACTIVITY_NEW_DOCUMENT to "FLAG_ACTIVITY_NEW_DOCUMENT")

                yield(Intent.FLAG_ACTIVITY_NEW_TASK to "FLAG_ACTIVITY_NEW_TASK")
                yield(Intent.FLAG_ACTIVITY_NO_ANIMATION to "FLAG_ACTIVITY_NO_ANIMATION")
                yield(Intent.FLAG_ACTIVITY_NO_HISTORY to "FLAG_ACTIVITY_NO_HISTORY")
                yield(Intent.FLAG_ACTIVITY_NO_USER_ACTION to "FLAG_ACTIVITY_NO_USER_ACTION")
                yield(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP to "FLAG_ACTIVITY_PREVIOUS_IS_TOP")
                yield(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT to "FLAG_ACTIVITY_REORDER_TO_FRONT")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    yield(Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT to "FLAG_ACTIVITY_REQUIRE_DEFAULT")
                    yield(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER to "FLAG_ACTIVITY_REQUIRE_NON_BROWSER")
                }

                yield(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED to "FLAG_ACTIVITY_RESET_TASK_IF_NEEDED")

                yield(Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS to "FLAG_ACTIVITY_RETAIN_IN_RECENTS")

                yield(Intent.FLAG_ACTIVITY_SINGLE_TOP to "FLAG_ACTIVITY_SINGLE_TOP")
                yield(Intent.FLAG_ACTIVITY_TASK_ON_HOME to "FLAG_ACTIVITY_TASK_ON_HOME")
                yield(Intent.FLAG_DEBUG_LOG_RESOLUTION to "FLAG_DEBUG_LOG_RESOLUTION")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    yield(Intent.FLAG_DIRECT_BOOT_AUTO to "FLAG_DIRECT_BOOT_AUTO")
                }

                yield(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES to "FLAG_EXCLUDE_STOPPED_PACKAGES")
                yield(Intent.FLAG_FROM_BACKGROUND to "FLAG_FROM_BACKGROUND")

                yield(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION to "FLAG_GRANT_PERSISTABLE_URI_PERMISSION")

                yield(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION to "FLAG_GRANT_PREFIX_URI_PERMISSION")

                yield(Intent.FLAG_GRANT_READ_URI_PERMISSION to "FLAG_GRANT_READ_URI_PERMISSION")
                yield(Intent.FLAG_GRANT_WRITE_URI_PERMISSION to "FLAG_GRANT_WRITE_URI_PERMISSION")
                yield(Intent.FLAG_INCLUDE_STOPPED_PACKAGES to "FLAG_INCLUDE_STOPPED_PACKAGES")
                yield(Intent.FLAG_RECEIVER_FOREGROUND to "FLAG_RECEIVER_FOREGROUND")

                yield(Intent.FLAG_RECEIVER_NO_ABORT to "FLAG_RECEIVER_NO_ABORT")

                yield(Intent.FLAG_RECEIVER_REGISTERED_ONLY to "FLAG_RECEIVER_REGISTERED_ONLY")
                yield(Intent.FLAG_RECEIVER_REPLACE_PENDING to "FLAG_RECEIVER_REPLACE_PENDING")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    yield(Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS to "FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS")
                }
            }.toList()
    }

    private val target = MutableStateFlow(IntentTarget.BROADCAST_RECEIVER)
    val checkedTarget: StateFlow<Int> = target.map {
        when (it) {
            IntentTarget.ACTIVITY -> R.id.radioButtonTargetActivity
            IntentTarget.BROADCAST_RECEIVER -> R.id.radioButtonTargetBroadcastReceiver
            IntentTarget.SERVICE -> R.id.radioButtonTargetService
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, R.id.radioButtonTargetBroadcastReceiver)

    val description = MutableStateFlow("")
    val action = MutableStateFlow("")
    val categoriesString = MutableStateFlow("")

    val data: MutableStateFlow<String> = MutableStateFlow("")
    val targetPackage: MutableStateFlow<String> = MutableStateFlow("")
    val targetClass: MutableStateFlow<String> = MutableStateFlow("")

    val flagsString: MutableStateFlow<String> = MutableStateFlow("")
    private val flags: Flow<Int> = flagsString.map {
        it.toIntOrNull() ?: 0
    }

    private val extras: MutableStateFlow<List<IntentExtraModel>> =
        MutableStateFlow(emptyList())

    val extraListItems: StateFlow<List<IntentExtraListItem>> = extras.map { extras ->
        extras.map { it.toListItem() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val isValid: Flow<Boolean> = combine(description, extras) { description, extras ->
        if (description.isEmpty()) {
            return@combine false
        }

        if (extras.any { !it.isValidValue || it.name.isEmpty() }) {
            return@combine false
        }

        true
    }

    val isDoneButtonEnabled = isValid.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false,
    )

    private val _returnResult = MutableSharedFlow<ConfigIntentResult>()
    val returnResult = _returnResult.asSharedFlow()

    fun setActivityTargetChecked(isChecked: Boolean) {
        if (isChecked) {
            target.value = IntentTarget.ACTIVITY
        }
    }

    fun setBroadcastReceiverTargetChecked(isChecked: Boolean) {
        if (isChecked) {
            target.value = IntentTarget.BROADCAST_RECEIVER
        }
    }

    fun setServiceTargetChecked(isChecked: Boolean) {
        if (isChecked) {
            target.value = IntentTarget.SERVICE
        }
    }

    fun onChooseActivityClick() {
        viewModelScope.launch {
            val activityInfo = navigate("choose_activity_for_intent", NavDestination.ChooseActivity)
                ?: return@launch

            setActivity(activityInfo)
        }
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val intent = Intent()

            if (this@ConfigIntentViewModel.action.value.isNotEmpty()) {
                intent.action = this@ConfigIntentViewModel.action.value
            }

            val categoriesStringValue = categoriesString.value

            if (categoriesStringValue.isNotBlank()) {
                categoriesStringValue
                    .split(',')
                    .map { it.trim() }
                    .forEach { intent.addCategory(it) }
            }

            if (this@ConfigIntentViewModel.data.value.isNotEmpty()) {
                intent.data = this@ConfigIntentViewModel.data.value.toUri()
            }

            if (this@ConfigIntentViewModel.flagsString.value.isNotBlank()) {
                intent.flags = flags.first()
            }

            if (this@ConfigIntentViewModel.targetPackage.value.isNotEmpty()) {
                intent.`package` = this@ConfigIntentViewModel.targetPackage.value

                if (targetClass.value.isNotEmpty()) {
                    intent.setClassName(targetPackage.value, targetClass.value)
                }
            }

            this@ConfigIntentViewModel.extras.value.forEach { extraModel ->
                if (extraModel.name.isEmpty()) return@forEach
                if (extraModel.parsedValue == null) return@forEach

                extraModel.type.putInIntent(intent, extraModel.name, extraModel.value)
            }

            val uri = intent.toUri(0)

            _returnResult.emit(
                ConfigIntentResult(
                    uri = uri,
                    target = target.value,
                    description = description.value,
                    extras = extras.value,
                ),
            )
        }
    }

    fun setExtraName(uid: String, name: String) {
        extras.value = extras.value.map {
            if (it.uid == uid && it.name != name) {
                return@map it.copy(name = name)
            }

            it
        }
    }

    fun setExtraValue(uid: String, value: String) {
        extras.value = extras.value.map {
            if (it.uid == uid && it.value != value) {
                return@map it.copy(value = value)
            }

            it
        }
    }

    fun removeExtra(uid: String) {
        extras.value = extras.value.toMutableList().apply {
            removeAll { it.uid == uid }
        }
    }

    fun onAddExtraClick() {
        viewModelScope.launch {
            val items = EXTRA_TYPES.map { it to getString(it.labelStringRes) }

            val dialog = PopupUi.SingleChoice(items)

            val extraType = showPopup("add_extra", dialog) ?: return@launch

            val modelValue = when (extraType) {
                is BoolExtraType -> "true"
                else -> ""
            }

            val model = IntentExtraModel(extraType, value = modelValue)

            extras.value = extras.value.plus(model)
        }
    }

    fun onShowExtraExampleClick(listItem: IntentExtraListItem) {
        viewModelScope.launch {
            if (listItem is GenericIntentExtraListItem) {
                val dialog = PopupUi.Ok(message = listItem.exampleString)
                showPopup("extra_example", dialog)
            }
        }
    }

    fun onShowCategoriesExampleClick() {
        viewModelScope.launch {
            val dialog = PopupUi.Ok(message = getString(R.string.intent_categories_example))
            showPopup("categories_example", dialog)
        }
    }

    fun showFlagsDialog() {
        viewModelScope.launch {
            val oldSelectedFlags: Int = flags.first()

            val dialogItems = availableIntentFlags.map { pair ->
                val intentFlagInt = pair.first
                val intentFlagText = pair.second

                val isChecked = oldSelectedFlags.hasFlag(intentFlagInt)

                MultiChoiceItem(intentFlagInt, intentFlagText, isChecked)
            }

            val dialog = PopupUi.MultiChoice(items = dialogItems)

            val selectedFlags = showPopup("set_flags", dialog) ?: return@launch

            var newFlags = 0

            selectedFlags.forEach {
                newFlags = newFlags.withFlag(it)
            }

            flagsString.value = newFlags.toString()
        }
    }

    fun loadResult(result: ConfigIntentResult) {
        val intent = Intent.parseUri(result.uri, 0)

        description.value = result.description
        target.value = result.target
        action.value = intent.action ?: ""

        categoriesString.value = intent.categories?.joinToString() ?: ""
        data.value = intent.dataString ?: ""
        targetPackage.value = intent.`package` ?: ""
        targetClass.value = intent.component?.className ?: ""

        if (intent.flags != 0) {
            flagsString.value = intent.flags.toString()
        } else {
            flagsString.value = ""
        }

        val extrasBundle = intent.extras ?: Bundle.EMPTY

        val intentExtras: MutableList<IntentExtraModel> = result.extras.toMutableList()

        /**
         * See issue #1171. Until version 2.6.1, the extras were assumed to be stored in
         * the URI representation of the intent. But the array extras were never saved.
         * So to maintain backwards compatibility with old intent actions that stored the arrays
         * in the URI, also add the extras from the URI.
         */
        for (key in extrasBundle.keySet()) {
            // skip the extra if the list already contains it.
            if (intentExtras.any { it.name == key }) {
                continue
            }

            val value = extrasBundle.get(key) ?: continue

            val extraType = when (value) {
                is Boolean -> BoolExtraType
                is BooleanArray -> BoolArrayExtraType()
                is Int -> IntExtraType()
                is IntArray -> IntArrayExtraType()
                is Long -> LongExtraType()
                is LongArrayExtraType -> LongArrayExtraType()
                is Byte -> ByteExtraType()
                is ByteArrayExtraType -> ByteArrayExtraType()
                is Double -> DoubleExtraType()
                is DoubleArray -> DoubleArrayExtraType()
                is Float -> FloatExtraType()
                is FloatArray -> FloatArrayExtraType()
                is Short -> ShortExtraType()
                is ShortArray -> ShortArrayExtraType()
                is String -> StringExtraType()
                is Array<*> -> StringArrayExtraType()
                else -> throw IllegalArgumentException("Don't know how to convert this extra (${value.javaClass.name}) to an IntentExtraType")
            }

            val extra = IntentExtraModel(
                type = extraType,
                name = key,
                value = value.toString(),
            )

            intentExtras.add(extra)
        }

        this.extras.value = intentExtras
    }

    fun setActivity(activityInfo: ActivityInfo) {
        targetPackage.value = activityInfo.packageName
        targetClass.value = activityInfo.activityName
    }

    fun onShowFlagsExampleClick() {
        viewModelScope.launch {
            val dialog = PopupUi.Dialog(
                message = getString(R.string.intent_flags_example),
                positiveButtonText = getString(R.string.pos_ok),
                neutralButtonText = getString(R.string.neutral_intent_docs),
            )

            val response = showPopup("flags_example", dialog) ?: return@launch

            if (response == DialogResponse.NEUTRAL) {
                showPopup(
                    "url_intent_flags",
                    PopupUi.OpenUrl(getString(R.string.url_intent_set_flags_help)),
                )
            }
        }
    }

    private fun IntentExtraModel.toListItem(): IntentExtraListItem = when (type) {
        is BoolExtraType -> BoolIntentExtraListItem(
            uid,
            name,
            parsedValue?.let { it as Boolean } ?: true,
            isValidValue,
        )

        else -> {
            val inputType = when (type) {
                is IntExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                is IntArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                is LongExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                is LongArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                is ByteExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                is ByteArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                is DoubleExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL

                is DoubleArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or
                        InputType.TYPE_CLASS_TEXT

                is FloatExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL

                is FloatArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or
                        InputType.TYPE_CLASS_TEXT

                is ShortExtraType ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                is ShortArrayExtraType ->
                    InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                else -> InputType.TYPE_CLASS_TEXT
            }

            GenericIntentExtraListItem(
                uid,
                getString(type.labelStringRes),
                name,
                value,
                isValidValue,
                getString(type.exampleStringRes),
                inputType,
            )
        }
    }
}
