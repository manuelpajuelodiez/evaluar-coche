package com.cocheonline.analizador

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("evaluar", Context.MODE_PRIVATE)
    fun server(c: Context): String = sp(c).getString("server", "") ?: ""
    fun setServer(c: Context, v: String) = sp(c).edit().putString("server", v.trim()).apply()
    /** Palabras que debe contener el nombre del paquete de la app donde aparece la burbuja. */
    fun keywords(c: Context): String = sp(c).getString("kw", "coches,ocasion,autocasion,flexicar,clicars,autohero,motor") ?: ""
    fun setKeywords(c: Context, v: String) = sp(c).edit().putString("kw", v.trim()).apply()
    fun anyApp(c: Context): Boolean = sp(c).getBoolean("any", false)
    fun setAnyApp(c: Context, v: Boolean) = sp(c).edit().putBoolean("any", v).apply()

    /** URL base del informe, siempre terminada en / y apuntando a la carpeta de la app web. */
    fun baseUrl(c: Context): String {
        var s = server(c)
        if (s.isEmpty()) return ""
        if (!s.startsWith("http")) s = "https://$s"
        s = s.substringBefore("index.html").substringBefore("?")
        return if (s.endsWith("/")) s else "$s/"
    }
}
