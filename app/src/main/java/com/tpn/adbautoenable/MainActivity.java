package com.tpn.adbautoenable;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "ADBAutoEnable";
    private static final int WEB_PORT = 9093;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the foreground service to keep web server alive
        Intent serviceIntent = new Intent(this, AdbConfigService.class);
        serviceIntent.putExtra("boot_config", false); // Not boot config, just start service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Create UI
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView titleText = new TextView(this);
        titleText.setText("ADB Auto-Enable");
        titleText.setTextSize(24);

        TextView statusText = new TextView(this);
        statusText.setTextSize(16);
        statusText.setText("\nWeb interface running on:\n\n");

        TextView urlText = new TextView(this);
        urlText.setTextSize(18);
        urlText.setTextColor(0xFF2196F3);
        urlText.setText("http://" + getLocalIpAddress() + ":" + WEB_PORT);

        TextView instructionText = new TextView(this);
        instructionText.setText("\n\nOpen this URL in your browser to configure the app.\n\nThe web server runs in the background even when you close this app.");

        layout.addView(titleText);
        layout.addView(statusText);
        layout.addView(urlText);
        layout.addView(instructionText);

        setContentView(layout);
    }

    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return Formatter.formatIpAddress(ipAddress);
    }
}
