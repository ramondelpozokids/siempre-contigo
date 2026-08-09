// =========================================
// Modo lectura fácil — texto grande y alto contraste
// =========================================

const EASY_READ_KEY = 'sc-easy-read';

export function initEasyRead() {
  const saved = localStorage.getItem(EASY_READ_KEY) === 'true';
  applyEasyRead(saved);

  let toggle = document.getElementById('easyReadToggle');
  if (!toggle) {
    const actions = document.querySelector('.nav__actions');
    if (actions) {
      toggle = document.createElement('button');
      toggle.type = 'button';
      toggle.id = 'easyReadToggle';
      toggle.className = 'easy-read-toggle';
      toggle.innerHTML =
        '<span class="easy-read-toggle__long">Texto grande</span><span class="easy-read-toggle__short" aria-hidden="true">Aa+</span>';
      toggle.setAttribute('aria-pressed', String(saved));
      toggle.setAttribute(
        'aria-label',
        saved
          ? 'Desactivar texto grande y alto contraste'
          : 'Activar texto grande y alto contraste'
      );
      actions.insertBefore(toggle, actions.firstChild);
    }
  } else {
    toggle.setAttribute('aria-pressed', String(saved));
  }

  toggle?.addEventListener('click', () => {
    const on = document.documentElement.getAttribute('data-easy-read') !== 'true';
    applyEasyRead(on);
    localStorage.setItem(EASY_READ_KEY, String(on));
    toggle.setAttribute('aria-pressed', String(on));
    toggle.setAttribute(
      'aria-label',
      on
        ? 'Desactivar texto grande y alto contraste'
        : 'Activar texto grande y alto contraste'
    );
  });
}

function applyEasyRead(on) {
  if (on) {
    document.documentElement.setAttribute('data-easy-read', 'true');
  } else {
    document.documentElement.removeAttribute('data-easy-read');
  }
}
