package com.kapdatalabs.kamo

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var printerService: SunmiPrinterService? = null

    private val bindHandler = Handler(Looper.getMainLooper())
    private var bindAttempts = 0
    private val MAX_ATTEMPTS = 15  // 45 secondes max

    // ── Retry loop ────────────────────────────────────────────────────────────
    private val bindRunnable = object : Runnable {
        override fun run() {
            if (printerService != null) {
                // Connecté — on s'arrête
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "✓ Imprimante prête", Toast.LENGTH_SHORT).show()
                }
                return
            }
            if (bindAttempts < MAX_ATTEMPTS) {
                bindAttempts++
                bindPrinter()
                bindHandler.postDelayed(this, 3000)
            }
            // Après MAX_ATTEMPTS tentatives, on abandonne silencieusement
        }
    }

    // ── Connexion à l'imprimante Sunmi ────────────────────────────────────────
    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            printerService = service
            // Annuler le retry loop dès qu'on est connecté
            bindHandler.removeCallbacks(bindRunnable)
            runOnUiThread {
                Toast.makeText(this@MainActivity,
                    "✓ Imprimante connectée", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onDisconnected() {
            printerService = null
        }
    }

    private fun bindPrinter() {
        try {
            InnerPrinterManager.getInstance().bindService(applicationContext, printerCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Plein écran sans barre de titre
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
            cacheMode                    = WebSettings.LOAD_DEFAULT
            textZoom                     = 100
        }

        // ── JS Bridge ──────────────────────────────────────────────────────
        webView.addJavascriptInterface(SunmiPrintBridge(), "SunmiPrint")
        webView.webViewClient = WebViewClient()

        // ── Charger l'app ──────────────────────────────────────────────────
        webView.loadUrl("file:///android_asset/index.html")

        // ── Connecter l'imprimante + démarrer le retry loop ────────────────
        bindPrinter()
        bindHandler.postDelayed(bindRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        bindHandler.removeCallbacks(bindRunnable)
        try {
            InnerPrinterManager.getInstance().unBindService(applicationContext, printerCallback)
        } catch (e: Exception) { /* ignore */ }
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
            val service = printerService ?: run {
                // Retenter le bind
                bindPrinter()
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Imprimante non connectée — réessayez dans 3s",
                        Toast.LENGTH_LONG).show()
                }
                return
            }
            try {
                service.printerInit(null)
                service.printText(text, null)
                service.lineWrap(4, null)
            } catch (e: RemoteException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity,
                        "Erreur impression: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /**
         * Force une reconnexion depuis JS.
         * Appelé depuis JS : window.SunmiPrint.reconnect()
         */
        @JavascriptInterface
        fun reconnect() {
            bindAttempts = 0
            bindPrinter()
            bindHandler.removeCallbacks(bindRunnable)
            bindHandler.postDelayed(bindRunnable, 3000)
            runOnUiThread {
                Toast.makeText(this@MainActivity,
                    "Reconnexion imprimante...", Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Affiche un toast Android depuis JS.
         */
        @JavascriptInterface
        fun toast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * Vérifie si l'imprimante est prête.
         */
        @JavascriptInterface
        fun isAvailable(): Boolean = printerService != null
    }
}
