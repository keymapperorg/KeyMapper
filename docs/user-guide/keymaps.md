## Options

There are options for each trigger key by pressing the 3-dots on them. There are also more options for the whole trigger under the "options" tab. Here are explanations of each option.

### Trigger from other apps (2.3.0+)

![](../images/hg-trigger-from-other-apps.png)

This allows you to trigger the key map by using a shortcut or by sending an [Intent](https://developer.android.com/reference/android/content/Intent) to Key Mapper. Turning this off will stop any shortcuts or Intents for this key map from working.

### Home screen (launcher) shortcut

You can create a home screen shortcut by tapping "create launcher shortcut" or by adding the shortcut to your home screen in the same way as a widget. You can also use this shortcut from automation apps like Tasker and Automate without having to create a home screen shortcut.

### Intent

There are many apps that can automate broadcasting Intents such as Tasker and Automate.

#### Intent action

```
io.github.sds100.keymapper.TRIGGER_KEYMAP_BY_UID
```

#### Intent string extra 

```
io.github.sds100.keymapper.KEYMAP_UID
```

and the value is the UUID of the key map. You can copy the UUID in Key Mapper by turning on the "Trigger from other apps" option.

#### Intent package (optional but recommended)

```
io.github.sds100.keymapper
```

This will only send the Intent to Key Mapper and no other packages.

!!! warning
    Add `.debug` or `.ci` to the end of all `io.github.sds100.keymapper` instances in the action, extra and package if your Key Mapper build is a debug or ci build respectively.

### Detect Trigger When Screen is Off (ROOT)

--8<-- "trigger-options/trigger-when-screen-off.md"