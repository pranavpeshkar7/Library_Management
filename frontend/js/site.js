/* Shared behaviour for the public pages: mobile menu + contact form. */

// ---- Mobile menu ----
(function () {
  const toggle = document.getElementById('navToggle');
  const nav = document.getElementById('siteNav');
  if (!toggle || !nav) return;

  function setOpen(open) {
    nav.classList.toggle('open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
  }

  toggle.addEventListener('click', () => setOpen(!nav.classList.contains('open')));
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') setOpen(false); });
  nav.addEventListener('click', (e) => { if (e.target.tagName === 'A') setOpen(false); });
})();

// ---- Contact form ----
// There is no server endpoint for messages, so the form composes an email in the
// visitor's own mail app. Change the address below (and the one in contact.html).
(function () {
  const form = document.getElementById('contactForm');
  if (!form) return;

  const DESK_EMAIL = 'librarian@readingroom.example';
  const note = document.getElementById('formNote');

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (!form.checkValidity()) { form.reportValidity(); return; }

    const name = document.getElementById('cName').value.trim();
    const email = document.getElementById('cEmail').value.trim();
    const topic = document.getElementById('cTopic').value;
    const message = document.getElementById('cMessage').value.trim();

    const subject = 'Reading Room: ' + topic;
    const body = message + '\r\n\r\n' + name + '\r\n' + email;

    const link = document.createElement('a');
    link.href = 'mailto:' + DESK_EMAIL +
      '?subject=' + encodeURIComponent(subject) +
      '&body=' + encodeURIComponent(body);
    document.body.appendChild(link);
    link.click(); // opens the visitor's email app
    link.remove();

    note.textContent = 'If your email app did not open, write to ' + DESK_EMAIL + ' instead.';
  });
})();
