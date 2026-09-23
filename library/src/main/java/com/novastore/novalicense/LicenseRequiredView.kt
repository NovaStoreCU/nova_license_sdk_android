package com.novastore.novalicense

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

/**
 * Pantalla completa mostrada cuando el dispositivo no tiene licencia.
 * Espejo de `LicenseRequiredScreen` del SDK Dart (mismo texto y acciones).
 */
object LicenseRequiredView {

    /** Color de acento por defecto (violeta tipo tienda). */
    const val DEFAULT_ACCENT = 0xFF6750A4.toInt()

    fun build(
        context: Context,
        deviceCode: String,
        storeUrl: String,
        offline: Boolean,
        errorMessage: String?,
        onRetry: (() -> Unit)?,
        onRefresh: (() -> Unit)?,
        brandColor: Int?,
    ): View {
        val accent = brandColor ?: DEFAULT_ACCENT
        val density = context.resources.displayMetrics
        fun dp(value: Float): Int = (value * density.density).toInt()
        fun sp(value: Float): Int =
            (value * density.density * context.resources.configuration.fontScale).toInt()

        val scroll = ScrollView(context).apply { isFillViewport = true }
        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        }
        scroll.addView(outer)

        val maxWidth = dp(440f).coerceAtMost(density.widthPixels)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        outer.addView(
            column,
            LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        column.addView(lockIcon(context, accent, dp(56f)))

        column.addView(
            TextView(context).apply {
                text = "Requiere licencia"
                textSize = sp(24f).toFloat()
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(0xFF1C1B1F.toInt())
                setPadding(0, dp(16f), 0, 0)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        val subtitle = if (offline) {
            "Este dispositivo todavía no tiene una licencia válida para esta app."
        } else {
            "Este dispositivo todavía no tiene una licencia para esta app."
        }
        column.addView(
            TextView(context).apply {
                text = subtitle
                textSize = sp(14f).toFloat()
                gravity = Gravity.CENTER
                setTextColor(0xFF49454F.toInt())
                setPadding(dp(8f), dp(8f), dp(8f), 0)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            background = GradientDrawable().apply {
                cornerRadius = dp(16f).toFloat()
                setColor(0xFFFFFFFF.toInt())
            }
            elevation = dp(2f).toFloat()
        }
        column.addView(
            card,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24f)
            },
        )

        card.addView(
            TextView(context).apply {
                text = "Tu código de dispositivo"
                textSize = sp(13f).toFloat()
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFF79747E.toInt())
            },
        )

        card.addView(
            TextView(context).apply {
                text = deviceCode
                textSize = sp(20f).toFloat()
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(accent)
                letterSpacing = 0.06f
                setTextIsSelectable(true)
                setPadding(dp(4f), dp(12f), dp(4f), dp(12f))
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        card.addView(outlineButton(context, "Copiar código", accent, dp(56f)) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("device_code", LicenseChecker.normalizeDeviceCode(deviceCode)))
            Toast.makeText(context, "Código copiado al portapapeles.", Toast.LENGTH_SHORT).show()
        })

        val instructions = if (offline) {
            "Sin conexión. Conéctate a internet y reintenta, o compra la app desde NovaStore con este código."
        } else {
            "1. Abre la app en NovaStore.\n" +
                "2. Toca Comprar y pega este código como \"Código de activación\".\n" +
                "3. Al confirmar, esta app se desbloquea en este dispositivo."
        }
        column.addView(
            TextView(context).apply {
                text = instructions
                textSize = sp(12f).toFloat()
                setTextColor(0xFF49454F.toInt())
                setPadding(0, dp(16f), 0, 0)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        val primaryLabel = if (offline) "Reintentar comprobación" else "Ya la compré, comprobar"
        val primaryAction = if (offline) onRetry else onRefresh
        column.addView(
            filledButton(context, primaryLabel, accent, dp(48f)) { primaryAction?.invoke() },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(24f)
            },
        )

        column.addView(
            textButton(context, "Abrir NovaStore en el navegador", accent) {
                if (storeUrl.isEmpty()) {
                    return@textButton
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
                    if (intent.resolveActivity(context.packageManager) == null) {
                        Toast.makeText(context, "No se pudo abrir: $storeUrl", Toast.LENGTH_SHORT).show()
                    } else {
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "No se pudo abrir: $storeUrl", Toast.LENGTH_SHORT).show()
                }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        if (!errorMessage.isNullOrEmpty()) {
            column.addView(
                TextView(context).apply {
                    text = errorMessage
                    textSize = sp(12f).toFloat()
                    gravity = Gravity.CENTER
                    setTextColor(0xFFB3261E.toInt())
                    setPadding(0, dp(8f), 0, 0)
                },
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }

        return scroll
    }

    private fun lockIcon(context: Context, accent: Int, size: Int): ImageView =
        ImageView(context).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            colorFilter = android.graphics.PorterDuffColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

    private fun filledButton(
        context: Context,
        label: String,
        accent: Int,
        height: Int,
        onClick: () -> Unit,
    ): Button =
        Button(context).apply {
            text = label
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            stateListAnimator = null
            background = GradientDrawable().apply {
                cornerRadius = (height / 2).toFloat()
                setColor(accent)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }

    private fun outlineButton(
        context: Context,
        label: String,
        accent: Int,
        height: Int,
        onClick: () -> Unit,
    ): Button =
        Button(context).apply {
            text = label
            textSize = 14f
            setTextColor(accent)
            stateListAnimator = null
            background = GradientDrawable().apply {
                cornerRadius = (height / 2).toFloat()
                setStroke(dp(context, 1f), accent)
                setColor(0)
            }
            setOnClickListener { onClick() }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height)
            lp.topMargin = dp(context, 12f)
            layoutParams = lp
        }

    private fun textButton(
        context: Context,
        label: String,
        accent: Int,
        onClick: () -> Unit,
    ): Button =
        Button(context).apply {
            text = label
            textSize = 13f
            setTextColor(accent)
            stateListAnimator = null
            background = null
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

    private fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt()
}