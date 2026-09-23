package com.novastore.novalicense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewOutlineProvider

/**
 * El logo de NovaStore dibujado con Canvas (sin dependencias de recursos ni de
 * AndroidX, minSdk 21). Espejo visual del `NovaLogo` del SDK Dart: un cuadrado
 * redondeado con degradado verde → azul, la marca "N" en trazo blanco y el
 * pequeño punto de la esquina superior derecha.
 */
class NovaLogoView(context: Context) : View(context) {

    private var topColor = LICENCE_ACCESS_BRAND_TOP
    private var bottomColor = LICENCE_ACCESS_BRAND_BOTTOM
    private var cornerRadius = 0f
    private var shadowEnabled = false

    fun setBrandColors(top: Int, bottom: Int): NovaLogoView {
        topColor = top
        bottomColor = bottom
        invalidate()
        return this
    }

    fun setCornerRadiusPx(radius: Float): NovaLogoView {
        cornerRadius = radius
        invalidate()
        return this
    }

    fun setShadowEnabled(enabled: Boolean): NovaLogoView {
        if (shadowEnabled == enabled) return this
        shadowEnabled = enabled
        if (enabled) {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
            clipToOutline = true
            elevation = 10f * resources.displayMetrics.density
        } else {
            outlineProvider = null
            clipToOutline = false
            elevation = 0f
        }
        invalidate()
        return this
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val r = if (cornerRadius > 0f) cornerRadius else w * 0.24f
        val bounds = RectF(0f, 0f, w, h)

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, w, h, topColor, bottomColor, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(bounds, r, r, background)

        // Destello superior izquierdo (como el SDK Dart).
        val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w * 0.62f, 0f,
                Color.argb(64, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bounds, r, r, highlight)

        // Marca "N" en trazo blanco: dos patas verticales + diagonal.
        val p = w * 0.28f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = w * 0.15f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(p, h - p)
            lineTo(p, p)
            lineTo(w - p, h - p)
            lineTo(w - p, p)
        }
        canvas.drawPath(path, stroke)

        // Punto de la esquina superior derecha.
        canvas.drawCircle(
            w * 0.85f,
            h * 0.17f,
            w * 0.07f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE },
        )
    }

    private companion object {
        const val LICENCE_ACCESS_BRAND_TOP = 0xFF00C853.toInt()
        const val LICENCE_ACCESS_BRAND_BOTTOM = 0xFF0091EA.toInt()
    }
}