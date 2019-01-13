package io.github.sds100.keymapper.Utils

import android.content.Context
import io.github.sds100.keymapper.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton
import java.io.IOException

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    private val ROOT_CHECK_COMMAND = arrayOf("su", "-c", "ls")

    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        return Shell.run("su", "-c", command)
    }

    fun checkAppHasRootPermission(): Boolean {
        var hasRootPermission = true

        try {
            val output = Shell.getCommandOutput(*ROOT_CHECK_COMMAND)

            if (output.contains("Permission denied")) {
                hasRootPermission = false
            }
        } catch (e: IOException) {
            hasRootPermission = false
        }

        return hasRootPermission
    }

    fun promptForRootPermission(ctx: Context) {
        ctx.alert {
            titleResource = R.string.dialog_title_root_prompt
            messageResource = R.string.dialog_message_root_prompt
            iconResource = R.drawable.ic_warning_black_24dp
            okButton { Shell.run("su") }
            cancelButton { dialog -> dialog.cancel() }
        }.show()
    }

    fun changeSecureSetting(name: String, value: String) {
        RootUtils.executeRootCommand("settings put secure $name $value")
    }
}