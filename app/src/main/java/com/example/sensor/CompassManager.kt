package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class CompassState(
    val azimuth: Float = 0f, // 0 - 360 degrees
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isAvailable: Boolean = true
)

class CompassManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassState = MutableStateFlow(CompassState(isAvailable = sensorManager != null))
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private var gravityValues = FloatArray(3)
    private var geoMagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeoMagnetic = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var currentFilteredAzimuth = 0f

    fun startListening() {
        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            azimuth = (azimuth + 360f) % 360f
            updateAzimuth(azimuth, event.accuracy)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3)
            hasGravity = true
            computeFromGravityAndMagnetic(event.accuracy)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geoMagneticValues, 0, 3)
            hasGeoMagnetic = true
            computeFromGravityAndMagnetic(event.accuracy)
        }
    }

    private fun computeFromGravityAndMagnetic(accuracy: Int) {
        if (hasGravity && hasGeoMagnetic) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geoMagneticValues)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                azimuth = (azimuth + 360f) % 360f
                updateAzimuth(azimuth, accuracy)
            }
        }
    }

    private fun updateAzimuth(newAzimuth: Float, accuracy: Int) {
        // Smooth interpolation (handling 360/0 degree wrap-around)
        var diff = newAzimuth - currentFilteredAzimuth
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f

        // Low-pass filter smoothing coefficient
        val alpha = 0.25f
        currentFilteredAzimuth = (currentFilteredAzimuth + alpha * diff + 360f) % 360f

        _compassState.value = CompassState(
            azimuth = currentFilteredAzimuth,
            accuracy = accuracy,
            isAvailable = true
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _compassState.value = _compassState.value.copy(accuracy = accuracy)
    }
}
