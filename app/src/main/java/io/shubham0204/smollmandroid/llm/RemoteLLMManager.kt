/*
 * Copyright (C) 2024 AI Chat Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shubham0204.smollmandroid.llm

import android.util.Log
import io.shubham0204.smollmandroid.data.AppDB
import io.shubham0204.smollmandroid.data.Chat
import io.shubham0204.smollmandroid.data.SharedPrefStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.annotation.Single
import java.io.BufferedReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.measureTime

/**
 * Remote LLM Manager — connects to an OpenAI-compatible API server
 * (LM Studio Link, Ollama, vLLM, etc.) for inference.
 *
 * Provides the same callback interface as SmolLMManager so the ViewModel
 * can switch between local and remote seamlessly.
 */
@Single
class RemoteLLMManager(
    private val appDB: AppDB,
    private val sharedPrefStore: SharedPrefStore,
) {

    companion object {
        private const val TAG = "RemoteLLMManager"
        const val PREF_REMOTE_ENABLED = "remote_llm_enabled"
        const val PREF_REMOTE_URL = "remote_llm_url"
        const val PREF_REMOTE_MODEL = "remote_llm_model"
        const val PREF_REMOTE_API_KEY = "remote_llm_api_key"
        const val DEFAULT_URL = "http://192.168.1.100:1234"
        const val DEFAULT_MODEL = ""
        const val DEFAULT_API_KEY = ""
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val isEnabled = AtomicBoolean(false)

    @Volatile
    var isInferenceOn = false
        private set

    @Volatile
    private var responseJob: Job? = null

    @Volatile
    private var chat: Chat? = null

    private val conversationMessages = mutableListOf<ChatApiMessage>()

    @Serializable
    data class ChatApiMessage(
        val role: String,
        val content: String,
    )

    @Serializable
    data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatApiMessage>,
        val temperature: Float = 0.8f,
        val stream: Boolean = true,
        val max_tokens: Int = 4096,
    )

    init {
        isEnabled.set(sharedPrefStore.get(PREF_REMOTE_ENABLED, false))
    }

    fun getServerUrl(): String = sharedPrefStore.get(PREF_REMOTE_URL, DEFAULT_URL)
    fun getModelName(): String = sharedPrefStore.get(PREF_REMOTE_MODEL, DEFAULT_MODEL)
    fun getApiKey(): String = sharedPrefStore.get(PREF_REMOTE_API_KEY, DEFAULT_API_KEY)

    fun setServerUrl(url: String) = sharedPrefStore.put(PREF_REMOTE_URL, url)
    fun setModelName(model: String) = sharedPrefStore.put(PREF_REMOTE_MODEL, model)
    fun setApiKey(key: String) = sharedPrefStore.put(PREF_REMOTE_API_KEY, key)

    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        sharedPrefStore.put(PREF_REMOTE_ENABLED, enabled)
    }

    /**
     * Initialize for a chat session. Loads conversation history.
     */
    fun load(chat: Chat, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        this.chat = chat
        conversationMessages.clear()

        // Add system prompt
        if (chat.systemPrompt.isNotEmpty()) {
            conversationMessages.add(ChatApiMessage("system", chat.systemPrompt))
        }

        // Load existing messages
        if (!chat.isTask) {
            appDB.getMessagesForModel(chat.id).forEach { message ->
                val role = if (message.isUserMessage) "user" else "assistant"
                conversationMessages.add(ChatApiMessage(role, message.message))
            }
        }

        onSuccess()
    }

    fun unload() {
        responseJob?.cancel()
        chat = null
        conversationMessages.clear()
        isInferenceOn = false
    }

    /**
     * Send a query to the remote server with streaming response.
     */
    fun getResponse(
        query: String,
        chat: Chat,
        responseTransform: (String) -> String,
        onPartialResponseGenerated: (String) -> Unit,
        onSuccess: (SmolLMManager.SmolLMResponse) -> Unit,
        onCancelled: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        responseJob?.cancel()

        responseJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                isInferenceOn = true

                // Add user message to conversation
                conversationMessages.add(ChatApiMessage("user", query))

                val serverUrl = getServerUrl().trimEnd('/')
                val modelName = getModelName()
                val apiKey = getApiKey()

                val request = ChatCompletionRequest(
                    model = modelName,
                    messages = conversationMessages.toList(),
                    temperature = chat.temperature,
                    stream = true,
                )

                val requestBody = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())

                val httpRequestBuilder = Request.Builder()
                    .url("$serverUrl/v1/chat/completions")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")

                if (apiKey.isNotBlank()) {
                    httpRequestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val httpRequest = httpRequestBuilder.build()

                var response = ""
                val startTime = System.currentTimeMillis()

                val httpResponse = client.newCall(httpRequest).execute()

                if (!httpResponse.isSuccessful) {
                    val errorBody = httpResponse.body?.string() ?: "Unknown error"
                    throw Exception("Server error ${httpResponse.code}: $errorBody")
                }

                val reader = BufferedReader(httpResponse.body!!.charStream())
                var tokenCount = 0

                reader.use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) {
                        if (line!!.startsWith("data: ")) {
                            val data = line!!.removePrefix("data: ").trim()
                            if (data == "[DONE]") break

                            try {
                                val chunk = json.parseToJsonElement(data) as JsonObject
                                val choices = chunk["choices"]?.jsonArray
                                val delta = choices?.get(0)?.jsonObject?.get("delta")?.jsonObject
                                val content = delta?.get("content")?.jsonPrimitive?.content

                                if (content != null) {
                                    response += content
                                    tokenCount++
                                    withContext(Dispatchers.Main) {
                                        onPartialResponseGenerated(response)
                                    }
                                }

                                // Check for finish_reason
                                val finishReason = choices?.get(0)?.jsonObject
                                    ?.get("finish_reason")?.jsonPrimitive?.content
                                if (finishReason != null && finishReason != "null") break
                            } catch (_: Exception) {
                                // Skip malformed SSE lines
                            }
                        }
                    }
                }

                response = responseTransform(response)
                val durationMs = System.currentTimeMillis() - startTime
                val durationSecs = (durationMs / 1000).toInt().coerceAtLeast(1)
                val tokensPerSec = tokenCount.toFloat() / durationSecs

                // Add assistant response to conversation
                conversationMessages.add(ChatApiMessage("assistant", response))

                // Save to DB
                val currentChat = chat
                appDB.addAssistantMessage(currentChat.id, response)

                withContext(Dispatchers.Main) {
                    isInferenceOn = false
                    onSuccess(
                        SmolLMManager.SmolLMResponse(
                            response = response,
                            generationSpeed = tokensPerSec,
                            generationTimeSecs = durationSecs,
                            contextLengthUsed = conversationMessages.sumOf { it.content.length / 4 },
                        )
                    )
                }

            } catch (e: CancellationException) {
                isInferenceOn = false
                withContext(Dispatchers.Main) { onCancelled() }
            } catch (e: Exception) {
                isInferenceOn = false
                Log.e(TAG, "Remote inference failed", e)
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    fun stopResponseGeneration() {
        responseJob?.cancel()
        isInferenceOn = false
    }

    /**
     * Test connection to the remote server. Returns the list of available models on success.
     */
    suspend fun testConnection(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl().trimEnd('/')
            val apiKey = getApiKey()

            val requestBuilder = Request.Builder()
                .url("$serverUrl/v1/models")
                .get()

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Server returned ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.success(emptyList())
            val parsed = json.parseToJsonElement(body) as JsonObject
            val models = parsed["data"]?.jsonArray?.mapNotNull { model ->
                model.jsonObject["id"]?.jsonPrimitive?.content
            } ?: emptyList()

            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
