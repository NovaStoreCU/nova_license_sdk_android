package com.novastore.example

import android.app.Application
import com.novastore.novalicense.NovaLicense
import com.novastore.novalicense.NovaLicenseGuardConfig

class ExampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        NovaLicense.configure(
            NovaLicenseGuardConfig(
                apiBase = "https://novastore.cu/api/v1",
                storeUrl = "https://novastore.cu",
                packageName = "com.novastore.example",
                storeSlug = "mi-juego",
                mainActivity = MainActivity::class.java,
            ),
        )
    }
}