package io.github.sds100.keymapper.util

import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import io.github.sds100.keymapper.R
import splitties.alertdialog.appcompat.*
import java.io.IOException
import java.io.InputStream

/**
 * Created by sds100 on 01/10/2018.
 */
object RootUtils {
    /**
     * @return whether the command was executed successfully
     */
    fun executeRootCommand(command: String, waitFor: Boolean = false): Boolean {
        return Shell.run("su", "-c", command, waitFor = waitFor)
    }

    /**
     * Remember to close it after using it.
     */
    @Throws(IOException::class)
    fun getRootCommandOutput(command: String): InputStream {
        return Shell.getShellCommandStdOut("su", "-c", command)
    }

    fun promptForRootPermission(activity: FragmentActivity) = activity.alertDialog {
        titleResource = R.string.dialog_title_root_prompt
        messageResource = R.string.dialog_message_root_prompt
        setIcon(R.drawable.ic_baseline_warning_24)
        okButton {
            activity.findNavController(R.id.container).navigate(R.id.action_global_settingsFragment)
            Shell.run("su")
        }

        cancelButton()

        show()
    }
}