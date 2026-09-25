package com.meta.wearable.dat.externalsampleapps.cameraaccess.settings

import android.content.Context
import android.content.SharedPreferences
import com.meta.wearable.dat.externalsampleapps.cameraaccess.Secrets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which realtime model answers. The choice travels to the agent worker as
 * room-token metadata; the phone never talks to either provider directly.
 */
// OpenAI is the default engine. The picker renders entries in declaration
// order, so listing it first also makes it the left-hand segment.
enum class IntelligenceEngine(val value: String, val label: String) {
    OPENAI("openai", "OpenAI"),
    GEMINI("gemini", "Gemini");

    companion object {
        fun fromValue(value: String?): IntelligenceEngine =
            entries.firstOrNull { it.value == value } ?: OPENAI
    }
}

/**
 * Where video comes from. The app is a vision assistant first -- it opens
 * looking at the world through the phone -- and glasses are one capture
 * source, selected in Settings, rather than a mode the user must decide about
 * at launch. Exposed as a flow so the root scaffold swaps the capture
 * pipeline live when the setting changes.
 */
enum class CaptureSource(val value: String, val label: String) {
    PHONE("phone", "Phone Camera"),
    GLASSES("glasses", "Glasses");

    companion object {
        fun fromValue(value: String?): CaptureSource =
            entries.firstOrNull { it.value == value } ?: PHONE
    }
}

object SettingsManager {
    private const val PREFS_NAME = "visionclaw_settings"
    private const val DEFAULT_SIGNALING_URL = "wss://YOUR_SIGNALING_SERVER"

    private lateinit var prefs: SharedPreferences

    private val _captureSourceFlow = MutableStateFlow(CaptureSource.PHONE)
    val captureSourceFlow: StateFlow<CaptureSource> = _captureSourceFlow.asStateFlow()

    // Whether the app may show anything beyond the sign-in gate. A flow so a
    // token cleared from deep inside a call (revoked account -> 401) drops the
    // root scaffold back to the gate without a settings round-trip.
    private val _unlockedFlow = MutableStateFlow(false)
    val unlockedFlow: StateFlow<Boolean> = _unlockedFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _captureSourceFlow.value = CaptureSource.fromValue(prefs.getString("captureSource", null))
        _intelligenceEngineFlow.value =
            IntelligenceEngine.fromValue(prefs.getString("intelligenceEngine", null))
        refreshUnlocked()
    }

    fun refreshUnlocked() {
        _unlockedFlow.value = isUnlocked
    }

    var captureSource: CaptureSource
        get() = _captureSourceFlow.value
        set(value) {
            prefs.edit().putString("captureSource", value.value).apply()
            _captureSourceFlow.value = value
        }

    /** Full base URL of the hosted gateway, scheme included (e.g. "https://gw.example.com"). */
    var gatewayBaseUrl: String
        get() = prefs.getString("gatewayBaseUrl", null) ?: Secrets.gatewayBaseUrl
        set(value) = prefs.edit().putString("gatewayBaseUrl", value).apply()

    var gatewayToken: String
        get() = prefs.getString("gatewayToken", null) ?: Secrets.gatewayToken
        set(value) {
            prefs.edit().putString("gatewayToken", value).apply()
            refreshUnlocked()
        }

    /** Google account the token was issued to; null for access-code sign-ins. */
    var accountEmail: String?
        get() = prefs.getString("accountEmail", null)
        set(value) = prefs.edit().putString("accountEmail", value).apply()

    var accountUserId: String?
        get() = prefs.getString("accountUserId", null)
        set(value) = prefs.edit().putString("accountUserId", value).apply()

    /** approved | pending | revoked, as last reported by GET /me. */
    var accountStatus: String?
        get() = prefs.getString("accountStatus", null)
        set(value) {
            prefs.edit().putString("accountStatus", value).apply()
            refreshUnlocked()
        }

    /** Pending accounts hold a real token but every endpoint answers 401. */
    val isUnlocked: Boolean
        get() = isGatewayConfigured && accountStatus != "pending"

    fun signOut() {
        prefs.edit()
            .remove("gatewayToken")
            .remove("accountEmail")
            .remove("accountUserId")
            .remove("accountStatus")
            .apply()
        refreshUnlocked()
    }

    // An unfilled Secrets.kt.example placeholder is not empty, so without this
    // a fresh clone reports "configured" and then fails with a 401 that looks
    // like a server problem rather than a missing token.
    val isGatewayConfigured: Boolean
        get() = gatewayBaseUrl.startsWith("http") &&
            gatewayToken.isNotEmpty() &&
            !gatewayToken.startsWith("YOUR_")


    // The scaffold observes this to swap call screens live when the engine
    // changes, the way captureSourceFlow swaps capture pipelines.
    // Seeded with a constant, never with prefs: this initializer can run before
    // init() has assigned the lateinit `prefs`, and a read there killed the app
    // on its first frame. init() fills in the stored value.
    private val _intelligenceEngineFlow =
        MutableStateFlow(IntelligenceEngine.OPENAI)
    val intelligenceEngineFlow: StateFlow<IntelligenceEngine> = _intelligenceEngineFlow.asStateFlow()

    // Kotlin generates a setIntelligenceEngine(IntelligenceEngine) bridge for the
    // property setter, so a setter function of the same shape clashes on the JVM.
    // The flow holder is the single owner of the value instead.
    val intelligenceEngine: IntelligenceEngine
        get() = _intelligenceEngineFlow.value

    fun setIntelligenceEngine(engine: IntelligenceEngine) {
        prefs.edit().putString("intelligenceEngine", engine.value).apply()
        _intelligenceEngineFlow.value = engine
    }

    var showCaptions: Boolean
        get() = prefs.getBoolean("showCaptions", true)
        set(value) = prefs.edit().putBoolean("showCaptions", value).apply()

    // Direct Gemini Live path. The key stays on the device and is sent only to
    // Google over the websocket (GeminiConfig.websocketURL); the default below
    // is the placeholder upstream ships, so an unset key reads as "not
    // configured" rather than as a bad key.
    var geminiAPIKey: String
        get() = prefs.getString("geminiAPIKey", null) ?: DEFAULT_GEMINI_API_KEY
        set(value) = prefs.edit().putString("geminiAPIKey", value).apply()

    var geminiSystemPrompt: String
        get() = prefs.getString("geminiSystemPrompt", null) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("geminiSystemPrompt", value).apply()

    var webrtcSignalingURL: String
        get() = prefs.getString("webrtcSignalingURL", null) ?: DEFAULT_SIGNALING_URL
        set(value) = prefs.edit().putString("webrtcSignalingURL", value).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
        _captureSourceFlow.value = CaptureSource.PHONE
        _intelligenceEngineFlow.value = IntelligenceEngine.fromValue(null)
        refreshUnlocked()
    }

    const val DEFAULT_GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"

    /** Voice-only assistant: it sees through the camera and answers out loud,
     *  with no tools and nothing persistent behind it. */
    const val DEFAULT_SYSTEM_PROMPT = """You are a voice assistant for someone wearing Meta Ray-Ban smart glasses. You see through their camera and speak with them. Keep replies short and natural -- a sentence or two, spoken language rather than written.

You have no tools, no memory and no way to act on anything: you cannot send messages, search the web, set reminders or keep lists. If the person asks for something like that, say plainly that you cannot do it and suggest they ask their assistant on the phone instead. Never pretend you did something you cannot do."""
}
