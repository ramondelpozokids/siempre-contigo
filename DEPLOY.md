# Checklist: GitHub + Vercel (cuando salgas de solo-local)

## Estado actual
- [x] Web estática lista (rutas relativas, `vercel.json`)
- [x] Repositorio Git inicializado en esta carpeta
- [x] `.gitignore` (sin `.env` ni `.vercel`)
- [ ] Primer commit local
- [ ] Repositorio creado en GitHub
- [ ] `git push`
- [ ] Proyecto importado en Vercel

## 1. Commit local (PowerShell)

```powershell
cd "C:\Users\X\Desktop\SIEMPRE CONTIGO"
git add -A
git status
git commit -m "Web Siempre Contigo lista para Vercel"
```

## 2. GitHub

1. github.com → **New repository** → nombre ej. `siempre-contigo` → **sin** README (ya tienes uno).
2.:

```powershell
git remote add origin https://github.com/TU_USUARIO/siempre-contigo.git
git branch -M main
git push -u origin main
```

## 3. Vercel

1. vercel.com → **Add New** → **Project** → importa el repo.
2. **Framework Preset:** Other  
3. **Build Command:** (vacío)  
4. **Output Directory:** (vacío)  
5. **Deploy**

Prueba: `https://tu-proyecto.vercel.app/precios` y `/index.html`.

## 4. Dominio (opcional)

Vercel → Project → **Domains** → añade `siemprecontigo.app` (DNS según indique Vercel).

## Seguir en local

El acceso del Escritorio y `iniciar-local.bat` siguen sirviendo para desarrollo; producción no usa Python.

**API keys:** esta web no las necesita. Configúralas solo en Vercel cuando añadas backend (Variables de entorno, nunca en Git).
