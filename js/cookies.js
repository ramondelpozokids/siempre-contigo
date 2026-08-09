// =========================================
// SIEMPRE CONTIGO — Banner de cookies
// =========================================

const COOKIE_KEY = 'sc-cookies';

export function initCookieBanner() {
  const banner = document.getElementById('cookieBanner');
  if (!banner) return;

  const saved = localStorage.getItem(COOKIE_KEY);
  if (saved) return; // Ya ha elegido

  // Mostrar banner después de 1.5s
  setTimeout(() => banner.classList.add('visible'), 1500);

  const accept = document.getElementById('cookieAccept');
  const reject = document.getElementById('cookieReject');
  const configure = document.getElementById('cookieConfigure');

  if (accept) {
    accept.addEventListener('click', () => {
      savePreferences('all');
      hideBanner(banner);
    });
  }

  if (reject) {
    reject.addEventListener('click', () => {
      savePreferences('essential');
      hideBanner(banner);
    });
  }

  if (configure) {
    configure.addEventListener('click', () => {
      // Aquí se abriría un modal de configuración avanzada
      savePreferences('all');
      hideBanner(banner);
    });
  }
}

function savePreferences(level) {
  const prefs = {
    level,
    date: new Date().toISOString(),
    version: '2.4',
  };
  localStorage.setItem(COOKIE_KEY, JSON.stringify(prefs));
  document.documentElement.dataset.cookies = level;
}

function hideBanner(banner) {
  banner.classList.remove('visible');
  setTimeout(() => banner.remove(), 700);
}

// API pública para botón "Configurar cookies" en la política
window.openCookiePreferences = function () {
  localStorage.removeItem(COOKIE_KEY);
  location.reload();
};

document.addEventListener('DOMContentLoaded', initCookieBanner);