package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.options.FingerprintActionOptions
import io.github.sds100.keymapper.data.model.options.FingerprintMapOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.EnableAccessibilityServicePrompt
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_DOWN
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_LEFT
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_RIGHT
import io.github.sds100.keymapper.util.FingerprintMapUtils.SWIPE_UP
import io.github.sds100.keymapper.util.FixFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 08/11/20.
 */

class ConfigFingerprintMapViewModel(private val mFingerprintMapRepository: FingerprintMapRepository,
                                    private val mDeviceInfoRepository: DeviceInfoRepository,
                                    preferenceDataStore: IPreferenceDataStore
) : ViewModel(), IPreferenceDataStore by preferenceDataStore {

    companion object {
        private const val MAP_STATE_KEY = "config_fingerprint_map"
        private const val GESTURE_ID_STATE_KEY = "config_fingerprint_map_gesture_id"
    }

    private var mGestureId: String? = null

    val actionListViewModel = object : ActionListViewModel<FingerprintActionOptions>(
        viewModelScope, mDeviceInfoRepository) {

        override val stateKey = "fingerprint_action_list_view_model"

        override fun getActionOptions(action: Action): FingerprintActionOptions {
            return FingerprintActionOptions(
                action,
                actionList.value!!.size
            )
        }
    }

    val optionsViewModel = FingerprintMapOptionsViewModel()

    val constraintListViewModel = ConstraintListViewModel(viewModelScope)

    val isEnabled = MutableLiveData(true)

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(constraintListViewModel.eventStream) {
            when (it) {
                is FixFailure -> value = it
            }
        }

        addSource(actionListViewModel.eventStream) {
            when (it) {
                is FixFailure, is EnableAccessibilityServicePrompt -> value = it
            }
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    fun save(scope: CoroutineScope) {
        scope.launch {
            val map = createFingerprintMap()

            mGestureId?.let {
                mFingerprintMapRepository.editGesture(it) {
                    map
                }
            }
        }
    }

    private fun createFingerprintMap(): FingerprintMap {
        return FingerprintMap(
            actionList = actionListViewModel.actionList.value ?: listOf(),
            constraintList = constraintListViewModel.constraintList.value ?: listOf(),
            constraintMode = constraintListViewModel.getConstraintMode(),
            isEnabled = isEnabled.value ?: true
        ).let {
            optionsViewModel.options.value?.apply(it) ?: it
        }
    }

    fun loadFingerprintMap(gestureId: String) {
        viewModelScope.launch {
            val map = when (gestureId) {
                SWIPE_DOWN -> mFingerprintMapRepository.swipeDown
                SWIPE_UP -> mFingerprintMapRepository.swipeUp
                SWIPE_LEFT -> mFingerprintMapRepository.swipeLeft
                SWIPE_RIGHT -> mFingerprintMapRepository.swipeRight

                else -> throw Exception("unknown fingerprint id $gestureId")
            }

            map.collect {
                loadFingerprintMap(gestureId, it)
                return@collect
            }
        }
    }

    private fun loadFingerprintMap(gestureId: String, map: FingerprintMap) {
        mGestureId = gestureId
        actionListViewModel.setActionList(map.actionList)
        constraintListViewModel.setConstraintList(map.constraintList, map.constraintMode)
        isEnabled.value = map.isEnabled
        optionsViewModel.setOptions(FingerprintMapOptions(gestureId, map))
    }

    fun saveState(outState: Bundle) {
        outState.putString(GESTURE_ID_STATE_KEY, mGestureId)
        outState.putParcelable(MAP_STATE_KEY, createFingerprintMap())
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState(state: Bundle) {
        val gestureId = state.getString(GESTURE_ID_STATE_KEY) ?: return
        val map = state.getParcelable<FingerprintMap>(MAP_STATE_KEY) ?: return

        loadFingerprintMap(gestureId, map)
    }

    class Factory(
        private val mFingerprintMapRepository: FingerprintMapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIPreferenceDataStore: IPreferenceDataStore) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigFingerprintMapViewModel(
                mFingerprintMapRepository,
                mDeviceInfoRepository,
                mIPreferenceDataStore
            ) as T
    }
}