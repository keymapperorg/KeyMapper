package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import io.github.sds100.keymapper.common.utils.withFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Keeps the accessibility service's [AccessibilityServiceInfo] in sync with the features that
 * the user's key maps and the app are actually using. See [AccessibilityServiceFeature] for why
 * requesting the bare minimum matters.
 *
 * The XML configuration deliberately declares no flags and no event types so that a service that
 * is doing nothing asks for nothing. The capabilities in the XML can not be narrowed at runtime
 * because they are fixed when the service is bound, only the flags and event types can.
 */
class AccessibilityServiceInfoController(
    private val service: IAccessibilityService,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        /**
         * Set an accessibility event timeout because WINDOW_CONTENT_CHANGED events can be sent
         * very frequently.
         */
        private const val DEFAULT_NOTIFICATION_TIMEOUT = 200L
    }

    private val featureSources:
        MutableStateFlow<Map<String, Flow<Set<AccessibilityServiceFeature>>>> =
        MutableStateFlow(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val features: StateFlow<Set<AccessibilityServiceFeature>> =
        featureSources
            .flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    flowOf(emptySet())
                } else {
                    combine(sources.values) { sets ->
                        sets.flatMapTo(mutableSetOf()) { it }
                    }
                }
            }.distinctUntilChanged()
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptySet())

    init {
        features.onEach { features ->
            Timber.d("Accessibility service features: ${features.joinToString()}")
            updateServiceInfo(features)
        }.launchIn(coroutineScope)
    }

    /**
     * The service flags are reset by the system whenever the service is bound so they must be
     * set again. On some devices onServiceConnected is called multiple times throughout the
     * lifecycle of the service.
     */
    fun onServiceConnected() {
        updateServiceInfo(features.value)
    }

    /**
     * Register something that needs accessibility service features. The [id] must be unique for
     * each source so that registering the same source again replaces it rather than duplicating
     * it, because onServiceConnected can be called multiple times.
     */
    fun setFeatureSource(id: String, source: Flow<Set<AccessibilityServiceFeature>>) {
        featureSources.update { it.plus(id to source) }
    }

    /**
     * Request [features] for the duration of [block] and then go back to only requesting the
     * features that are needed. This is used to check whether the device supports fingerprint
     * gestures because that can only be queried while the service is requesting them.
     */
    suspend fun <T> withFeature(
        tempFeatures: Set<AccessibilityServiceFeature>,
        block: suspend () -> T,
    ): T {
        updateServiceInfo(features.value.plus(tempFeatures))

        try {
            return block()
        } finally {
            updateServiceInfo(features.value)
        }
    }

    private fun updateServiceInfo(features: Set<AccessibilityServiceFeature>) {
        // Check that it isn't null because this can only be set once the service is bound.
        if (service.serviceFlags == null) {
            return
        }

        service.serviceFlags = buildFlags(features)
        service.serviceEventTypes = buildEventTypes(features)
        service.serviceFeedbackType = buildFeedbackType(features)
        service.notificationTimeout = buildNotificationTimeout(features)
    }

    private fun buildFlags(features: Set<AccessibilityServiceFeature>): Int {
        var flags = AccessibilityServiceInfo.DEFAULT

        for (feature in features) {
            flags = when (feature) {
                AccessibilityServiceFeature.FILTER_KEY_EVENTS ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)

                AccessibilityServiceFeature.WINDOW_STATE ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS)

                AccessibilityServiceFeature.NODE_INFO ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS)
                        .withFlag(AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS)

                AccessibilityServiceFeature.INPUT_METHOD_EDITOR ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR)

                AccessibilityServiceFeature.FINGERPRINT_GESTURES ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES)

                AccessibilityServiceFeature.ACCESSIBILITY_VOLUME ->
                    flags.withFlag(AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME)

                AccessibilityServiceFeature.WINDOW_CONTENT,
                AccessibilityServiceFeature.VIEW_INTERACTION,
                AccessibilityServiceFeature.IMMEDIATE_EVENTS,
                    -> flags
            }
        }

        return flags
    }

    private fun buildEventTypes(features: Set<AccessibilityServiceFeature>): Int {
        var eventTypes = 0

        for (feature in features) {
            eventTypes = when (feature) {
                AccessibilityServiceFeature.WINDOW_STATE ->
                    eventTypes.withFlag(AccessibilityEvent.TYPE_WINDOWS_CHANGED)

                AccessibilityServiceFeature.WINDOW_CONTENT ->
                    eventTypes.withFlag(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)

                AccessibilityServiceFeature.VIEW_INTERACTION ->
                    eventTypes.withFlag(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                        .withFlag(AccessibilityEvent.TYPE_VIEW_CLICKED)

                else -> eventTypes
            }
        }

        return eventTypes
    }

    /**
     * FEEDBACK_GENERIC is for some reason required on Android 8.0 to get accessibility events.
     */
    private fun buildFeedbackType(features: Set<AccessibilityServiceFeature>): Int {
        var feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

        if (features.contains(AccessibilityServiceFeature.ACCESSIBILITY_VOLUME)) {
            feedbackType = feedbackType.withFlag(AccessibilityServiceInfo.FEEDBACK_AUDIBLE)
        }

        return feedbackType
    }

    private fun buildNotificationTimeout(features: Set<AccessibilityServiceFeature>): Long {
        if (features.contains(AccessibilityServiceFeature.IMMEDIATE_EVENTS)) {
            return 0L
        }

        return DEFAULT_NOTIFICATION_TIMEOUT
    }
}
