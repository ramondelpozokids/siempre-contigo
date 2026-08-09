# Siempre Contigo — Web

Landing y páginas legales del asistente para personas mayores. Sitio **estático** (HTML, CSS, JavaScript modular).

## Ver en local (ahora)

1. **Recomendado:** doble clic en `Siempre Contigo.lnk` o `Siempre Contigo - Abrir web.bat` en el Escritorio.  
2. O en esta carpeta: `iniciar-local.bat` / `servir-local.ps1`  
3. Abre **http://127.0.0.1:8080/index.html**  
4. Necesitas **Python 3** (`python` o `py` en PATH).

No abras `index.html` con doble clic desde el explorador: los módulos ES requieren HTTP.

## Publicar en GitHub (cuando quieras)

```powershell
cd "C:\Users\X\Desktop\SIEMPRE CONTIGO"
git init
git add .
git commit -m "Initial commit: web Siempre Contigo"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/siempre-contigo.git
git push -u origin main
```

Sustituye la URL del remoto por tu repositorio. No subas `.env` ni claves (ya están en `.gitignore`).

## Publicar en Vercel (después de GitHub)

1. [vercel.com](https://vercel.com) → **Add New Project** → importa el repo de GitHub.  
2. **Framework Preset:** Other (sitio estático).  
3. **Root Directory:** `./` (raíz del repo).  
4. **Build Command:** vacío. **Output Directory:** vacío (Vercel sirve los archivos tal cual).  
5. Deploy. La URL será `https://tu-proyecto.vercel.app`.

`vercel.json` en la raíz configura URLs limpias (`/precios` → `precios.html`).

### Variables de entorno

Esta web **no usa API keys** en producción. Cuando añadas backend o formularios reales, configura variables solo en el panel de Vercel (nunca en el repo).

## Estructura

| Ruta | Descripción |
|------|-------------|
| `index.html` | Página principal |
| `precios.html` | Tarifas |
| `contacto.html` | Contacto (formulario demo local) |
| `css/`, `js/`, `images/` | Assets |
| `ayuda/`, `blog/` | Subsecciones |

## Scripts locales

| Archivo | Uso |
|---------|-----|
| `abrir-web.ps1` | Abre navegador; levanta servidor si hace falta |
| `servir-local.ps1` | Servidor Python puerto 8080 |
