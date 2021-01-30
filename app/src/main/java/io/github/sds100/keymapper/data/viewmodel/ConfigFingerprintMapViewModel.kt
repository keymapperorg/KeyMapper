package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 08/11/20.
 */

class ConfigFingerprintMapViewModel(private val fingerprintMapRepository: FingerprintMapRepository,
                                    private val deviceInfoRepository: DeviceInfoRepository
) : ViewModel(), IConfigMappingViewModel {

    companion object {
        private const val MAP_STATE_KEY = "config_fingerprint_map"
        private const val GESTURE_ID_STATE_KEY = "config_fingerprint_map_gesture_id"
    }

    private var gestureId: String? = null

    override val actionListViewModel = object : ActionListViewModel<FingerprintActionOptions>(
        viewModelScope, deviceInfoRepository) {

        override val stateKey = "fingerprint_action_list_view_model"

        override fun getActionOptions(action: Action): FingerprintActionOptions {
            return FingerprintActionOptions(
                action,
                actionList.value!!.size
            )
        }
    }

    val optionsViewModel = FingerprintMapOptionsViewModel()

    val constraintListViewModel = ConstraintListViewModel(
        viewModelScope,
        Constraint.COMMON_SUPPORTED_CONSTRAINTS
    )

    override val isEnabled = MutableLiveData(true)

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

    override val eventStream: LiveData<Event> = _eventStream

    override fun save() {
        val map = createFingerprintMap()

        gestureId?.let {
            fingerprintMapRepository.updateGesture(it) { map }
        }
    }

    override fun saveState(outState: Bundle) {
        outState.putString(GESTURE_ID_STATE_KEY, gestureId)

        outState.putParcelable(MAP_STATE_KEY, createFingerprintMap())
    }

    @Suppress("UNCHECKED_CAST")
    override fun restoreState(state: Bundle) {
        val gestureId = state.getString(GESTURE_ID_STATE_KEY) ?: return
        val map = state.getParcelable<FingerprintMap>(MAP_STATE_KEY) ?: return

        loadFingerprintMap(gestureId, map)
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
                SWIPE_DOWN -> fingerprintMapRepository.swipeDown
                SWIPE_UP -> fingerprintMapRepository.swipeUp
                SWIPE_LEFT -> fingerprintMapRepository.swipeLeft
                SWIPE_RIGHT -> fingerprintMapRepository.swipeRight

                else -> throw Exception("unknown fingerprint id $gestureId")
            }

            map.collect {
                loadFingerprintMap(gestureId, it)
                return@collect
            }
        }
    }

    private fun loadFingerprintMap(gestureId: String, map: FingerprintMap) {
        this.gestureId = gestureId
        actionListViewModel.setActionList(map.actionList)
        constraintListViewModel.setConstraintList(map.constraintList, map.constraintMode)
        isEnabled.value = map.isEnabled
        optionsViewModel.setOptions(FingerprintMapOptions(gestureId, map))
    }

    class Factory(
        private val fingerprintMapRepository: FingerprintMapRepository,
        private val deviceInfoRepository: DeviceInfoRepository) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigFingerprintMapViewModel(
                fingerprintMapRepository,
                deviceInfoRepository
            ) as T
    }
}