// =========================================
// SIEMPRE CONTIGO — Chat demo animations
// =========================================

export function initChats() {
  const chats = document.querySelectorAll('[data-chat]');

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting && !entry.target.dataset.started) {
          entry.target.dataset.started = 'true';
          runChat(entry.target);
        }
      });
    },
    { threshold: 0.4 }
  );

  chats.forEach((chat) => observer.observe(chat));
}

function runChat(container) {
  const msgs = container.querySelectorAll('.chat-msg');
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (reduced) {
    msgs.forEach((msg) => msg.classList.add('show'));
    return;
  }

  let delay = 0;
  msgs.forEach((msg) => {
    const isUser = msg.classList.contains('chat-msg--user');
    const typingTime = isUser ? 600 : 900;
    setTimeout(() => {
      typeMessage(msg, typingTime);
    }, delay);
    delay += typingTime + 400;
  });
}

function typeMessage(msg, duration) {
  const text = (msg.textContent || '').trim();
  if (!text.length) {
    msg.classList.add('show');
    return;
  }
  msg.textContent = '';
  msg.classList.add('show');
  let i = 0;
  const step = duration / text.length;

  function tick() {
    if (i < text.length) {
      msg.textContent += text[i];
      i++;
      setTimeout(tick, step);
    }
  }
  tick();
}