# novalicense (Android)

Validación suave (*lazy*) de licencias para **apps de pago de NovaStore**, para
**Android nativo (Java) y Unity**. Es el espejo del paquete Dart
[`nova_license_sdk`](https://github.com/NovaStoreCU/nova_license_sdk): misma
lógica, mismas claves de persistencia y misma pantalla de licencia.

Cuando un usuario compra una app en NovaStore, la compra queda vinculada a un
dispositivo concreto. Este SDK, integrado dentro de la app del desarrollador,
comprueba en cada arranque si el dispositivo que está ejecutando la app tiene
licencia. Solo **2 líneas** de integración.

## Novedades de v1.1.0

- **Java puro**: el SDK ya no es Kotlin. Sirve igual en Kotlin/Java y, sobre
  todo, en **Unity 6 / IL2CPP**, donde una dependencia Kotlin obliga a meter el
  runtime de Kotlin en el APK. Mismo paquete, misma API lógica y mismos
  `SharedPreferences` que la v1.0.0: **actualizar no invalida la licencia ya
  cacheada** ni cambia el código del dispositivo.
- **Sin reflexión ni desugaring**:
  - `NovaLicenseGuardConfig.createByClassName(...)` permite configurar el gate
    desde C# pasando el nombre de la Activity como `String` (un `Class` no se
    puede pasar por `AndroidJavaObject`).
  - `NovaLicense.checkAsync` toma una **interfaz** (`NovaLicense.Callback`) en
    vez de una lambda, así que no hace falta *core library desugaring*.
- **`storeSlug` abre la ficha real** (`/store/apps/{slug}/`); la v1.0.0 generaba
  `/apps/{slug}`, una ruta que no existe, así que el botón "Abrir NovaStore" de
  la pantalla de licencia caía en la home de la tienda en vez de en la app.
- **`brandColor` se aplica también al logo** de la pantalla de licencia (antes
  solo teñía el banner y los botones).
- Tests reescritos en Java (27 tests JVM), incluidos los de HTTP real del
  transporte (antes solo se cubría con dobles).

## Características

- Valida contra `POST {apiBase}/validate` de la API v1 de NovaStore por
  **`package_name`**.
- **Gracia offline de 3 días**: si la licencia ya era válida y no hay red, la
  app sigue abriéndose durante la gracia.
- **Anti re-firma**: si se informa el SHA-1 de firma del APK (`apkSha1`), un APK
  re-firmado por otra persona no pasa la validación.
- Si no hay licencia, muestra una pantalla con el **código de dispositivo** que el
  usuario pega en NovaStore al comprar (vínculo manual) y un botón que abre la
  tienda.
- **Sin dependencias externas**: solo el framework Android (`HttpURLConnection`,
  `SharedPreferences`). No requiere AndroidX ni Kotlin.
- `minSdk 21+`, `minCompileSdk 34`, artefacto `.aar` de ~35 KB.

## Distribución

Versión actual: **v1.1.0**.

| Cómo obtenerla | Qué es |
|----------------|--------|
| **`.aar` compilado** (recomendado) | `dist/novalicense-1.1.0.aar` dentro de este repo. Descárgalo y listo. |
| **Módulo desde git** | Clonar el repo y agregar `:novalicense` con el código fuente (instrucciones abajo). |

> Como Gradle no puede consumir un repo git "crudo" directamente, los dos caminos
> son: bajar el `.aar` una vez, o integrar el módulo por git. Cuando haya una
> versión nueva solo reemplazas el `.aar` (o haces `git pull`).

## Integración (3 pasos)

### 1. Añade el SDK

**Opción A - `.aar`** (Android nativo y Unity):

Copia `dist/novalicense-1.1.0.aar` a `app/libs/` y en tu `build.gradle`:

```groovy
dependencies {
    implementation files('libs/novalicense-1.1.0.aar')
}
```

En Kotlin DSL: `implementation(files("libs/novalicense-1.1.0.aar"))`.

**Opción B - módulo git** (juega con el código fuente):

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

```java
import com.novastore.novalicense.NovaLicense;
import com.novastore.novalicense.NovaLicenseGuardConfig;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NovaLicense.configure(
                NovaLicenseGuardConfig.builder(
                                "https://api.novastore.cu/api/v1", // base de la API v1
                                "https://novastore.cu",              // base de la tienda
                                "com.tuempresa.tujuego")             // package_name en NovaStore
                        .storeSlug("tu-juego")                       // opcional: slug del botón de compra
                        .mainActivity(MainActivity.class)           // tu pantalla real
                        // .apkSha1("AA:BB:CC:...")                 // opcional: SHA-1 de firma del APK
                        .build());
    }
}
```

También hay un constructor de 3 argumentos (`new NovaLicenseGuardConfig(apiBase,
storeUrl, packageName)`) para lo mínimo, y `NovaLicenseGuardConfig.create(...)`
con **todos** los parámetros en una sola llamada (cómodo desde Unity).

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

`NovaLicenseActivity` muestra un splash mientras valida; si la licencia es válida
salta a `mainActivity` y se cierra; si no, pinta la pantalla de licencia.

> **Nota**: la librería **no declara `android:exported`** en su manifest (ni la
> v1.1.0 ni la v1.0.0 lo hacían), porque quien declara la Activity es tu app. Por
> eso el `android:exported="true"` de arriba es obligatorio desde Android 12 para
> actividades con `intent-filter`.

### 3. Compila y así lo ve el usuario

- Dispositivo con licencia → se abre tu `MainActivity` normalmente.
- Dispositivo sin licencia → pantalla de licencia con el **código del
  dispositivo** y las instrucciones:
  1. Abre la app en NovaStore.
  2. Toca Comprar y pega el código como *Código de activación*.
  3. Al confirmar, la app se desbloquea en ese dispositivo (reinstalarla no
     exige recompra; cambiar de teléfono sí).

## Unity 6 (IL2CPP)

Desde C# no se puede pasar un `Class<Activity>` a Java, así que usa la fábrica
por nombre de clase. Todo se hace con `AndroidJavaClass`.

**1. Copia el `.aar`**: deja `novalicense-1.1.0.aar` en `Assets/Plugins/Android/`
y marca el plugin para **Android** (Platform Settings → Android → *Include*).

**2. Configura el gate** desde el primer script que se ejecute (por ejemplo un
`[RuntimeInitializeOnLoadMethod]`):

```csharp
using UnityEngine;

public static class NovaLicenseSetup
{
    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
    static void Init()
    {
        using (var config = new AndroidJavaClass("com.novastore.novalicense.NovaLicenseGuardConfig"))
        {
            var cfg = config.CallStatic<AndroidJavaObject>(
                "createByClassName",
                "https://api.novastore.cu/api/v1", // apiBase
                "https://novastore.cu",              // storeUrl
                Application.identifier,              // package_name (el de tu app)
                "com.unity3d.player.UnityPlayerActivity", // tu Activity real
                "tu-juego",                          // storeSlug (opcional, "" si no)
                "",                                  // deviceId (opcional)
                "",                                  // apkSha1 (opcional)
                3L,                                  // graceDays
                10000L,                              // httpTimeoutMillis
                null                                 // brandColor (opcional)
            );

            using (var license = new AndroidJavaClass("com.novastore.novalicense.NovaLicense"))
            {
                license.CallStatic("configure", cfg);
            }
        }
    }
}
```

`Application.identifier` devuelve el `package_name` de tu app, que es el que
debe estar registrado en NovaStore.

**3. Manifest**: declara la Activity del gate como launcher, en
`Assets/Plugins/Android/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <activity
            android:name="com.novastore.novalicense.NovaLicenseActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Quita (o deja de ser launcher) la `UnityPlayerActivity` original del manifest,
o si no el gate no arrive a launching.

**4. Protege solo lo que quieras** (modo *verify*, sin gate bloqueante): llama a
`checkAsync` desde C# y decide tú qué abres. Como `Callback` es una **interfaz
Java**, se implementa con un `AndroidJavaProxy` (el puente de Unity), y el
resultado llega al hilo principal:

```csharp
using (var license = new AndroidJavaClass("com.novastore.novalicense.NovaLicense"))
using (var unityCtx = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
using (var activity = unityCtx.GetStatic<AndroidJavaObject>("currentActivity"))
{
    license.CallStatic("checkAsync", activity, new LicenseCallback(result =>
    {
        // result: getStatus() / isAllowed() / isOffline() / getErrorMessage() / getDeviceCode()
        if (result.Call<bool>("isAllowed")) OpenPremium();
        else ShowMyOwnPaywall();
    }));
}

// La interfaz Java NovaLicense.Callback, implementada desde C#.
class LicenseCallback : AndroidJavaProxy
{
    readonly Action<AndroidJavaObject> _onResult;

    public LicenseCallback(Action<AndroidJavaObject> onResult)
        : base("com.novastore.novalicense.NovaLicense$Callback")
    {
        _onResult = onResult;
    }

    // El método de la interfaz es onResult(NovaLicenseResult).
    void onResult(AndroidJavaObject result) => _onResult(result);
}
```

Ojo con el `$` en el nombre de la interfaz anidada: `NovaLicense$Callback`.

> **Limitación conocida de IL2CPP**: el *managed stripping* puede borrar la
> clase de tu Activity si solo la referencia el manifest. Añade
> `-keep class com.miapp.** { *; }` (o la clase concreta) en
> `Assets/Plugins/Android/proguard-user.txt`.

## Uso programático (opcional)

En vez del patrón launcher, controla la licencia desde tu código. Esto te
permite **decidir qué proteger** (modo premium, trial, protección por funciones) y
no solo bloquear toda la app:

```java
NovaLicense.configure(config);                  // una vez, antes de usar el resto
NovaLicense.checkAsync(this, new NovaLicense.Callback() {
    @Override public void onResult(NovaLicenseResult result) {
        if (result.isAllowed()) {
            openPremium();          // con licencia: abre todo
        } else {
            showMyOwnPaywall();     // sin licencia: tú decides
        }
    }
});
```

También disponibles: `NovaLicense.check(context)` (síncrono, **no** llamar desde
el hilo de UI), `NovaLicense.openStore(context)`, `NovaLicense.store(context)` y
las sobrecargas de `check(...)` con `LicenseTransport`/`LicenseStorage`
inyectados (tests o lógica propia).

`check`/`checkAsync` devuelven un `NovaLicenseResult` con:

| Método         | Descripción |
|----------------|-------------|
| `getStatus()`  | `VALID`, `INVALID`, `OFFLINE` o `ERROR`. |
| `isAllowed()`  | `true` solo si la licencia es válida. |
| `isOffline()`  | `true` si no hay red y no hay caché fresca. |
| `getErrorMessage()` | Detalle del error (si lo hay). |
| `getDeviceCode()`   | Código de activación del dispositivo (muéstralo en tu pantalla). |

> **Ambos estilos usan el mismo backend** (`POST /validate`) y la misma lógica de
> caché/gracia. Este modo solo te entrega la respuesta para que decidas cuánto
> parte de tu app proteger.

## Opciones de configuración

| Parámetro          | Requerido | Descripción |
|--------------------|-----------|-------------|
| `apiBase`          | sí        | Base de la API v1 (ej. `https://api.novastore.cu/api/v1`). |
| `storeUrl`         | sí        | Base de la tienda para el botón "Abrir NovaStore". |
| `packageName`      | sí        | `package_name` de tu app registrado en NovaStore. |
| `mainActivity`     | sí*       | Clase de tu pantalla real (la abre el gate tras la licencia). *Solo en el patrón launcher. |
| `storeSlug`        | no        | Slug de tu app; si va, el botón abre el detalle directo (`/store/apps/{slug}/`). |
| `deviceId`         | no        | Id de dispositivo estable (opcional; el SDK lo persiste solo). |
| `apkSha1`          | no        | SHA-1 de firma del APK (muro anti re-firma). |
| `graceDays`        | no        | Gracia offline en días (por defecto **3**). |
| `httpTimeoutMillis`| no        | Timeout de la validación en ms (por defecto 10000). |
| `brandColor`       | no        | Color ARGB de acento de la pantalla de licencia. |

> El `apiBase` es la **base**, no el endpoint: el SDK le añade `/validate`. Pasarle
> la URL completa acabaría en `/validate/validate`, que da 404 (el SDK tolera
> barras finales, pero no quita un sufijo `/validate` que ya venga en la base).

## Notas

- El `device_id` se genera una vez (16 bytes aleatorios en hex) y se persiste en
  `SharedPreferences` (`novalicense`). Por eso **reinstalar no exige recompra**:
  el código es el mismo. Cambiar de teléfono cambia el código → nueva compra (o
  re-activación desde NovaStore).
- En debug sin firma puedes omitir `apkSha1`; en release pasa el SHA-1 de tu
  keystore para activar el muro anti re-firma.
- El APK firmado del ejemplo vive en `example/` (usa `NovaLicenseActivity` de
  launcher y una `MainActivity` de prueba). Compílalo con
  `./gradlew :example:assembleDebug`.
- **ProGuard/R8**: el `.aar` incluye `consumer-rules.pro` con las reglas para
  conservar la API pública y la `NovaLicenseActivity`, así que no necesitas
  añadir nada en tu app (salvo el *managed stripping* de Unity, ver arriba).
- Versión nueva del SDK: descarga el nuevo `.aar` y reemplaza el archivo en
  `app/libs/` (no hace falta tocar código).

## Compilar desde el fuente

```bash
./gradlew :novalicense:testDebugUnitTest   # 27 tests JVM (sin Android device)
./gradlew :novalicense:assembleRelease     # → library/build/outputs/aar/
./gradlew :example:assembleDebug           # APK de prueba
```

Requiere **JDK 11+** (el de Android Studio, `jbr`, sirve) y AGP 9 / Gradle 9.

## Licencia

Ver el repositorio de NovaStore. Uso comercial sujeto a las reglas de NovaStore.
