package io.github.sds100.keymapper.base.system.accessibility

/**
 * The capabilities that the accessibility service requests at any given moment.
 *
 * Only the features that are actually needed right now are requested. Google Play Integrity's
 * "app access risk" check reports running apps that can view the screen or control input, and
 * Google Pay refuses to make payments while it sees one, so requesting the bare minimum means
 * fewer users are affected. It also means far fewer accessibility events are sent to the service.
 * See issue #2194.
 */
enum class AccessibilityServiceFeature {
    /**
     * Receive key events in the accessibility service so they can be consumed.
     */
    FILTER_KEY_EVENTS,

    /**
     * Be told when the windows on the screen change so the foreground app, the notification
     * shade state and the input method visibility can be tracked.
     */
    WINDOW_STATE,

    /**
     * Be told when the content of a window changes. This is only needed on the lock screen
     * because window changed events are not sent when dragging down the status bar.
     */
    WINDOW_CONTENT,

    /**
     * Read the view IDs of the nodes on the screen, including the ones that are not important
     * for accessibility.
     */
    NODE_INFO,

    /**
     * Be told when the user focuses or clicks a view.
     */
    VIEW_INTERACTION,

    /**
     * Send accessibility events as soon as they happen rather than batching them.
     */
    IMMEDIATE_EVENTS,

    /**
     * Register the accessibility service as an input method so it is told when input
     * starts and finishes.
     */
    INPUT_METHOD_EDITOR,

    /**
     * Detect swipes on the fingerprint reader.
     */
    FINGERPRINT_GESTURES,

    /**
     * Play sounds on the accessibility volume stream.
     */
    ACCESSIBILITY_VOLUME,
}
