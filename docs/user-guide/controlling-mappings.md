There are many ways to pause and resume your mappings. These are very useful features because they allow to easily turn off your key maps if you don't need to use them.

###  Press the button in the home screen menu
![](../images/home-menu.png)

### Use the notification

![](../images/notification-toggle-mappings.png)

### Use the quick settings tile

![](../images/pause-keymaps-quick-settings.png)

### Intent API (2.3.0+)

This will allow other apps to pause and resume your mappings by broadcasting an Intent to Key Mapper. The properties for the Intent are shown below

#### Action (choose one)
```
io.github.sds100.keymapper.ACTION_PAUSE_MAPPINGS
```
```
io.github.sds100.keymapper.ACTION_RESUME_MAPPINGS
```
```
io.github.sds100.keymapper.ACTION_TOGGLE_MAPPINGS
```
  
#### Package

```
io.github.sds100.keymapper
```

Add `.debug` or `.ci` to the end of the package name if you are using a debug or ci build of Key Mapper.

#### Class

```
io.github.sds100.keymapper.api.PauseMappingsBroadcastReceiver
```