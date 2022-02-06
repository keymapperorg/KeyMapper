package io.github.sds100.keymapper.actions

import java.util.*

/**
 * Created by sds100 on 28/04/2021.
 */
data class FakeAction(
    override val uid: String = UUID.randomUUID().toString(),
    override val data: ActionData,
    override val multiplier: Int? = null,
    override val delayBeforeNextAction: Int? = null,
    override val repeat: Boolean = false,
    override val repeatRate: Int? = null,
    override val repeatLimit: Int? = null,
    override val repeatMode: RepeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN,
    override val holdDown: Boolean = false,
    override val holdDownDuration: Int? = null,
) : Action
