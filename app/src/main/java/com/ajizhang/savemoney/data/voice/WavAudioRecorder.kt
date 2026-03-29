package com.ajizhang.savemoney.data.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class WavAudioRecorder @Inject constructor() {
    private val sampleRate = 16_000
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var outputStream: ByteArrayOutputStream? = null
    private var startedAtMs: Long = 0L
    private var isRecording: Boolean = false

    @SuppressLint("MissingPermission")
    suspend fun start(): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (isRecording) {
                return@withContext Result.success(Unit)
            }

            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            if (minBufferSize <= 0) {
                return@withContext Result.failure(IllegalStateException("无法初始化录音器"))
            }

            val recorder =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder.release()
                return@withContext Result.failure(IllegalStateException("麦克风不可用"))
            }

            val stream = ByteArrayOutputStream()
            recorder.startRecording()

            audioRecord = recorder
            outputStream = stream
            startedAtMs = SystemClock.elapsedRealtime()
            isRecording = true

            recordJob =
                recorderScope.launch {
                    val buffer = ByteArray(minBufferSize)
                    while (isRecording) {
                        val readSize = recorder.read(buffer, 0, buffer.size)
                        if (readSize > 0) {
                            stream.write(buffer, 0, readSize)
                        }
                    }
                }

            Result.success(Unit)
        }

    suspend fun stop(): RecordedAudio? =
        withContext(Dispatchers.IO) {
            if (!isRecording) {
                return@withContext null
            }

            isRecording = false
            recordJob?.join()
            recordJob = null

            val recorder = audioRecord
            audioRecord = null
            val rawPcm = outputStream?.toByteArray() ?: ByteArray(0)
            outputStream = null
            val durationMs = SystemClock.elapsedRealtime() - startedAtMs

            runCatching { recorder?.stop() }
            recorder?.release()

            if (rawPcm.isEmpty()) {
                return@withContext null
            }

            RecordedAudio(
                wavBytes = rawPcm.toWave(sampleRate = sampleRate),
                durationMs = durationMs,
            )
        }

    suspend fun cancel() {
        withContext(Dispatchers.IO) {
            if (!isRecording) {
                return@withContext
            }
            isRecording = false
            recordJob?.cancelAndJoin()
            recordJob = null
            runCatching { audioRecord?.stop() }
            audioRecord?.release()
            audioRecord = null
            outputStream = null
        }
    }

    private fun ByteArray.toWave(sampleRate: Int): ByteArray {
        val totalDataLen = size + 36
        val byteRate = sampleRate * 2
        val header =
            ByteBuffer.allocate(44)
                .order(ByteOrder.LITTLE_ENDIAN)
                .apply {
                    put("RIFF".toByteArray())
                    putInt(totalDataLen)
                    put("WAVE".toByteArray())
                    put("fmt ".toByteArray())
                    putInt(16)
                    putShort(1)
                    putShort(1)
                    putInt(sampleRate)
                    putInt(byteRate)
                    putShort(2)
                    putShort(16)
                    put("data".toByteArray())
                    putInt(size)
                }.array()

        return header + this
    }
}
