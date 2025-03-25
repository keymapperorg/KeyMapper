## Becoming a tester

You can get the apks for the pre-release versions in 2 ways:

1. Join [the Discord server](http://keymapper.club) and download the apks from the #testing-builds channel.
2. Download the apk from GitHub Actions. This requires a GitHub account.
    You can get alpha builds [here](https://github.com/keymapperorg/KeyMapper/actions/workflows/android.yml) and beta builds [here](https://github.com/keymapperorg/KeyMapper/actions/workflows/android-release.yml).

    Click on a 'workflow run' and then scroll down to see the 'artifacts'. If a build was successful then you can find the apk here.

!!! info
    All testing builds have `.ci.X` at the end of the version name where 'X' is a number is incremented every time a new build is made. Builds are made when a new feature or bug fix is implemented.

    There are two types of pre-release versions:

    - **Alpha**.  These have ".alpha" in the version name and are the most unstable. Expect the most crashes and broken features in these builds. BEWARE! Your data in Key Mapper isn't considered compatible between alpha builds so it is possible Key Mapper will crash and refuse to fix itself if you update to a new build that can't understand the data.
    - **Beta**. These builds have some of the latest features and contain a few bugs. You can safely update between versions. These have ".beta.X" in the version name. These are pre-release builds for the the open-testing channel on Google Play and F-droid always has beta builds. When all known bugs are fixed a new build is released to the app stores.

### How can I help?
- Test and experiment new features. All features and bug-fixes that are being worked on for a release can be found on the Projects page [here](https://github.com/keymapperorg/KeyMapper/projects).
- If you find any bugs or crashes then report them by following the guide [here](../report-issues.md).

## Contributing code

### Setting up the environment

1. Fork the KeyMapper repository (repo).
2. Clone the repo to your device. It will clone to a folder called KeyMapper by default.
3. [Install](https://developer.android.com/studio/install) Android Studio if you don't have it already. It is available for Windows, Linux and macOS.
4. Open the cloned KeyMapper folder in Android Studio. Install anything Android Studio prompts you to install. E.g the gradle wrapper version used by KeyMapper or older Android SDK versions.
5. Create a new branch off develop which begins with "feature/" if it is a new feature or "fix/" if it is a bug fix. Then put a brief description of the feature/bug fix.
6. Make any changes then commit them to your forked repo then make a pull request!

!!! info
    To build the documentation website you need to install [mkdocs-material](https://squidfunk.github.io/mkdocs-material/getting-started/) with Python. Just run `pip install -r requirements.txt` in the root of the project to install it.
    Then run `mkdocs serve` in the project root.
    
### Build flavors and types

After version 2.7.0 Key Mapper will have 2 build flavours: _free_ and _pro_. The pro flavor includes the closed-source features (e.g assistant trigger) and non-FOSS libraries such as the Google Play Billing library. The free variant stubs out these closed-source features and only uses FOSS libraries.

There are also 4 build types, which have different optimizations and package names.

- **debug** = This is the default debug build type that has no optimizations and builds rapidly. It has a `.debug` package name suffix.
- **release** = This is the default release build type that includes a lot of optimizations and creates an apk/app bundle suitable for releasing. There is no package name suffix.
- **debug_release** = This is a debug build type that does not include a package name suffix so that it is possible to test how the production app will look. It is the only way to get the Google Play Billing library functioning because it will break if the package name isn't the same as on the Play store.
- **ci** = This is used for alpha builds to the community in Discord. It includes optimizations to dramatically shrink the apk size, improve performance, and has obfuscation. It has a `.ci` package name suffix.

### Branches 🌴

 - master: Everything in the latest stable release.
 - develop: The most recent changes. The app is potentially unstable but it can be successfully compiled. A new release is branched off of here.
 - feature/*: Any new changes currently being developed. Merges into develop.
 - fix/*: A bug fix. This branch should be merged into a release branch and develop.

### Committing

Format:
```
<issue id> <type>: <subject>
```

Every feature or bug fix commit should have an issue associated with it. This is a cue for the developer to plan what they are doing which improves efficiency. A feature should be split up into multiple tasks and each task should have its own commit. The feature should be developed on a separate branch and then merged into develop.

#### Example
```
#100 feat: This a new feature
```

#### Types
- feat: a new feature
- fix: a bug fix
- docs: changes to documentation
- style: formatting, missing semi colons, etc; no code change
- refactor: refactoring production code
- test: adding tests, refactoring test; no production code change
- chore: updating build tasks, package manager configs, version name changes, etc; no production code change
- release: a new release.
- website: stuff to do with the website.

The README, License, Credits, Changelog and Privacy Policy files should just be changed in the master branch.

### Versioning

This project uses semantic versioning. e.g 1.2.3-alpha.01

- 1st digit: major release. Only incremented when a big enough change happens to the project.
- 2nd digit: minor releases. Incremented when a new feature or a few are added.
- 3rd digit: patches. Incrememtend after a hotfix or bug fix.

Additional labels can be used as a suffix. e.g "alpha".

The version code in the develop branch should always be the highest. Therefore, when a new version is released in the release branch, it should be incremented as well.

### Releasing

Fastlane is used to partially automate the releasing process. Follow the [guide](https://docs.fastlane.tools/) on the Fastlane website to set it up.

#### Beta releases

##### Only for the first beta release
1. Branch off develop into a new release branch (e.g release/2.3.0).
2. Change the version name and version code in `version.properties` in the release branch.
3. Change the version name and version code in `version.properties` in the develop branch to be one version ahead of the release branch.

##### For every release
1. Manually edit CHANGELOG.md **in the develop branch** with *all* changes. Cherry pick this into the release branch.
2. Open the KeyMapper folder in a terminal and run `fastlane beta`.
3. Squash and merge the release branch into master. Then delete the release branch.

#### Production releases

1. Check that all translations are merged.
2. Credit the translators in the About screen in the app and in the index.md on the documentation website.
3. Manually edit CHANGELOG.md **in the develop branch** with *all* changes. Cherry pick this into the release branch.
4. Open the KeyMapper folder in a terminal and run `fastlane prod`. This will release the production build to the
   open-testing track on Google Play. Once it is approved by Google Play you must promote the release from open testing
   to the production track in Google Play.
5. Squash and merge the release branch into master. Then delete the release branch.

### Code Style

Follow Google's Kotlin style guide. [https://developer.android.com/kotlin/style-guide](https://developer.android.com/kotlin/style-guide)

## Translating 🌍

You can translate this project on the [CrowdIn page](https://crowdin.com/project/key-mapper). Translations will be
merged into production once they are >80% translated. If your language isn't available on the CrowdIn page then contact
the developer so we can add it. Our contact details are in the footer of every page on this site.

We really appreciate translators so thank you! 🙂

## Consistency Standards

### Writing

It is important to use consistent language within the app's code and documentation. Make sure that your submissions comply with these standards. If you have noticed inconsistencies you can alert the developer with [an issue](https://github.com/keymapperorg/KeyMapper/issues/new) or in [the Discord](http://keymapper.club). These standards don't need to be followed in places other than the documentation and source code. Follow this Material Design [guide](https://material.io/design/communication/writing.html).

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

E.g "Trigger When Screen is Off (ROOT, 2.1.0+, Android 8.1+)" or "A feature (up to Android 10)".
