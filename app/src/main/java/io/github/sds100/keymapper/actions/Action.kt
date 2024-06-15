package io.github.sds100.keymapper.actions

/**
 * Created by sds100 on 21/02/2021.
 */

interface Action {
    val uid: String
    val data: ActionData
    val multiplier: Int?
    val delayBeforeNextAction: Int?

    val repeat: Boolean
    val repeatMode: RepeatMode
    val repeatRate: Int?
    val repeatLimit: Int?

    val holdDown: Boolean
    val holdDownDuration: Int?
}
