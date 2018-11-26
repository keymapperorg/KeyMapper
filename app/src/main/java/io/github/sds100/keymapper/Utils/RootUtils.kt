package io.github.sds100.keymapper.Utils

import android.content.Context
import io.github.sds100.keymapper.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.okButton

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    private val ROOT_CHECK_COMMAND = arrayOf("su", "-c", "ls")

    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String): Boolean {
        return ShellUtils.executeCommand("su", "-c", command)
    }

    fun checkAppHasRootPermission(): Boolean {
        val output = ShellUtils.getCommandOutput(*ROOT_CHECK_COMMAND)

        if (output.contains("Permission denied")) {
            return false
        } else if (output.isNullOrEmpty()) {
            return true
        }

        return false
    }

    fun promptForRootPermission(ctx: Context) {
        ctx.alert {
            titleResource = R.string.dialog_title_root_prompt
            messageResource = R.string.dialog_message_root_prompt
            iconResource = R.drawable.ic_warning_black_24dp
            okButton { ShellUtils.executeCommand("su") }
            cancelButton { dialog -> dialog.cancel() }
        }.show()
    }
}