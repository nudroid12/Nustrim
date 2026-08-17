package app.nudroidlabs.nustrim.core.diagnostics

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

object NustrimDiagnostics {
    private const val TAG = "NustrimDiag"
    private const val MAX_LINES = 800
    private val lock = Any()
    private val buffer = ArrayDeque<String>()
    private val clock = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val _entries = MutableStateFlow<List<String>>(emptyList())

    val entries: StateFlow<List<String>> = _entries

    fun log(event: String, message: String = "") {
        val line = "${clock.format(Date())} [$event] ${sanitize(message)}".trimEnd()
        Log.i(TAG, line)
        append(line)
    }

    fun error(event: String, throwable: Throwable, message: String = "") {
        val detail = buildString {
            if (message.isNotBlank()) append(message).append(" | ")
            append(throwable.javaClass.simpleName)
            throwable.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
        }
        val line = "${clock.format(Date())} [$event] ${sanitize(detail)}".trimEnd()
        Log.e(TAG, line, throwable)
        append(line)
    }

    fun headers(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "{}"
        return headers.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            val safeValue = when {
                key.equals("authorization", true) -> "<redacted>"
                key.equals("cookie", true) -> "<redacted>"
                key.equals("set-cookie", true) -> "<redacted>"
                key.contains("token", true) -> "<redacted>"
                key.contains("api-key", true) -> "<redacted>"
                else -> value
            }
            "$key=$safeValue"
        }
    }

    fun snapshotText(): String = synchronized(lock) {
        buffer.joinToString("\n")
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entries.value = emptyList()
        }
        Log.i(TAG, "diagnostics cleared")
    }

    private fun append(line: String) {
        synchronized(lock) {
            buffer.addLast(line)
            while (buffer.size > MAX_LINES) buffer.removeFirst()
            _entries.value = buffer.toList()
        }
    }

    private fun sanitize(value: String): String {
        return value
            .replace(Regex("(?i)(authorization|cookie|set-cookie)\\s*[=:]\\s*[^\\s,;&]+"), "$1=<redacted>")
            .replace(Regex("(?i)(token|api[_-]?key)=([^&\\s]+)"), "$1=<redacted>")
    }
}
