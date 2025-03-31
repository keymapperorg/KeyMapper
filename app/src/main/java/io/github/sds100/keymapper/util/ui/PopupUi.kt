package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 23/03/2021.
 */

sealed class PopupUi<R> {

    data class SnackBar(
        val message: String,
        val long: Boolean = false,
        val actionText: String? = null,
    ) : PopupUi<Unit>()

    data class Ok(val message: String, val title: String? = null) : PopupUi<Unit>()

    data class Dialog(
        val title: CharSequence? = null,
        val message: CharSequence,
        val positiveButtonText: CharSequence,
        val neutralButtonText: CharSequence? = null,
        val negativeButtonText: CharSequence? = null,
    ) : PopupUi<DialogResponse>()

    data class Text(
        val hint: String,
        val allowEmpty: Boolean,
        val text: String = "",
        val inputType: Int? = null,
        val message: CharSequence? = null,
        val autoCompleteEntries: List<String> = emptyList(),
    ) : PopupUi<String>()

    data class SingleChoice<ID>(
        val items: List<Pair<ID, String>>,
    ) : PopupUi<ID>()

    data class MultiChoice<ID>(val items: List<MultiChoiceItem<ID>>) : PopupUi<List<ID>>()

    data class Toast(val text: String) : PopupUi<Unit>()

    data class ChooseAppStore(
        val title: CharSequence,
        val message: CharSequence,
        val model: ChooseAppStoreModel,
        val positiveButtonText: CharSequence? = null,
        val negativeButtonText: CharSequence? = null,
    ) : PopupUi<DialogResponse>()

    data class OpenUrl(val url: String) : PopupUi<Unit>()
}

enum class DialogResponse {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
}
