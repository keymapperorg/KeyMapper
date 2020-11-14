package io.github.sds100.keymapper.data.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FingerprintGestureViewModel(private val mPreferenceDataStore: IPreferenceDataStore) : ViewModel() {

    val models = MutableLiveData<State<List<FingerprintGestureMapListItemModel>>>()

    val buildModels = MutableLiveData<Event<Map<String, FingerprintGestureMap>>>()
    private val mGson = GsonBuilder()
        .registerTypeAdapter(Action.DESERIALIZER)
        .registerTypeAdapter(Extra.DESERIALIZER).create()

    init {
        rebuildModels()
    }

    fun setModels(models: List<FingerprintGestureMapListItemModel>) {
        this.models.value = Data(models)
    }

    fun setAction(id: String, action: Action) = editFingerprintMap(id) {
        it.clone(action = action)
    }

    fun removeAction(id: String) = editFingerprintMap(id) {
        it.clone(action = null)
    }

    fun setEnabled(id: String, isEnabled: Boolean) = editFingerprintMap(id) {
        it.clone(isEnabled = isEnabled)
    }

    private fun editFingerprintMap(
        gestureId: String,
        block: (old: FingerprintGestureMap) -> FingerprintGestureMap
    ) {
        val fingerprintGestureMap = retrieveFingerprintMap(FingerprintGestureUtils.PREF_KEYS[gestureId]!!)
            ?: FingerprintGestureMap()

        fingerprintGestureMap.apply {
            val prefKey = FingerprintGestureUtils.PREF_KEYS[gestureId]!!

            saveFingerprintMap(prefKey, block.invoke(fingerprintGestureMap))
        }
    }

    private fun saveFingerprintMap(@StringRes prefKey: Int, gestureMap: FingerprintGestureMap) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = mGson.toJson(gestureMap)
            mPreferenceDataStore.setStringPref(prefKey, json)

            withContext(Dispatchers.Main) {
                rebuildModels()
            }
        }
    }

    private fun retrieveFingerprintMap(@StringRes prefKey: Int): FingerprintGestureMap? {

        val json = mPreferenceDataStore.getStringPref(prefKey)

        return json?.let { mGson.fromJson(it) }
    }

    fun rebuildModels() {
        models.value = Loading()

        viewModelScope.launch(Dispatchers.IO) {
            val gestureMaps = FingerprintGestureUtils.GESTURES.map {
                it to (retrieveFingerprintMap(FingerprintGestureUtils.PREF_KEYS[it]!!) ?: FingerprintGestureMap())
            }.toMap()

            withContext(Dispatchers.Main) {
                buildModels.value = Event(gestureMaps)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mPreferenceDataStore: IPreferenceDataStore) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mPreferenceDataStore) as T
        }
    }
}