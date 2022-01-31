package io.github.sds100.keymapper.system.intents

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import androidx.core.net.toUri
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 01/01/21.
 */

class ConfigIntentViewModel(resourceProvider: ResourceProvider) : ViewModel(),
    ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    companion object {
        private val EXTRA_TYPES = arrayOf(
            BoolExtraType(),
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
            ShortArrayExtraType()
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

    val showChooseActivityButton: StateFlow<Boolean> = target.map {
        it == IntentTarget.ACTIVITY
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val data: MutableStateFlow<String> = MutableStateFlow("")
    val targetPackage: MutableStateFlow<String> = MutableStateFlow("")
    val targetClass: MutableStateFlow<String> = MutableStateFlow("")

    val flagsString: MutableStateFlow<String> = MutableStateFlow("")

    private val extras: MutableStateFlow<List<IntentExtraModel>> =
        MutableStateFlow(emptyList<IntentExtraModel>())

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
        viewModelScope, SharingStarted.Eagerly, false
    )

    private val _returnResult = MutableSharedFlow<ConfigIntentResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val _chooseActivity = MutableSharedFlow<Unit>()
    val chooseActivity = _chooseActivity.asSharedFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

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
            _chooseActivity.emit(Unit)
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
                intent.flags = flagsString.value.toIntOrNull() ?: 0
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
                    description = description.value
                )
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

            val response = showPopup("add_extra", dialog) ?: return@launch

            val model = IntentExtraModel(response.item)

            extras.value = extras.value.toMutableList().apply {
                add(model)
            }
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

            val dialog = PopupUi.MultiChoice(items = availableIntentFlags)

            val response = showPopup("set_flags", dialog) ?: return@launch

            var newFlags = 0

            response.items.forEach {
                newFlags = newFlags.withFlag(it)
            }

            flagsString.value = newFlags.toString()
        }
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
                neutralButtonText = getString(R.string.neutral_intent_docs)
            )

            val response = showPopup("flags_example", dialog) ?: return@launch

            if (response == DialogResponse.NEUTRAL) {
                _openUrl.emit(getString(R.string.url_intent_set_flags_help))
            }
        }
    }

    private fun IntentExtraModel.toListItem(): IntentExtraListItem {
        return when (type) {
            is BoolExtraType -> BoolIntentExtraListItem(
                uid,
                name,
                parsedValue?.let { it as Boolean } ?: true,
                isValidValue
            )

            else -> {
                val inputType = when (type) {
                    is IntExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                    is IntArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                    is LongExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                    is LongArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                    is ByteExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                    is ByteArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_TEXT

                    is DoubleExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or
                                InputType.TYPE_NUMBER_FLAG_DECIMAL

                    is DoubleArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            InputType.TYPE_NUMBER_FLAG_SIGNED or
                            InputType.TYPE_CLASS_TEXT

                    is FloatExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or
                                InputType.TYPE_NUMBER_FLAG_DECIMAL

                    is FloatArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL or
                            InputType.TYPE_NUMBER_FLAG_SIGNED or
                            InputType.TYPE_CLASS_TEXT

                    is ShortExtraType ->
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED

                    is ShortArrayExtraType -> InputType.TYPE_CLASS_NUMBER or
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
                    inputType
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ConfigIntentViewModel(resourceProvider) as T
        }
    }
}
