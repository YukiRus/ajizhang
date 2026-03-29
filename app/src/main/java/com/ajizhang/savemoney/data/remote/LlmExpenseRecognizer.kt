

package com.ajizhang.savemoney.data.remote

import android.util.Base64
import com.ajizhang.savemoney.data.model.ExpenseRecognitionResult
import com.ajizhang.savemoney.data.repository.LlmSettingsRepository
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class LlmExpenseRecognizer @Inject constructor(
    private val settingsRepository: LlmSettingsRepository,
) {
    suspend fun recognizeExpense(
        wavBytes: ByteArray,
        categories: List<String>,
        today: LocalDate,
        now: LocalDateTime,
    ): Result<ExpenseRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = settingsRepository.getSettings()
                require(settings.isComplete) { "请先在设置里填写 API 地址、API Key 和模型名称" }

                val requestUrl = buildChatCompletionsUrl(settings.apiBaseUrl)
                val categoryJson = JSONArray(categories).toString()
                val prompt =
                    """
                    你是记账助手。请根据这段语音识别一笔“支出”，并只返回一个 JSON 对象，不要 markdown，不要解释。
                    当前日期是 ${today}，当前时间是 ${now.toLocalTime()}，当前完整时间是 ${now}。
                    category 必须从这个分类数组中选择，并且输出时必须逐字复用其中某一个值，不能改写，不能新增：$categoryJson
                    如果语音里没有明确日期，就使用今天 ${today}。
                    如果语音里没有明确时间，就结合上下文推断一个最可能的时间；如果仍然无法判断，就使用当前时间 ${now.toLocalTime()}。
                    JSON 字段格式如下：
                    {"amount":"12.50","category":"餐饮","note":"午饭","date":"${today}","time":"12:30"}
                    amount 使用元为单位的阿拉伯数字，不带货币符号；无法识别时填空字符串。
                    note 用简短中文描述具体支出。
                    date 必须是 yyyy-MM-dd。
                    time 必须是 HH:mm。
                    """.trimIndent()

                val requestBody =
                    JSONObject().apply {
                        put("model", settings.modelName)
                        put("temperature", 0.1)
                        if (isOpenRouterUrl(settings.apiBaseUrl)) {
                            // OpenRouter documents `reasoning.effort = "none"` as the way to disable reasoning.
                            put(
                                "reasoning",
                                JSONObject().apply {
                                    put("effort", "none")
                                },
                            )
                        }
                        put(
                            "messages",
                            JSONArray().apply {
                                put(
                                    JSONObject().apply {
                                        put("role", "system")
                                        put("content", "你是一个严格输出 JSON 的中文记账助手。")
                                    },
                                )
                                put(
                                    JSONObject().apply {
                                        put("role", "user")
                                        put(
                                            "content",
                                            JSONArray().apply {
                                                put(
                                                    JSONObject().apply {
                                                        put("type", "text")
                                                        put("text", prompt)
                                                    },
                                                )
                                                put(
                                                    JSONObject().apply {
                                                        put("type", "input_audio")
                                                        put(
                                                            "input_audio",
                                                            JSONObject().apply {
                                                                put(
                                                                    "data",
                                                                    Base64.encodeToString(wavBytes, Base64.NO_WRAP),
                                                                )
                                                                put("format", "wav")
                                                            },
                                                        )
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }

                val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
                }

                connection.outputStream.use { output ->
                    output.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                val responseText =
                    ((if (responseCode in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()
                        ?.use(BufferedReader::readText)).orEmpty()

                require(responseCode in 200..299) {
                    parseErrorMessage(responseText).ifBlank { "大模型调用失败($responseCode)" }
                }

                val assistantText = extractAssistantText(responseText)
                val payload = JSONObject(stripCodeFence(assistantText))

                ExpenseRecognitionResult(
                    amountText = payload.optString("amount"),
                    category = payload.optString("category"),
                    note = payload.optString("note"),
                    dateText = payload.optString("date"),
                    timeText = payload.optString("time"),
                    rawResponse = assistantText,
                )
            }
        }

    private fun buildChatCompletionsUrl(input: String): String {
        val normalized = input.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) {
            normalized
        } else {
            "$normalized/chat/completions"
        }
    }

    private fun isOpenRouterUrl(input: String): Boolean =
        runCatching {
            val host = URL(input.trim()).host.lowercase()
            host == "openrouter.ai" || host.endsWith(".openrouter.ai")
        }.getOrDefault(false)

    private fun parseErrorMessage(responseText: String): String =
        runCatching {
            JSONObject(responseText).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")

    private fun extractAssistantText(responseText: String): String {
        val root = JSONObject(responseText)
        val messageContent =
            root.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.opt("content")
                ?: error("大模型响应缺少内容")

        return when (messageContent) {
            is String -> messageContent
            is JSONArray -> buildString {
                for (index in 0 until messageContent.length()) {
                    val item = messageContent.opt(index)
                    when (item) {
                        is JSONObject -> append(item.optString("text"))
                        is String -> append(item)
                    }
                }
            }

            else -> error("无法解析大模型响应")
        }.trim()
    }

    private fun stripCodeFence(text: String): String =
        text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
