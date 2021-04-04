## Becoming a tester

You can get the apks for the pre-release versions in 2 ways:

1. Join [the Discord server](http://keymapper.club) and download the apks from the #testing-builds channel.
2. Download the apk from GitHub Actions. This requires a GitHub account.
    You can get alpha builds [here](https://github.com/sds100/KeyMapper/actions/workflows/android.yml) and beta builds [here](https://github.com/sds100/KeyMapper/actions/workflows/android-release.yml).

    Click on a 'workflow run' and then scroll down to see the 'artifacts'. If a build was successful then you can find the apk here.

!!! info
    All testing builds have `.ci.X` at the end of the version name where 'X' is a number is incremented every time a new build is made. Builds are made when a new feature or bug fix is implemented.

    There are two types of pre-release versions:

    - **Alpha**.  These have ".alpha" in the version name and are the most unstable. Expect the most crashes and broken features in these builds. BEWARE! Your data in Key Mapper isn't considered compatible between alpha builds so it is possible that Key Mapper will crash and refuse to fix itself.
    - **Beta**. These builds have some of the latest features and contain a few bugs. You can safely update between versions. These have ".beta.X" in the version name. These are pre-release builds for the the open-testing channel on Google Play and F-droid always has beta builds. When all known bugs are fixed a new build is released to the app stores.

### How can I help?
- Test and experiment new features. All features and bug-fixes that are being worked on for a release can be found on the Projects page [here](https://github.com/sds100/KeyMapper/projects).
- If you find any bugs or crashes then report them by following the guide [here](report-issues.md).

## Contributing code

### Committing

### Versioning

### Code Style

Follow Google's Kotlin style guide. [https://developer.android.com/kotlin/style-guide](https://developer.android.com/kotlin/style-guide)

## Translating
You can translate this project on the [CrowdIn page](https://crowdin.com/project/key-mapper). Translations will be merged into production once 70% of the language has been translated. This is to improve the user experience. If your language isn't available on the CrowdIn page then contact the developer so we can add it. Our contact details are in the footer of every page on this site.

We really appreciate translators so thank you! 🙂

## Consistency Standards

### Writing

It is important to use consistent language within the app's code and documentation. Make sure that your submissions comply with these standards. If you have noticed inconsistencies you can alert the developer with [an issue](https://github.com/sds100/KeyMapper/issues/new) or in [the Discord](http://keymapper.club). These standards don't need to be followed in places other than the documentation and source code. Follow this Material Design [guide](https://material.io/design/communication/writing.html).

#### Case

No title case unless it is the title of a webpage, section heading. No title case in the string resources in the source code at all.

Here are words and phrases that have been spelt inconsistently by the developer and everyone should follow these guidelines. 

| Terminology           | Correct 😍                                                    | Incorrect 👿                        |
| --------------- | ------------------------------------------------------------ | ---------------------------------- |
| The app's name. | **Key Mapper**<br />Use **Keyboard/Button Mapper** when advertising the app as this is how it appears in the app stores. You can use "Key Mapper" in the rest of the advert since this is much shorter. | key mapper, keymapper              |
| A key map       | **key map**. KeyMap, keyMap in variable and class names. | keymap, Key Map. key-map. Key-Map. |
| A trigger       | **trigger**                                                  | Trigger                            |
| An action       | **action**                                                   | Action                             |
| A constraint    | **constraint**                                               | Constraint                         |
| A key event | **key event** | Key Event, keyevent |
| A key code | **key code**. In variable and class names it should be keyCode instead of keycode. | keycode, Key Code |
| A home screen | **home screen** | homescreen, Home Screen |
| A backup/ to back up | Noun: **backup**. Verb; **back up** | Backup when using the verb. |

### Documentation

#### Headings

If a heading is for a particular feature then it should include the minimum supported Key Mapper version except version 2.0.

E.g "Trigger When Screen is Off (ROOT, 2.1.0+, Android 8.1+)"
