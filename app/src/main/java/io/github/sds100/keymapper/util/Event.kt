/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.sds100.keymapper.util

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.util.result.Failure

sealed class Event

open class MessageEvent(@StringRes val textRes: Int) : Event()

class FixFailure(val failure: Failure) : Event()
class Vibrate(val duration: Long) : Event()

data class PerformAction(val action: Action,
                         val additionalMetaState: Int = 0,
                         val keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP) : Event()

class ImitateButtonPress(val keyCode: Int, val metaState: Int = 0, val deviceId: Int = 0) : Event()
class ChoosePackage : Event()
class ChooseBluetoothDevice : Event()
class OpenUrl(val url: String) : Event()
class CloseDialog : Event()
class SelectScreenshot : Event()
class ChooseKeycode : Event()
class BuildDeviceInfoModels : Event()
class RequestBackupSelectedKeymaps : Event()
class BuildKeymapListModels(val keymapList: List<KeyMap>) : Event()
class OkDialog(@StringRes val message: Int, val onOk: () -> Unit) : Event()
class EnableAccessibilityServicePrompt : Event()
class RequestBackup(val keymapList: List<KeyMap>) : Event()
class RequestRestore : Event()
class RequestBackupAll : Event()
class ShowErrorMessage(val failure: Failure) : Event()
class BuildIntentExtraListItemModels(val extraModels: List<IntentExtraModel>) : Event()
class SetTheme(@AppCompatDelegate.NightMode val theme: Int) : Event()
class CreateKeymapShortcutEvent(
    val uuid: String,
    val actionList: List<Action>
) : Event()

//trigger
class BuildTriggerKeyModels(val source: List<Trigger.Key>) : Event()
class EditTriggerKeyOptions(val options: TriggerKeyOptions) : Event()
class EnableCapsLockKeyboardLayoutPrompt : Event()
class StartRecordingTriggerInService : Event()
class StopRecordingTriggerInService : Event()

//action list
class BuildActionListModels(val source: List<Action>) : Event()
class TestAction(val action: Action) : Event()
class EditActionOptions(val options: BaseOptions<Action>) : Event()

//constraints
class DuplicateConstraints : MessageEvent(R.string.error_duplicate_constraint)
class BuildConstraintListModels(val source: List<Constraint>) : Event()
class SelectConstraint(val constraint: Constraint) : Event()

//fingerprint gesture maps
class BuildFingerprintMapModels(val maps: Map<String, FingerprintMap>) : Event()
class BackupFingerprintMaps : Event()
class RequestFingerprintMapReset : Event()

//menu
class OpenSettings : Event()
class OpenAbout : Event()
class ChooseKeyboard : Event()
class SendFeedback : Event()
class ResumeKeymaps : Event()
class PauseKeymaps : Event()
class EnableAccessibilityService : Event()

//accessibility service
class ShowFingerprintFeatureNotification : Event()