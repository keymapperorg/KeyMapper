package io.github.sds100.keymapper.data.repository

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.util.RequestBackup
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 24/01/21.
 */
interface FingerprintMapRepository {
    val requestBackup: LiveData<RequestBackup<Map<String, FingerprintMap>>>

    val swipeDown: Flow<FingerprintMap>
    val swipeUp: Flow<FingerprintMap>
    val swipeLeft: Flow<FingerprintMap>
    val swipeRight: Flow<FingerprintMap>

    val fingerprintGestureMaps: Flow<Map<String, FingerprintMap>>
    val fingerprintGesturesAvailable: Flow<Boolean?>

    fun setFingerprintGesturesAvailable(available: Boolean)
    fun restore(gestureId: String, fingerprintMapJson: String)
    fun updateGesture(gestureId: String, block: (old: FingerprintMap) -> FingerprintMap)
    fun reset()
}