package com.example.lightkeyboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class LightKeyboardService : InputMethodService() {

    private val ui = Handler(Looper.getMainLooper())
    private var web: WebView? = null
    private var box: FrameLayout? = null
    private var glowPx = 0   // transparent light area above the keys
    private var totalPx = 0  // glow area + keyboard

    override fun onEvaluateFullscreenMode(): Boolean = false

    // The top "glow" area is transparent and lets touches pass through to the app below.
    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        if (totalPx > 0) {
            outInsets.contentTopInsets = glowPx
            outInsets.visibleTopInsets = glowPx
            outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(0, glowPx, resources.displayMetrics.widthPixels, totalPx)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateInputView(): View {
        val d = resources.displayMetrics.density
        glowPx = (150 * d).toInt()
        totalPx = (480 * d).toInt()   // JS shrinks this to the real keyboard height

        try { window?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) } catch (e: Exception) {}

        val w = WebView(this)
        web = w
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.allowFileAccess = true
        w.settings.mediaPlaybackRequiresUserGesture = false
        w.setBackgroundColor(Color.TRANSPARENT)
        w.isHapticFeedbackEnabled = false
        w.isFocusable = false                 // never steal focus from the text field
        w.isFocusableInTouchMode = false
        w.addJavascriptInterface(Bridge(), "Android")
        w.webViewClient = object : WebViewClient() {
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) {
                    v.loadData("<body style='background:#300;color:#fff;font:16px sans-serif'>Load error: " + e.description + "</body>", "text/html", "utf-8")
                }
            }
        }
        w.loadUrl("file:///android_asset/index.html")

        val b = FrameLayout(this)
        box = b
        b.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, totalPx)
        b.addView(w, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, totalPx))
        return b
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        web?.evaluateJavascript("window.resetComp&&resetComp()", null)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        web?.evaluateJavascript("window.resetComp&&resetComp()", null)
    }

    inner class Bridge {
        @JavascriptInterface
        fun commit(s: String) {
            ui.post { currentInputConnection?.commitText(s, 1) }
        }

        @JavascriptInterface
        fun setComposing(s: String) {
            ui.post { currentInputConnection?.setComposingText(s, 1) }
        }

        @JavascriptInterface
        fun backspace() {
            ui.post { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) }
        }

        @JavascriptInterface
        fun enter() {
            ui.post { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
        }

        @JavascriptInterface
        fun resize(cssPx: Int) {
            ui.post {
                val px = (cssPx * resources.displayMetrics.density).toInt()
                if (px > glowPx && px != totalPx) {
                    totalPx = px
                    box?.layoutParams?.height = px
                    box?.requestLayout()
                    web?.layoutParams?.height = px
                    web?.requestLayout()
                }
            }
        }
    }
}
