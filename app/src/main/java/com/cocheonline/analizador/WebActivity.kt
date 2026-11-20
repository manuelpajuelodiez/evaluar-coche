package com.cocheonline.analizador

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
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
    private var showingError = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        web = WebView(this)
        setContentView(web)
        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = true }
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView, url: String, f: Bitmap?) { showingError = false }
            override fun onPageFinished(v: WebView, url: String) { if (!showingError) deliver() }
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val base = Prefs.baseUrl(this@WebActivity)
                if (r.url.toString().startsWith(base)) return false
                startActivity(Intent(Intent.ACTION_VIEW, r.url)); return true
            }
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) showError("No se pudo abrir el servidor.", e.description.toString(), r.url.toString())
            }
            override fun onReceivedHttpError(v: WebView, r: WebResourceRequest, e: WebResourceResponse) {
                if (r.isForMainFrame && e.statusCode >= 400 && e.statusCode != 401) showError("El servidor respondió con error " + e.statusCode, e.reasonPhrase ?: "", r.url.toString())
            }
            override fun onReceivedSslError(v: WebView, h: SslErrorHandler, e: SslError) {
                h.cancel()
                showError("El certificado HTTPS no es válido.", "Usa una dirección https con certificado de confianza (por ejemplo un túnel de Cloudflare).", e.url ?: "")
            }
            override fun onReceivedHttpAuthRequest(v: WebView, h: HttpAuthHandler, host: String, realm: String?) {
                val saved = web.getHttpAuthUsernamePassword(host, realm ?: "")
                if (saved != null && saved.size == 2 && saved[0] != null && saved[1] != null && h.useHttpAuthUsernamePassword()) { h.proceed(saved[0]!!, saved[1]!!); return }
                askLogin(h, host, realm)
            }
        }
        handle(intent)
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun showError(title: String, detail: String, url: String) {
        showingError = true
        val html = "<html><body style='font-family:sans-serif;padding:24px'><h2>" + esc(title) + "</h2><p>" + esc(detail) +
            "</p><p style='color:#666;word-break:break-all'>" + esc(url) + "</p>" +
            "<p>Comprueba la dirección en la app (debe ser accesible desde el móvil, no localhost), que el servidor esté encendido y que tengas conexión.</p></body></html>"
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    override fun onNewIntent(i: Intent) { super.onNewIntent(i); setIntent(i); handle(i) }

    private fun handle(i: Intent) {
        val base = Prefs.baseUrl(this)
        if (base.isEmpty()) { Toast.makeText(this, "Configura primero la dirección del servidor", Toast.LENGTH_LONG).show(); startActivity(Intent(this, MainActivity::class.java)); finish(); return }
        val screen = i.getStringExtra("screenText")
        val shared = if (i.action == Intent.ACTION_SEND) i.getStringExtra(Intent.EXTRA_TEXT) else null
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
        AlertDialog.Builder(this).setTitle("Acceso a " + host).setView(box)
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
