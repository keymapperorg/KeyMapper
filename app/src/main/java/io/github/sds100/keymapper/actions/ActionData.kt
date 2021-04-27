package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.intents.IntentTarget
import kotlinx.serialization.Serializable

@Serializable
sealed class ActionData

@Serializable
object CorruptAction : ActionData()

@Serializable
data class OpenAppAction(
    val packageName: String
) : ActionData()

@Serializable
data class OpenAppShortcutAction(
    val packageName: String?,
    val shortcutTitle: String,
    val uri: String
) : ActionData()

@Serializable
data class KeyEventAction(
    val keyCode: Int,
    val metaState: Int = 0,
    val useShell: Boolean = false,
    val device: Device? = null
) : ActionData() {

    @Serializable
    data class Device(
        val descriptor: String,
        val name: String
    )
}

@Serializable
sealed class SystemAction : ActionData() {
    abstract val id: SystemActionId
}

@Serializable
data class SimpleSystemAction(
    override val id: SystemActionId,
) : SystemAction()

@Serializable
sealed class VolumeSystemAction : SystemAction() {
    abstract val showVolumeUi: Boolean

    sealed class Stream : VolumeSystemAction() {
        abstract val volumeStream: VolumeStream

        @Serializable
        data class Increase(
            override val showVolumeUi: Boolean, //TODO #639 show volume ui option
            override val volumeStream: VolumeStream
        ) : Stream() {
            override val id = SystemActionId.VOLUME_INCREASE_STREAM
        }

        @Serializable
        data class Decrease(
            override val showVolumeUi: Boolean,
            override val volumeStream: VolumeStream
        ) : Stream() {
            override val id = SystemActionId.VOLUME_DECREASE_STREAM
        }
    }

    @Serializable
    data class Up(override val showVolumeUi: Boolean) : VolumeSystemAction() {
        override val id = SystemActionId.VOLUME_UP
    }

    @Serializable
    data class Down(override val showVolumeUi: Boolean) : VolumeSystemAction() {
        override val id = SystemActionId.VOLUME_DOWN
    }

    @Serializable
    data class Mute(override val showVolumeUi: Boolean) : VolumeSystemAction() {
        override val id = SystemActionId.VOLUME_MUTE
    }

    @Serializable
    data class UnMute(override val showVolumeUi: Boolean) : VolumeSystemAction() {
        override val id = SystemActionId.VOLUME_UNMUTE
    }

    @Serializable
    data class ToggleMute(override val showVolumeUi: Boolean) : VolumeSystemAction() {
        override val id = SystemActionId.VOLUME_TOGGLE_MUTE
    }
}

@Serializable
sealed class FlashlightSystemAction : SystemAction() {
    abstract val lens: CameraLens

    @Serializable
    data class Toggle(override val lens: CameraLens) : FlashlightSystemAction() {
        override val id = SystemActionId.TOGGLE_FLASHLIGHT
    }

    @Serializable
    data class Enable(override val lens: CameraLens) : FlashlightSystemAction() {
        override val id = SystemActionId.ENABLE_FLASHLIGHT
    }

    @Serializable
    data class Disable(override val lens: CameraLens) : FlashlightSystemAction() {
        override val id = SystemActionId.DISABLE_FLASHLIGHT
    }
}

@Serializable
data class ChangeRingerModeSystemAction(
    val ringerMode: RingerMode
) : SystemAction() {
    override val id: SystemActionId = SystemActionId.CHANGE_RINGER_MODE
}

@Serializable
data class SwitchKeyboardSystemAction(
    val imeId: String,
    val savedImeName: String
) : SystemAction() {
    override val id = SystemActionId.SWITCH_KEYBOARD
}

@Serializable
data class ToggleDndMode(val dndMode: DndMode):SystemAction(){
    override val id: SystemActionId = SystemActionId.TOGGLE_DND_MODE
}

@Serializable
data class EnableDndMode(val dndMode: DndMode):SystemAction(){
    override val id: SystemActionId = SystemActionId.ENABLE_DND_MODE
}

@Serializable
data class CycleRotationsSystemAction(
    val orientations: List<Orientation>
) : SystemAction() {
    override val id = SystemActionId.CYCLE_ROTATIONS
}

@Serializable
sealed class ControlMediaForAppSystemAction : SystemAction() {
    abstract val packageName: String

    @Serializable
    data class Pause(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.PAUSE_MEDIA_PACKAGE
    }

    @Serializable
    data class Play(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.PLAY_MEDIA_PACKAGE
    }

    @Serializable
    data class PlayPause(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.PLAY_PAUSE_MEDIA_PACKAGE
    }

    @Serializable
    data class NextTrack(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.NEXT_TRACK_PACKAGE
    }

    @Serializable
    data class PreviousTrack(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.PREVIOUS_TRACK_PACKAGE
    }

    @Serializable
    data class FastForward(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.FAST_FORWARD_PACKAGE
    }

    @Serializable
    data class Rewind(override val packageName: String) : ControlMediaForAppSystemAction() {
        override val id = SystemActionId.REWIND_PACKAGE
    }
}

@Serializable
data class IntentAction(
    val description: String,
    val target: IntentTarget,
    val uri: String
) : ActionData()

@Serializable
data class TapCoordinateAction(
    val x: Int,
    val y: Int,
    val description: String?
) : ActionData()

@Serializable
data class PhoneCallAction(
    val number: String
) : ActionData()

@Serializable
data class UrlAction(
    val url: String
) : ActionData()

@Serializable
data class TextAction(
    val text: String
) : ActionData()