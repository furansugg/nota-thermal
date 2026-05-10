package com.notathermal.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Minimal client for Google's Gemini `generateContent` REST endpoint. We use
 * `gemini-1.5-flash` for low latency and high free-tier quota; the response is
 * constrained to JSON via `responseMimeType` so we can safely parse it without
 * extra unwrapping.
 *
 * Stays dependency-free (no OkHttp / Retrofit) on purpose — keeps APK small
 * and the call happens at most once per invoice creation.
 */
class GeminiClient(
    private val model: String = "gemini-1.5-flash"
) {

    suspend fun generateJson(
        apiKey: String,
        systemInstruction: String,
        userText: String,
        responseSchema: JSONObject
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Kunci API Gemini belum diisi di Pengaturan." }

            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "$model:generateContent?key=" + URLEncoder.encode(apiKey, "UTF-8")
            )

            val payload = JSONObject().apply {
                put(
                    "system_instruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", userText))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.2)
                        .put("response_mime_type", "application/json")
                        .put("response_schema", responseSchema)
                )
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use(BufferedReader::readText)
                if (code !in 200..299) {
                    val message = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    error("Gemini error $code: ${message ?: body.take(300)}")
                }

                val root = JSONObject(body)
                val text = root
                    .optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")
                    ?.optJSONObject(0)?.optString("text")
                    ?: error("Respon Gemini kosong / tidak dikenali.")
                JSONObject(text)
            } finally {
                conn.disconnect()
            }
        }
    }
}
