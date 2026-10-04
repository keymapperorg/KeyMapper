package io.github.sds100.keymapper.data.repositories

import kotlinx.coroutines.flow.StateFlow

/**
 * Stores the user defined variables that key maps can manipulate and read. A variable that has
 * never been set reads as 0 so there is no distinction between deleting a variable and setting
 * it to 0.
 */
interface VariableRepository {
    /**
     * The values are kept in memory because they must be read synchronously while a key event
     * is being handled.
     */
    val values: StateFlow<Map<String, Long>>

    fun set(name: String, value: Long)
    fun delete(name: String)
    fun deleteAll()
}
