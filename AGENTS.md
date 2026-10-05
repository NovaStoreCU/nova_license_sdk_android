# novalicense (SDK Android/Java) — Contexto del proyecto

Documento de continuidad para agentes/desarrolladores. Describe **qué existe**, **cómo
compilarlo** y **las trampas de AGP 9 / Java**. El README es la documentación de uso para
el developer; este archivo es para quien toca el código.

## Qué es

SDK de **verificación suave (lazy) de licencias** para apps de pago de NovaStore. Se integra
en la app del developer (2 líneas) y comprueba en cada arranque si el **dispositivo** tiene
licencia (la compra en NovaStore se vincula a un `device_id`). Sin licencia → pantalla con el
**código de activación** que el usuario pega en NovaStore (vínculo manual) + botón "Abrir
NovaStore".

Es el **espejo 1:1 del SDK Dart** (`../nova_license_sdk`): misma lógica, mismos
`SharedPreferences`, mismo endpoint. Cuando cambies un comportamiento, cambia en los dos.

## Estructura

- `library/src/main/java/com/novastore/novalicense/` — **13 clases Java**, sin Kotlin y sin
  dependencias externas (solo framework: `HttpURLConnection` + `SharedPreferences`).
  - `NovaLicense` — fachada estática: `configure`, `check`, `checkAsync`, `openStore`, `store`.
    Interface anidada `NovaLicense.Callback` (`onResult`).
  - `NovaLicenseGuardConfig` — config. Constructores de 3 y 4 args, `Builder`,
    `create(...)` (fábrica de 10 parámetros) y **`createByClassName(...)`** (la vía desde
    Unity/C#, que no puede pasar un `Class`); `resolveActivityClass(String)`.
  - `NovaLicenseActivity` — launcher del gate: splash → valida → `mainActivity` o pantalla de licencia.
  - `LicenseRequiredView` — la pantalla de licencia (banner, código, botones, error).
  - `NovaLogoView` — logo Canvas (paridad visual con el Kotlin anterior + `applyBrandColor`).
  - `LicenseChecker` — toda la lógica: `deviceId`, caché, gracia, SHA-1, normalizaciones.
  - `LicenseTransport` — interfaz `Outcome ok/failed` + `HttpUrlConnectionTransport`.
  - `LicenseUrls` — `normalizeBase`, `validateUrlByPackage`, `validateUrl`, `storeUrl`.
  - `PreferencesLicenseStorage`, `LicenseStorage`, `NovaLicenseResult`, `NovaLicenseStatus`.
- `library/src/test/java/.../` — **5 ficheros / 27 tests JVM**: `TestSupport` (fakes +
  `TestConfigs`), `LicenseCheckerTest`, `HttpUrlConnectionTransportTest` (**socket real** con
  `ServerSocket`, sin `com.sun.net.httpserver`), `LicenseUrlsTest`, `DeviceIdTest`.
- `library/consumer-rules.pro` — reglas R8 (conservar API + `NovaLicenseActivity`).
- `example/src/main/java/com/novastore/example/` — `ExampleApp` + `MainActivity` (Java).
  `applicationId = "com.novastore.sdktestkotlin"` (**se conserva aunque sea Java**: cambiarlo
  rompería la Continuity/deploy ya hecha).
- `dist/` — `novalicense-1.0.0.aar` (Kotlin, para comparar) y **`novalicense-1.1.0.aar`** (el
  que se distribuye).

## Stack y build

- **AGP 9.0.1** + **Gradle 9.1.0** + **JDK 21 (JBR de Android Studio)**. Bytecode Java 11.
- `compileSdk 34`, `minSdk 21`, sin `namespace` de Kotlin, **sin** plugin `kotlin.android`
  (Kotlin es built-in en AGP 9; y este módulo ya no tiene Kotlin).

```bash
# Windows (PowerShell) — workdir: nova_license_sdk_android
.\gradlew.bat :novalicense:testDebugUnitTest          # 27 tests JVM
.\gradlew.bat :novalicense:testDebugUnitTest --tests "*LicenseUrlsTest"
.\gradlew.bat :novalicense:assembleRelease            # → library/build/outputs/aar/
.\gradlew.bat :novalicense:assembleRelease :example:assembleDebug
```

Un test con red real (`HttpUrlConnectionTransportTest`) necesita un puerto libre; los contextos
`/api`, `/api-invalid`, `/api-down` son los del servidor de pruebas.

## Publicar una versión

1. Compilar `:novalicense:assembleRelease`.
2. Copiar el `.aar` a `dist/novalicense-<version>.aar` (**el `dist/` es lo que descarga el
   developer**: si no lo copias, el README miente).
3. Reportar **tamaño + SHA-256** del `.aar` en el mensaje final.
4. Actualizar la versión en el README y el tag de git (los commits los hace el usuario).

## Contrato con la API (no cambiar sin tocar el SDK Dart)

- `POST {apiBase}/validate` (por `package_name` en el body; también existe
  `POST {apiBase}/apps/{slug}/validate`). El transporte **añade** `/validate` → `apiBase` es
  la **base**, no el endpoint.
- Body: `{package_name, device_id, apk_sha1?}`. Respuesta: `{data:{valid}}`.
- Semántica que implementa `LicenseChecker`: **2xx con `valid:true`** → cachea el timestamp;
  cualquier otro 2xx, **404** o cuerpo malformado → inválida (cachea el fallo); **5xx** o
  error de red → `OFFLINE`, y solo pasa si la caché es más nueva que `graceDays`.
- `apk_sha1` se compara con el `signing_sha1` de la última versión publicada: re-firmado ≠ válido.
- **Gracia offline por defecto 3 días**, **timeout 10 s**, `device_id` = 16 bytes aleatorios en
  hex, persistido en `SharedPreferences("novalicense")` con las claves
  `nova_license_sdk_device_id` / `nova_license_sdk_cached_valid_at` (**no cambiar**: son el
  contrato con el SDK Dart y con las instalaciones ya hechas).

## Reglas del proyecto

- **Java puro.** No reintroducir Kotlin, ni dependencias externas (AndroidX, OkHttp, Gson).
- **El paquete sigue siendo `com.novastore.novalicense`** (compatibilidad 1.0.0 → 1.1.0).
- Toda URL se construye en `LicenseUrls` (tolerante a `null`/vacío, sin excepción); toda
  constante de UI en `LicenseRequiredView`/`NovaLogoView` (degradado de marca
  `#00C853`→`#0091EA`, acento por defecto `0xFF0091EA`).
- La pantalla de licencia debe seguir mostrando **código de dispositivo copiable + instrucciones
  + abrir tienda + reintentar**: es el único camino de activación.
- Un cambio de comportamiento va **acompañado de test** (suite objetivo: verde y sin tests
  borrados; nada de tests "placeholder" que no comprueban nada).

## Trampas (AGP 9 / Java, verificadas)

- **No apliques `org.jetbrains.kotlin.android`**: en AGP 9 Kotlin es built-in y los módulos sin
  Kotlin dan error. Solo `com.android.library` / `com.android.application`.
- Los **módulos library no admiten `versionCode`/`versionName`** en `defaultConfig`.
- `ScrollView.LayoutParams` ya no existe → usa `FrameLayout.LayoutParams`.
- `setTextIsSelectable(true)` en vez de `setTextIsMovable`; **`scaledDensity` → `fontScale`**
  para `sp`; `@Suppress("DEPRECATION")` en `onBackPressed`.
- En tests, no uses `com.sun.net.httpserver` ni lambdas que dependan de desugaring; los
  callbacks son interfaces.
- **Encoding**: nunca edites `.java`/`.md`/`.xml` con `Get-Content`/`Set-Content` de
  PowerShell (double-encoda acentos y mete BOM). Usa las herramientas de edición. Para
  revisar: el fichero debe estar en UTF-8 **sin BOM** y sin `U+FFFD`.

## Regla de documentación (aplicada ya)

Antes de escribir "arreglado", "corregido" o "nuevo en v1.1.0" en el README/AGENTS, **verifícalo
contra el código o contra el artefacto anterior**. Durante la conversión Java se documentaron
como cambios de v1.1.0 tres cosas que **no eran cambios** (el `exported` del manifest y el
`Long`/`Int` de la caché ya venían de la v1.0.0, y no existía bug de `/validate/validate`); se
retiraron tras comparar con `dist/novalicense-1.0.0.aar` y el Kotlin de respaldo. La
paridad visual con el Kotlin se comprobó con `NovaLogoView` lado a lado.

## Pendiente / no hecho

- **Sin validar en un proyecto Unity 6 real** (compila y los tests pasan, pero el consumo desde
  C#/IL2CPP y el *managed stripping* no se han probado en un proyecto de Unity).
- El **SDK Dart sigue con `/apps/{slug}`** para la URL de tienda (el Java ya usa
  `/store/apps/{slug}/`): divergencia consciente, pendiente de unificar.
- Distribución: `.aar` en `dist/` + repo git. No se publica en un repositorio de artefactos.
