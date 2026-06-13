package com.lx.simplewatermarkcamera.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.lx.simplewatermarkcamera.domain.BearingFormatter
import com.lx.simplewatermarkcamera.domain.HeadingSnapshot
import kotlin.math.roundToInt

class HeadingRepository(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var callback: ((HeadingSnapshot) -> Unit)? = null

    var latest: HeadingSnapshot? = null
        private set

    fun start(onHeading: (HeadingSnapshot) -> Unit) {
        callback = onHeading
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        callback = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publish(rotationMatrix)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                smooth(event.values, gravity)
                publishFallback()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                smooth(event.values, geomagnetic)
                publishFallback()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun smooth(source: FloatArray, target: FloatArray) {
        source.take(3).forEachIndexed { index, value ->
            target[index] = target[index] * 0.85f + value * 0.15f
        }
    }

    private fun publishFallback() {
        val rotationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            publish(rotationMatrix)
        }
    }

    private fun publish(rotationMatrix: FloatArray) {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val formatted = BearingFormatter.format(azimuth)
        val rounded = formatted.copy(azimuthDegrees = formatted.azimuthDegrees.roundToInt().toFloat())
        latest = rounded
        callback?.invoke(rounded)
    }
}
