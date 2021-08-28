## App

Launch an app.

## App shortcut

Launch an app shortcut. This is a great way to add more functionality to Key Mapper.

## Key code

--8<-- "requires-key-mapper-keyboard-or-shizuku.md"

Input a simple key event with this key code.

## Key

--8<-- "requires-key-mapper-keyboard.md"

This is a shortcut for creating a key code action if you aren't sure what the key code of a button that you have is.

## Tap screen (2.1.0+, Android 7.0+)

This will tap a point on your screen.

!!! warning
    Android restricts what apps can do with this so you won't be able to tap the screen at the same time as Key Mapper and Key Mapper can't tap multiple places at once.

## Key event (2.1.0+)

--8<-- "requires-key-mapper-keyboard-or-shizuku.md"

This will input a more complicated key event than the simple key code action.

## Text

--8<-- "requires-key-mapper-keyboard.md"

This will input any text that you want.

## Intent (2.3.0+)

This is a way to communicate with other apps, especially automation apps because they often have a way to trigger their own actions from an intent.

Read about intents in the Android SDK documentation [here](https://developer.android.com/reference/android/content/Intent).

## Phone call (2.3.0+)

This will start calling the number that you put.

## Sound (2.4.0+)

This action will play a sound. Key Mapper will copy the sound file to its own folder, which means you won't have to worry about losing them. Key Mapper will only play one sound at a time and the sound will play forever until it stops or you pause your key maps. Your sounds will be backed up and restored with your key maps as well.

## System

There are many actions of this type. They all do something related to your device's system.

#### Toggle/enable/disable WiFi

!!! attention "Requires ROOT permission on Android 10+"

#### Toggle/enable/disable Bluetooth

#### Toggle/enable/disable mobile data

!!! attention "Requires ROOT permission"

#### Toggle/enable/disable auto brightness

#### Increase/decrease brightness

#### Toggle/enable/disable auto-rotate

#### Set screen to portrait/landscape

#### Set screen orientation to 0°, 90°, 180° or 270°

#### Cycle through screen orientations

#### Volume up/down

#### Mute/un-mute/toggle volume (Android 6.0+)

#### Show volume popup

#### Increase/decrease volume stream
This will increase or decrease a specific one of these volume streams.

- Alarm
- DTMF
- Music
- Notification
- Ring
- System
- Voice call
- Accessibility

#### Cycle through ringer modes

#### Change ringer mode

#### Cycle between vibrate and ring

#### Toggle/enable/disable do not disturb mode (Android 6.0+)

#### Expand/toggle notification drawer

#### Expand/toggle quick settings

#### Collapse the status bar

#### Play/pause/toggle media

#### Next track

#### Previous track

#### Fast forward

#### Rewind

#### Go back

#### Go home

#### Open recent apps

#### Toggle split screen (Android 7.0+)

#### Go to the last app (Android 7.0+)

#### Open menu

#### Toggle/enable/disable flashlight

#### Toggle/enable/disable NFC

!!! attention "Requires ROOT permission"

#### Move cursor to the end of text

#### Toggle/show/hide on-screen keyboard (Android 7.0+)

#### Show keyboard picker (up to Android 9.0)

!!! attention "Requires ROOT permission on Android 8.1 and 9.0"

#### Cut/copy/paste

#### Select word at cursor

#### Switch keyboard

#### Toggle/enable/disable airplane mode

#### Take screenshot

!!! attention "Requires ROOT permission on Android 8.1 and older"

#### Open voice assistant

#### Open device assistant

#### Open camera app

#### Open settings app

#### Lock device

!!! attention "Requires ROOT permission on Android 8.1 and older"

#### Secure lock device
This is different to 'lock device' because this will force you to unlock your device with your PIN or password and not with biometrics.

#### Turn on/off device

!!! attention "Requires ROOT permission"

#### Show power menu

#### Do nothing

#### Dismiss all notifications (2.4.0+)

#### Dismiss most recent notification (2.4.0+)