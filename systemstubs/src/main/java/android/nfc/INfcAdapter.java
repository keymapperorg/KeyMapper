package android.nfc;

import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;

public interface INfcAdapter extends android.os.IInterface {
    boolean enable();

    boolean disable(boolean saveState);

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    boolean enable(String pkg);

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    boolean disable(boolean saveState, String pkg);

    abstract class Stub extends android.os.Binder implements INfcAdapter {
        public static INfcAdapter asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}