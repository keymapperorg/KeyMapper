package io.github.sds100.keymapper.screenshots

import android.graphics.drawable.Drawable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.sds100.keymapper.base.constraints.ConstraintMode
import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeChipModel
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

fun keyMap(
    uid: String,
    triggerKeys: List<String>,
    actions: List<ComposeChipModel>,
    constraints: List<ComposeChipModel> = emptyList(),
    options: List<String> = emptyList(),
): KeyMapListItemModel {
    return KeyMapListItemModel(
        isSelected = false,
        KeyMapListItemModel.Content(
            uid = uid,
            triggerKeys = triggerKeys,
            triggerSeparatorIcon = Icons.Outlined.Add,
            actions = actions,
            constraintMode = ConstraintMode.AND,
            constraints = constraints,
            options = options,
            isEnabled = true,
            hasError = false,
        ),
    )
}

fun chip(text: String, icon: ImageVector): ComposeChipModel {
    return ComposeChipModel.Normal(
        id = text,
        icon = ComposeIconInfo.Vector(icon),
        text = text,
    )
}

fun chip(text: String, icon: Drawable): ComposeChipModel {
    return ComposeChipModel.Normal(
        id = text,
        icon = ComposeIconInfo.Drawable(icon),
        text = text,
    )
}
