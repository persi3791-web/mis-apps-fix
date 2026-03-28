# Cornell Notes - App Android

App Android nativa (Kotlin + Jetpack Compose) que reemplaza la versión PyDroid/Kivy. Usa las **APIs de Gemini** para generar notas Cornell, cuestionarios, flashcards y chat sobre tus apuntes.

## Requisitos

- Android Studio Ladybug (2024.2.1) o superior
- JDK 17
- Android SDK 35 (compileSdk / targetSdk 35)
- minSdk 24

## Si el build falla (metadata.bin o "lock protocol")

Son errores de **caché de Gradle corrupta**. Haz esto en orden:

### Opción 1 – Script automático (recomendado)

En la carpeta del proyecto, **doble clic** en:

- **`fix-gradle-cache.bat`** (en CMD), o  
- **`fix-gradle-cache.ps1`** (en PowerShell; si pide permisos: `Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned`)

El script para Gradle, borra la caché corrupta y ejecuta el build.

### Opción 2 – A mano (PowerShell)

Abre **PowerShell**, ve a la carpeta del proyecto y ejecuta:

```powershell
cd "c:\Users\josep\OneDrive\Escritorio\cornell"
.\gradlew.bat --stop
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\9.1.0" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force ".gradle" -ErrorAction SilentlyContinue
.\gradlew.bat clean assembleDebug
```

### Opción 3 – Desde Android Studio

1. **File → Invalidate Caches → Invalidate and Restart**
2. Después de reiniciar: **Build → Rebuild Project**

## Cómo compilar

```bat
cd c:\Users\josep\OneDrive\Escritorio\cornell
.\gradlew.bat assembleDebug
```

El APK queda en: `app\build\outputs\apk\debug\app-debug.apk`

## Estructura del proyecto

- **`data/`** – Modelos (Note, CornellData, Quiz, Flashcards) y `DataManager` (JSON en `filesDir`).
- **`api/`** – Cliente Gemini (Retrofit), DTOs y `GeminiRepository` con rotación de API keys y generación de Cornell, quiz, flashcards y chat.
- **`ui/`** – Compose: `CornellViewModel`, navegación, pantallas (Home, Manage, Result con pestañas Resumen / Cuestionario / Flashcards / Chat / Transcripción).

## Funciones (igual que la app original)

- **Notas Cornell**: texto libre → Gemini genera ideas clave, notas detalladas y resumen.
- **Carpetas**: crear, renombrar, eliminar; mover notas entre carpetas.
- **Cuestionario**: generar N preguntas tipo test desde la nota (Gemini); navegar preguntas; guardar estado.
- **Flashcards**: generar N tarjetas; voltear y guardar estado.
- **Chat**: preguntas sobre la nota con contexto enviado a Gemini.
- **Transcripción**: ver el texto original de la nota.
- **Edición**: editar título, ideas clave, notas y resumen; guardar.
- **API Keys**: rotación de varias API keys de Gemini en `GeminiRepository`.

## API Keys de Gemini

Están definidas en `api/GeminiRepository.kt`. Para producción, conviene usar variables de entorno o `BuildConfig` en lugar de strings en el código.

## Permisos

- `INTERNET` – llamadas a la API de Gemini.
