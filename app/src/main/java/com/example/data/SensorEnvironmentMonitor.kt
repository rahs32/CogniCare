package com.example.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.log10

class SensorEnvironmentMonitor(private val context: Context) {

    fun getLightLevel(): Flow<Float> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor == null) {
            // No light sensor available, return default 0f or error handled upstream
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    trySend(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    fun getSoundLevelDecibels(): Flow<Float> = flow {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            return@flow
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            return@flow
        }

        val buffer = ShortArray(bufferSize)
        audioRecord.startRecording()

        try {
            var smoothedDb = 40f
            val alpha = 0.2f // Smoothing factor for EMA (lower = smoother)
            
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val readSize = audioRecord.read(buffer, 0, bufferSize)
                if (readSize > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until readSize) {
                        val amplitude = buffer[i].toDouble()
                        sumSquares += amplitude * amplitude
                    }
                    val rms = Math.sqrt(sumSquares / readSize)
                    
                    // Standard reference pressure for air is 20 micropascals, but digital PCM max is 32767
                    // A better estimation for Android mobile mics is normalizing against the max digital value
                    val referenceAmplitude = 32767.0
                    val normalizedAmplitude = (rms / referenceAmplitude).coerceIn(0.0001, 1.0)
                    
                    val rawDb = (20 * log10(normalizedAmplitude)).toFloat() + 100f // Adjusted offset
                    
                    smoothedDb = (alpha * rawDb) + ((1f - alpha) * smoothedDb)
                    
                    emit(smoothedDb)
                }
                kotlinx.coroutines.delay(200)
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)
}
