Constraints allow you to restrict your mappings to only work in some situations.

If you have more than two constraints you can choose between an 'AND' and 'OR' mode. For 'AND' mode all the constraints need to be satisfied and for 'OR' mode at least one needs to be.
![](../images/constraint-mode-radio-buttons.png)

### Screen is on/off (ROOT)
!!! info "Only for key maps"

Only allow the key map to be triggered when the screen is on/off.

If you have [root permission](settings.md#key-mapper-has-root-permission) and you have selected the [option](../keymaps#special-options) to detect the key map when the screen is off, you can restrict your mappings to only work when the screen is on or off.

### App in/not foreground
Your mapping will only work if your app of choice is the actively selected window, i.e. on screen and being interacted with. 'App not in foreground' will likewise restrict your map to working only if your app of choice isn't in focus.

### Specific app playing media (2.2.0+)
Your mapping will only work if your app of choice is playing media.

### Bluetooth device is connected/disconnected
Your mapping will only work if a specific bluetooth device is connected/disconnected.

### Orientation (2.2.0+)
This will restrict your gesture map to work only when the device is set to a specific screen orientation.