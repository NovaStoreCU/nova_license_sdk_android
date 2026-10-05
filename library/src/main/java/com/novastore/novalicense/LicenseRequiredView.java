package com.novastore.novalicense;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Pantalla de licencia NovaStore (tema claro, estilo UI moderna).
 *
 * <p>Toda la UI es programática (sin XML) y compatible con cualquier tamaño de pantalla: columna
 * con ancho máximo + {@link ScrollView} con viewport.
 */
public final class LicenseRequiredView {

    /** Color de acento por defecto (azul de marca). */
    public static final int DEFAULT_ACCENT = 0xFF0091EA;

    /** Colores del degradado de marca de NovaStore (verde → azul). */
    public static final int BRAND_TOP = 0xFF00C853;
    public static final int BRAND_BOTTOM = 0xFF0091EA;

    // Paleta clara.
    private static final int BG_TOP = 0xFFF8FAFC;
    private static final int BG_BOTTOM = 0xFFEDF2F7;
    private static final int SURFACE = 0xFFFFFFFF;
    private static final int CARD_STROKE = 0xFFE2E8F0;
    private static final int TEXT_MAIN = 0xFF0F172A;
    private static final int TEXT_SUB = 0xFF475569;
    private static final int TEXT_DIM = 0xFF94A3B8;
    private static final int CODE_GREEN = 0xFF059669;
    private static final int ERROR_BG = 0xFFFEF2F2;
    private static final int ERROR_STROKE = 0xFFFCA5A5;
    private static final int ERROR_TEXT = 0xFFDC2626;

    private LicenseRequiredView() {
    }

    /**
     * Construye la pantalla de licencia.
     *
     * @param deviceCode   código de activación (visible y copiable).
     * @param storeUrl     URL de la ficha en NovaStore; vacío = no muestra el enlace.
     * @param offline      true si el fallo fue de red (cambia textos y el botón principal).
     * @param errorMessage detalle del error, o {@code null}.
     * @param onRetry      callback del botón principal en modo offline (puede ser {@code null}).
     * @param onRefresh    callback del botón principal con licencia inválida (puede ser {@code null}).
     * @param brandColor   color de marca ARGB, o {@code null} para el azul por defecto.
     */
    public static View build(
            Context context,
            final String deviceCode,
            final String storeUrl,
            final boolean offline,
            String errorMessage,
            Runnable onRetry,
            Runnable onRefresh,
            Integer brandColor) {
        final int accent = brandColor != null ? brandColor : DEFAULT_ACCENT;
        final int brandTop = brandColor != null ? brandColor : BRAND_TOP;
        final int brandBottom = brandColor != null ? accentTone(brandColor) : BRAND_BOTTOM;
        final Runnable primaryAction = offline ? onRetry : onRefresh;
        final Context ctx = context;

        android.util.Log.d(
                "NOVALICENSE",
                "density=" + context.getResources().getDisplayMetrics().density
                        + " scaledDensity=" + context.getResources().getDisplayMetrics().scaledDensity
                        + " fontScale=" + context.getResources().getConfiguration().fontScale
                        + " widthPx=" + context.getResources().getDisplayMetrics().widthPixels);

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER_HORIZONTAL);
        outer.setBackground(gradient(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{BG_TOP, BG_BOTTOM}, 0, 0, 0));
        outer.setPadding(dp(context, 16f), dp(context, 36f), dp(context, 16f), dp(context, 32f));
        scroll.addView(outer);

        // Ancho máximo: centra en tablets/landscape y no desborda en móviles.
        int maxWidth = Math.min(dp(context, 420f), context.getResources().getDisplayMetrics().widthPixels);
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        outer.addView(column, new LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT));

        // ----- Logo y marca superior -----
        LinearLayout wordmark = new LinearLayout(context);
        wordmark.setOrientation(LinearLayout.HORIZONTAL);
        wordmark.setGravity(Gravity.CENTER);
        NovaLogoView wordmarkLogo = new NovaLogoView(context);
        wordmarkLogo.setBrandColors(brandTop, brandBottom);
        wordmarkLogo.setCornerRadiusPx(dp(context, 8f));
        wordmarkLogo.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 26f), dp(context, 26f)));
        wordmark.addView(wordmarkLogo);

        TextView wordmarkText = new TextView(context);
        wordmarkText.setText("NovaStore");
        wordmarkText.setTextSize(16f);
        wordmarkText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        wordmarkText.setLetterSpacing(0.05f);
        wordmarkText.setTextColor(TEXT_MAIN);
        wordmarkText.setPadding(dp(context, 8f), 0, 0, 0);
        wordmark.addView(wordmarkText);
        column.addView(wordmark);

        // ----- Header hero (banner degradado de marca) -----
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(dp(context, 20f), dp(context, 20f), dp(context, 20f), dp(context, 20f));
        hero.setBackground(gradient(
                GradientDrawable.Orientation.TL_BR, new int[]{brandTop, brandBottom}, dp(context, 20f), 0, 0));
        hero.setElevation(dp(context, 4f));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        heroParams.topMargin = dp(context, 20f);
        column.addView(hero, heroParams);

        NovaLogoView heroLogo = new NovaLogoView(context);
        heroLogo.setBrandColors(brandTop, brandBottom);
        heroLogo.setCornerRadiusPx(dp(context, 14f));
        heroLogo.setShadowEnabled(true);
        heroLogo.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 54f), dp(context, 54f)));
        hero.addView(heroLogo);

        TextView heroLabel = new TextView(context);
        heroLabel.setText(offline ? "SIN CONEXIÓN A INTERNET" : "LICENCIA REQUERIDA");
        heroLabel.setTextSize(12f);
        heroLabel.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        heroLabel.setLetterSpacing(0.12f);
        heroLabel.setGravity(Gravity.CENTER);
        heroLabel.setTextColor(Color.WHITE);
        heroLabel.setPadding(0, dp(context, 12f), 0, 0);
        hero.addView(heroLabel);

        TextView heroSubtitle = new TextView(context);
        heroSubtitle.setText(offline
                ? "Conéctate a una red para verificar tu licencia y comenzar a usar esta aplicación."
                : "Este dispositivo requiere una licencia activa para desbloquear esta aplicación.");
        heroSubtitle.setTextSize(12f);
        heroSubtitle.setGravity(Gravity.CENTER);
        heroSubtitle.setTextColor(0xE6FFFFFF);
        heroSubtitle.setLineSpacing(dp(context, 2f), 1f);
        heroSubtitle.setPadding(0, dp(context, 6f), 0, 0);
        hero.addView(heroSubtitle);

        // ----- Tarjeta de código de activación -----
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(context, 16f), dp(context, 16f), dp(context, 16f), dp(context, 16f));
        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setCornerRadius(dp(context, 20f));
        cardBackground.setColor(SURFACE);
        cardBackground.setStroke(dp(context, 1f), CARD_STROKE);
        card.setBackground(cardBackground);
        card.setElevation(dp(context, 2f));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(context, 16f);
        column.addView(card, cardParams);

        TextView codeTitle = new TextView(context);
        codeTitle.setText("CÓDIGO DE ACTIVACIÓN DE TU DISPOSITIVO");
        codeTitle.setTextSize(10f);
        codeTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        codeTitle.setLetterSpacing(0.08f);
        codeTitle.setGravity(Gravity.CENTER);
        codeTitle.setTextColor(TEXT_DIM);
        card.addView(codeTitle);

        // Caja de código destacada.
        LinearLayout codeBox = new LinearLayout(context);
        codeBox.setOrientation(LinearLayout.VERTICAL);
        codeBox.setGravity(Gravity.CENTER_HORIZONTAL);
        codeBox.setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f));
        GradientDrawable codeBackground = new GradientDrawable();
        codeBackground.setCornerRadius(dp(context, 12f));
        codeBackground.setColor(accentColor(accent, 0x0D));
        codeBackground.setStroke(Math.round(dp(context, 1.5f)), accentColor(accent, 0x33));
        codeBox.setBackground(codeBackground);
        LinearLayout.LayoutParams codeBoxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        codeBoxParams.topMargin = dp(context, 10f);
        card.addView(codeBox, codeBoxParams);

        TextView codeLabel = new TextView(context);
        codeLabel.setText(deviceCode);
        codeLabel.setTextSize(18f);
        codeLabel.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        codeLabel.setGravity(Gravity.CENTER);
        codeLabel.setTextColor(CODE_GREEN);
        codeLabel.setLetterSpacing(0.08f);
        codeLabel.setTextIsSelectable(true);
        codeLabel.setPadding(dp(context, 2f), 0, dp(context, 2f), 0);
        codeBox.addView(codeLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        codeLabel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                copyCode(ctx, deviceCode);
                return true;
            }
        });

        TextView codeHint = new TextView(context);
        codeHint.setText("Mantén presionado para copiar");
        codeHint.setTextSize(10f);
        codeHint.setGravity(Gravity.CENTER);
        codeHint.setTextColor(TEXT_DIM);
        codeHint.setPadding(0, dp(context, 2f), 0, 0);
        codeBox.addView(codeHint);

        // Botón secundario: copiar código.
        View copyButton = outlineButton(context, "Copiar Código", accent, dp(context, 40f), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyCode(ctx, deviceCode);
            }
        });
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        copyParams.topMargin = dp(context, 12f);
        card.addView(copyButton, copyParams);

        // ----- Pasos de instrucciones en tarjeta -----
        LinearLayout stepsContainer = new LinearLayout(context);
        stepsContainer.setOrientation(LinearLayout.VERTICAL);
        // Padding interno generoso para despegar los círculos de los bordes.
        stepsContainer.setPadding(dp(context, 16f), dp(context, 8f), dp(context, 16f), dp(context, 8f));
        GradientDrawable stepsBackground = new GradientDrawable();
        stepsBackground.setCornerRadius(dp(context, 16f));
        stepsBackground.setColor(SURFACE);
        stepsBackground.setStroke(dp(context, 1f), CARD_STROKE);
        stepsContainer.setBackground(stepsBackground);
        stepsContainer.setElevation(dp(context, 2f));
        LinearLayout.LayoutParams stepsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        stepsParams.topMargin = dp(context, 16f);
        column.addView(stepsContainer, stepsParams);

        LinearLayout.LayoutParams fillParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        if (offline) {
            stepsContainer.addView(infoRow(context, accent,
                    "Verifica tu conexión a internet y pulsa Reintentar. Si el problema persiste, "
                            + "adquiere la app desde NovaStore."), fillParams);
        } else {
            stepsContainer.addView(
                    stepRow(context, "1", "Abre la ficha oficial de esta app en NovaStore.", brandTop, brandBottom),
                    fillParams);
            stepsContainer.addView(
                    stepRow(context, "2", "Toca en «Activar Licencia» e ingresa tu código de activación.",
                            brandTop, brandBottom),
                    fillParams);
            stepsContainer.addView(
                    stepRow(context, "3", "Regresa a esta app y presiona «Comprobar Licencia».",
                            brandTop, brandBottom),
                    fillParams);
        }

        // ----- Tarjeta de error (si existe) -----
        if (errorMessage != null && !errorMessage.isEmpty()) {
            LinearLayout errorBox = new LinearLayout(context);
            errorBox.setOrientation(LinearLayout.HORIZONTAL);
            errorBox.setGravity(Gravity.CENTER_VERTICAL);
            errorBox.setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f));
            GradientDrawable errorBackground = new GradientDrawable();
            errorBackground.setCornerRadius(dp(context, 12f));
            errorBackground.setColor(ERROR_BG);
            errorBackground.setStroke(dp(context, 1f), ERROR_STROKE);
            errorBox.setBackground(errorBackground);

            ImageView errorIcon = new ImageView(context);
            errorIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            errorIcon.setColorFilter(new PorterDuffColorFilter(ERROR_TEXT, PorterDuff.Mode.SRC_IN));
            errorIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 18f), dp(context, 18f)));
            errorBox.addView(errorIcon);

            TextView errorText = new TextView(context);
            errorText.setText(errorMessage);
            errorText.setTextSize(11f);
            errorText.setTextColor(ERROR_TEXT);
            errorText.setLineSpacing(dp(context, 1.5f), 1f);
            errorText.setPadding(dp(context, 8f), 0, 0, 0);
            errorBox.addView(errorText, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            errorParams.topMargin = dp(context, 14f);
            column.addView(errorBox, errorParams);
        }

        // ----- Botón principal de acción -----
        String primaryLabel = offline ? "Reintentar Conexión" : "Comprobar Licencia";
        View primary = gradientButton(context, primaryLabel, brandTop, brandBottom, dp(context, 48f),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (primaryAction != null) {
                            primaryAction.run();
                        }
                    }
                });
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        primaryParams.topMargin = dp(context, 18f);
        column.addView(primary, primaryParams);

        // Enlace web NovaStore.
        View storeLink = bodyText(context, "Abrir NovaStore en el navegador", accent, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (storeUrl != null && !storeUrl.isEmpty()) {
                    try {
                        ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)));
                    } catch (Exception e) {
                        Toast.makeText(ctx, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linkParams.topMargin = dp(context, 12f);
        column.addView(storeLink, linkParams);

        return scroll;
    }

    private static GradientDrawable gradient(
            GradientDrawable.Orientation orientation, int[] colors, int cornerRadius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable(orientation, colors);
        drawable.setCornerRadius(cornerRadius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private static void copyCode(Context context, String deviceCode) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(
                ClipData.newPlainText("device_code", LicenseChecker.normalizeDeviceCode(deviceCode)));
        Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private static View stepRow(Context context, String number, String text, int topColor, int bottomColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 10f), 0, dp(context, 10f));

        // Círculo de tamaño fijo: el fondo vive en un FrameLayout de dimensión exacta, así el
        // badge nunca se recorta (un TextView con background OVAL dibuja el círculo al alto del
        // texto y se ve cortado).
        FrameLayout badgeContainer = new FrameLayout(context);
        badgeContainer.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 28f), dp(context, 28f)));
        GradientDrawable badgeBackground =
                new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{topColor, bottomColor});
        badgeBackground.setShape(GradientDrawable.OVAL);
        badgeContainer.setBackground(badgeBackground);

        TextView badgeText = new TextView(context);
        badgeText.setText(number);
        badgeText.setTextSize(12f);
        badgeText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badgeText.setGravity(Gravity.CENTER);
        badgeText.setTextColor(Color.WHITE);
        badgeText.setIncludeFontPadding(false);
        badgeContainer.addView(badgeText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        row.addView(badgeContainer);

        TextView body = new TextView(context);
        body.setText(text);
        body.setTextSize(12f);
        body.setTextColor(TEXT_SUB);
        body.setLineSpacing(dp(context, 2f), 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.leftMargin = dp(context, 12f);
        row.addView(body, bodyParams);
        return row;
    }

    private static LinearLayout infoRow(Context context, int accent, String text) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f));
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(context, 12f));
        background.setColor(accentColor(accent, 0x12));
        background.setStroke(dp(context, 1f), accentColor(accent, 0x40));
        row.setBackground(background);

        ImageView icon = new ImageView(context);
        icon.setImageResource(android.R.drawable.ic_dialog_info);
        icon.setColorFilter(new PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN));
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 18f), dp(context, 18f)));
        row.addView(icon);

        TextView body = new TextView(context);
        body.setText(text);
        body.setTextSize(11.5f);
        body.setTextColor(TEXT_SUB);
        body.setLineSpacing(dp(context, 1.5f), 1f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.leftMargin = dp(context, 10f);
        row.addView(body, bodyParams);
        return row;
    }

    private static Button gradientButton(
            Context context, String label, int top, int bottom, int height, View.OnClickListener onClick) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(13f);
        button.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        button.setTextColor(Color.WHITE);
        button.setStateListAnimator(null);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);

        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        shape.setCornerRadius(dp(context, 12f));

        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(context, 12f));
        mask.setColor(Color.WHITE);

        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, mask));
        button.setOnClickListener(onClick);
        button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        return button;
    }

    private static Button outlineButton(
            Context context, String label, int accent, int height, View.OnClickListener onClick) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(12f);
        button.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        button.setTextColor(accent);
        button.setStateListAnimator(null);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(context, 10f));
        shape.setStroke(Math.round(dp(context, 1.5f)), accentColor(accent, 0x80));
        shape.setColor(SURFACE);

        GradientDrawable mask = new GradientDrawable();
        mask.setCornerRadius(dp(context, 10f));
        mask.setColor(Color.WHITE);

        button.setBackground(new RippleDrawable(ColorStateList.valueOf(accentColor(accent, 0x22)), shape, mask));
        button.setOnClickListener(onClick);
        button.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        return button;
    }

    private static TextView bodyText(Context context, String label, int accent, View.OnClickListener onClick) {
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextSize(12f);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        text.setGravity(Gravity.CENTER);
        text.setTextColor(accent);
        text.setPadding(dp(context, 8f), dp(context, 6f), dp(context, 8f), dp(context, 6f));
        text.setOnClickListener(onClick);
        return text;
    }

    private static int accentTone(int color) {
        return Color.rgb(
                Math.min((int) (Color.red(color) * 0.72f), 255),
                Math.min((int) (Color.green(color) * 0.72f), 255),
                Math.min((int) (Color.blue(color) * 0.72f), 255));
    }

    private static int accentColor(int accent, int alpha) {
        return Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent));
    }

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }
}
