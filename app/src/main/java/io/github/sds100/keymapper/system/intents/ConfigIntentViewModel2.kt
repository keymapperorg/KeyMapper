package io.github.sds100.keymapper.system.intents

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import splitties.bitflags.withFlag
import javax.inject.Inject

@HiltViewModel
class ConfigIntentViewModel2 @Inject constructor() : ViewModel() {
    var state: ConfigIntentState by mutableStateOf(ConfigIntentState.DEFAULT)
        private set

    fun onDescriptionChange(text: String) {
        state = state.copy(description = text, descriptionError = validateDescription(text))
    }

    private fun validateDescription(text: String): IntentDescriptionError {
        return if (text.isEmpty()) {
            IntentDescriptionError.EMPTY
        } else {
            IntentDescriptionError.NONE
        }
    }

    fun onActionChange(text: String) {
        state = state.copy(action = text)
    }

    fun onPackageChange(text: String) {
        state = state.copy(componentPackage = text)
    }

    fun onClassChange(text: String) {
        state = state.copy(componentClass = text)
    }

    fun onDataChange(text: String) {
        state = state.copy(data = text)
    }

    fun onFlagsTextChange(text: String) {
        state = state.copy(flagsText = text, flagsError = validateFlags(text))
    }

    fun onChooseFlags(flags: Set<Int>) {
        if (flags.isEmpty()) {
            state = state.copy(flagsText = "", flags = emptySet(), flagsError = IntentFlagsError.NONE)
            return
        }

        var flagsInt = 0

        flags.forEach { flagsInt = flagsInt.withFlag(it) }

        state = state.copy(flagsText = flagsInt.toString(),
            flags = flags,
            flagsError = IntentFlagsError.NONE)
    }

    private fun validateFlags(text: String): IntentFlagsError {
        return if (text.toIntOrNull() == null) {
            IntentFlagsError.NOT_NUMBER
        } else {
            IntentFlagsError.NONE
        }

    }

    fun onSelectIntentTarget(target: IntentTarget) {
        state = state.copy(target = target)
    }

    fun onAddCategory(name: String) {
        state = state.copy(categories = state.categories.plus(name))
    }

    fun onEditCategory(old: String, new: String) {
        state = state.copy(categories = state.categories.minus(old).plus(new))
    }

    fun onDeleteCategory(name: String) {
        state = state.copy(categories = state.categories.minus(name))
    }

    fun onAddExtra(extra: IntentExtraRow) {
        state = state.copy(extras = state.extras.plus(extra))
    }

    fun onEditExtra(key: String, newExtra: IntentExtraRow) {
        val newExtras = state.extras.map {
            if (it.key == key) {
                newExtra
            } else {
                it
            }
        }

        state = state.copy(extras = newExtras)
    }

    fun onDeleteExtra(key: String) {
        val newExtras = state.extras
            .toMutableList()
            .also { list -> list.removeAll { it.key == key } }

        state = state.copy(extras = newExtras)
    }

    fun buildResult(): ConfigIntentResult? {
        return null
    }
}

sealed class IntentExtraRow : Parcelable {
    abstract val key: String
    abstract val valueString: String

    @Parcelize
    data class BooleanExtra(override val key: String, val value: Boolean = true) : IntentExtraRow() {
        override val valueString: String = if (value) {
            "true"
        } else {
            "false"
        }
    }

    @Parcelize
    data class StringExtra(override val key: String, val value: String = "") : IntentExtraRow() {
        override val valueString: String = value
    }
}

enum class IntentDescriptionError {
    NONE,
    EMPTY
}

enum class IntentFlagsError {
    NONE,
    NOT_NUMBER
}

data class ConfigIntentState(
    val description: String,
    val descriptionError: IntentDescriptionError,
    val target: IntentTarget,
    val action: String,
    val data: String,
    val flagsText: String,
    val flags: Set<Int>,
    val flagsError: IntentFlagsError,
    val categories: Set<String>,
    val extras: List<IntentExtraRow>,
    val componentPackage: String,
    val componentClass: String,
    val isDoneButtonEnabled: Boolean
) {
    companion object {
        val DEFAULT: ConfigIntentState = ConfigIntentState(
            description = "",
            descriptionError = IntentDescriptionError.EMPTY,
            target = IntentTarget.ACTIVITY,
            action = "",
            data = "",
            flagsText = "",
            flags = emptySet(),
            flagsError = IntentFlagsError.NONE,
            categories = emptySet(),
            extras = emptyList(),
            componentPackage = "",
            componentClass = "",
            isDoneButtonEnabled = false
        )
    }
}