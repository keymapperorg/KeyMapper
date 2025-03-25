Constraints allow you to restrict your mappings to only work in some situations.

If you have more than two constraints you can choose between an 'AND' and 'OR' mode. For 'AND' mode all the constraints need to be satisfied and for 'OR' mode at least one needs to be.
![](../images/constraint-mode-radio-buttons.png)

### App in/not foreground
Your mapping will only work if your app of choice is the actively selected window, i.e. on screen and being interacted with. 'App not in foreground' will likewise restrict your map to working only if your app of choice isn't in focus.

### App playing media (2.2.0+)
Your mapping will only work if your app of choice is playing media.

### App not playing media (2.4.0+)
Your mapping will only work if your app of choice is not playing media.

### Media playing (2.4.0+)
Your mapping will only work if any media is playing.

### Media not playing (2.4.0+)
Your mapping will only work if no media is playing.

### Bluetooth device is connected/disconnected
Your mapping will only work if a specific bluetooth device is connected/disconnected.

### Orientation (2.2.0+)
This will restrict your gesture map to work only when the device is set to a specific screen orientation.

### Screen is on/off

!!! info "Only for key maps"

!!! attention
    If you are not using a custom trigger then you must [grant Key Mapper root permission](settings.md#key-mapper-has-root-permission) and select the [option](../keymaps#special-options) to detect the key map when the screen is off.

Only allow the key map to be triggered when the screen is on or off.

### Flashlight is on/off (2.4.0+, Android 6.0+)

Your mapping will only work if the front or back flashlight is on/off.

### WiFi on/off/connected/disconnect (2.4.0+)

Your mapping will only work if it your device's WiFi is on/off or connected/disconnected to a
network.

### Input method is/not chosen (2.4.0+)

Your mapping will only work if the input method that you are using matches the constraint.

### Device is locked (2.4.0+, Android 5.1+)

Your mapping will only work if the device is locked. The screen must still be on because Android doesn't allow apps to
detect button presses when the screen is off.

### Device is unlocked (2.4.0+, Android 5.1+)

Your mapping will only work if the device is unlocked.

### Lockscreen is (not) showing (3.0.0+)

Your key map will only work if the lock screen is (not) showing.

### In phone call/not in phone call/phone ringing (2.4.0+)

Your mapping will only be triggered if you are (not) in a phone call or if your device is ringing.

### Charging/discharging (2.4.1+)

Your mapping will only be triggered if your device is charging or discharging.