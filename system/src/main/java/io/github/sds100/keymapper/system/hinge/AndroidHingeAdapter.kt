package io.github.sds100.keymapper.system.hinge

import android.app.Activity
import android.content.Context
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidHingeAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : HingeAdapter {

    private val _hingeState = MutableStateFlow(HingeState(isAvailable = false, angle = null))
    override val hingeState: StateFlow<HingeState> = _hingeState.asStateFlow()

    /**
     * This should be called with an Activity context when the accessibility service
     * or main activity is available. The WindowInfoTracker needs an Activity.
     */
    fun startMonitoring(activity: Activity) {
        val windowInfoTracker = WindowInfoTracker.getOrCreate(activity)
        
        coroutineScope.launch {
            windowInfoTracker.windowLayoutInfo(activity).collect { layoutInfo ->
                updateHingeState(layoutInfo)
            }
        }
    }

    private fun updateHingeState(layoutInfo: WindowLayoutInfo) {
        val foldingFeature = layoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
            .firstOrNull()

        if (foldingFeature == null) {
            _hingeState.value = HingeState(isAvailable = false, angle = null)
            return
        }

        // FoldingFeature.State has FLAT and HALF_OPENED states
        // We can estimate the angle based on the state
        val angle = when (foldingFeature.state) {
            FoldingFeature.State.FLAT -> 180f // Fully open
            FoldingFeature.State.HALF_OPENED -> {
                // For HALF_OPENED state, we don't have the exact angle from the API
                // We'll use 90 degrees as a reasonable estimate for half-opened
                90f
            }
            else -> null
        }

        _hingeState.value = HingeState(
            isAvailable = true,
            angle = angle,
        )
    }

    override fun getCachedHingeState(): HingeState = _hingeState.value
}
