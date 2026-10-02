package com.shilapi.xcertplay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class HsaeHomeKeyService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var pressedAt: Long? = null
    private val openCarPlay = Runnable {
        Log.i("xcertplay-home-key", "Long HOME: opening CarPlay")
        startActivity(Intent(this, DiPlayActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        ))
    }

    override fun onServiceConnected() {
        Log.i("xcertplay-home-key", "HSAE long HOME enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        pressedAt = null
        handler.removeCallbacks(openCarPlay)
    }

    public override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != 251 || event.device?.name != "cyttsp6_btn") return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) pressedAt = event.eventTime
            KeyEvent.ACTION_UP -> {
                val downTime = pressedAt
                pressedAt = null
                if (downTime != null && !event.isCanceled && event.eventTime - downTime >= 800) {
                    // HSAE dispatches its original HOME on release; let the launcher settle first.
                    handler.postDelayed(openCarPlay, 250)
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        onInterrupt()
        super.onDestroy()
    }
}
