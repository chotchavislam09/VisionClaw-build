package com.meta.wearable.dat.externalsampleapps.cameraaccess.gemini

import com.meta.wearable.dat.externalsampleapps.cameraaccess.settings.SettingsManager

object GeminiConfig {
    const val WEBSOCKET_BASE_URL =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    const val MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"

    // Ephemeral-token path. A bare API key can no longer open this socket:
    // Google is retiring key auth on the Live API in favour of short-lived
    // tokens, and the rejection reads as a generic 1008 either way. Mirrors
    // the two calls Google's own ephemeral-token curl examples make.
    //
    // Both calls are pinned to v1alpha, and that is not cosmetic. The
    // ephemeral-token docs and the JS SDK source both say the token is only
    // accepted on v1alpha -- minted on v1beta and opened on v1beta, the
    // handshake succeeds and then the server goes silent instead of rejecting
    // it with a code. That silence is exactly what a device run reported back
    // before this line was changed.
    const val TOKENS_URL =
        "https://generativelanguage.googleapis.com/v1alpha/auth_tokens"
    const val CONSTRAINED_WEBSOCKET_BASE_URL =
        "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContentConstrained"

    const val INPUT_AUDIO_SAMPLE_RATE = 16000
    const val OUTPUT_AUDIO_SAMPLE_RATE = 24000
    const val AUDIO_CHANNELS = 1
    const val AUDIO_BITS_PER_SAMPLE = 16

    const val VIDEO_FRAME_INTERVAL_MS = 1000L
    const val VIDEO_JPEG_QUALITY = 50

    /** System prompt plus the user's own addition, already composed. */
    val systemInstruction: String
        get() = SettingsManager.geminiEffectiveInstruction

    val apiKey: String
        get() = SettingsManager.geminiAPIKey

    /** Who the key is, as far as the settings screen can tell without a call:
     *  Google's current keys begin "AIza", newer ones "AQ.". A key that is
     *  neither is almost certainly a truncated paste, and saying so beats
     *  letting the server answer 1008 with a generic auth error. */
    val apiKeyLooksMalformed: Boolean
        get() {
            val k = apiKey.trim()
            if (k.isEmpty() || k == "YOUR_GEMINI_API_KEY") return false
            return !(k.startsWith("AIza") || k.startsWith("AQ."))
        }

    // The key does NOT go in the URL. It rides in the x-goog-api-key header
    // instead: query strings are the first thing a proxy or an HTTP logger
    // keeps, and a key sitting in `?key=` is a key that leaks. Google accepts
    // either form on this endpoint.
    fun websocketURL(): String? {
        if (apiKey == "YOUR_GEMINI_API_KEY" || apiKey.isEmpty()) return null
        return WEBSOCKET_BASE_URL
    }

    val apiKeyHeader: String?
        get() = if (apiKey == "YOUR_GEMINI_API_KEY" || apiKey.isEmpty()) null else apiKey

    /** The websocket address to open for a freshly minted ephemeral token. */
    fun constrainedWebsocketURL(token: String): String =
        "$CONSTRAINED_WEBSOCKET_BASE_URL?access_token=$token"

    val isConfigured: Boolean
        get() = apiKey != "YOUR_GEMINI_API_KEY" && apiKey.isNotEmpty()
}
