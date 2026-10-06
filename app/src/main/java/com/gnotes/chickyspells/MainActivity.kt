package com.gnotes.chickyspells

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import java.util.Locale

/**
 * Chicky Spells: the game itself is the web page in assets/index.html.
 * This activity hosts it in a WebView and gives it two things WebView lacks:
 * Android text-to-speech (Chicky's voice) and Android speech recognition (the kid's voice).
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null
    private var listenAfterPermission = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true          // keeps word lists, stars and stickers
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "AndroidBridge")
        web.loadUrl("file:///android_asset/index.html")

        tts = TextToSpeech(this, this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.US)
            tts?.setPitch(1.25f)   // a bit higher for a cheerful little rooster
            ttsReady = true
        }
    }

    private fun js(code: String) = runOnUiThread { web.evaluateJavascript(code, null) }
    private fun q(s: String) = JSONArray().put(s).toString().let { it.substring(1, it.length - 1) }

    inner class Bridge {
        @JavascriptInterface
        fun speak(text: String, rate: Double) {
            if (!ttsReady) return
            tts?.setSpeechRate(rate.toFloat().coerceIn(0.3f, 1.5f))
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chicky")
        }

        @JavascriptInterface
        fun stopSpeaking() { tts?.stop() }

        @JavascriptInterface
        fun startListening() = runOnUiThread { beginListening() }

        @JavascriptInterface
        fun stopListening() = runOnUiThread {
            recognizer?.cancel()
            js("window.__chicky&&__chicky.end()")
        }
    }

    private fun beginListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            listenAfterPermission = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 7)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            js("window.__chicky&&__chicky.error('unavailable')")
            return
        }
        tts?.stop()
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(listener) }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // give kids time to say letters one at a time
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        recognizer?.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (ok && listenAfterPermission) beginListening()
            else if (!ok) js("window.__chicky&&__chicky.error('not-allowed')")
            listenAfterPermission = false
        }
    }

    private fun sendResults(b: Bundle?, final: Boolean) {
        val list = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        if (list.isEmpty()) return
        val arr = JSONArray(); list.forEach { arr.put(it) }
        js("window.__chicky&&__chicky.result($arr,$final)")
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = js("window.__chicky&&__chicky.start()")
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) = sendResults(partialResults, false)
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onResults(results: Bundle?) {
            sendResults(results, true)
            js("window.__chicky&&__chicky.end()")
        }
        override fun onError(error: Int) {
            val code = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "not-allowed"
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no-speech"
                SpeechRecognizer.ERROR_AUDIO -> "audio-capture"
                else -> "other"
            }
            js("window.__chicky&&__chicky.error(${q(code)});window.__chicky&&__chicky.end()")
        }
    }

    override fun onPause() {
        super.onPause()
        recognizer?.cancel()
        tts?.stop()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        web.destroy()
        super.onDestroy()
    }
}
