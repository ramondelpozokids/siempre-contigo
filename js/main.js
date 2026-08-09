// =========================================
// SIEMPRE CONTIGO — Main entry (module)
// =========================================

import { initScrollReveal } from './animations.js';
import { initTheme } from './theme.js';
import { initChats } from './chat.js';
import { initFaq } from './faq.js';
import { initNav, initParallax, initSmoothScroll, initDemoButton } from './interactions.js';

import { initEasyRead } from './easy-read.js';

document.addEventListener('DOMContentLoaded', () => {
  initEasyRead();
  initTheme();
  initNav();
  initScrollReveal();
  initParallax();
  initSmoothScroll();
  initDemoButton();
  initChats();
  initFaq();
});