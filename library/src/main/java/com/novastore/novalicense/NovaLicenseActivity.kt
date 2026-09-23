package com.novastore.novalicense

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast

/**
 * Gate de licencia que envuelve la app protegida (equivalente a
 * `novaLicenseGuard(...)` del SDK Dart).
 *
 * Úsalo como actividad principal en el manifest:
 * `<activity android:name="com.novastore.novalicense.NovaLicenseActivity" ...>`
 * con el intent-filter MAIN/LAUNCHER. Mientras comprueba muestra un splash;
 * si la licencia es válida salta a `config.mainActivity` y se cierra; si no,
 * pinta la pantalla de licencia con el código de dispositivo.
 */
class NovaLicenseActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = NovaLicense.config
        if (config == null) {
            Toast.makeText(
                this,
                "NovaLicense: llama a NovaLicense.configure(...) antes de iniciar la app.",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }
        showLoading()
        runCheck(config)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Bloqueado: no se puede saltar la licencia con el botón atrás.
    }

    private fun showLoading() {
        val container = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(ProgressBar(this@NovaLicenseActivity))
        }
        setContentView(container)
    }

    private fun runCheck(config: NovaLicenseGuardConfig) {
        NovaLicense.checkAsync(this) { result ->
            if (result.allowed) {
                launchMain(config)
                finish()
            } else {
                showLicenseScreen(config, result)
            }
        }
    }

    private fun showLicenseScreen(config: NovaLicenseGuardConfig, result: NovaLicenseResult) {
        val deviceCode = result.deviceCode ?: LicenseChecker.resolveDeviceId(config, NovaLicense.store(this))
        setContentView(
            LicenseRequiredView.build(
                context = this,
                deviceCode = deviceCode,
                storeUrl = LicenseUrls.storeUrl(config),
                offline = result.offline,
                errorMessage = if (result.offline) result.errorMessage else null,
                onRetry = if (result.offline) { { runCheck(config) } } else null,
                onRefresh = if (!result.offline) { { runCheck(config) } } else null,
                brandColor = config.brandColor,
            ),
        )
    }

    private fun launchMain(config: NovaLicenseGuardConfig) {
        val target = config.mainActivity ?: return
        val intent = Intent(this, target)
        startActivity(intent)
    }
}