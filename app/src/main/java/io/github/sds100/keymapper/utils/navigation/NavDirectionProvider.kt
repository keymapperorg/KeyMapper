package io.github.sds100.keymapper.utils.navigation

import androidx.navigation.NavDirections
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.base.utils.navigation.BaseNavDirectionProvider
import io.github.sds100.keymapper.base.utils.navigation.NavDestination

class NavDirectionProvider : BaseNavDirectionProvider() {
    override fun getDirection(destination: NavDestination<*>, requestKey: String): NavDirections {
        return when (destination) {
            is NavDestination.ConfigKeyMap -> when (destination) {
                is NavDestination.ConfigKeyMap.New ->
                    NavAppDirections.actionToConfigKeymap(
                        groupUid = destination.groupUid,
                        showAdvancedTriggers = destination.showAdvancedTriggers,
                    )

                is NavDestination.ConfigKeyMap.Open ->
                    NavAppDirections.actionToConfigKeymap(
                        keyMapUid = destination.keyMapUid,
                        showAdvancedTriggers = destination.showAdvancedTriggers,
                    )
            }

            else -> super.getDirection(destination, requestKey)
        }
    }
}
