package com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini

import android.util.Log
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Mints a short-lived token for one Live API session.
 *
 * The Live API no longer opens on a bare API key -- Google is moving that
 * endpoint to ephemeral tokens, and the refusal is a generic close code 1008
 * that reads the same whether the key is malformed, restricted, or simply not
 * accepted there. Minting a token removes the question: this call either
 * produces a token or returns Google's own error text explaining why.
 *
 * The token is scoped to a single use and the one model the app talks to, so
 * it is worth little if pulled off the wire -- unlike the long-lived key,
 * which is exactly why Google wants this shape for a client that connects
 * directly.
 */
object EphemeralTokenProvider {
    private const val TAG = "EphemeralToken"

    /** New sessions must start within this window; Google defaults to 60s. */
    private const val NEW_SESSION_WINDOW_MINUTES = 2L

    /** How long the whole session may run; Google defaults to 30 minutes. */
    private const val SESSION_WINDOW_MINUTES = 30L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Token(val value: String) : Result()
        data class Failure(val message: String) : Result()
    }

    /** Blocking: call from a background thread, never from the main one. */
    fun mint(apiKey: String, model: String): Result {
        val now = System.currentTimeMillis()
        val newSessionExpire = isoUtc(now + NEW_SESSION_WINDOW_MINUTES * 60_000)
        val expire = isoUtc(now + SESSION_WINDOW_MINUTES * 60_000)

        // The body is an AuthToken, and AuthToken has exactly five fields:
        // name (output only), expireTime, newSessionExpireTime, fieldMask and
        // the config union -- which is `bidiGenerateContentSetup` or `uses`.
        // There is no `liveConnectConstraints`: that name came off a curl
        // example read at a glance, and the API answers 400 "Unknown name" for
        // it. A variant with is omitted on purpose too -- if it is present,
        // the docs say it *replaces* the setup frame whole, and the setup
        // frame is where this app's system prompt and audio format live.
        // Omit both and the token is a plain bearer credential, with the
        // session still pinned by the model id in the setup frame.
        val body = """
            {
              "uses": 1,
              "expireTime": "$expire",
              "newSessionExpireTime": "$newSessionExpire"
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(GeminiConfig.TOKENS_URL)
            .header("x-goog-api-key", apiKey)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // Google's own copy, not a guess: this is the one place
                    // that says why the key was refused.
                    val msg = "Token request failed (HTTP ${response.code}): ${response.message}" +
                        if (text.isNotEmpty()) " -- ${text.take(300)}" else ""
                    Log.e(TAG, msg)
                    return Result.Failure(msg)
                }
                // The token is NOT the response body -- it is the response
                // body's "name" field (see AuthToken.name in the API docs).
                val token = extractName(text)
                if (token.isNullOrEmpty()) {
                    val msg = "Token response had no name field: ${text.take(300)}"
                    Log.e(TAG, msg)
                    Result.Failure(msg)
                } else {
                    Log.d(TAG, "Minted ephemeral token")
                    Result.Token(token)
                }
            }
        } catch (e: Exception) {
            val msg = "Token request error: ${e.message ?: e.javaClass.simpleName}"
            Log.e(TAG, msg)
            Result.Failure(msg)
        }
    }

    private fun extractName(json: String): String? =
        try {
            org.json.JSONObject(json).optString("name", "").ifEmpty { null }
        } catch (e: Exception) {
            null
        }

    /** Google's Timestamp format; millis is what the API examples pass. */
    private fun isoUtc(millis: Long): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date(millis))
    }
}
