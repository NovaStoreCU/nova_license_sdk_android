# Reglas de ProGuard/R8 para el consumidor del SDK.
#
# La app protegida llama a NovaLicense por reflexión-free (Java directo), pero Unity
# (IL2CPP) y los usos programáticos sí necesitan que los nombres de clase/método se
# conserven: el gate se configura desde C# con AndroidJavaObject y la pantalla de licencia
# se instancia desde el manifest.

# La Activity del gate se declara en el manifest de la app -> R8 no puede borrarla.
-keep public class com.novastore.novalicense.NovaLicenseActivity { *; }

# API pública: se conserva por si el consumidor la llama por reflexión (típico en Unity/C#).
-keep public class com.novastore.novalicense.NovaLicense { *; }
-keep public class com.novastore.novalicense.NovaLicenseGuardConfig { *; }
-keep public class com.novastore.novalicense.NovaLicenseGuardConfig$* { *; }
-keep public class com.novastore.novalicense.NovaLicenseResult { *; }
-keep public class com.novastore.novalicense.NovaLicenseStatus { *; }
-keep public class com.novastore.novalicense.LicenseTransport { *; }
-keep public class com.novastore.novalicense.LicenseTransport$* { *; }
-keep public class com.novastore.novalicense.LicenseStorage { *; }
-keep public class com.novastore.novalicense.PreferencesLicenseStorage { *; }
-keep public class com.novastore.novalicense.HttpUrlConnectionTransport { *; }
-keep public class com.novastore.novalicense.LicenseChecker { *; }
-keep public class com.novastore.novalicense.LicenseUrls { *; }
-keep public class com.novastore.novalicense.NovaLogoView { *; }
-keep public class com.novastore.novalicense.LicenseRequiredView { *; }
