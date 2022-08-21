package io.github.sds100.keymapper.system.intents

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import splitties.bitflags.withFlag
import javax.inject.Inject

@HiltViewModel
class ConfigIntentViewModel2 @Inject constructor() : ViewModel() {
    var state: ConfigIntentState by mutableStateOf(ConfigIntentState.DEFAULT)
        private set

    fun onDescriptionChange(text: String) {
        val error = validateDescription(text)
        state = state.copy(
            description = text,
            descriptionError = error,
            isDoneButtonEnabled = error == IntentDescriptionError.NONE
        )
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
        state = state.copy(extraRows = state.extraRows.plus(extra))
    }

    fun onEditExtra(name: String, newExtra: IntentExtraRow) {
        val newExtras = state.extraRows.map {
            if (it.name == name) {
                newExtra
            } else {
                it
            }
        }

        state = state.copy(extraRows = newExtras)
    }

    fun onDeleteExtra(name: String) {
        val newExtras = state.extraRows
            .toMutableList()
            .also { list -> list.removeAll { it.name == name } }

        state = state.copy(extraRows = newExtras)
    }

    fun buildResult(): ConfigIntentResult? {
        val intent = Intent()

        val state = state

        require(state.description.isNotEmpty())

        if (state.action.isNotEmpty()) {
            intent.action = state.action
        }

        state.categories.forEach { intent.addCategory(it) }

        if (state.data.isNotEmpty()) {
            intent.data = state.data.toUri()
        }

        var flags = 0

        state.flags.forEach { flags = flags.withFlag(it) }

        if (state.componentPackage.isNotEmpty()) {
            intent.`package` = state.componentPackage

            if (state.componentClass.isNotEmpty()) {
                intent.setClassName(state.componentPackage, state.componentClass)
            }
        }

        state.extraRows.forEach { extraRow -> addExtraToIntent(extraRow, intent) }

        val intentUri = intent.toUri(0)

        return ConfigIntentResult(intentUri, state.target, state.description)
    }

    private fun addExtraToIntent(extraRow: IntentExtraRow, intent: Intent) {
        when (extraRow) {
            is IntentExtraRow.BooleanArrayExtra -> {
                val booleanArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it == "true" }
                    .toBooleanArray()

                intent.putExtra(extraRow.name, booleanArray)
            }

            is IntentExtraRow.BooleanExtra -> {
                intent.putExtra(extraRow.name, extraRow.value)
            }

            is IntentExtraRow.IntegerExtra -> {
                val integer = extraRow.value.toIntOrNull() ?: return
                intent.putExtra(extraRow.name, integer)
            }

            is IntentExtraRow.StringExtra -> {
                intent.putExtra(extraRow.name, extraRow.value)
            }

            is IntentExtraRow.ByteArrayExtra -> {
                val byteArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toByte() }
                    .toByteArray()

                intent.putExtra(extraRow.name, byteArray)
            }

            is IntentExtraRow.ByteExtra -> {
                val byte = extraRow.value.toByteOrNull() ?: return
                intent.putExtra(extraRow.name, byte)
            }

            is IntentExtraRow.CharArrayExtra -> {
                val charArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it[0] }
                    .toCharArray()

                intent.putExtra(extraRow.name, charArray)
            }

            is IntentExtraRow.CharExtra -> {
                intent.putExtra(extraRow.name, extraRow.value)
            }

            is IntentExtraRow.DoubleArrayExtra -> {
                val doubleArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toDouble() }
                    .toDoubleArray()

                intent.putExtra(extraRow.name, doubleArray)
            }
            is IntentExtraRow.DoubleExtra -> {
                val double = extraRow.value.toDoubleOrNull() ?: return
                intent.putExtra(extraRow.name, double)
            }

            is IntentExtraRow.FloatArrayExtra -> {
                val floatArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toFloat() }
                    .toFloatArray()

                intent.putExtra(extraRow.name, floatArray)
            }

            is IntentExtraRow.FloatExtra -> {
                val float = extraRow.value.toFloatOrNull() ?: return
                intent.putExtra(extraRow.name, float)
            }

            is IntentExtraRow.IntegerArrayExtra -> {
                val intArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toInt() }
                    .toIntArray()

                intent.putExtra(extraRow.name, intArray)
            }

            is IntentExtraRow.LongArrayExtra -> {
                val longArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toLong() }
                    .toLongArray()

                intent.putExtra(extraRow.name, longArray)
            }

            is IntentExtraRow.LongExtra -> {
                val long = extraRow.value.toLongOrNull() ?: return
                intent.putExtra(extraRow.name, long)
            }

            is IntentExtraRow.ShortArrayExtra -> {
                val shortArray = extraRow
                    .value
                    .split(',')
                    .map { it.trim() }
                    .map { it.toShort() }
                    .toShortArray()

                intent.putExtra(extraRow.name, shortArray)
            }

            is IntentExtraRow.ShortExtra -> {
                val short = extraRow.value.toShortOrNull() ?: return
                intent.putExtra(extraRow.name, short)
            }

            is IntentExtraRow.StringArrayExtra -> {
                val stringArray = extraRow
                    .value
                    .split(',')
                    .toTypedArray()

                intent.putExtra(extraRow.name, stringArray)
            }
        }
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
    val extraRows: List<IntentExtraRow>,
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
            extraRows = emptyList(),
            componentPackage = "",
            componentClass = "",
            isDoneButtonEnabled = false
        )
    }
}