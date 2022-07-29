package io.github.sds100.keymapper.actions

import android.os.Bundle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 13/07/2022.
 */
sealed class ActionDestination {
    abstract val route: String

    object ChooseAction : ActionDestination() {
        const val ROUTE_PLACEHOLDER: String = "choose_action"
        override val route: String = ROUTE_PLACEHOLDER
    }

    class ChooseKeyCode(val requestKey: String) : ActionDestination() {
        companion object {
            const val ROUTE_PLACEHOLDER: String = "choose_keycode/{requestKey}"
            val ARGUMENTS: List<NamedNavArgument> = listOf(
                navArgument("requestKey") { type = NavType.StringType }
            )

            fun getRequestKey(arguments: Bundle): String {
                return arguments.getString("requestKey")!!
            }
        }

        override val route: String = ROUTE_PLACEHOLDER.replace("{requestKey}", requestKey)
    }

    data class ConfigKeyEvent(val keyEventAction: ActionData.InputKeyEvent) : ActionDestination() {
        companion object {
            const val ROUTE_PLACEHOLDER: String = "config_key_event/{action}"
            val ARGUMENTS: List<NamedNavArgument> = listOf(navArgument("action") { type = NavType.StringType })

            fun getKeyEventAction(arguments: Bundle): ActionData.InputKeyEvent {
                return arguments["action"]!! as ActionData.InputKeyEvent
            }
        }

        override val route: String = "$ROUTE_PLACEHOLDER/${Json.encodeToString(keyEventAction)}"
    }
}
