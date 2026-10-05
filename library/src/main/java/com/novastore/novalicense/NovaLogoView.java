package com.novastore.novalicense;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * El logo de NovaStore dibujado con Canvas (sin dependencias de recursos ni de AndroidX,
 * minSdk 21). Espejo visual del {@code NovaLogo} del SDK Dart: un cuadrado redondeado con
 * degradado verde → azul, la marca "N" en trazo blanco y el pequeño punto de la esquina
 * superior derecha.
 *
 * <p>Existe como View (y no como drawable en XML) para poder inyectar el color de marca desde
 * Java/Kotlin/C# sin recursos extra.
 */
public final class NovaLogoView extends View {

    private static final int BRAND_TOP_DEFAULT = 0xFF00C853;
    private static final int BRAND_BOTTOM_DEFAULT = 0xFF0091EA;

    private int topColor = BRAND_TOP_DEFAULT;
    private int bottomColor = BRAND_BOTTOM_DEFAULT;
    private float cornerRadius = 0f;
    private boolean shadowEnabled = false;

    public NovaLogoView(Context context) {
        super(context);
    }

    public NovaLogoView setBrandColors(int top, int bottom) {
        this.topColor = top;
        this.bottomColor = bottom;
        invalidate();
        return this;
    }

    /** Aplica un único color de marca (el de la config), o el azul por defecto si es {@code null}. */
    public NovaLogoView applyBrandColor(Integer brandColor) {
        if (brandColor != null) {
            setBrandColors(brandColor, brandColor);
        }
        return this;
    }

    public NovaLogoView setCornerRadiusPx(float radius) {
        this.cornerRadius = radius;
        invalidate();
        return this;
    }

    /** Sombra de la tarjeta (API 21+): recorta el contorno y eleva la vista. */
    public NovaLogoView setShadowEnabled(boolean enabled) {
        if (shadowEnabled == enabled) {
            return this;
        }
        shadowEnabled = enabled;
        if (enabled) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
            setClipToOutline(true);
            setElevation(10f * getResources().getDisplayMetrics().density);
        } else {
            setOutlineProvider(null);
            setClipToOutline(false);
            setElevation(0f);
        }
        invalidate();
        return this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0f || h <= 0f) {
            return;
        }

        float radius = cornerRadius > 0f ? cornerRadius : w * 0.24f;
        RectF bounds = new RectF(0f, 0f, w, h);

        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setShader(new LinearGradient(0f, 0f, w, h, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, radius, radius, background);

        // Destello superior izquierdo (como el SDK Dart).
        Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlight.setShader(new LinearGradient(
                0f, 0f, w * 0.62f, 0f,
                Color.argb(64, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, radius, radius, highlight);

        // Marca "N" en trazo blanco: dos patas verticales + diagonal.
        float p = w * 0.28f;
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(Color.WHITE);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(w * 0.15f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        Path path = new Path();
        path.moveTo(p, h - p);
        path.lineTo(p, p);
        path.lineTo(w - p, h - p);
        path.lineTo(w - p, p);
        canvas.drawPath(path, stroke);

        // Punto de la esquina superior derecha.
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(Color.WHITE);
        canvas.drawCircle(w * 0.85f, h * 0.17f, w * 0.07f, dot);
    }
}
