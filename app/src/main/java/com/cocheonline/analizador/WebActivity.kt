package com.cocheonline.analizador

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/** Muestra el informe (la web de tu servidor) y le pasa el texto leído de la pantalla. */
class WebActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var pendingText: String? = null
    private var loaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        setContentView(web)
        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = true }
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, url: String) { loaded = true; deliver() }
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val base = Prefs.baseUrl(this@WebActivity)
                if (r.url.toString().startsWith(base)) return false
                startActivity(Intent(Intent.ACTION_VIEW, r.url)); return true
            }
            override fun onReceivedHttpAuthRequest(v: WebView, h: HttpAuthHandler, host: String, realm: String?) {
                val saved = web.getHttpAuthUsernamePassword(host, realm ?: "")
                if (saved != null && saved.size == 2 && saved[0] != null && saved[1] != null && h.useHttpAuthUsernamePassword()) { h.proceed(saved[0]!!, saved[1]!!); return }
                askLogin(h, host, realm)
            }
        }
        handle(intent)
    }

    override fun onNewIntent(i: Intent) { super.onNewIntent(i); setIntent(i); handle(i) }

    private fun handle(i: Intent) {
        val base = Prefs.baseUrl(this)
        if (base.isEmpty()) { Toast.makeText(this, "Configura primero la dirección del servidor", Toast.LENGTH_LONG).show(); startActivity(Intent(this, MainActivity::class.java)); finish(); return }
        val screen = i.getStringExtra("screenText")
        val shared = if (i.action == Intent.ACTION_SEND) i.getStringExtra(Intent.EXTRA_TEXT) else null
        loaded = false
        when {
            screen != null -> { pendingText = screen; web.loadUrl(base + "index.html?native=1") }
            shared != null -> web.loadUrl(base + "index.html?native=1&text=" + Uri.encode(shared))
            else -> web.loadUrl(base + "index.html?native=1")
        }
    }

    private fun deliver() {
        val t = pendingText ?: return
        pendingText = null
        // Espera un instante a que la aplicación termine de arrancar
        web.postDelayed({
            val js = "(function(){try{var r=window.evFromScreen(" + JSONObject.quote(t) + ");return r&&r.ok?'':(r&&r.msg)||'No se pudo leer el anuncio'}catch(e){return 'Error: '+e}})()"
            web.evaluateJavascript(js) { res ->
                val msg = res?.trim('"').orEmpty()
                if (msg.isNotEmpty() && msg != "null") Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }, 700)
    }

    private fun askLogin(h: HttpAuthHandler, host: String, realm: String?) {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val u = EditText(this).apply { hint = "Usuario"; setSingleLine() }
        val p = EditText(this).apply { hint = "Contraseña"; setSingleLine(); inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0); addView(u); addView(p) }
        AlertDialog.Builder(this).setTitle("Acceso a $host").setView(box)
            .setPositiveButton("Entrar") { _, _ ->
                web.setHttpAuthUsernamePassword(host, realm ?: "", u.text.toString(), p.text.toString())
                h.proceed(u.text.toString(), p.text.toString())
            }
            .setNegativeButton("Cancelar") { _, _ -> h.cancel() }
            .setOnCancelListener { h.cancel() }.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
