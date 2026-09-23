package com.novastore.novalicense

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Pantalla de licencia NovaStore (tema claro, estilo UI moderna).
 * Toda la UI es programática (sin XML), fluida y compatible con cualquier
 * tamaño de pantalla (columna con ancho máximo + ScrollView con viewport).
 */
object LicenseRequiredView {

    /** Color de acento por defecto (azul de marca). */
    const val DEFAULT_ACCENT = 0xFF0091EA.toInt()

    /** Colores del degradado de marca de NovaStore (verde → azul). */
    const val BRAND_TOP = 0xFF00C853.toInt()
    const val BRAND_BOTTOM = 0xFF0091EA.toInt()

    // Paleta clara.
    private const val BG_TOP = 0xFFF8FAFC.toInt()
    private const val BG_BOTTOM = 0xFFEDF2F7.toInt()
    private const val SURFACE = 0xFFFFFFFF.toInt()
    private const val CARD_STROKE = 0xFFE2E8F0.toInt()
    private const val TEXT_MAIN = 0xFF0F172A.toInt()
    private const val TEXT_SUB = 0xFF475569.toInt()
    private const val TEXT_DIM = 0xFF94A3B8.toInt()
    private const val CODE_GREEN = 0xFF059669.toInt()
    private const val ERROR_BG = 0xFFFEF2F2.toInt()
    private const val ERROR_STROKE = 0xFFFCA5A5.toInt()
    private const val ERROR_TEXT = 0xFFDC2626.toInt()

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
        val brandTop = brandColor ?: BRAND_TOP
        val brandBottom = brandColor?.let { accentTone(it) } ?: BRAND_BOTTOM

        val dm = context.resources.displayMetrics
        android.util.Log.d(
            "NOVALICENSE",
            "density=${dm.density} scaledDensity=${dm.scaledDensity} fontScale=${context.resources.configuration.fontScale} widthPx=${dm.widthPixels}",
        )

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val outer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(BG_TOP, BG_BOTTOM),
            )
            setPadding(context.dp(16f), context.dp(36f), context.dp(16f), context.dp(32f))
        }
        scroll.addView(outer)

        // Ancho máximo: centra en tablets/landscape y no desborda en móviles.
        val maxWidth = context.dp(420f).coerceAtMost(context.resources.displayMetrics.widthPixels)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        outer.addView(
            column,
            LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT),
        )

        // ----- Logo y Marca Superior -----
        val wordmark = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        wordmark.addView(
            NovaLogoView(context)
                .setCornerRadiusPx(context.dp(8f).toFloat())
                .apply { layoutParams = LinearLayout.LayoutParams(context.dp(26f), context.dp(26f)) },
        )
        wordmark.addView(
            TextView(context).apply {
                text = "NovaStore"
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.05f
                setTextColor(TEXT_MAIN)
                setPadding(context.dp(8f), 0, 0, 0)
            },
        )
        column.addView(wordmark)

        // ----- Header Hero (banner degradado de marca) -----
        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dp(20f), context.dp(20f), context.dp(20f), context.dp(20f))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(brandTop, brandBottom),
            ).apply { cornerRadius = context.dp(20f).toFloat() }
            elevation = context.dp(4f).toFloat()
        }
        column.addView(
            hero,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(20f)
            },
        )

        hero.addView(
            NovaLogoView(context)
                .setCornerRadiusPx(context.dp(14f).toFloat())
                .setShadowEnabled(true)
                .apply { layoutParams = LinearLayout.LayoutParams(context.dp(54f), context.dp(54f)) },
        )

        hero.addView(
            TextView(context).apply {
                text = if (offline) "SIN CONEXIÓN A INTERNET" else "LICENCIA REQUERIDA"
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setPadding(0, context.dp(12f), 0, 0)
            },
        )

        val subtitle = if (offline) {
            "Conéctate a una red para verificar tu licencia y comenzar a usar esta aplicación."
        } else {
            "Este dispositivo requiere una licencia activa para desbloquear esta aplicación."
        }
        hero.addView(
            TextView(context).apply {
                text = subtitle
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(0xE6FFFFFF.toInt())
                setLineSpacing(context.dp(2f).toFloat(), 1f)
                setPadding(0, context.dp(6f), 0, 0)
            },
        )

        // ----- Tarjeta de código de activación -----
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dp(16f), context.dp(16f), context.dp(16f), context.dp(16f))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(20f).toFloat()
                setColor(SURFACE)
                setStroke(context.dp(1f), CARD_STROKE)
            }
            elevation = context.dp(2f).toFloat()
        }
        column.addView(
            card,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(16f)
            },
        )

        card.addView(
            TextView(context).apply {
                text = "CÓDIGO DE ACTIVACIÓN DE TU DISPOSITIVO"
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.08f
                gravity = Gravity.CENTER
                setTextColor(TEXT_DIM)
            },
        )

        // Caja de código destacada.
        val codeBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(context.dp(12f), context.dp(10f), context.dp(12f), context.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(12f).toFloat()
                setColor(accentColor(accent, 0x0D))
                setStroke(context.dp(1.5f), accentColor(accent, 0x33))
            }
        }
        card.addView(
            codeBox,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(10f)
            },
        )

        val codeLabel = TextView(context).apply {
            text = deviceCode
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(CODE_GREEN)
            letterSpacing = 0.08f
            setTextIsSelectable(true)
            setPadding(context.dp(2f), 0, context.dp(2f), 0)
        }
        codeBox.addView(
            codeLabel,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
        codeLabel.setOnLongClickListener {
            copyCode(context, deviceCode)
            true
        }

        codeBox.addView(
            TextView(context).apply {
                text = "Mantén presionado para copiar"
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(TEXT_DIM)
                setPadding(0, context.dp(2f), 0, 0)
            },
        )

        // Botón secundario: copiar código.
        card.addView(
            outlineButton(context, "Copiar Código", accent, context.dp(40f)) {
                copyCode(context, deviceCode)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(12f)
            },
        )

        // ----- Pasos de instrucciones en tarjeta -----
        val stepsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // Padding interno generoso para despegar los círculos de los bordes.
            setPadding(context.dp(16f), context.dp(8f), context.dp(16f), context.dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(16f).toFloat()
                setColor(SURFACE)
                setStroke(context.dp(1f), CARD_STROKE)
            }
            elevation = context.dp(2f).toFloat()
        }
        column.addView(
            stepsContainer,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(16f)
            },
        )

        if (offline) {
            stepsContainer.addView(
                infoRow(context, accent, "Verifica tu conexión a internet y pulsa Reintentar. Si el problema persiste, adquiere la app desde NovaStore."),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        } else {
            stepsContainer.addView(
                stepRow(context, "1", "Abre la ficha oficial de esta app en NovaStore.", brandTop, brandBottom),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            stepsContainer.addView(
                stepRow(context, "2", "Toca en «Activar Licencia» e ingresa tu código de activación.", brandTop, brandBottom),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
            stepsContainer.addView(
                stepRow(context, "3", "Regresa a esta app y presiona «Comprobar Licencia».", brandTop, brandBottom),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }

        // ----- Tarjeta de error (si existe) -----
        if (!errorMessage.isNullOrEmpty()) {
            val errorBox = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(context.dp(12f), context.dp(10f), context.dp(12f), context.dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = context.dp(12f).toFloat()
                    setColor(ERROR_BG)
                    setStroke(context.dp(1f), ERROR_STROKE)
                }
            }
            errorBox.addView(
                ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_dialog_alert)
                    colorFilter = android.graphics.PorterDuffColorFilter(ERROR_TEXT, android.graphics.PorterDuff.Mode.SRC_IN)
                    layoutParams = LinearLayout.LayoutParams(context.dp(18f), context.dp(18f))
                },
            )
            errorBox.addView(
                TextView(context).apply {
                    text = errorMessage
                    textSize = 11f
                    setTextColor(ERROR_TEXT)
                    setLineSpacing(context.dp(1.5f).toFloat(), 1f)
                    setPadding(context.dp(8f), 0, 0, 0)
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
            column.addView(
                errorBox,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = context.dp(14f)
                },
            )
        }

        // ----- Botón principal de acción -----
        val primaryLabel = if (offline) "Reintentar Conexión" else "Comprobar Licencia"
        val primaryAction = if (offline) onRetry else onRefresh
        column.addView(
            gradientButton(context, primaryLabel, brandTop, brandBottom, context.dp(48f)) { primaryAction?.invoke() },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(18f)
            },
        )

        // Enlace web NovaStore.
        column.addView(
            bodyText(context, "Abrir NovaStore en el navegador", accent) {
                if (storeUrl.isNotEmpty()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dp(12f)
            },
        )

        return scroll
    }

    private fun copyCode(context: Context, deviceCode: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("device_code", LicenseChecker.normalizeDeviceCode(deviceCode)))
        Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun stepRow(context: Context, number: String, text: String, topColor: Int, bottomColor: Int): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dp(10f), 0, context.dp(10f))
        }
        // Círculo de tamaño fijo: el fondo vive en un FrameLayout de dimensión
        // exacta, así el badge nunca se recorta (un TextView con background OVAL
        // dibuja el círculo al alto del texto y se ve cortado).
        val badgeContainer = android.widget.FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(context.dp(28f), context.dp(28f))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(topColor, bottomColor),
            ).apply { shape = GradientDrawable.OVAL }
        }
        val badgeText = TextView(context).apply {
            this.text = number
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            includeFontPadding = false
        }
        badgeContainer.addView(
            badgeText,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        row.addView(badgeContainer)
        row.addView(
            TextView(context).apply {
                this.text = text
                textSize = 12f
                setTextColor(TEXT_SUB)
                setLineSpacing(context.dp(2f).toFloat(), 1f)
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = context.dp(12f)
            },
        )
        return row
    }

    private fun infoRow(context: Context, accent: Int, text: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(12f), context.dp(10f), context.dp(12f), context.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(12f).toFloat()
                setColor(accentColor(accent, 0x12))
                setStroke(context.dp(1f), accentColor(accent, 0x40))
            }
            addView(
                ImageView(context).apply {
                    setImageResource(android.R.drawable.ic_dialog_info)
                    colorFilter = android.graphics.PorterDuffColorFilter(accent, android.graphics.PorterDuff.Mode.SRC_IN)
                    layoutParams = LinearLayout.LayoutParams(context.dp(18f), context.dp(18f))
                },
            )
            addView(
                TextView(context).apply {
                    this.text = text
                    textSize = 11.5f
                    setTextColor(TEXT_SUB)
                    setLineSpacing(context.dp(1.5f).toFloat(), 1f)
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = context.dp(10f)
                },
            )
        }
    }

    private fun gradientButton(
        context: Context,
        label: String,
        top: Int,
        bottom: Int,
        height: Int,
        onClick: () -> Unit,
    ): Button =
        Button(context).apply {
            text = label
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.WHITE)
            stateListAnimator = null
            minHeight = 0
            minWidth = 0
            setMinimumHeight(0)
            setMinimumWidth(0)

            val shape = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(top, bottom),
            ).apply { cornerRadius = context.dp(12f).toFloat() }

            val mask = GradientDrawable().apply {
                cornerRadius = context.dp(12f).toFloat()
                setColor(Color.WHITE)
            }

            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF.toInt()), shape, mask)
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
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(accent)
            stateListAnimator = null
            minHeight = 0
            minWidth = 0
            setMinimumHeight(0)
            setMinimumWidth(0)

            val shape = GradientDrawable().apply {
                cornerRadius = context.dp(10f).toFloat()
                setStroke(context.dp(1.5f), accentColor(accent, 0x80))
                setColor(SURFACE)
            }

            val mask = GradientDrawable().apply {
                cornerRadius = context.dp(10f).toFloat()
                setColor(Color.WHITE)
            }

            background = RippleDrawable(ColorStateList.valueOf(accentColor(accent, 0x22)), shape, mask)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }

    private fun bodyText(
        context: Context,
        label: String,
        accent: Int,
        onClick: () -> Unit,
    ): TextView =
        TextView(context).apply {
            text = label
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(accent)
            setPadding(context.dp(8f), context.dp(6f), context.dp(8f), context.dp(6f))
            setOnClickListener { onClick() }
        }

    private fun accentTone(color: Int): Int =
        Color.rgb(
            (Color.red(color) * 0.72f).toInt().coerceAtMost(255),
            (Color.green(color) * 0.72f).toInt().coerceAtMost(255),
            (Color.blue(color) * 0.72f).toInt().coerceAtMost(255),
        )

    private fun accentColor(accent: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent))

    private fun Context.dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()
}