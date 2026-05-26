
package com.ajizhang.savemoney.data.remote

import android.util.Base64
import com.ajizhang.savemoney.data.model.ExpenseRecognitionResult
import com.ajizhang.savemoney.data.model.ImageExpenseRecognitionResult
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
        subBudgetNames: List<String> = emptyList(),
        today: LocalDate,
        now: LocalDateTime,
    ): Result<ExpenseRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = settingsRepository.getSettings()
                require(settings.isComplete) { "请先在设置里填写 API 地址、API Key 和模型名称" }

                val requestUrl = buildChatCompletionsUrl(settings.apiBaseUrl)
                val categoryJson = JSONArray(categories).toString()
                val subBudgetJson = JSONArray(subBudgetNames).toString()
                val subBudgetHint = if (subBudgetNames.isNotEmpty()) {
                    """
                    当前月份的子预算列表：$subBudgetJson
                    请根据支出描述判断属于哪个子预算，将子预算名称填入 budgetSubName 字段；无法判断时填空字符串。
                    """.trimIndent()
                } else ""

                val prompt =
                    """
                    你是记账助手。请根据这段语音识别一笔"支出"，并只返回一个 JSON 对象，不要 markdown，不要解释。
                    当前日期是 ${today}，当前时间是 ${now.toLocalTime()}，当前完整时间是 ${now}。
                    category 必须从这个分类数组中选择，并且输出时必须逐字复用其中某一个值，不能改写，不能新增：$categoryJson
                    如果语音里没有明确日期，就使用今天 ${today}。
                    如果语音里没有明确时间，就结合上下文推断一个最可能的时间；如果仍然无法判断，就使用当前时间 ${now.toLocalTime()}。
                    $subBudgetHint
                    JSON 字段格式如下：
                    {"amount":"12.50","category":"餐饮","note":"午饭","date":"${today}","time":"12:30","budgetSubName":""}
                    amount 使用元为单位的阿拉伯数字，不带货币符号；无法识别时填空字符串。
                    note 用简短中文描述具体支出。
                    date 必须是 yyyy-MM-dd。
                    time 必须是 HH:mm。
                    budgetSubName 必须是子预算列表中的值，不能改写、不能新增；无法判断时填空字符串。
                    """.trimIndent()

                val requestBody =
                    JSONObject().apply {
                        put("model", settings.modelName)
                        put("temperature", 0.1)
                        if (isOpenRouterUrl(settings.apiBaseUrl)) {
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
                    budgetSubName = payload.optString("budgetSubName"),
                    rawResponse = assistantText,
                )
            }
        }

    suspend fun recognizeExpensesFromImage(
        imageBytes: ByteArray,
        mimeType: String,
        categories: List<String>,
        subBudgetNames: List<String> = emptyList(),
        today: LocalDate,
        now: LocalDateTime,
    ): Result<ImageExpenseRecognitionResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val settings = settingsRepository.getSettings()
                require(settings.isComplete) { "请先在设置里填写 API 地址、API Key 和模型名称" }

                val requestUrl = buildChatCompletionsUrl(settings.apiBaseUrl)
                val categoryJson = JSONArray(categories).toString()
                val dataUrl = "data:$mimeType;base64,${Base64.encodeToString(imageBytes, Base64.NO_WRAP)}"
                val subBudgetJson = JSONArray(subBudgetNames).toString()
                val subBudgetHint = if (subBudgetNames.isNotEmpty()) {
                    """
                    
                    当前月份的子预算列表：$subBudgetJson
                    请根据支出内容判断每笔支出属于哪个子预算，将子预算名称填入每条记录的 budgetSubName 字段；无法判断时填空字符串。
                    """.trimIndent()
                } else ""

                val prompt =
                    """
                    你是一个记账助手。请识别这张图片中的所有支出项目，返回一个严格 JSON 数组。
                    图片可能是一张小票、收据、账单截图或者手写账本照片。

                    category 必须从以下分类中选择一个最接近的，逐字复制，不能改写或新增：
                    $categoryJson
                    如果图片中没有明显匹配的分类，使用"其他"。

                    日期默认使用今天：${today}，时间默认用当前时间：${now.toLocalTime()}。
                    如果图片中有明确的日期或时间，优先使用图片中的信息。
                    $subBudgetHint
                    每项格式：
                    {"amount":"金额(元)","category":"分类","note":"简短描述(10字内)","date":"yyyy-MM-dd","time":"HH:mm","budgetSubName":""}

                    只返回 JSON 数组，不要 markdown，不要解释，不要多余文字。
                    示例：[{"amount":"12.50","category":"餐饮","note":"午饭","date":"${today}","time":"12:30","budgetSubName":"日常餐饮"}]

                    如果图片中没有任何支出信息，返回空数组 []。
                    """.trimIndent()

                val requestBody =
                    JSONObject().apply {
                        put("model", settings.modelName)
                        put("temperature", 0.1)
                        if (isOpenRouterUrl(settings.apiBaseUrl)) {
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
                                                        put("type", "image_url")
                                                        put(
                                                            "image_url",
                                                            JSONObject().apply {
                                                                put("url", dataUrl)
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
                    readTimeout = 120_000
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
                val items = ImageExpenseParser.parseExpenseArray(
                    ImageExpenseParser.stripCodeFence(assistantText),
                )

                ImageExpenseRecognitionResult(
                    items = items,
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
