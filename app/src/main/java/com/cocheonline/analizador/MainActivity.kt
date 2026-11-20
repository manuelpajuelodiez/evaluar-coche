package com.cocheonline.analizador

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val root = ScrollView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(28), dp(20), dp(28)) }
        root.addView(col)
        fun tv(t: String, size: Float = 15f, bold: Boolean = false, top: Int = 12) = TextView(this).apply {
            text = t; textSize = size; if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(top), 0, 0)
        }
        col.addView(tv("🕵️ Evaluar coche", 26f, true, 0))
        col.addView(tv("Un botón flotante sobre la app del portal de coches que analiza el anuncio que estás viendo."))

        col.addView(tv("1. Dirección de tu servidor", 17f, true, 22))
        col.addView(tv("La carpeta donde tienes publicada la aplicación (debe ser accesible desde el móvil, mejor con https). Ej.: https://midominio.es/portal/", 13f, false, 4))
        val server = EditText(this).apply { setText(Prefs.server(this@MainActivity)); hint = "https://…/portal/"; inputType = InputType.TYPE_TEXT_VARIATION_URI; setSingleLine() }
        col.addView(server)

        col.addView(tv("2. Apps donde aparece el botón", 17f, true, 22))
        col.addView(tv("Se muestra cuando el nombre de la app contiene alguna de estas palabras (separadas por comas). Si no aparece en tu app, activa «en cualquier app» y avísame del nombre.", 13f, false, 4))
        val kw = EditText(this).apply { setText(Prefs.keywords(this@MainActivity)); setSingleLine() }
        col.addView(kw)
        val any = CheckBox(this).apply { text = "Mostrar en cualquier app (pruebas)"; isChecked = Prefs.anyApp(this@MainActivity) }
        col.addView(any)

        fun save() {
            Prefs.setServer(this, server.text.toString()); Prefs.setKeywords(this, kw.text.toString()); Prefs.setAnyApp(this, any.isChecked)
        }
        col.addView(tv("3. Activar el botón flotante", 17f, true, 22))
        col.addView(tv("Ajustes → Accesibilidad → Apps instaladas → «Evaluar coche» → Activar. Android avisará de que el servicio puede leer la pantalla: solo lee el texto del anuncio al pulsar el botón y lo envía a TU servidor.", 13f, false, 4))
        col.addView(Button(this).apply {
            text = "Abrir ajustes de accesibilidad"
            setOnClickListener { save(); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        col.addView(tv("En Android 13+ puede que el interruptor salga gris: Ajustes → Apps → Evaluar coche → ⋮ → «Permitir ajustes restringidos», y vuelve a intentarlo.", 12f, false, 2).apply { setTextColor(Color.GRAY) })

        col.addView(Button(this).apply {
            text = "Guardar y abrir el evaluador"
            setOnClickListener { save(); if (Prefs.baseUrl(this@MainActivity).isEmpty()) toast("Escribe la dirección del servidor") else startActivity(Intent(this@MainActivity, WebActivity::class.java)) }
        })
        col.addView(tv("También puedes usar «Compartir» → «Evaluar coche» desde cualquier app con el enlace del anuncio.", 13f, false, 14).apply { gravity = Gravity.START })
        setContentView(root)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
