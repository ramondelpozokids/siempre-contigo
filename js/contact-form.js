// Formulario de contacto (solo local / demo — sin backend)
export function initContactForm() {
  const form = document.getElementById('contactForm');
  const success = document.getElementById('contactSuccess');
  if (!form || !success) return;

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    form.hidden = true;
    success.hidden = false;
    success.focus();
  });
}

document.addEventListener('DOMContentLoaded', initContactForm);
