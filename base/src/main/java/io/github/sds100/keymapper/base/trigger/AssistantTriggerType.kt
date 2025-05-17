package io.github.sds100.keymapper.base.trigger

/**
 * The type of assistant that triggers an assistant trigger key. The voice assistant
 * is the assistant that handles voice commands. If you press the voice command on a headset or
 * keyboard then Android asks you which app it should use as default.
 *
 * The device assistant is the one selected in the settings as the default for reading on-screen
 * content and only one app can have this permission at a time. This is the one used when
 * long-pressing the power button on Pixels and other Android skins.
 */
enum class AssistantTriggerType {
    ANY,
    VOICE,
    DEVICE,
}
