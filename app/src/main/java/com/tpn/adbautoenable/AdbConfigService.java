package com.tpn.adbautoenable;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AdbConfigService extends Service {
    private static final String TAG = "ADBAutoEnable";
    private static final String CHANNEL_ID = "ADBAutoEnableChannel";
    private static final String SERVICE_TYPE = "_adb-tls-connect._tcp";
    private static final String PREFS_NAME = "ADBAutoEnablePrefs";
    private static final String KEY_LAST_STATUS = "last_status";
    private static final String KEY_LAST_PORT = "last_port";
    private static final int INITIAL_BOOT_DELAY_SECONDS = 30;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_SECONDS = 10;
    private static final int WEB_SERVER_PORT = 9093;

    private WebServer webServer;
    private boolean isBootConfigMode = false;
    private volatile boolean isConfiguring = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "AdbConfigService onCreate() called");
        try {
            createNotificationChannel();
            Log.i(TAG, "Notification channel created");

            // Start web server in the service
            startWebServer();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "AdbConfigService onStartCommand() called");

        // Check if this is boot configuration or just keeping the service alive
        isBootConfigMode = intent != null && intent.getBooleanExtra("boot_config", false);

        try {
            // Start as foreground service IMMEDIATELY
            Notification notification = createNotification(
                    isBootConfigMode ? "Starting ADB configuration..." : "Web server running on port " + WEB_SERVER_PORT
            );
            startForeground(1, notification);
            Log.i(TAG, "Started foreground service with notification");

            // Only run boot configuration if this is a boot event
            if (isBootConfigMode) {
                // Prevent duplicate configuration threads
                if (isConfiguring) {
                    Log.w(TAG, "Configuration already in progress, ignoring duplicate request");
                    return START_STICKY;
                }

                isConfiguring = true;

                // Run configuration in background thread
                new Thread(() -> {
                    try {
                        // Step 1: Wait for WiFi to be connected
                        waitForWifiConnection();
                        // Step 2: Wait for system to stabilize
                        waitForBootStabilization();
                        // Step 3: Attempt configuration with retries
                        configureAdbWithRetries();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in configuration thread", e);
                        updateStatus("Failed - " + e.getMessage());
                        updateNotification("Web server running - Boot config failed");
                    } finally {
                        isConfiguring = false;
                    }
                    // Don't stop service after boot config - keep web server running
                    updateNotification("Web server running on port " + WEB_SERVER_PORT);
                }).start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
        }

        return START_STICKY; // Changed from START_NOT_STICKY to keep service alive
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "AdbConfigService onDestroy() called");

        // Stop web server when service is destroyed
        if (webServer != null) {
            webServer.stop();
            Log.i(TAG, "Web server stopped");
        }
    }

    private void startWebServer() {
        try {
            webServer = new WebServer(this, WEB_SERVER_PORT);
            webServer.start();
            Log.i(TAG, "Web server started on port " + WEB_SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "Failed to start web server", e);
        }
    }

    private void waitForWifiConnection() throws InterruptedException {
        Log.i(TAG, "Waiting for WiFi connection...");
        updateNotification("Waiting for WiFi...");
        int maxWaitSeconds = 60;
        int waitedSeconds = 0;

        while (waitedSeconds < maxWaitSeconds) {
            if (isWifiConnected()) {
                String deviceIP = getDeviceIP();
                if (!deviceIP.equals("127.0.0.1") && !deviceIP.equals("0.0.0.0")) {
                    Log.i(TAG, "WiFi connected! Device IP: " + deviceIP);
                    return;
                }
            }
            Thread.sleep(1000);
            waitedSeconds++;
            if (waitedSeconds % 10 == 0) {
                Log.i(TAG, "Still waiting for WiFi... (" + waitedSeconds + "s)");
                updateNotification("Waiting for WiFi... (" + waitedSeconds + "s)");
            }
        }
        Log.w(TAG, "WiFi wait timeout - proceeding anyway");
    }

    private boolean isWifiConnected() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return false;
            }

            if (!wifiManager.isWifiEnabled()) {
                return false;
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                return false;
            }

            int ipAddress = wifiInfo.getIpAddress();
            if (ipAddress == 0) {
                return false;
            }

            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network network = cm.getActiveNetwork();
                    if (network != null) {
                        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                        return capabilities != null &&
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                    }
                } else {
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    return networkInfo != null &&
                            networkInfo.isConnected() &&
                            networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi status", e);
            return false;
        }
    }

    private void waitForBootStabilization() throws InterruptedException {
        Log.i(TAG, "Waiting " + INITIAL_BOOT_DELAY_SECONDS + " seconds for system to stabilize...");
        for (int i = INITIAL_BOOT_DELAY_SECONDS; i > 0; i--) {
            updateNotification("System stabilizing... " + i + "s");
            Thread.sleep(1000);
            if (i % 10 == 0) {
                Log.i(TAG, "Boot stabilization: " + i + " seconds remaining");
            }
        }
        Log.i(TAG, "Boot stabilization complete");
    }

    private void configureAdbWithRetries() {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            Log.i(TAG, "Configuration attempt " + attempt + " of " + MAX_RETRY_ATTEMPTS);
            updateNotification("Attempt " + attempt + " of " + MAX_RETRY_ATTEMPTS);

            boolean success = configureAdb();
            if (success) {
                Log.i(TAG, "Configuration successful on attempt " + attempt);
                return; // Exit immediately on success
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                Log.i(TAG, "Attempt " + attempt + " failed, waiting " + RETRY_DELAY_SECONDS + "s before retry...");
                updateNotification("Failed, retrying in " + RETRY_DELAY_SECONDS + "s...");
                try {
                    Thread.sleep(RETRY_DELAY_SECONDS * 1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Retry delay interrupted", e);
                    return; // Exit if interrupted
                }
            }
        }

        Log.e(TAG, "All " + MAX_RETRY_ATTEMPTS + " attempts failed");
        updateStatus("Failed after " + MAX_RETRY_ATTEMPTS + " attempts");
        updateNotification("Failed after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    private boolean configureAdb() {
        try {
            Log.i(TAG, "Step 1: Enabling wireless debugging...");
            updateNotification("Enabling wireless debugging...");
            updateStatus("Enabling wireless debugging...");

            Settings.Global.putInt(
                    getContentResolver(),
                    "adb_wifi_enabled",
                    1
            );

            Log.i(TAG, "Step 2: Waiting for ADB service to start...");
            updateNotification("Waiting for ADB service...");
            Thread.sleep(15000);

            String deviceIP = getDeviceIP();
            Log.i(TAG, "Device IP: " + deviceIP);

            if (deviceIP.equals("127.0.0.1") || deviceIP.equals("0.0.0.0")) {
                Log.e(TAG, "Invalid device IP: " + deviceIP);
                updateStatus("Failed - no valid IP address");
                return false;
            }

            Log.i(TAG, "Step 3: Discovering ADB port...");
            updateNotification("Discovering ADB port...");
            updateStatus("Discovering ADB port...");

            int port = discoverAdbPortViaMdns();
            if (port == -1) {
                Log.i(TAG, "mDNS failed, falling back to port scan...");
                updateNotification("mDNS failed, scanning ports...");
                updateStatus("mDNS failed, scanning ports...");
                port = scanForAdbPort();
            }

            if (port == -1) {
                Log.e(TAG, "Could not find ADB port");
                updateStatus("Failed - port not found");
                updateNotification("Failed - port not found");
                return false;
            }

            Log.i(TAG, "Found ADB on port " + port);
            saveLastPort(port);

            Log.i(TAG, "Step 4: Switching to port 5555...");
            updateNotification("Switching to port 5555...");
            updateStatus("Switching to port 5555...");

            AdbHelper adbHelper = new AdbHelper(this);
            boolean success = adbHelper.switchToPort5555("127.0.0.1", port);  // Use localhost


            if (success) {
                Log.i(TAG, "Successfully configured ADB on port 5555!");
                updateStatus("Success - ADB on port 5555");
                updateNotification("Success - ADB on port 5555");
                return true;
            } else {
                Log.e(TAG, "Failed to switch to port 5555");
                updateStatus("Failed - could not switch port");
                updateNotification("Failed - could not switch port");
                return false;
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied - grant WRITE_SECURE_SETTINGS via ADB", e);
            updateStatus("Failed - permission denied");
            updateNotification("Failed - permission denied");
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
            updateStatus("Failed - interrupted");
            updateNotification("Failed - interrupted");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            updateStatus("Failed - " + e.getMessage());
            updateNotification("Failed - error");
            return false;
        }
    }

    private int discoverAdbPortViaMdns() {
        final int[] discoveredPort = {-1};
        final CountDownLatch latch = new CountDownLatch(1);
        String deviceIP = getDeviceIP();
        Log.i(TAG, "Looking for mDNS service on device IP: " + deviceIP);

        NsdManager nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (nsdManager == null) {
            Log.e(TAG, "NsdManager not available");
            return -1;
        }

        final NsdManager.DiscoveryListener[] discoveryListenerHolder = new NsdManager.DiscoveryListener[1];

        NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.i(TAG, "mDNS discovery started for " + serviceType);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Service found: " + serviceInfo.getServiceName());
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "Resolve failed: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.i(TAG, "Service resolved: " + serviceInfo.getServiceName());
                        if (serviceInfo.getHost() != null) {
                            InetAddress hostAddress = serviceInfo.getHost();
                            String host = hostAddress.getHostAddress();
                            if (host == null) {
                                Log.w(TAG, "Host address is null");
                                return;
                            }

                            int port = serviceInfo.getPort();
                            Log.i(TAG, "Host: " + host + ", Port: " + port);
                            if (host.startsWith("127.") || host.equals("::1") ||
                                    host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                                if (host.equals(deviceIP)) {
                                    Log.i(TAG, "Found matching device with IP: " + deviceIP + ", Port: " + port);
                                    discoveredPort[0] = port;
                                    // DON'T countdown - let timeout handle it to get the latest port
                                } else {
                                    Log.w(TAG, "Skipping device with IP " + host + " (looking for " + deviceIP + ")");
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "Service lost: " + serviceInfo.getServiceName());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: error " + errorCode);
                latch.countDown();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: error " + errorCode);
            }
        };

        discoveryListenerHolder[0] = discoveryListener;

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            // Wait full 10 seconds for discovery to find all updates and get the latest port
            boolean found = latch.await(10, TimeUnit.SECONDS);
            Log.i(TAG, "Discovery timeout reached, final port: " + discoveredPort[0]);

            try {
                if (discoveryListenerHolder[0] != null) {
                    nsdManager.stopServiceDiscovery(discoveryListenerHolder[0]);
                    Log.i(TAG, "Discovery stopped, using port: " + discoveredPort[0]);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error stopping discovery", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "mDNS discovery error", e);
        }

        return discoveredPort[0];
    }


    private int scanForAdbPort() {
        Log.i(TAG, "Starting optimized port scan...");
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int lastPort = prefs.getInt(KEY_LAST_PORT, -1);
        String deviceIP = getDeviceIP();

        Log.i(TAG, "Scanning with device IP: " + deviceIP);

        AdbHelper adbHelper = new AdbHelper(this);

        if (lastPort > 0 && adbHelper.connect(deviceIP, lastPort)) {
            Log.i(TAG, "Found ADB on previously used port: " + lastPort);
            return lastPort;
        }

        int[] commonBases = {37000, 38000, 39000, 40000, 41000, 42000, 35000, 36000, 43000, 44000};
        for (int base : commonBases) {
            Log.i(TAG, "Checking range " + base + "-" + (base + 999) + "...");
            updateNotification("Scanning ports " + base + "...");

            for (int offset = 0; offset < 1000; offset += 5) {
                int port = base + offset;
                if (adbHelper.connect(deviceIP, port)) {
                    Log.i(TAG, "Found ADB on port: " + port);
                    return port;
                }
            }
        }
        return -1;
    }

    private String getDeviceIP() {
        try {
            WifiManager wifiManager = (WifiManager)
                    getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return "127.0.0.1";
            }

            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            byte[] ipBytes = ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(ipAddress)
                    .array();

            InetAddress inetAddress = InetAddress.getByAddress(ipBytes);
            String result = inetAddress.getHostAddress();
            return (result != null) ? result : "127.0.0.1";
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device IP", e);
            return "127.0.0.1";
        }
    }

    private void updateStatus(String status) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_STATUS, status).apply();
    }

    private void saveLastPort(int port) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_LAST_PORT, port).apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ADB Configuration",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("ADB auto-configuration service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("ADB Auto-Enable")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build();
    }

    private void updateNotification(String text) {
        try {
            Notification notification = createNotification(text);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(1, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }
}
