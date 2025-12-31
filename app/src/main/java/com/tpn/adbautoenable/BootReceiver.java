package com.tpn.adbautoenable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "ADBAutoEnable";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received broadcast: " + action);

        // Respond to LOCKED_BOOT_COMPLETED for early start
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            Log.i(TAG, "Boot event detected, starting ADB configuration service...");

            Intent serviceIntent = new Intent(context, AdbConfigService.class);
            serviceIntent.putExtra("boot_config", true); // Flag for boot configuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            Log.i(TAG, "Ignoring broadcast: " + action);
        }
    }
}
