[![Discord](https://img.shields.io/discord/717499872219103263?style=for-the-badge)](https://discord.gg/Suj6nyw)

![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/sds100/KeyMapper.svg)
![GitHub Releases Downloads](https://img.shields.io/github/downloads/sds100/keymapper/total.svg?label=GitHub%20Releases%20Downloads)
![GitHub release](https://img.shields.io/github/release/sds100/KeyMapper.svg)
![fdroid release](https://img.shields.io/f-droid/v/io.github.sds100.keymapper.svg)

Key Mapper is a free and open source Android app that can map a single or multiple key events to a custom action. The aim of this project is to allow anyone to map their buttons in any combination to anything.

[XDA Developers thread](https://forum.xda-developers.com/android/apps-games/app-keyboard-button-mapper-t3914005)  

![](app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png?raw=true)
<a href='https://play.google.com/store/apps/details?id=io.github.sds100.keymapper&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_gb/badges/images/generic/en_badge_web_generic.png' height=75px/> </a>
<a href='https://f-droid.org/en/packages/io.github.sds100.keymapper/'><img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height=75px/> </a>

### How do I contribute?
You can help by suggesting ideas, notifying me of any bugs or contributing to the code directly. You can post suggestions and bug fixes in the Discord server, XDA thread, GitHub issues page for this repo. The latest cutting edge builds are uploaded to the #testing-builds channel in the Discord server everytime someone pushes to this repo. 

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/M4M41032E)
<span class="badge-paypal"><a href="https://www.paypal.com/donate?hosted_button_id=K9NBSSWJY9TVY" title="Donate to this project using Paypal"><img src="https://img.shields.io/badge/paypal-donate-blue.svg" alt="PayPal donate button" /></a></span>

To build and help with code stuff...
1. Fork the KeyMapper repository (repo).
2. Clone the repo to your device. It will clone to a folder called KeyMapper by default.
3. [Install](https://developer.android.com/studio/install) Android Studio if you don't have it already. It is available for Windows, Linux and macOS.
4. Open the cloned KeyMapper folder in Android Studio. Install anything Android Studio prompts you to install. E.g the gradle wrapper version used by KeyMapper or older Android SDK versions.
5. Create a new branch off develop which begins with "feature/" if it is a new feature or "fix/" if it is a bug fix. Then put a brief description of the feature/bug fix. 
6. Make any changes then commit them to your forked repo then make a pull request!

### I need help in the app!
Look at the [help](https://github.com/sds100/KeyMapper/wiki/Help) page in the wiki.

### Translations 🌍
Take a look at the [translations](https://github.com/sds100/KeyMapper/wiki/Translate) page in the wiki. Any translations will be appreciated. 😊

### Branches 🌴
 - master: Everything in the latest stable release.
 - develop: The most recent changes. The app is potentially unstable but it can be successfully compiled. Merges into a release branch when enough has been changed for a new release.

 - release/*: Branched off develop. Beta releases for a particular release are compiled from here. Once the code is stable, it will be merged into master. No big changes should be made/merged here as the purpose of this branch is to make a release stable. By separating upcoming releases from develop, new features can be worked on in develop without affecting the upcoming release's code base.
 - feature/*: Any new changes currently being developed. Merges into develop.
 - hotfix/*: Any small, quick (atleast things which SHOULD be quick) changes that need to be made. Merge into develop and release. If there is no release already being worked on, quickly release a new version depending on how critical the issue is and merge the new release branch into master.

### Versioning
This project uses semantic versioning. e.g 1.2.3-alpha.1

- 1st digit: major release. Only incremented when a big enough change happens to the project.
- 2nd digit: minor releases. Incremented when a new feature or a few are added.
- 3rd digit: patches. Incrememtend after a hotfix or bug fix.

Additional labels can be used as a suffix. e.g "alpha".

#### Version codes
The version code in the develop branch should always be the highest. Therefore, when a new version is released in the release branch, it should be incremented as well.

### Commit message format

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
