# Installing Keymapper on Oculus Quest v40+ Software

### About
The Oculus Quest v40 software broke accessibility permissions for a lot of apps, Key Mapper included. These steps should allow you to use Key Mapper on the Oculus Quest freely. For this, you will need your Quest up and connected to ADB. You will likely need the ADB drivers for the ADB shell to pick it up.

[https://developer.oculus.com/downloads/package/oculus-adb-drivers/](https://developer.oculus.com/downloads/package/oculus-adb-drivers/)

### Enable Accessibility and Ability to Modify System Settings:

```
adb shell settings put secure enabled_accessibility_services io.github.sds100.keymapper/io.github.sds100.keymapper.debug.service.MyAccessibilityService
```
```
adb shell pm grant io.github.sds100.keymapper android.permission.WRITE_SECURE_SETTINGS
```
### Set Key Mapper as Device Admin so it Cannot Be Overridden:

```
adb shell dpm set-active-admin --user current io.github.sds100.keymapper/io.github.sds100.keymapper.system.DeviceAdmin
```

!!! Uninstalling
    If you ever wish to uninstall KeyMapper from your Quest after running this command you must run the following command first:
    ```
    adb shell pm disable-user io.github.sds100.keymapper
    ```


### Enable Do Not Disturb Permissions

```
adb shell cmd notification allow_listener io.github.sds100.keymapper/io.github.sds100.keymapper.system.notifications.NotificationReceiverio.github.sds100.keymapper/io.github.sds100.keymapper.system.notifications.NotificationReceiver
```
### Disable Battery Optimisation:
```
adb shell dumpsys deviceidle whitelist +io.github.sds100.keymapper
```

<!---Written by GL513 - https://github.com/GL513 :)--->
