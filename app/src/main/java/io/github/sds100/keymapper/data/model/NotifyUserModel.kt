package io.github.sds100.keymapper.data.model

import androidx.annotation.StringRes

/**
 * Created by sds100 on 19/05/2020.
 */
data class NotifyUserModel(@StringRes val message: Int, val onApproved: () -> Unit)