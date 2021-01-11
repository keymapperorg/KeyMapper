This page aims to give users an introduction to the Key Mapper interface and a demonstration on how to perform typical tasks with the app.
For specific troubleshooting, consult the FAQ.



Where screenshots are necessary, this guide uses two devices to demonstrate.

1. OnePlus 7 Pro, Android 11, Dark Theme, high resolution/dpi screen/1440p 16:9
2. Samsung J3, Android 8, Light Theme, typical resolution/dpi screen/720p 16:9

App interface pictured: `keymapper-2.3.0-alpha-ci.709`

The guide is updated to match current stable release UI. [Report inaccuracies.](https://github.com/sds100/KeyMapper/issues/new)

## First time setup

When you open Key Mapper, you will be presented with this screen.
This screen is referred to as the *Key Mapper home screen*.

![](images/hg-home-empty.png)

Tapping the FIX button next to the accessibility service prompt will allow you to grant Key Mapper permission to run its accessibility service. You must do this for the app to work at all.

![](images/hg-warn-service.png)

You can then tap Key Mapper in your list of available services. There may be many others on this screen and your interface may be slightly different. If nothing happens when you tap FIX, or if you can't enable the service, see [this page](#) or ask for help in the [Discord server](http://keymapper.club).

![](images/hg-settings-services.png)

Tapping the FIX button next to the input method prompt will allow you to enable a Key Mapper compatible keyboard. If you want to perform actions like pressing keyboard keys or entering text you must enable this.

![](images/hg-warn-input-method.png)

If you have only installed the Key Mapper app, your list of available keyboards will contain "Key Mapper Basic Input Method" only. If you aim to use a physical keyboard (not on-screen) when using Key Mapper, this will be suitable for you.

If you want an on-screen keyboard to be available while using Key Mapper, you need to install "Key Mapper GUI Keyboard". It is available on the [Google Play Store](http://gui.keymapper.club), [F-Droid](https://www.f-droid.org/en/packages/io.github.sds100.keymapper.inputmethod.latin/) and [GitHub](https://github.com/sds100/KeyMapperKeyboard/releases).

![](images/hg-settings-input-method.png)

If you are using a Huawei or Xiaomi branded device, a device with 2GB of RAM or less, or intend to use the app while playing a mobile game, it is especially important to follow the next step.

Tapping the FIX button next to the battery optimisation prompt will allow you to turn off features of your device that may prevent Key Mapper's essential services from running in the background. This is usually the source of the common 'Key Mapper randomly stops working' issue.

![](images/hg-warn-battery-optimisation.png)

The screen that opens after tapping FIX may vary depending on your device. In my case, to disable optimisation I found the app(s) in the list, tapped them to open their settings, and chose 'Don't optimise'.
There is an excellent guide at [dontkillmyapp.com](http://dontkillmyapp.com) that explains how to disable battery and/or memory optimisation for your specific device. If Key Mapper randomly stops working even after you complete these steps, ask for help in [the Discord server.](http://keymapper.club)