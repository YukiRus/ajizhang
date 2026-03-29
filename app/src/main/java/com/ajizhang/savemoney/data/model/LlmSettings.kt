package com.ajizhang.savemoney.data.model

data class LlmSettings(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
) {
    val isComplete: Boolean
        get() = apiBaseUrl.isNotBlank() && apiKey.isNotBlank() && modelName.isNotBlank()
}
