package android.bluetooth;
import android.content.AttributionSource;

interface IBluetoothManager {
    // Requires Android 13+
    boolean enable(in AttributionSource attributionSource);
    // Requires Android 13+
    boolean disable(in AttributionSource attributionSource, boolean persist);
}