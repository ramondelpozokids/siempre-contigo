// =========================================
// SIEMPRE CONTIGO — Theme manager
// =========================================

const THEME_KEY = 'sc-theme';

export function initTheme() {
  const saved = localStorage.getItem(THEME_KEY);
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const initial = saved || (prefersDark ? 'dark' : 'light');
  applyTheme(initial);

  const toggle = document.getElementById('themeToggle');
  if (toggle) {
    toggle.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      applyTheme(next);
    });
  }

  window.matchMedia('(prefers-color-scheme: dark)')
    .addEventListener('change', (e) => {
      if (!localStorage.getItem(THEME_KEY)) {
        applyTheme(e.matches ? 'dark' : 'light');
      }
    });
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme);
  localStorage.setItem(THEME_KEY, theme);
  document.querySelectorAll('meta[name="theme-color"]').forEach((meta) => {
    meta.setAttribute('content', theme === 'dark' ? '#0f172a' : '#ffffff');
  });

  document.querySelectorAll('.nav__logo-icon').forEach((img) => {
    const src = img.getAttribute('src') || '';
    const base = src.includes('../images/brand/') ? '../images/brand/' : 'images/brand/';
    img.setAttribute('src', base + 'logo-isotipo-nav.png?v=10');
  });
}
