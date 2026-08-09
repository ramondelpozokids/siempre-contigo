// Formulario de contacto — envío real vía Formspree (AJAX, sin recargar la página)
export function initContactForm() {
  const form = document.getElementById('contactForm');
  const success = document.getElementById('contactSuccess');
  const error = document.getElementById('contactError');
  if (!form || !success) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    error && (error.hidden = true);

    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn ? submitBtn.textContent : '';
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Enviando…';
    }

    try {
      const response = await fetch(form.action, {
        method: 'POST',
        body: new FormData(form),
        headers: { Accept: 'application/json' },
      });

      if (response.ok) {
        form.hidden = true;
        success.hidden = false;
        success.focus();
      } else {
        throw new Error('Respuesta no válida del servidor');
      }
    } catch (err) {
      if (error) {
        error.hidden = false;
        error.focus();
      }
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
      }
    }
  });
}

document.addEventListener('DOMContentLoaded', initContactForm);
