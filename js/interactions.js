// =========================================
// SIEMPRE CONTIGO — Nav, parallax, smooth scroll
// =========================================

export function initNav() {
  const nav = document.querySelector('.nav');
  if (!nav) return;

  let ticking = false;
  window.addEventListener('scroll', () => {
    if (!ticking) {
      window.requestAnimationFrame(() => {
        nav.classList.toggle('scrolled', window.scrollY > 20);
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });

  initMobileNav(nav);
}

function initMobileNav(nav) {
  const toggle = nav.querySelector('.nav__menu-btn');
  const menu = nav.querySelector('.nav__links');
  if (!toggle || !menu) return;

  const close = () => {
    toggle.setAttribute('aria-expanded', 'false');
    menu.classList.remove('nav__links--open');
    document.body.classList.remove('nav-open');
  };

  toggle.addEventListener('click', () => {
    const open = toggle.getAttribute('aria-expanded') !== 'true';
    toggle.setAttribute('aria-expanded', String(open));
    menu.classList.toggle('nav__links--open', open);
    document.body.classList.toggle('nav-open', open);
  });

  menu.querySelectorAll('a').forEach((link) => {
    link.addEventListener('click', close);
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') close();
  });
}

export function initParallax() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  const orbs = document.querySelectorAll('.orb');
  if (!orbs.length) return;

  let scrollY = 0;
  let ticking = false;

  window.addEventListener('scroll', () => {
    scrollY = window.scrollY;
    if (!ticking) {
      window.requestAnimationFrame(() => {
        orbs.forEach((orb, i) => {
          const speed = 0.05 * (i + 1);
          orb.style.transform = `translate3d(0, ${scrollY * speed}px, 0)`;
        });
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });
}

export function initDemoButton() {
  const btn = document.getElementById('demoBtn');
  const target = document.querySelector('.demos') || document.getElementById('demos');
  if (!btn || !target) return;

  btn.addEventListener('click', () => {
    const top = target.getBoundingClientRect().top + window.scrollY - 80;
    window.scrollTo({ top, behavior: 'smooth' });
    target.setAttribute('tabindex', '-1');
    target.focus({ preventScroll: true });
  });
}

export function initSmoothScroll() {
  document.querySelectorAll('a[href^="#"], a[href*="index.html#"]').forEach((link) => {
    link.addEventListener('click', (e) => {
      const raw = link.getAttribute('href');
      let id = raw;
      const path = location.pathname.replace(/\\/g, '/');
      const onIndex =
        path === '/' ||
        path.endsWith('/index.html') ||
        path.endsWith('/');
      if (raw.includes('index.html#')) {
        if (!onIndex) return;
        id = raw.slice(raw.indexOf('#'));
      }
      if (!id || id.length <= 1 || id[0] !== '#') return;
      const target = document.querySelector(id);
      if (target) {
        e.preventDefault();
        const top = target.getBoundingClientRect().top + window.scrollY - 80;
        window.scrollTo({ top, behavior: 'smooth' });
        target.setAttribute('tabindex', '-1');
        target.focus({ preventScroll: true });
      }
    });
  });
}