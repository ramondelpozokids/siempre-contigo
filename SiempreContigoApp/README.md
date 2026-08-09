# Siempre Contigo — App Android (Fase 1)

Asistente por **texto** para personas mayores: WhatsApp, correo, recordatorios y llamadas.
Alcance limitado a Fase 1 (sin voz, sin iOS).

Documentos de referencia en la raíz del repositorio:

- `especificacion-tecnica-siempre-contigo.docx`
- `guion-tecnico-ia-siempre-contigo.md` (**fuente de verdad** de las 4 acciones)

---

## Estado del encargo (honesto)

### Hecho

1. **Proyecto compilable** — Scaffold completado (Gradle Wrapper, recursos, iconos, dependencias Material3/Compose).
2. **IA real vía backend** — La app llama a `POST /api/interpret`. La clave `ANTHROPIC_API_KEY` **no** va en el APK (`LlmInterpreterStub` eliminado).
3. **Permiso `READ_CONTACTS`** — Onboarding + diálogo explicativo con `RequestPermission` la primera vez que hace falta en el chat.
4. **Contactos ambiguos / no encontrados** — Estados en `ChatViewModel` (elegir entre coincidencias o pedir número/correo).
5. **Onboarding** — 3 pasos la primera vez que se abre la app.
6. **Icono adaptable** — Desde `images/brand/app-icon-light.png` (`mipmap-anydpi-v26` + densidades).
7. **Ficha Play Store** — Texto en `play-store-listing.md` (sin publicar).
8. **Firma release** — Preparada en `app/build.gradle.kts` con variables de entorno (el keystore lo crea el cliente).

### Probado de verdad

| Qué | Resultado |
|-----|-----------|
| `gradlew assembleDebug` | **OK** — `BUILD SUCCESSFUL` (APK ~12,9 MB en `app/build/outputs/apk/debug/app-debug.apk`) |
| Emulador Android | **No arrancó en esta máquina** — el AVD pide ~7,4 GB libres para `userdata` y el disco C: tenía ~6,6–6,8 GB. Hay que liberar espacio o probar en un móvil físico. |
| Las 4 acciones con Claude de punta a punta | **No verificado** — falta desplegar Vercel con `ANTHROPIC_API_KEY` y probar en dispositivo |

### Pendiente del cliente (fuera del desarrollo)

- Liberar espacio / usar dispositivo real para la prueba visual
- Desplegar API en Vercel con `ANTHROPIC_API_KEY`
- Crear keystore y generar el `.aab` firmado
- Cuenta Google Play + subida manual
- Sustituir `[PENDIENTE: dato real de la empresa]` (CIF, etc.)
- Capturas de pantalla para la ficha

---

## Cómo abrir y ejecutar

1. Android Studio (Koala o superior) → **Open** → carpeta `SiempreContigoApp/`
2. Espera a que Gradle sincronice
3. Emulador o móvil con **Android 8.0 (API 26)+**
4. En `local.properties` (no se sube a Git):

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
api.base.url=https://tu-proyecto.vercel.app
```

Sin backend, la app abre el chat, pero al enviar texto verás el aviso de conexión.

Compilar debug por consola:

```powershell
cd SiempreContigoApp
.\gradlew.bat assembleDebug
```

---

## Backend (IA, sin clave en el APK)

En la raíz del monorepo (junto a la web):

- `api/interpret.js` — función serverless con las 4 tools del guion técnico
- `package.json` — dependencia `@anthropic-ai/sdk` (opcional / reserva)

**Proveedor activo ahora: Groq** (`llama-3.3-70b-versatile`), porque las claves
de Anthropic, OpenAI y Gemini disponibles en otros proyectos están sin crédito
o sin cuota. Las 4 tools se mantienen iguales. Si más adelante hay crédito en
Anthropic, se puede forzar con `LLM_PROVIDER=anthropic`.

### Despliegue en Vercel

1. Importa este repositorio en Vercel
2. Variables de entorno:
   - `GROQ_API_KEY` — obligatoria (proveedor actual)
   - `GROQ_MODEL` — opcional (por defecto `llama-3.3-70b-versatile`)
   - `LLM_PROVIDER` — opcional (`groq` | `anthropic`)
   - `ANTHROPIC_API_KEY` — opcional (si se vuelve a Claude)
3. Deploy
4. Pon la URL en `local.properties` → `api.base.url=...`

La app llama a: `{API_BASE_URL}/api/interpret`

---

## Generar un `.aab` firmado (release)

### 1. Crear el keystore (solo el cliente, una vez)

```powershell
keytool -genkey -v `
  -keystore C:\ruta\segura\siempre-contigo-upload.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias siemprecontigo
```

Guarda el archivo y las contraseñas **fuera de Git**.

### 2. Variables de entorno

```powershell
$env:SIEMPRE_CONTIGO_KEYSTORE = "C:\ruta\segura\siempre-contigo-upload.jks"
$env:SIEMPRE_CONTIGO_KEYSTORE_PASSWORD = "****"
$env:SIEMPRE_CONTIGO_KEY_ALIAS = "siemprecontigo"
$env:SIEMPRE_CONTIGO_KEY_PASSWORD = "****"
```

### 3. Generar el bundle

```powershell
cd SiempreContigoApp
.\gradlew.bat bundleRelease
```

Salida: `app/build/outputs/bundle/release/app-release.aab`

---

## Estructura principal

```
SiempreContigoApp/
  app/src/main/java/com/siemprecontigo/app/
    MainActivity.kt       # UI chat + onboarding + permiso
    ChatViewModel.kt      # Conversación, contactos, confirmación
    LlmClient.kt          # HTTP al backend
    ContactResolver.kt    # Agenda
    ActionExecutor.kt     # Intents (WhatsApp, mail, calendario, dial)
  play-store-listing.md
  README.md
```
