package com.cocheonline.analizador

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast

/**
 * Servicio de accesibilidad: dibuja un botón flotante cuando estás en una app de anuncios de coches
 * y, al pulsarlo, lee el texto visible de la pantalla y lo manda al informe.
 */
class BubbleService : AccessibilityService() {
    private var wm: WindowManager? = null
    private var bubble: View? = null
    private var currentPkg = ""
    /** Líneas vistas en esta ficha (así también cuenta lo que ya has desplazado fuera de pantalla). */
    private val seen = LinkedHashSet<String>()

    override fun onServiceConnected() { wm = getSystemService(WINDOW_SERVICE) as WindowManager }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName || pkg.startsWith("com.android.systemui")) return
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && pkg != currentPkg) { seen.clear() }
        if (pkg != currentPkg) currentPkg = pkg
        if (matches(pkg)) { showBubble(); if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) collect() } else hideBubble()
    }

    override fun onInterrupt() {}
    override fun onDestroy() { hideBubble(); super.onDestroy() }

    private fun matches(pkg: String): Boolean {
        if (Prefs.anyApp(this)) return !pkg.contains("launcher") && !pkg.contains("inputmethod")
        return Prefs.keywords(this).split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.any { pkg.lowercase().contains(it) }
    }

    private fun collect(): Int {
        val root = rootInActiveWindow ?: return seen.size
        if (root.packageName?.toString() != currentPkg) return seen.size
        val acc = ArrayList<String>()
        walk(root, acc, 0)
        for (l in acc) if (seen.size < 400) seen.add(l)
        return seen.size
    }

    private fun walk(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (n == null || depth > 40) return
        if (n.isVisibleToUser) {
            val t = (n.text ?: n.contentDescription)?.toString()?.trim()
            if (!t.isNullOrEmpty() && t.length < 400 && !n.isPassword) out.add(t)
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), out, depth + 1)
    }

    private fun analyze() {
        collect()
        if (seen.isEmpty()) { Toast.makeText(this, "No veo texto en pantalla. Abre la ficha del coche.", Toast.LENGTH_LONG).show(); return }
        val text = seen.joinToString("\n")
        seen.clear()
        startActivity(Intent(this, WebActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("screenText", text))
    }

    private fun showBubble() {
        if (bubble != null) return
        val m = wm ?: return
        val d = resources.displayMetrics.density
        val size = (58 * d).toInt()
        val v = TextView(this).apply {
            text = "🕵️"; textSize = 26f; gravity = Gravity.CENTER; setBackgroundResource(R.drawable.bubble_bg); elevation = 8f
        }
        val p = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.TOP or Gravity.START; p.x = resources.displayMetrics.widthPixels - size - 12; p.y = (resources.displayMetrics.heightPixels * 0.55).toInt()
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sx = ev.rawX; sy = ev.rawY; ox = p.x; oy = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - sx; val dy = ev.rawY - sy
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) moved = true
                    if (moved) { p.x = ox + dx.toInt(); p.y = oy + dy.toInt(); try { m.updateViewLayout(v, p) } catch (_: Exception) {} }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) analyze(); true }
                else -> false
            }
        }
        try { m.addView(v, p); bubble = v } catch (_: Exception) {}
    }

    private fun hideBubble() {
        val v = bubble ?: return
        try { wm?.removeView(v) } catch (_: Exception) {}
        bubble = null
    }
}
