package io.github.sds100.keymapper.base.home

// TODO Move this out of FOSS
sealed class HomeDestination(val route: String) {
    data object KeyMaps : HomeDestination("key_maps")
    data object FloatingButtons : HomeDestination("floating_buttons")
}
