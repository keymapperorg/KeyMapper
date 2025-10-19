package io.github.sds100.keymapper.common.models

/**
 * Represents the execution mode for shell commands.
 */
enum class ShellExecutionMode {
    /**
     * Execute using the standard shell (non-root)
     */
    STANDARD,

    /**
     * Execute using root privileges (su)
     */
    ROOT,

    /**
     * Execute using ADB/system bridge (Pro mode)
     */
    ADB
}