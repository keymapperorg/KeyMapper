package com.tpn.adbautoenable;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.provider.Settings;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebServer extends NanoHTTPD {
    private static final String TAG = "ADBAutoEnable";
    private static final String PREFS_NAME = "ADBAutoEnablePrefs";
    private static final String SERVICE_TYPE = "_adb-tls-connect._tcp";

    private final Context context;
    private final AdbHelper adbHelper;
    private Boolean permissionCached = null;
    public WebServer(Context context, int port) {
        super(port);
        this.context = context;
        this.adbHelper = new AdbHelper(context);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        if (uri.equals("/api/pair") && method == Method.POST) {
            return handlePairing(session);
        } else if (uri.equals("/api/status")) {
            return handleStatus();
        } else if (uri.equals("/api/test")) {
            return handleTest();
        } else if (uri.equals("/api/switch")) {
            return handleSwitch();
        } else if (uri.equals("/api/logs")) {
            return handleLogs();
        } else if (uri.equals("/api/reset") && method == Method.POST) {
            return handleReset();
        } else {
            return newFixedLengthResponse(getHTML());
        }
    }

    private Response handlePairing(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, List<String>> params = session.getParameters();

            List<String> portList = params.get("port");
            List<String> codeList = params.get("code");

            String portStr = (portList != null && !portList.isEmpty()) ? portList.get(0) : null;
            String code = (codeList != null && !codeList.isEmpty()) ? codeList.get(0) : null;

            Log.i(TAG, "Web API: Received pairing request - port: " + portStr + ", code: " + code);

            if (portStr == null || code == null || portStr.isEmpty() || code.isEmpty()) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                        "{\"error\":\"Port and code required\"}");
            }

            int port = Integer.parseInt(portStr);
            Log.i(TAG, "Web API: Pairing on port " + port + " with code " + code);

            boolean success = adbHelper.pair("127.0.0.1", port, code);

            if (success) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean("is_paired", true).apply();
                Log.i(TAG, "Web API: Pairing successful");

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        Log.i(TAG, "Attempting to self-grant WRITE_SECURE_SETTINGS permission");

                        int adbPort = discoverAdbPort();
                        if (adbPort == -1) {
                            Log.w(TAG, "Could not discover ADB port for self-grant, skipping");
                            return;
                        }

                        Log.i(TAG, "Found ADB on port " + adbPort + ", attempting self-grant");
                        boolean granted = adbHelper.selfGrantPermission("127.0.0.1", adbPort,
                                "com.tpn.adbautoenable", "android.permission.WRITE_SECURE_SETTINGS");

                        if (granted) {
                            Log.i(TAG, "Successfully self-granted WRITE_SECURE_SETTINGS permission!");
                        } else {
                            Log.w(TAG, "Failed to self-grant permission, user will need to grant manually");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error during self-grant attempt", e);
                    }
                }).start();

                return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"success\":true,\"message\":\"Pairing successful! Attempting to self-grant permissions...\"}");
            } else {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                        "{\"error\":\"Pairing failed. Make sure wireless debugging is enabled and code is correct.\"}");
            }

        } catch (NumberFormatException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                    "{\"error\":\"Invalid port number\"}");
        } catch (Exception e) {
            Log.e(TAG, "Web API: Pairing error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }



    private Response handleStatus() {
        Log.d(TAG, "handleStatus() called - creating socket for port check");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastStatus = prefs.getString("last_status", "Not run yet");
        int lastPort = prefs.getInt("last_port", -1);
        boolean isPaired = prefs.getBoolean("is_paired", false);

        Log.d(TAG, "handleStatus() - calling checkPort5555()");
        boolean adb5555Available = checkPort5555();

        // Cache permission check - only do it once
        if (permissionCached == null) {
            Log.d(TAG, "handleStatus() - checking WRITE_SECURE_SETTINGS permission (cached)");
            try {
                Settings.Global.putInt(context.getContentResolver(), "adb_wifi_enabled", 1);
                permissionCached = true;
                Log.d(TAG, "handleStatus() - permission check SUCCESS");
            } catch (SecurityException e) {
                permissionCached = false;
                Log.d(TAG, "handleStatus() - permission check FAILED");
            }
        }

        String json = String.format(Locale.US,
                "{\"lastStatus\":\"%s\",\"lastPort\":%d,\"isPaired\":%b,\"hasPermission\":%b,\"adb5555Available\":%b}",
                lastStatus, lastPort, isPaired, permissionCached, adb5555Available
        );
        Log.d(TAG, "handleStatus() completed");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }


    private boolean checkPermission() {
        try {
            // Just READ, don't write!
            Settings.Global.getInt(context.getContentResolver(), "adb_wifi_enabled", 0);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }


    // Cache the permission check result
    private Boolean cachedPermissionCheck = null;

    private boolean checkWriteSettingsPermission() {
        if (cachedPermissionCheck != null) {
            return cachedPermissionCheck;
        }

        try {
            // Just CHECK, don't actually write
            int current = Settings.Global.getInt(context.getContentResolver(), "adb_wifi_enabled", 0);
            cachedPermissionCheck = true;  // If we can read, we likely have permission
            return true;
        } catch (Exception e) {
            cachedPermissionCheck = false;
            return false;
        }
    }


    private Response handleLogs() {
        Log.d(TAG, "handleLogs() called - spawning logcat process");
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-s", "ADBAutoEnable:*"});

            StringBuilder logs = new StringBuilder();
            // Use try-with-resources for BOTH InputStreamReader and BufferedReader
            try (InputStreamReader isr = new InputStreamReader(process.getInputStream());
                 BufferedReader reader = new BufferedReader(isr)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                }
            } // Readers automatically closed here

            // Wait for process to complete
            process.waitFor();

            String logsText = logs.toString();
            if (logsText.isEmpty()) {
                logsText = "No logs found for ADBAutoEnable";
            }

            logsText = logsText.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            Log.d(TAG, "handleLogs() completed - returning " + logsText.length() + " characters");
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    "{\"logs\":\"" + logsText + "\"}");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read logs", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"Failed to read logs: " + e.getMessage() + "\"}");
        } finally {
            if (process != null) {
                process.destroy();
                Log.d(TAG, "handleLogs() - process destroyed");
            }
        }
    }



    private Response handleReset() {
        try {
            Log.i(TAG, "Web API: Resetting pairing status");

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_paired", false)
                    .apply();

            File keyDir = new File(context.getFilesDir(), "adb_key");
            File pubKeyFile = new File(context.getFilesDir(), "adb_key.pub");
            File certFile = new File(context.getFilesDir(), "adb_cert");

            boolean deleted1 = keyDir.delete();
            boolean deleted2 = pubKeyFile.delete();
            boolean deleted3 = certFile.delete();

            Log.i(TAG, "Deleted adb_key: " + deleted1);
            Log.i(TAG, "Deleted adb_key.pub: " + deleted2);
            Log.i(TAG, "Deleted adb_cert: " + deleted3);

            Log.i(TAG, "Pairing reset successful");
            return newFixedLengthResponse(Response.Status.OK, "application/json",
                    "{\"success\":true,\"message\":\"Pairing reset successful. Please pair again.\"}");

        } catch (Exception e) {
            Log.e(TAG, "Web API: Reset error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private boolean checkPort5555() {
        try {
            Socket socket = new Socket("127.0.0.1", 5555);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Response handleTest() {
        new Thread(() -> {
            BootReceiver receiver = new BootReceiver();
            receiver.onReceive(context, new android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED));
        }).start();

        return newFixedLengthResponse(Response.Status.OK, "application/json",
                "{\"success\":true,\"message\":\"Boot test started. Check logs below for progress.\"}");
    }

    private Response handleSwitch() {
        new Thread(() -> {
            try {
                Log.i(TAG, "Web API: Discovering ADB port...");
                int port = discoverAdbPort();

                if (port == -1) {
                    Log.e(TAG, "Web API: Could not find ADB port");
                    return;
                }

                Log.i(TAG, "Web API: Found ADB on port " + port + ", switching to 5555...");
                boolean success = adbHelper.switchToPort5555("127.0.0.1", port);  // Use localhost for port 5555

                if (success) {
                    Log.i(TAG, "Web API: Successfully switched to port 5555");
                } else {
                    Log.e(TAG, "Web API: Failed to switch to port 5555");
                }

            } catch (Exception e) {
                Log.e(TAG, "Web API: Switch error", e);
            }

        }).start();

        return newFixedLengthResponse(Response.Status.OK, "application/json",
                "{\"success\":true,\"message\":\"Port switch started. Check logs below for status.\"}");
    }


    private int discoverAdbPort() {
        final int[] discoveredPort = {-1};
        final CountDownLatch latch = new CountDownLatch(1);
        String deviceIP = getDeviceIP();

        Log.i(TAG, "Looking for mDNS service on device IP: " + deviceIP);

        NsdManager nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
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
            boolean found = latch.await(10, TimeUnit.SECONDS);
            if (!found) {
                Log.e(TAG, "mDNS discovery timed out after 10 seconds");
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener);
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping discovery after timeout", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "mDNS discovery error", e);
        }

        return discoveredPort[0];
    }

    private String getDeviceIP() {
        try {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

    private String getHTML() {
        String deviceIP = getDeviceIP();
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>ADB Auto-Enable Configuration</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; max-width: 800px; margin: 20px auto; padding: 20px; background: #f5f5f5; }\n" +
                "        .card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                "        h1 { color: #333; margin-top: 0; }\n" +
                "        h2 { color: #666; font-size: 18px; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }\n" +
                "        button { background: #4CAF50; color: white; border: none; padding: 12px 24px; font-size: 16px; border-radius: 4px; cursor: pointer; margin: 5px; }\n" +
                "        button:hover { background: #45a049; }\n" +
                "        button.secondary { background: #2196F3; }\n" +
                "        button.secondary:hover { background: #0b7dda; }\n" +
                "        button.danger { background: #f44336; }\n" +
                "        button.danger:hover { background: #da190b; }\n" +
                "        input { padding: 10px; font-size: 14px; border: 1px solid #ddd; border-radius: 4px; width: 200px; margin: 5px; }\n" +
                "        .status { padding: 8px; border-radius: 4px; margin: 5px 0; }\n" +
                "        .status.good { background: #d4edda; color: #155724; }\n" +
                "        .status.bad { background: #f8d7da; color: #721c24; }\n" +
                "        code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: monospace; display: block; margin: 10px 0; white-space: pre-wrap; word-break: break-all; }\n" +
                "        .instruction { background: #e3f2fd; padding: 15px; border-radius: 4px; margin: 10px 0; }\n" +
                "        .success { background: #d4edda; color: #155724; padding: 10px; border-radius: 4px; margin: 10px 0; display: none; }\n" +
                "        .error { background: #f8d7da; color: #721c24; padding: 10px; border-radius: 4px; margin: 10px 0; display: none; }\n" +
                "        .info { background: #d1ecf1; color: #0c5460; padding: 10px; border-radius: 4px; margin: 10px 0; display: none; }\n" +
                "        .status-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #eee; }\n" +
                "        .status-row:last-child { border-bottom: none; }\n" +
                "        .status-label { font-weight: bold; color: #666; min-width: 150px; }\n" +
                "        .status-value { flex: 1; text-align: right; }\n" +
                "        #logs-container { background: #1e1e1e; color: #d4d4d4; font-family: 'Courier New', monospace; font-size: 12px; padding: 15px; border-radius: 4px; max-height: 400px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; user-select: text; }\n" +
                "        .logs-controls { margin-bottom: 10px; }\n" +
                "        .paused { background: #ff9800; color: white; padding: 5px 10px; border-radius: 3px; font-size: 12px; margin-left: 10px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>🔧 ADB Auto-Enable Configuration</h1>\n" +
                "    \n" +
                "    <div class=\"card\">\n" +
                "        <h2>📊 System Status</h2>\n" +
                "        <div id=\"status-display\">\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">Permission:</div>\n" +
                "                <div class=\"status-value\" id=\"permission-status\">Loading...</div>\n" +
                "            </div>\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">Pairing Status:</div>\n" +
                "                <div class=\"status-value\" id=\"pairing-status\">Loading...</div>\n" +
                "            </div>\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">ADB Port 5555:</div>\n" +
                "                <div class=\"status-value\" id=\"port-status\">Loading...</div>\n" +
                "            </div>\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">Device IP:</div>\n" +
                "                <div class=\"status-value\">" + deviceIP + "</div>\n" +
                "            </div>\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">Last Boot Status:</div>\n" +
                "                <div class=\"status-value\" id=\"last-status\">Loading...</div>\n" +
                "            </div>\n" +
                "            <div class=\"status-row\">\n" +
                "                <div class=\"status-label\">Last Port:</div>\n" +
                "                <div class=\"status-value\" id=\"last-port\">Loading...</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <button onclick=\"refreshStatus()\">🔄 Refresh Status</button>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"card\" id=\"pairing-card\">\n" +
                "        <h2>🔐 Initial Pairing (One-Time Setup)</h2>\n" +
                "        <div class=\"instruction\">\n" +
                "            <strong>Step 1:</strong> On your Android device, go to:<br>\n" +
                "            <strong>Settings → Developer Options → Wireless Debugging</strong><br>\n" +
                "            Tap <strong>\"Pair device with pairing code\"</strong><br><br>\n" +
                "            <strong>Step 2:</strong> Copy the pairing code and port shown and enter them below:<br>\n" +
                "        </div>\n" +
                "        <div>\n" +
                "            <input type=\"text\" id=\"pair-code\" placeholder=\"Pairing Code\" />\n" +
                "            <input type=\"number\" id=\"pair-port\" placeholder=\"Pairing Port\" />\n" +
                "            <button onclick=\"pairDevice()\">🔗 Pair Device</button>\n" +
                "        </div>\n" +
                "        <div id=\"pair-success\" class=\"success\"></div>\n" +
                "        <div id=\"pair-error\" class=\"error\"></div>\n" +
                "        <p><em>After pairing, the app will attempt to automatically grant itself permissions. Check the status above to verify.</em></p>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"card\" id=\"paired-card\" style=\"display:none\">\n" +
                "        <h2>✅ Device Paired</h2>\n" +
                "        <p>Your device is successfully paired and ready to use!</p>\n" +
                "        <button onclick=\"resetPairing()\" class=\"danger\">🔄 Reset Pairing</button>\n" +
                "        <div id=\"reset-success\" class=\"success\"></div>\n" +
                "        <div id=\"reset-error\" class=\"error\"></div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"card\" id=\"switch-card\">\n" +
                "        <h2>🔄 Switch to Port 5555</h2>\n" +
                "        <div class=\"instruction\">\n" +
                "            After pairing and enabling wireless debugging, switch ADB to port 5555:\n" +
                "        </div>\n" +
                "        <button onclick=\"switchPort()\">🔀 Switch to Port 5555 Now</button>\n" +
                "        <div id=\"switch-info\" class=\"info\"></div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"card\">\n" +
                "        <h2>🧪 Testing</h2>\n" +
                "        <div class=\"instruction\">\n" +
                "            Test the full boot configuration sequence:\n" +
                "        </div>\n" +
                "        <button onclick=\"runTest()\">▶️ Run Test Now</button>\n" +
                "        <div id=\"test-info\" class=\"info\"></div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"card\">\n" +
                "        <h2>📋 Live Logs</h2>\n" +
                "        <div class=\"logs-controls\">\n" +
                "            <button onclick=\"copyLogs()\" class=\"secondary\">📋 Copy to Clipboard</button>\n" +
                "            <span id=\"paused-indicator\" class=\"paused\" style=\"display:none\">Auto-refresh paused</span>\n" +
                "        </div>\n" +
                "        <div id=\"logs-container\">Loading logs...</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <script>\n" +
                "        let autoRefreshPaused = false;\n" +
                "        let logsRefreshInterval;\n" +
                "        \n" +
                "        function refreshStatus() {\n" +
                "            fetch('/api/status')\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    document.getElementById('permission-status').innerHTML = data.hasPermission ? \n" +
                "                        '<span class=\"status good\">✓ Granted</span>' : \n" +
                "                        '<span class=\"status bad\">✗ Not granted</span>';\n" +
                "                    document.getElementById('pairing-status').innerHTML = data.isPaired ? \n" +
                "                        '<span class=\"status good\">✓ Paired</span>' : \n" +
                "                        '<span class=\"status bad\">✗ Not paired</span>';\n" +
                "                    document.getElementById('port-status').innerHTML = data.adb5555Available ? \n" +
                "                        '<span class=\"status good\">✓ Available</span>' : \n" +
                "                        '<span class=\"status bad\">✗ Not available</span>';\n" +
                "                    document.getElementById('last-status').textContent = data.lastStatus;\n" +
                "                    document.getElementById('last-port').textContent = data.lastPort;\n" +
                "                    \n" +
                "                    // Show/hide pairing cards based on status\n" +
                "                    if (data.isPaired) {\n" +
                "                        document.getElementById('pairing-card').style.display = 'none';\n" +
                "                        document.getElementById('paired-card').style.display = 'block';\n" +
                "                    } else {\n" +
                "                        document.getElementById('pairing-card').style.display = 'block';\n" +
                "                        document.getElementById('paired-card').style.display = 'none';\n" +
                "                    }\n" +
                "                    \n" +
                "                    // Show/hide switch card based on port 5555 availability\n" +
                "                    if (data.adb5555Available) {\n" +
                "                        document.getElementById('switch-card').style.display = 'none';\n" +
                "                    } else {\n" +
                "                        document.getElementById('switch-card').style.display = 'block';\n" +
                "                    }\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        function refreshLogs() {\n" +
                "            fetch('/api/logs')\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    const container = document.getElementById('logs-container');\n" +
                "                    const wasScrolledToBottom = container.scrollHeight - container.clientHeight <= container.scrollTop + 1;\n" +
                "                    container.textContent = data.logs || 'No logs available';\n" +
                "                    if (wasScrolledToBottom) {\n" +
                "                        container.scrollTop = container.scrollHeight;\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(e => {\n" +
                "                    document.getElementById('logs-container').textContent = 'Error loading logs: ' + e.message;\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        function copyLogs() {\n" +
                "            const logs = document.getElementById('logs-container').textContent;\n" +
                "            \n" +
                "            // Try modern Clipboard API first (secure contexts only)\n" +
                "            if (navigator.clipboard && navigator.clipboard.writeText) {\n" +
                "                navigator.clipboard.writeText(logs).then(() => {\n" +
                "                    const btn = event.target;\n" +
                "                    const originalText = btn.textContent;\n" +
                "                    btn.textContent = '✓ Copied!';\n" +
                "                    setTimeout(() => { btn.textContent = originalText; }, 2000);\n" +
                "                }).catch(e => {\n" +
                "                    console.log('Clipboard API failed, using fallback method');\n" +
                "                    copyLogsViaTextarea(logs, event.target);\n" +
                "                });\n" +
                "            } else {\n" +
                "                // Fallback for non-secure contexts\n" +
                "                copyLogsViaTextarea(logs, event.target);\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function copyLogsViaTextarea(text, btn) {\n" +
                "            const textarea = document.createElement('textarea');\n" +
                "            textarea.value = text;\n" +
                "            document.body.appendChild(textarea);\n" +
                "            textarea.select();\n" +
                "            document.execCommand('copy');\n" +
                "            document.body.removeChild(textarea);\n" +
                "            const originalText = btn.textContent;\n" +
                "            btn.textContent = '✓ Copied!';\n" +
                "            setTimeout(() => { btn.textContent = originalText; }, 2000);\n" +
                "        }\n" +
                "        \n" +
                "        function resetPairing() {\n" +
                "            const successDiv = document.getElementById('reset-success');\n" +
                "            const errorDiv = document.getElementById('reset-error');\n" +
                "            \n" +
                "            if (confirm('Are you sure you want to reset pairing? You will need to pair again.')) {\n" +
                "                fetch('/api/reset', {\n" +
                "                    method: 'POST'\n" +
                "                })\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    if (data.success) {\n" +
                "                        successDiv.textContent = data.message;\n" +
                "                        successDiv.style.display = 'block';\n" +
                "                        errorDiv.style.display = 'none';\n" +
                "                        setTimeout(() => {\n" +
                "                            successDiv.style.display = 'none';\n" +
                "                            refreshStatus();\n" +
                "                        }, 3000);\n" +
                "                    } else {\n" +
                "                        errorDiv.textContent = 'Reset failed: ' + (data.error || 'Unknown error');\n" +
                "                        errorDiv.style.display = 'block';\n" +
                "                        successDiv.style.display = 'none';\n" +
                "                    }\n" +
                "                })\n" +
                "                .catch(e => {\n" +
                "                    errorDiv.textContent = 'Error: ' + e.message;\n" +
                "                    errorDiv.style.display = 'block';\n" +
                "                    successDiv.style.display = 'none';\n" +
                "                });\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        function pairDevice() {\n" +
                "            const port = document.getElementById('pair-port').value;\n" +
                "            const code = document.getElementById('pair-code').value;\n" +
                "            const successDiv = document.getElementById('pair-success');\n" +
                "            const errorDiv = document.getElementById('pair-error');\n" +
                "            \n" +
                "            successDiv.style.display = 'none';\n" +
                "            errorDiv.style.display = 'none';\n" +
                "            \n" +
                "            fetch('/api/pair', {\n" +
                "                method: 'POST',\n" +
                "                headers: {'Content-Type': 'application/x-www-form-urlencoded'},\n" +
                "                body: 'port=' + port + '&code=' + code\n" +
                "            })\n" +
                "            .then(r => r.json())\n" +
                "            .then(data => {\n" +
                "                if (data.success) {\n" +
                "                    successDiv.textContent = data.message;\n" +
                "                    successDiv.style.display = 'block';\n" +
                "                    setTimeout(refreshStatus, 2000);\n" +
                "                } else {\n" +
                "                    errorDiv.textContent = data.error || 'Pairing failed';\n" +
                "                    errorDiv.style.display = 'block';\n" +
                "                }\n" +
                "            })\n" +
                "            .catch(e => {\n" +
                "                errorDiv.textContent = 'Error: ' + e.message;\n" +
                "                errorDiv.style.display = 'block';\n" +
                "            });\n" +
                "        }\n" +
                "        \n" +
                "        function switchPort() {\n" +
                "            const infoDiv = document.getElementById('switch-info');\n" +
                "            \n" +
                "            fetch('/api/switch')\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    infoDiv.textContent = data.message;\n" +
                "                    infoDiv.style.display = 'block';\n" +
                "                    setTimeout(() => {\n" +
                "                        infoDiv.style.display = 'none';\n" +
                "                        refreshStatus();\n" +
                "                        refreshLogs();\n" +
                "                    }, 5000);\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        function runTest() {\n" +
                "            const infoDiv = document.getElementById('test-info');\n" +
                "            \n" +
                "            fetch('/api/test')\n" +
                "                .then(r => r.json())\n" +
                "                .then(data => {\n" +
                "                    infoDiv.textContent = data.message;\n" +
                "                    infoDiv.style.display = 'block';\n" +
                "                    setTimeout(() => {\n" +
                "                        infoDiv.style.display = 'none';\n" +
                "                        refreshLogs();\n" +
                "                    }, 3000);\n" +
                "                });\n" +
                "        }\n" +
                "        \n" +
                "        // Pause auto-refresh when user selects text in logs\n" +
                "        document.addEventListener('DOMContentLoaded', function() {\n" +
                "            const logsContainer = document.getElementById('logs-container');\n" +
                "            const pausedIndicator = document.getElementById('paused-indicator');\n" +
                "            \n" +
                "            logsContainer.addEventListener('mousedown', function() {\n" +
                "                autoRefreshPaused = true;\n" +
                "                pausedIndicator.style.display = 'inline';\n" +
                "                clearInterval(logsRefreshInterval);\n" +
                "            });\n" +
                "            \n" +
                "            document.addEventListener('mouseup', function() {\n" +
                "                setTimeout(() => {\n" +
                "                    if (window.getSelection().toString().length === 0) {\n" +
                "                        autoRefreshPaused = false;\n" +
                "                        pausedIndicator.style.display = 'none';\n" +
                "                        startLogsAutoRefresh();\n" +
                "                    }\n" +
                "                }, 100);\n" +
                "            });\n" +
                "        });\n" +
                "        \n" +
                "        function startLogsAutoRefresh() {\n" +
                "            if (!autoRefreshPaused) {\n" +
                "                logsRefreshInterval = setInterval(() => {\n" +
                "                    if (!autoRefreshPaused) {\n" +
                "                        refreshLogs();\n" +
                "                    }\n" +
                "                }, 3000);\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        // Initial load\n" +
                "        refreshStatus();\n" +
                "        refreshLogs();\n" +
                "        \n" +
                "        // Auto-refresh\n" +
                "        setInterval(refreshStatus, 5000);\n" +
                "        startLogsAutoRefresh();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}