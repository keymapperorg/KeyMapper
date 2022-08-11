package io.github.sds100.keymapper.actions.sound

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChooseSoundFileViewModel2 @Inject constructor(private val useCase: ChooseSoundFileUseCase) : ViewModel() {

    val listItems: StateFlow<List<ChooseSoundListItem>> = useCase.soundFiles.map { sounds ->
        sounds.map {
            ChooseSoundListItem(uid = it.uid, title = it.name)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var configState: ConfigSoundActionState by mutableStateOf(ConfigSoundActionState.Idle)

    fun onListItemClick(uid: String) {
        val soundFileInfo = useCase.soundFiles.value.find { it.uid == uid } ?: return
        configState = ConfigSoundActionState.Finished(ChooseSoundResult(uid, soundFileInfo.name))
    }

    fun onChooseNewSoundFile(uri: String) {
        val fileName = useCase.getSoundFileName(uri).valueOrNull() ?: return
        val baseName = File(fileName).nameWithoutExtension

        configState = ConfigSoundActionState.CreateSoundName(uri, baseName)
    }

    fun onCreateNewSoundFileName(name: String) {
        viewModelScope.launch {
            val uri = (configState as? ConfigSoundActionState.CreateSoundName)?.uri ?: return@launch

            useCase.saveSound(uri, name).onSuccess { soundFileUid ->
                configState = ConfigSoundActionState.Finished(ChooseSoundResult(soundFileUid, name))
            }
        }
    }

    fun onDismissCreatingSoundFileName() {
        configState = ConfigSoundActionState.Idle
    }
}

sealed class ConfigSoundActionState {
    object Idle : ConfigSoundActionState()
    data class CreateSoundName(val uri: String, val fileName: String) : ConfigSoundActionState()
    data class Finished(val result: ChooseSoundResult) : ConfigSoundActionState()
}

data class ChooseSoundListItem(val uid: String, val title: String)