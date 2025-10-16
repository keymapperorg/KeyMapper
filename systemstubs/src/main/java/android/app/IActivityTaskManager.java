package android.app;

import android.os.Build;
import android.os.IBinder;

import java.util.List;

import androidx.annotation.RequiresApi;

public interface IActivityTaskManager extends android.os.IInterface {
    List<ActivityManager.RunningTaskInfo> getTasks(int maxNum);

    @RequiresApi(Build.VERSION_CODES.S)
    List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents,
                                                   boolean keepIntentExtra);

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    List<ActivityManager.RunningTaskInfo> getTasks(int maxNum, boolean filterOnlyVisibleRecents, boolean keepIntentExtra, int displayId);

    abstract class Stub extends android.os.Binder implements IActivityTaskManager {
        public static IActivityTaskManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
