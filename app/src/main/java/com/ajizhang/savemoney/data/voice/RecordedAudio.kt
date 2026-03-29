package com.ajizhang.savemoney.data.voice

data class RecordedAudio(
    val wavBytes: ByteArray,
    val durationMs: Long,
)
