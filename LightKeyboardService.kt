package com.example.lightkeyboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class LightKeyboardService : InputMethodService() {

    private val ui = Handler(Looper.getMainLooper())

    override fun onEvaluateFullscreenMode(): Boolean = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateInputView(): View {
        val h = (resources.displayMetrics.density * 380).toInt()
        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.setBackgroundColor(Color.parseColor("#120C08"))
        web.isHapticFeedbackEnabled = false
        // WebView must never take focus, otherwise the text field loses its input connection
        web.isFocusable = false
        web.isFocusableInTouchMode = false
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) {
                    v.loadData("<body style='background:#300;color:#fff;font:16px sans-serif'>Load error: " + e.description + "</body>", "text/html", "utf-8")
                }
            }
        }
        web.loadUrl("file:///android_asset/index.html")

        val box = FrameLayout(this)
        box.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
        box.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h))
        return box
    }

    inner class Bridge {
        @JavascriptInterface
        fun commit(s: String) {
            ui.post { currentInputConnection?.commitText(s, 1) }
        }

        @JavascriptInterface
        fun backspace() {
            ui.post { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) }
        }

        @JavascriptInterface
        fun enter() {
            ui.post { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
        }
    }
}
