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
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Loading
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FingerprintGestureViewModel(
    @StringRes private val mPrefKey: Int,
    private val mPreferenceDataStore: IPreferenceDataStore) : ViewModel() {

    val model = MutableLiveData<State<FingerprintGestureMapListItemModel>>()

    val buildModel = MutableLiveData<Event<FingerprintGestureMap?>>()

    init {
        rebuildModel()
    }

    fun setModel(model: FingerprintGestureMapListItemModel) {
        this.model.value = Data(model)
    }

    private fun getFingerprintMap(@StringRes prefKey: Int): FingerprintGestureMap? {
        val gson = GsonBuilder()
            .registerTypeAdapter(Action.DESERIALIZER)
            .registerTypeAdapter(Extra.DESERIALIZER).create()

        val json = mPreferenceDataStore.getStringPref(prefKey)

        return json?.let { gson.fromJson(it) }
    }

    fun rebuildModel() {
        model.value = Loading()

        viewModelScope.launch(Dispatchers.IO) {

            val fingerprintMap = getFingerprintMap(mPrefKey) ?: FingerprintGestureMap()

            withContext(Dispatchers.Main) {
                buildModel.value = Event(fingerprintMap)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mPrefKey: Int,
                  private val mPreferenceDataStore: IPreferenceDataStore) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mPrefKey, mPreferenceDataStore) as T
        }
    }
}