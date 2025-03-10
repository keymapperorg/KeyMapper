package io.github.sds100.keymapper.home

sealed class HomeDestination(val route: String) {
    data object KeyMaps : HomeDestination("key_maps")
    data object FloatingButtons : HomeDestination("floating_buttons")
}
