package io.github.sds100.keymapper.system.foldable

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.R)
@Singleton
class AndroidFoldableAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
) : FoldableAdapter {

    private val _hingeState = MutableStateFlow<HingeState>(HingeState.Unavailable)
    override val hingeState: StateFlow<HingeState> = _hingeState.asStateFlow()

    private val sensorManager: SensorManager? = context.getSystemService()
    private val hingeSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_HINGE_ANGLE && it.values.isNotEmpty()) {
                    val angle = it.values[0]
                    _hingeState.value = HingeState.Available(angle)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed for hinge angle sensor
        }
    }

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        if (hingeSensor != null) {
            try {
                sensorManager?.registerListener(
                    sensorEventListener,
                    hingeSensor,
                    SensorManager.SENSOR_DELAY_NORMAL,
                )
                Timber.d("Hinge angle sensor monitoring started")
            } catch (e: Exception) {
                Timber.e(e, "Failed to start hinge angle sensor monitoring")
                _hingeState.value = HingeState.Unavailable
            }
        } else {
            Timber.d("Hinge angle sensor not available on this device")
            _hingeState.value = HingeState.Unavailable
        }
    }
}
