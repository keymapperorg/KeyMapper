:fontawesome-solid-check-square:{: .accent-light } &nbsp; Detect trigger when screen is off. ROOT only.

These are the buttons which can be detected when the screen is off. Let the developer know about any buttons you would
also like to be supported.

!!! note Please send the output of pressing the buttons while running the `adb shell getevent -lq` command so the
developer knows which key event name they need to add.

* Volume Up
* Volume Down
* Headset button
* Camera focus button
* Camera button
* Menu button (on some devices the Bixby button is detected by Key Mapper as a menu button)
* Search button