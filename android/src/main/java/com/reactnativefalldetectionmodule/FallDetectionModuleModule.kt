package com.reactnativefalldetectionmodule

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import java.lang.RuntimeException
import kotlin.math.sqrt

class FallDetectionModuleModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), SensorEventListener {

    private val sensorManager: SensorManager = reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var INTERVAL_MS = 20

    private val DURATION_S = 10
    private val N = DURATION_S * 1000 / INTERVAL_MS
    private val FALLING_WAIST_SV_TOT = 0.6
    private val IMPACT_WAIST_SV_TOT = 2.0
    private val IMPACT_WAIST_SV_D = 1.7
    private val IMPACT_WAIST_SV_MAX_MIN = 2.0
    private val IMPACT_WAIST_Z_2 = 1.5

    private val SPAN_MAX_MIN = 100 / INTERVAL_MS
    private val SPAN_FALLING = 1000 / INTERVAL_MS
    private val SPAN_IMPACT = 2000 / INTERVAL_MS
    private val SPAN_AVERAGING = 400 / INTERVAL_MS

    private val FILTER_N_ZEROS = 2
    private val FILTER_N_POLES = 2
    private val FILTER_LPF_GAIN = 4.143204922e+03
    private val FILTER_HPF_GAIN = 1.022463023e+00
    private val FILTER_FACTOR_0 = -0.9565436765
    private val FILTER_FACTOR_1 = +1.9555782403

    private val G = 1.0

    private val LYING_AVERAGE_Z_LPF = 0.5

    private val BUFFER_X: Int = 0
    private val BUFFER_Y: Int = 1
    private val BUFFER_Z: Int = 2
    private val BUFFER_X_LPF: Int = 3
    private val BUFFER_Y_LPF: Int = 4
    private val BUFFER_Z_LPF: Int = 5
    private val BUFFER_X_HPF: Int = 6
    private val BUFFER_Y_HPF: Int = 7
    private val BUFFER_Z_HPF: Int = 8
    private val BUFFER_X_MAX_MIN: Int = 9
    private val BUFFER_Y_MAX_MIN: Int = 10
    private val BUFFER_Z_MAX_MIN: Int = 11
    private val BUFFER_SV_TOT: Int = 12
    private val BUFFER_SV_D: Int = 13
    private val BUFFER_SV_MAX_MIN: Int = 14
    private val BUFFER_Z_2: Int = 15
    private val BUFFER_FALLING: Int = 16
    private val BUFFER_IMPACT: Int = 17
    private val BUFFER_LYING: Int = 18
    private val BUFFER_COUNT: Int = 19
    private var timeoutFalling: Int = -1
    private var timeoutImpact: Int = -1
    val buffers: Buffers = Buffers(BUFFER_COUNT, N, 0, Double.NaN)
    private val x: DoubleArray = buffers.buffers[BUFFER_X]
    private val y: DoubleArray = buffers.buffers[BUFFER_Y]
    private val z: DoubleArray = buffers.buffers[BUFFER_Z]
    private val xLPF: DoubleArray = buffers.buffers[BUFFER_X_LPF]
    private val yLPF: DoubleArray = buffers.buffers[BUFFER_Y_LPF]
    private val zLPF: DoubleArray = buffers.buffers[BUFFER_Z_LPF]
    private val xHPF: DoubleArray = buffers.buffers[BUFFER_X_HPF]
    private val yHPF: DoubleArray = buffers.buffers[BUFFER_Y_HPF]
    private val zHPF: DoubleArray = buffers.buffers[BUFFER_Z_HPF]
    private val xMaxMin: DoubleArray = buffers.buffers[BUFFER_X_MAX_MIN]
    private val yMaxMin: DoubleArray = buffers.buffers[BUFFER_Y_MAX_MIN]
    private val zMaxMin: DoubleArray = buffers.buffers[BUFFER_Z_MAX_MIN]
    private val svTOT: DoubleArray = buffers.buffers[BUFFER_SV_TOT]
    private val svD: DoubleArray = buffers.buffers[BUFFER_SV_D]
    private val svMaxMin: DoubleArray = buffers.buffers[BUFFER_SV_MAX_MIN]
    private val z2: DoubleArray = buffers.buffers[BUFFER_Z_2]
    private val falling: DoubleArray = buffers.buffers[BUFFER_FALLING]
    private val impact: DoubleArray = buffers.buffers[BUFFER_IMPACT]
    private val lying: DoubleArray = buffers.buffers[BUFFER_LYING]
    private val xLpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val xLpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private val yLpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val yLpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private val zLpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val zLpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private val xHpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val xHpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private val yHpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val yHpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private val zHpfXV = DoubleArray(FILTER_N_ZEROS + 1) { 0.0 }
    private val zHpfYV = DoubleArray(FILTER_N_POLES + 1) { 0.0 }
    private var anteX: Double = Double.NaN
    private var anteY: Double = Double.NaN
    private var anteZ: Double = Double.NaN
    private var anteTime: Long = 0
    private var regular: Long = 0

    override fun getName(): String {
        return "FallDetectionModule"
    }

    private fun linear(before: Long, ante: Double, after: Long, post: Double, now: Long): Double {
        return ante + (post - ante) * (now - before).toDouble() / (after - before).toDouble()
    }

    @Suppress("SameParameterValue")
    private fun at(array: DoubleArray, index: Int, size: Int): Double {
        return array[(index + size) % size]
    }

    private fun expire(timeout: Int): Int {
        return if (timeout > -1) {
            timeout - 1
        } else {
            -1
        }
    }

    private fun sv(x: Double, y: Double, z: Double): Double {
        return sqrt(x * x + y * y + z * z)
    }

    private fun min(array: DoubleArray): Double {
        var min: Double = at(array, buffers.position, N)
        for (i: Int in 1 until SPAN_MAX_MIN) {
            val value: Double = at(array, buffers.position - i, N)
            if (!value.isNaN() && value < min) {
                min = value
            }
        }
        return min
    }

    private fun max(array: DoubleArray): Double {
        var max: Double = at(array, buffers.position, N)
        for (i: Int in 1 until SPAN_MAX_MIN) {
            val value: Double = at(array, buffers.position - i, N)
            if (!value.isNaN() && max < value) {
                max = value
            }
        }
        return max
    }

    // Low-pass Butterworth filter, 2nd order, 50 Hz sampling rate, corner frequency 0.25 Hz
    private fun lpf(value: Double, xv: DoubleArray, yv: DoubleArray): Double {
        xv[0] = xv[1]
        xv[1] = xv[2]
        xv[2] = value / FILTER_LPF_GAIN
        yv[0] = yv[1]
        yv[1] = yv[2]
        yv[2] = (xv[0] + xv[2]) + 2 * xv[1] + (FILTER_FACTOR_0 * yv[0]) + (FILTER_FACTOR_1 * yv[1])
        return yv[2]
    }

    // High-pass Butterworth filter, 2nd order, 50 Hz sampling rate, corner frequency 0.25 Hz
    private fun hpf(value: Double, xv: DoubleArray, yv: DoubleArray): Double {
        xv[0] = xv[1]
        xv[1] = xv[2]
        xv[2] = value / FILTER_HPF_GAIN
        yv[0] = yv[1]
        yv[1] = yv[2]
        yv[2] = (xv[0] + xv[2]) - 2 * xv[1] + (FILTER_FACTOR_0 * yv[0]) + (FILTER_FACTOR_1 * yv[1])
        return yv[2]
    }

    private fun process() {
        val at: Int = buffers.position
        timeoutFalling = expire(timeoutFalling)
        timeoutImpact = expire(timeoutImpact)
        xLPF[at] = lpf(x[at], xLpfXV, xLpfYV)
        yLPF[at] = lpf(y[at], yLpfXV, yLpfYV)
        zLPF[at] = lpf(z[at], zLpfXV, zLpfYV)
        xHPF[at] = hpf(x[at], xHpfXV, xHpfYV)
        yHPF[at] = hpf(y[at], yHpfXV, yHpfYV)
        zHPF[at] = hpf(z[at], zHpfXV, zHpfYV)
        xMaxMin[at] = max(x) - min(x)
        yMaxMin[at] = max(y) - min(y)
        zMaxMin[at] = max(z) - min(z)
        val svTOTAt: Double = sv(x[at], y[at], z[at])
        svTOT[at] = svTOTAt
        val svDAt: Double = sv(xHPF[at], yHPF[at], zHPF[at])
        svD[at] = svDAt
        svMaxMin[at] = sv(xMaxMin[at], yMaxMin[at], zMaxMin[at])
        z2[at] = (svTOTAt * svTOTAt - svDAt * svDAt - G * G) / (2.0 * G)
        val svTOTBefore: Double = at(svTOT, at - 1, N)
        falling[at] = 0.0
        if (FALLING_WAIST_SV_TOT <= svTOTBefore && svTOTAt < FALLING_WAIST_SV_TOT) {
            timeoutFalling = SPAN_FALLING
            falling[at] = 1.0
        }
        impact[at] = 0.0
        if (-1 < timeoutFalling) {
            val svMaxMinAt: Double = svMaxMin[at]
            val z2At: Double = z2[at]
            if (IMPACT_WAIST_SV_TOT <= svTOTAt || IMPACT_WAIST_SV_D <= svDAt ||
                IMPACT_WAIST_SV_MAX_MIN <= svMaxMinAt || IMPACT_WAIST_Z_2 <= z2At
            ) {
                timeoutImpact = SPAN_IMPACT
                impact[at] = 1.0
            }
        }
        lying[at] = 0.0
        if (0 == timeoutImpact) {
            var sum = 0.0
            var count = 0.0
            for (i: Int in 0 until SPAN_AVERAGING) {
                val value: Double = at(zLPF, at - i, N)
                if (!value.isNaN()) {
                    sum += value
                    count += 1.0
                }
            }
            if (LYING_AVERAGE_Z_LPF < (sum / count)) {
                lying[at] = 1.0
                alert()
            }
        }
    }

    // Android sampling is irregular, thus the signal is (linearly) resampled at 50 Hz
    private fun resample(postTime: Long, postX: Double, postY: Double, postZ: Double) {
        if (0L == anteTime) {
            regular = postTime + INTERVAL_MS
            return
        }
        while (regular < postTime) {
            val at: Int = buffers.position
            x[at] = linear(anteTime, anteX, postTime, postX, regular)
            y[at] = linear(anteTime, anteY, postTime, postY, regular)
            z[at] = linear(anteTime, anteZ, postTime, postZ, regular)
            process()
            buffers.position = (buffers.position + 1) % N
            regular += INTERVAL_MS
        }
    }

    private fun protect(postTime: Long, postX: Double, postY: Double, postZ: Double) {
        synchronized(buffers) {
            resample(postTime, postX, postY, postZ)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (Sensor.TYPE_ACCELEROMETER == sensor.type) {
            println("Accuracy of the accelerometer is now equal to ")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (Sensor.TYPE_ACCELEROMETER == event.sensor.type) {
            val postTime: Long = event.timestamp / 1000000
            val postX = event.values[0].toDouble() / SensorManager.STANDARD_GRAVITY
            val postY = event.values[1].toDouble() / SensorManager.STANDARD_GRAVITY
            val postZ = event.values[2].toDouble() / SensorManager.STANDARD_GRAVITY
            protect(postTime, postX, postY, postZ)
            anteTime = postTime
            anteX = postX
            anteY = postY
            anteZ = postZ
        }
    }

    private fun alert() {
        val map: WritableMap = Arguments.createMap()
        map.putBoolean("detected", true)
        sendEvent(reactContext, "fall", map)
    }

    private fun sendEvent(
        reactContext: ReactContext,
        eventName: String,
        params: WritableMap
    ) {
        try {
            reactContext
                .getJSModule(RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        } catch (e: RuntimeException) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!")
        }
    }

    @ReactMethod
    fun startUpdates() {
        // Milliseconds to Microseconds conversion
        sensorManager.registerListener(this, sensor, INTERVAL_MS * 1000)
    }

    @ReactMethod
    fun stopUpdates() {
        sensorManager.unregisterListener(this)
    }

    @ReactMethod
    fun setUpdateInterval(newInterval: Int) {
        INTERVAL_MS = newInterval
    }
}
