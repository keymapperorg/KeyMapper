package io.github.sds100.keymapper.base.constraints

enum class NotificationField {
    PACKAGE,
    TITLE,
    TEXT,
    ;

    /**
     * Whether the value is free text that the user types rather than an app they pick. Only these
     * fields have a [TextMatchMode].
     */
    val isFreeText: Boolean
        get() = this != PACKAGE

    companion object {
        fun of(data: ConstraintData.NotificationPosted): NotificationField = when (data) {
            is ConstraintData.NotificationPosted.FromApp -> PACKAGE
            is ConstraintData.NotificationPosted.Title -> TITLE
            is ConstraintData.NotificationPosted.Text -> TEXT
        }
    }
}
