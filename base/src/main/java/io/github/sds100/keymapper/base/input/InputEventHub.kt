package io.github.sds100.keymapper.base.input

import io.github.sds100.keymapper.system.inputevents.KMInputEvent

interface InputEventHub {
    /**
     * Register a client that will receive input events through the [callback]. The same [clientId]
     * must be used for any requests to other methods in this class. The input events will either
     * come from the key event relay service, accessibility service, or system bridge
     * depending on the type of event and Key Mapper's permissions.
     */
    fun registerClient(clientId: String, callback: InputEventHubCallback)
    fun unregisterClient(clientId: String)

    /**
     * Set the devices that a client wants to listen to.
     */
    fun setCallbackDevices(clientId: String, deviceIds: Array<String>)

    /**
     * Inject an input event. This may either use the key event relay service or the system
     * bridge depending on the permissions granted to Key Mapper.
     */
    fun injectEvent(event: KMInputEvent)
}