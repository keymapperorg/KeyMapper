package android.app;

import android.os.IBinder;

public interface IActivityManager extends android.os.IInterface {
    boolean removeTask(int taskId);

    void forceStopPackage(String packageName, int userId);

    abstract class Stub extends android.os.Binder implements IActivityManager {
        public static IActivityManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
