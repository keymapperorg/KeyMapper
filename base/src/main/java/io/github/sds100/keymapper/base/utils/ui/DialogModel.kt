package io.github.sds100.keymapper.base.utils.ui

sealed class DialogModel<R> {

    data class SnackBar(
        val message: String,
        val long: Boolean = false,
        val actionText: String? = null,
    ) : DialogModel<Unit>()

    data class Ok(val message: String, val title: String? = null) : DialogModel<Unit>()

    data class Alert(
        val title: CharSequence? = null,
        val message: CharSequence,
        val positiveButtonText: CharSequence,
        val neutralButtonText: CharSequence? = null,
        val negativeButtonText: CharSequence? = null,
    ) : DialogModel<DialogResponse>()

    data class Text(
        val hint: String,
        val allowEmpty: Boolean,
        val text: String = "",
        val inputType: Int? = null,
        val message: CharSequence? = null,
        val autoCompleteEntries: List<String> = emptyList(),
    ) : DialogModel<String>()

    data class SingleChoice<ID>(val items: List<Pair<ID, String>>) : DialogModel<ID>()

    data class MultiChoice<ID>(val items: List<MultiChoiceItem<ID>>) : DialogModel<List<ID>>()

    data class Toast(val text: String) : DialogModel<Unit>()

    data class OpenUrl(val url: String) : DialogModel<Unit>()
}

enum class DialogResponse {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
}
