package android.os;

interface IPowerManager {
    void reboot(boolean confirm, String reason, boolean wait);
    void shutdown(boolean confirm, String reason, boolean wait);
}
