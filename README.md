# novalicense (Android)

Validación suave (*lazy*) de licencias para **apps de pago de NovaStore**,
para **Android nativo (Kotlin/Java)**. Es el espejo exacto del paquete Dart
[`nova_license_sdk`](https://github.com/NovaStoreCU/nova_license_sdk): misma
lógica, mismas claves de persistencia y misma pantalla de licencia.

Cuando un usuario compra una app en NovaStore, la compra queda vinculada a un
dispositivo concreto. Este SDK, integrado dentro de la app del desarrollador,
comprueba en cada arranque si el dispositivo que está ejecutando la app tiene
licencia. Solo **2 líneas** de integración.

## Características

- Valida contra `POST {apiBase}/validate` de la API v1 de NovaStore por
  **`package_name`**.
- **Gracia offline de 3 días**: si la licencia ya era válida y no hay red, la
  app sigue abriéndose durante la gracia.
- **Anti re-firma**: si se informa el SHA-1 de firma del APK (`apkSha1`), un APK
  re-firmado por otra persona no pasa la validación.
- Si no hay licencia, muestra una pantalla con el **código de dispositivo** que
  el usuario pega en NovaStore al comprar (vínculo manual) y un botón que abre
  la tienda.
- **Sin dependencias externas**: solo el framework Android
  (`HttpURLConnection`, `SharedPreferences`). No requiere AndroidX.
- `minSdk 21+`, artefacto `.aar` de 34 KB.

## Distribución

Versión actual: **v1.0.0**.

| Cómo obtenerla | Qué es |
|----------------|--------|
| **`.aar` compilado** (recomendado) | `dist/novalicense-1.0.0.aar` dentro de este repo. Descárgalo y listo. |
| **Módulo desde git** | Clonar el repo y agregar `:novalicense` con el código fuente (instrucciones abajo). |

> Como Gradle no puede consumir un repo git "crudo" directamente, los dos
> caminos son: bajar el `.aar` una vez, o integrar el módulo por git. Cuando
> haya una versión nueva solo reemplazas el `.aar` (o haces `git pull`).

## Integración (3 pasos)

### 1. Añade el SDK

**Opción A — `.aar`** (solo Android nativo):

Descarga `dist/novalicense-1.0.0.aar`, cópialo en `app/libs/` y en
`app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(files("libs/novalicense-1.0.0.aar"))
}
```

En Groovy: `implementation files("libs/novalicense-1.0.0.aar")`.

**Opción B — módulo git** (juega con el código fuente):

Clona el repo y en tu raíz agrega el módulo:

```kotlin
// settings.gradle.kts (raíz de tu app)
include(":novalicense")
project(":novalicense").projectDir = file("../nova_license_sdk_android/library")
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":novalicense"))
}
```

### 2. Configura el gate

**2.1** En tu `Application` (o antes de lanzar la actividad), llama a
`NovaLicense.configure(...)`:

```kotlin
import com.novastore.novalicense.NovaLicense
import com.novastore.novalicense.NovaLicenseGuardConfig

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NovaLicense.configure(
            NovaLicenseGuardConfig(
                apiBase = "https://novastore.cu/api/v1",  // base de la API v1
                storeUrl = "https://novastore.cu",        // base de la tienda
                packageName = "com.tuempresa.tujuego",    // package_name registrado en NovaStore
                storeSlug = "tu-juego",                   // opcional: slug para el botón de compra
                mainActivity = MainActivity::class.java,  // tu pantalla real
                // apkSha1 = "AA:BB:CC:...",              // opcional: SHA-1 de firma del APK
            ),
        )
    }
}
```

**2.2** En tu `AndroidManifest.xml`, haz de `NovaLicenseActivity` la actividad
principal (launcher) y deja tu `MainActivity` como actividad normal:

```xml
<application android:name=".App" ...>
    <activity
        android:name="com.novastore.novalicense.NovaLicenseActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <activity android:name=".MainActivity" android:exported="false" />
</application>
```

`NovaLicenseActivity` muestra un splash mientras valida; si la licencia es
válida salta a `mainActivity` y se cierra; si no, pinta la pantalla de licencia.

### 3. Compila y así lo ve el usuario

- Dispositivo con licencia → se abre tu `MainActivity` normalmente.
- Dispositivo sin licencia → pantalla de licencia con el **código del
  dispositivo** y las instrucciones:
  1. Abre la app en NovaStore.
  2. Toca Comprar y pega el código como *Código de activación*.
  3. Al confirmar, la app se desbloquea en ese dispositivo (reinstalarla no
     exige recompra; cambiar de teléfono sí).

## Uso programático (opcional)

En vez del patrón launcher, controla la licencia desde tu código. Esto te
permite **decidir qué proteger** (modo premium, trial, protección por
funciones) y no solo bloquear toda la app:

```kotlin
NovaLicense.configure(config)             // una vez, antes de usar el resto
NovaLicense.checkAsync(this) { result ->  // asíncrono, callback en el hilo principal
    if (result.allowed) {
        openPremium()          // con licencia: abre todo
    } else {
        showMyOwnPaywall()     // sin licencia: tú decides
    }
}
```

También disponibles: `NovaLicense.check(context)` (síncrono, **no** llamar desde
el hilo de UI), `NovaLicense.openStore(context)` y
`NovaLicense.store(context)`.

`NovaLicense.check`/`checkAsync` devuelven un `NovaLicenseResult` con:

| Campo          | Descripción |
|----------------|-------------|
| `status`       | `VALID`, `INVALID`, `OFFLINE` o `ERROR`. |
| `allowed`      | `true` solo si la licencia es válida. |
| `offline`      | `true` si no hay red y no hay caché fresca. |
| `errorMessage` | Detalle del error (si lo hay). |
| `deviceCode`   | Código de activación del dispositivo (muéstralo en tu pantalla). |

> **Ambos estilos usan el mismo backend** (`POST /validate`) y la misma lógica
> de caché/gracia. Este modo solo te entrega la respuesta para que decidas
> cuánta parte de tu app proteger.

## Opciones de configuración

| Parámetro          | Requerido | Descripción |
|--------------------|-----------|-------------|
| `apiBase`          | sí        | Base de la API v1 (ej. `https://novastore.cu/api/v1`). |
| `storeUrl`         | sí        | Base de la tienda para el botón "Abrir NovaStore". |
| `packageName`      | sí        | `package_name` de tu app registrado en NovaStore. |
| `mainActivity`     | sí*       | Clase de tu pantalla real (la abre el gate tras la licencia). *Solo en el patrón launcher. |
| `storeSlug`        | no        | Slug de tu app; si va, el botón abre el detalle directo. |
| `deviceId`         | no        | Id de dispositivo estable (opcional; el SDK lo persiste solo). |
| `apkSha1`          | no        | SHA-1 de firma del APK (muro anti re-firma). |
| `graceDays`        | no        | Gracia offline en días (por defecto **3**). |
| `httpTimeoutMillis`| no        | Timeout de la validación en ms (por defecto 10000). |
| `brandColor`       | no        | Color ARGB de acento de la pantalla de licencia. |

## Notas

- El `device_id` se genera una vez (16 bytes aleatorios en hex) y se persiste en
  `SharedPreferences` (`novalicense`). Por eso **reinstalar no exige recompra**:
  el código es el mismo. Cambiar de teléfono cambia el código → nueva compra (o
  re-activación desde NovaStore).
- En debug sin firma puedes omitir `apkSha1`; en release pasa el SHA-1 de tu
  keystore para activar el muro anti re-firma.
- El APK firmado del ejemplo vive en `example/` (usa `NovaLicenseActivity` de
  launcher y un `MainActivity` de prueba). Compílalo con
  `./gradlew :example:assembleDebug`.
- Versión nueva del SDK: descarga el nuevo `.aar` y reemplaza el archivo en
  `app/libs/` (no hace falta tocar código).

## Licencia

Ver el repositorio de NovaStore. Uso comercial sujeto a las reglas de NovaStore.