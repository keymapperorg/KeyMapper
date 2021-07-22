package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.home.ChooseAppStoreModel

/**
 * Created by sds100 on 23/03/2021.
 */

sealed class PopupUi<RESPONSE : PopupResponse> {

    data class SnackBar(
        val message: String, val long: Boolean = false, val actionText: String? = null
    ) : PopupUi<SnackBarActionResponse>()

    object SnackBarActionResponse : PopupResponse

    data class Ok(val message: String, val title: String? = null) : PopupUi<OkResponse>()
    object OkResponse : PopupResponse

    data class Dialog(
        val title: CharSequence? = null,
        val message: CharSequence,
        val positiveButtonText: CharSequence,
        val neutralButtonText: CharSequence? = null,
        val negativeButtonText: CharSequence? = null
    ) : PopupUi<DialogResponse>()

    data class Text(val hint: String, val allowEmpty: Boolean, val text: String = "") :
        PopupUi<TextResponse>()

    data class TextResponse(val text: String) : PopupResponse

    data class SingleChoice<ID>(
        val items: List<Pair<ID, String>>
    ) : PopupUi<SingleChoiceResponse<ID>>()

    data class SingleChoiceResponse<ID>(val item: ID) : PopupResponse

    data class MultiChoice<ID>(val items: List<Pair<ID, String>>) :
        PopupUi<MultiChoiceResponse<ID>>()

    data class MultiChoiceResponse<ID>(val items: List<ID>) : PopupResponse

    data class Toast(val text: String) : PopupUi<PopupResponse>()

    data class ChooseAppStore(
        val title: CharSequence,
        val message: CharSequence,
        val model: ChooseAppStoreModel,
        val positiveButtonText: CharSequence? = null,
        val negativeButtonText: CharSequence? = null
    ) : PopupUi<DialogResponse>()
}

interface PopupResponse

enum class DialogResponse : PopupResponse {
    POSITIVE, NEUTRAL, NEGATIVE
}