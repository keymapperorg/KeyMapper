package io.github.sds100.keymapper.utils.navigation

import androidx.navigation.NavDirections
import dagger.hilt.android.scopes.FragmentScoped
import io.github.sds100.keymapper.base.utils.navigation.BaseNavDirectionProvider
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import javax.inject.Inject

// TODO delete
@FragmentScoped
class NavDirectionProvider @Inject constructor() : BaseNavDirectionProvider() {
    override fun getDirection(destination: NavDestination<*>, requestKey: String): NavDirections {
        return when (destination) {

            else -> super.getDirection(destination, requestKey)
        }
    }
}
