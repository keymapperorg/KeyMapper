package io.github.sds100.keymapper.base.utils.navigation

import androidx.navigation.NavDirections
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import io.github.sds100.keymapper.base.NavBaseAppDirections
import kotlinx.serialization.json.Json

abstract class BaseNavDirectionProvider {
    open fun getDirection(destination: NavDestination<*>, requestKey: String): NavDirections {
        return when (destination) {
            is NavDestination.ChooseApp -> NavBaseAppDirections.chooseApp(
                destination.allowHiddenApps,
                requestKey,
            )

            NavDestination.ChooseAppShortcut -> NavBaseAppDirections.chooseAppShortcut(requestKey)
            NavDestination.ChooseKeyCode -> NavBaseAppDirections.chooseKeyCode(requestKey)
            is NavDestination.ConfigKeyEventAction -> {
                val json = destination.action?.let {
                    Json.encodeToString(it)
                }

                NavBaseAppDirections.configKeyEvent(requestKey, json)
            }

            is NavDestination.PickCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavBaseAppDirections.pickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.PickSwipeCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavBaseAppDirections.swipePickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.PickPinchCoordinate -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavBaseAppDirections.pinchPickDisplayCoordinate(requestKey, json)
            }

            is NavDestination.ConfigIntent -> {
                val json = destination.result?.let {
                    Json.encodeToString(it)
                }

                NavBaseAppDirections.configIntent(requestKey, json)
            }

            is NavDestination.ChooseActivity -> NavBaseAppDirections.chooseActivity(requestKey)
            is NavDestination.ChooseSound -> NavBaseAppDirections.chooseSoundFile(requestKey)
            NavDestination.ChooseAction -> NavBaseAppDirections.toChooseActionFragment(requestKey)
            is NavDestination.ChooseConstraint -> NavBaseAppDirections.chooseConstraint(
                requestKey = requestKey,
            )

            is NavDestination.ChooseBluetoothDevice -> NavBaseAppDirections.chooseBluetoothDevice(
                requestKey,
            )

            NavDestination.About -> NavBaseAppDirections.actionGlobalAboutFragment()
            NavDestination.Settings -> NavBaseAppDirections.toSettingsFragment()

            NavDestination.ShizukuSettings -> NavBaseAppDirections.toShizukuSettingsFragment()
            is NavDestination.InteractUiElement -> NavBaseAppDirections.interactUiElement(
                requestKey = requestKey,
                action = destination.action?.let { Json.encodeToString(destination.action) },
            )

            else -> throw IllegalArgumentException("Can not find a direction for this destination: $destination")
        }
    }
}

@EntryPoint
@InstallIn(FragmentComponent::class)
interface NavDirectionsProviderEntryPoint {
    fun provider(): BaseNavDirectionProvider
}
