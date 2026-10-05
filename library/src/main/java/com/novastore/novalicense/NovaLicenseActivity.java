package com.novastore.novalicense;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Gate de licencia que envuelve la app protegida (equivalente a {@code novaLicenseGuard(...)} del
 * SDK Dart).
 *
 * <p>Úsalo como actividad principal en el manifest:
 * {@code <activity android:name="com.novastore.novalicense.NovaLicenseActivity" ...>} con el
 * intent-filter MAIN/LAUNCHER. Mientras comprueba muestra un splash; si la licencia es válida salta
 * a {@code config.mainActivity} y se cierra; si no, pinta la pantalla de licencia con el código de
 * dispositivo.
 */
public final class NovaLicenseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NovaLicenseGuardConfig config = NovaLicense.getConfig();
        if (config == null) {
            Toast.makeText(
                    this,
                    "NovaLicense: llama a NovaLicense.configure(...) antes de iniciar la app.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        showLoading();
        runCheck(config);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Bloqueado: no se puede saltar la licencia con el botón atrás.
    }

    private void showLoading() {
        LinearLayout container = new LinearLayout(this);
        container.setGravity(Gravity.CENTER);
        container.addView(new ProgressBar(this));
        setContentView(container);
    }

    private void runCheck(final NovaLicenseGuardConfig config) {
        NovaLicense.checkAsync(this, new NovaLicense.Callback() {
            @Override
            public void onResult(NovaLicenseResult result) {
                if (result.isAllowed()) {
                    launchMain(config);
                    finish();
                } else {
                    showLicenseScreen(config, result);
                }
            }
        });
    }

    private void showLicenseScreen(NovaLicenseGuardConfig config, NovaLicenseResult result) {
        String deviceCode = result.getDeviceCode();
        if (deviceCode == null || deviceCode.isEmpty()) {
            deviceCode = LicenseChecker.resolveDeviceId(config, NovaLicense.store(this));
        }
        final String code = deviceCode;
        final boolean offline = result.isOffline();
        setContentView(LicenseRequiredView.build(
                this,
                code,
                LicenseUrls.storeUrl(config),
                offline,
                offline ? result.getErrorMessage() : null,
                offline ? new Runnable() {
                    @Override
                    public void run() {
                        runCheck(config);
                    }
                } : null,
                offline ? null : new Runnable() {
                    @Override
                    public void run() {
                        runCheck(config);
                    }
                },
                config.getBrandColor()));
    }

    private void launchMain(NovaLicenseGuardConfig config) {
        Class<?> target = config.getMainActivity();
        if (target == null) {
            return;
        }
        startActivity(new Intent(this, target));
    }
}
