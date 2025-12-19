package android.app

import android.app.ActivityManager.RunningTaskInfo
import android.os.Build

object ActivityTaskManagerApis {
    fun getTasks(
        activityTaskManager: IActivityTaskManager,
        maxNum: Int,
        filterOnlyVisibleRecents: Boolean,
        keepIntentExtra: Boolean,
        displayId: Int,
    ): MutableList<RunningTaskInfo?>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return activityTaskManager.getTasks(
                maxNum,
                filterOnlyVisibleRecents,
                keepIntentExtra,
                displayId,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                return activityTaskManager.getTasks(
                    maxNum,
                    filterOnlyVisibleRecents,
                    keepIntentExtra,
                )
            } catch (_: NoSuchMethodError) {
                // In later revisions of Android 13 this method was added.
                return activityTaskManager.getTasks(
                    maxNum,
                    filterOnlyVisibleRecents,
                    keepIntentExtra,
                    displayId,
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return activityTaskManager.getTasks(maxNum, filterOnlyVisibleRecents, keepIntentExtra)
        } else {
            return activityTaskManager.getTasks(maxNum)
        }
    }
}
