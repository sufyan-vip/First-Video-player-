package com.aether.player.ai

import android.content.Context
import com.aether.player.data.prefs.AiProvider
import com.aether.player.data.prefs.SecureStore
import com.aether.player.data.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiException(message: String) : Exception(message)

data class AiConfig(
    val provider: AiProvider,
    val endpoint: String,
    val model: String,
    val apiKey: String,
)

/**
 * Minimal HTTP client for the supported AI providers.
 * No network call is ever made unless the user enables AI and provides a key.
 */
class AiClient(
    context: Context,
    private val prefs: UserPreferences,
    private val secure: SecureStore,
) {
    private val appContext = context.applicationContext

    suspend fun config(): AiConfig {
        val s = prefs.settings.first()
        val provider = if (!s.aiEnabled) AiProvider.OFF else s.aiProvider
        val model = s.aiModel.ifBlank { defaultModel(provider) }
        val endpoint = s.aiEndpoint.ifBlank { defaultEndpoint(provider, model) }
        return AiConfig(provider, endpoint, model, secure.get(AI_API_KEY))
    }

    suspend fun isReady(): Boolean {
        val c = config()
        return c.provider != AiProvider.OFF && c.apiKey.isNotBlank()
    }

    suspend fun generate(prompt: String, system: String? = null, temperature: Double = 0.3): String {
        val c = config()
        if (c.provider == AiProvider.OFF) throw AiException("AI is turned off. Enable it in Settings → AI assistant.")
        if (c.apiKey.isBlank()) throw AiException("No API key saved. Add one in Settings → AI assistant.")
        return when (c.provider) {
            AiProvider.GEMINI -> gemini(c, prompt, system, temperature)
            AiProvider.OPENROUTER -> openRouter(c, prompt, system, temperature)
            AiProvider.OFF -> throw AiException("AI provider is off.")
        }
    }

    private suspend fun gemini(c: AiConfig, prompt: String, system: String?, temperature: Double): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
            if (!system.isNullOrBlank()) {
                body.put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            }
            body.put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            body.put("generationConfig", JSONObject().put("temperature", temperature).put("maxOutputTokens", 2048))
            val raw = post(c.endpoint, body.toString(), mapOf("x-goog-api-key" to c.apiKey))
            val root = JSONObject(raw)
            if (root.has("error")) {
                throw AiException("Gemini: " + root.optJSONObject("error")?.optString("message", "request failed"))
            }
            root.optJSONArray("candidates")
                ?.optJSONObject(0)?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)
                ?.optString("text", "")?.trim()
                .takeUnless { it.isNullOrEmpty() }
                ?: throw AiException("Gemini returned an empty response.")
        }

    private suspend fun openRouter(c: AiConfig, prompt: String, system: String?, temperature: Double): String =
        withContext(Dispatchers.IO) {
            val messages = JSONArray()
            if (!system.isNullOrBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", system))
            }
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            val body = JSONObject()
                .put("model", c.model)
                .put("messages", messages)
                .put("temperature", temperature)
                .put("max_tokens", 2048)
            val raw = post(
                c.endpoint,
                body.toString(),
                mapOf(
                    "Authorization" to "Bearer ${c.apiKey}",
                    "HTTP-Referer" to "https://aether.player.local",
                    "X-Title" to "Aether Player",
                ),
            )
            val root = JSONObject(raw)
            if (root.has("error")) {
                val err = root.opt("error")
                val msg = if (err is JSONObject) err.optString("message", "request failed") else err.toString()
                throw AiException("OpenRouter: $msg")
            }
            root.optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content", "")?.trim()
                .takeUnless { it.isNullOrEmpty() }
                ?: throw AiException("OpenRouter returned an empty response.")
        }

    private fun post(url: String, body: String, headers: Map<String, String>): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) throw AiException("HTTP $code: ${text.take(300).ifBlank { "request failed" }}")
            text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val AI_API_KEY = SecureStore.AI_API_KEY

        fun defaultModel(provider: AiProvider): String = when (provider) {
            AiProvider.GEMINI -> "gemini-2.0-flash"
            AiProvider.OPENROUTER -> "openrouter/auto"
            AiProvider.OFF -> ""
        }

        fun defaultEndpoint(provider: AiProvider, model: String): String = when (provider) {
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.OFF -> ""
        }

        fun label(provider: AiProvider): String = when (provider) {
            AiProvider.OFF -> "Off"
            AiProvider.GEMINI -> "Google Gemini"
            AiProvider.OPENROUTER -> "OpenRouter"
        }
    }
}
