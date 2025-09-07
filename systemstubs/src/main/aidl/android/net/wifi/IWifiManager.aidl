package android.net.wifi;

interface IWifiManager {
  boolean setWifiEnabled(String packageName, boolean enable);
}