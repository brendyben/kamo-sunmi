package com.kapdatalabs.kamo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.SdkException
import com.sunmi.printerx.api.PrinterApi
import com.sunmi.printerx.enums.PrinterFormat
import java.nio.charset.Charset

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var printerApi: PrinterApi? = null

    // ── Sunmi PrinterSdk initialisation ──────────────────────────────────────
    private val printerListener = object : PrinterSdk.PrinterListen {
        override fun onDefPrinter(printer: PrinterSdk.Printer?) {
            printer?.let { p ->
                try {
                    printerApi = p.getApi(PrinterFormat.TEXT)
                } catch (e: SdkException) {
                    e.printStackTrace()
                }
            }
        }
        override fun onPrinters(printers: MutableList<PrinterSdk.Printer>?) {}
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen sans barre de titre
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        webView = WebView(this)
        setContentView(webView)

        // ── WebView settings ────────────────────────────────────────────────
        webView.settings.apply {
            javaScriptEnabled            = true
            domStorageEnabled            = true
            allowFileAccess              = true
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs  = true
            databaseEnabled              = true
            cacheMode                    = WebSettings.LOAD_DEFAULT
            textZoom                     = 100
        }

        // ── JS Bridge ──────────────────────────────────────────────────────
        webView.addJavascriptInterface(SunmiPrintBridge(), "SunmiPrint")

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                // silencieux — page locale, ne devrait pas échouer
            }
        }

        // ── Charger l'app ──────────────────────────────────────────────────
        webView.loadUrl("file:///android_asset/index.html")

        // ── Connecter l'imprimante Sunmi ───────────────────────────────────
        try {
            PrinterSdk.getInstance().getPrinter(this, printerListener)
        } catch (e: SdkException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { PrinterSdk.getInstance().destroy(this) } catch (e: Exception) { /* ignore */ }
        webView.destroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BRIDGE JS ↔ Android
    // ─────────────────────────────────────────────────────────────────────────
    inner class SunmiPrintBridge {

        /**
         * Imprime le texte brut sur la thermique 58 mm.
         * Appelé depuis JS : window.SunmiPrint.printRaw(ticket)
         */
        @JavascriptInterface
        fun printRaw(text: String) {
            val api = printerApi ?: run {
                runOnUiThread { Toast.makeText(this@MainActivity,
                    "Imprimante non disponible", Toast.LENGTH_SHORT).show() }
                return
            }
            try {
                api.config()
                    .initPrinter()
                    .commit()
                api.lineApi()
                    .printText(text)
                    .commit()
                api.lineApi()
                    .feedPaper(4)
                    .commit()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Erreur impression : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Affiche un toast Android depuis JS.
         * Appelé depuis JS : window.SunmiPrint.toast(message)
         */
        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Vérifie si l'imprimante est prête.
         * Appelé depuis JS : window.SunmiPrint.isAvailable() → boolean
         */
        @JavascriptInterface
        fun isAvailable(): Boolean = printerApi != null
    }
}
