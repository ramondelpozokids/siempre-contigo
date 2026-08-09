/**
 * Aviso sin servidor local (file:// no carga módulos ES).
 * Script clásico — debe incluirse antes de type="module".
 */
(function () {
  try {
    if (localStorage.getItem('sc-easy-read') === 'true') {
      document.documentElement.setAttribute('data-easy-read', 'true');
    }
  } catch (e) { /* localStorage no disponible */ }

  if (location.protocol !== 'file:') return;

  function showBanner() {
    if (document.getElementById('sc-file-protocol-banner')) return;

    var bar = document.createElement('div');
    bar.id = 'sc-file-protocol-banner';
    bar.setAttribute('role', 'alert');
    bar.style.cssText =
      'position:fixed;top:0;left:0;right:0;z-index:9999;padding:14px 20px;' +
      'background:#1e293b;color:#f8fafc;font:600 14px/1.5 Inter,system-ui,sans-serif;' +
      'text-align:center;box-shadow:0 4px 24px rgba(0,0,0,.25);';
    bar.innerHTML =
      'Esta web necesita un servidor local. Ejecuta <strong style="color:#93c5fd">iniciar-local.bat</strong> ' +
      'o usa el botón <strong style="color:#93c5fd">Crear acceso en el Escritorio</strong> (con el servidor ya abierto en localhost).';

    document.body.prepend(bar);
    document.documentElement.style.scrollPaddingTop = '52px';
  }

  if (document.body) showBanner();
  else document.addEventListener('DOMContentLoaded', showBanner);
})();

(function () {
  function isLocalDev() {
    var h = location.hostname;
    return h === '127.0.0.1' || h === 'localhost' || location.protocol === 'file:';
  }

  function installDesktopButton() {
    if (!isLocalDev() || document.getElementById('sc-desktop-shortcut-btn')) return;

    var wrap = document.createElement('a');
    wrap.id = 'sc-desktop-shortcut-btn';
    var inSub = /\/(blog|ayuda)\//.test(location.pathname);
    var prefix = inSub ? '../' : '';

    wrap.href = prefix + 'crear-acceso-escritorio.bat';
    wrap.setAttribute('role', 'button');
    wrap.title = 'Crea accesos en el Escritorio con el icono de la app';
    wrap.style.cssText =
      'position:fixed;bottom:20px;left:20px;z-index:9998;display:flex;align-items:center;gap:12px;' +
      'padding:10px 14px 10px 10px;background:#fff;color:#0f172a;font:600 13px/1.25 Inter,system-ui,sans-serif;' +
      'border-radius:16px;text-decoration:none;box-shadow:0 12px 40px rgba(15,23,42,.18);' +
      'border:1px solid #e2e8f0;max-width:min(340px,calc(100vw - 40px));';

    var img = document.createElement('img');
    img.src = prefix + 'images/brand/app-icon-light.png';
    img.alt = '';
    img.width = 48;
    img.height = 48;
    img.style.cssText = 'display:block;border-radius:12px;flex-shrink:0;';

    var text = document.createElement('span');
    text.innerHTML = '<strong style="display:block;font-size:14px;margin-bottom:2px;">Acceso en el Escritorio</strong>' +
      '<span style="font-weight:500;color:#64748b;font-size:12px;">Icono de app en los 2 accesos directos</span>';

    wrap.appendChild(img);
    wrap.appendChild(text);

    wrap.addEventListener('click', function (e) {
      if (!window.confirm(
        'Se abrira o descargara crear-acceso-escritorio.bat.\n\n' +
          'Ejecutalo para crear en el Escritorio:\n' +
          '- Siempre Contigo.lnk\n' +
          '- Siempre Contigo - Servidor.lnk\n\n' +
          'Ambos con el icono de la app.\n\nContinuar?'
      )) {
        e.preventDefault();
      }
    });

    document.body.appendChild(wrap);
  }

  if (document.body) installDesktopButton();
  else document.addEventListener('DOMContentLoaded', installDesktopButton);
})();
