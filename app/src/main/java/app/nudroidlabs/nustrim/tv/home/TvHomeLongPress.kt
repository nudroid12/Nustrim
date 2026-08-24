package app.nudroidlabs.nustrim.tv.home

import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberTvHomeLongPressTracker(): TvHomeLongPressTracker =
    remember { TvHomeLongPressTracker() }

internal class TvHomeLongPressTracker(
    private val timeoutMs: Long = ViewConfiguration.getLongPressTimeout().toLong(),
) {
    private var activeKeyCode: Int? = null
    private var pressedAtMs: Long = 0L
    private var longPressHandled = false

    fun handle(
        event: KeyEvent,
        onClick: () -> Unit,
        onLongPress: () -> Unit,
    ): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) onLongPress()
            return event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP
        }
        if (!isSelectKey(event.keyCode)) return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (activeKeyCode != event.keyCode || event.repeatCount == 0) {
                    activeKeyCode = event.keyCode
                    pressedAtMs = event.eventTime
                    longPressHandled = false
                }
                val heldMs = event.eventTime - pressedAtMs
                if (!longPressHandled && (event.isLongPress || event.repeatCount > 0 || heldMs >= timeoutMs)) {
                    longPressHandled = true
                    onLongPress()
                }
                true
            }

            KeyEvent.ACTION_UP -> {
                val heldMs = event.eventTime - pressedAtMs
                if (!longPressHandled && activeKeyCode == event.keyCode) {
                    if (heldMs >= timeoutMs) onLongPress() else onClick()
                }
                reset()
                true
            }

            else -> false
        }
    }

    private fun reset() {
        activeKeyCode = null
        pressedAtMs = 0L
        longPressHandled = false
    }

    private fun isSelectKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_BUTTON_A
        -> true

        else -> false
    }
}
