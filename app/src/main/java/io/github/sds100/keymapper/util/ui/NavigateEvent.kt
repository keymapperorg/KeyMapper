package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 25/07/2021.
 */
data class NavigateEvent(val key: String, val destination: NavDestination<*>)