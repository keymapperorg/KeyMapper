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
    object ChooseAction : ActionDestination() {
        const val NAME: String = "choose_action"
    }

    data class ConfigKeyEvent(val keyEventAction: ActionData.InputKeyEvent) : ActionDestination() {
        companion object {
            const val NAME: String = "config_key_event/{action}"
            val ARGUMENTS: List<NamedNavArgument> = listOf(navArgument("action") { type = NavType.StringType })

            fun getKeyEventAction(arguments: Bundle): ActionData.InputKeyEvent {
                return arguments["action"]!! as ActionData.InputKeyEvent
            }
        }

        val route: String = "$NAME/${Json.encodeToString(keyEventAction)}"
    }
}
