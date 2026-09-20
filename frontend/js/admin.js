if (!Api.isLoggedIn()) {
  window.location.href = 'index.html';
}

const user = Api.currentUser();
if (!user || user.role !== 'LIBRARIAN') {
  window.location.href = 'dashboard.html';
}

document.getElementById('welcomeText').textContent = 'Hi, ' + (user ? user.name : '');
document.getElementById('roleTag').textContent = 'librarian';

document.getElementById('logoutBtn').addEventListener('click', async () => {
  try { await Api.post('/api/auth/logout'); } catch (e) { /* ignore */ }
  Api.clearSession();
  window.location.href = 'index.html';
});

async function loadCatalogue() {
  const container = document.getElementById('catalogueList');
  try {
    const resources = await Api.get('/api/resources');

    if (resources.length === 0) {
      container.innerHTML = '<div class="empty">The catalogue is empty.</div>';
      return;
    }

    container.innerHTML = resources.map(r => `
      <div class="entry">
        <div class="entry-main">
          <p class="entry-title">
            <span class="type-tag">${r.type}</span>${escapeHtml(r.title)}
          </p>
          <p class="entry-meta">${escapeHtml(r.author || 'Unknown author')} · ${r.borrowDurationDays}-day loan</p>
        </div>
        <div class="entry-actions">
          <span class="availability ${r.availableCopies === 0 ? 'zero' : ''}">
            ${r.availableCopies} / ${r.totalCopies} available
          </span>
          <button class="btn btn-small btn-danger" data-remove="${r.id}">Remove</button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('[data-remove]').forEach(btn => {
      btn.addEventListener('click', () => removeResource(btn.getAttribute('data-remove')));
    });
  } catch (err) {
    container.innerHTML = `<div class="empty">Could not load the catalogue: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadUsers() {
  const container = document.getElementById('usersList');
  try {
    const users = await Api.get('/api/admin/users');
    const nonLibrarians = users.filter(u => u.role !== 'LIBRARIAN');

    if (nonLibrarians.length === 0) {
      container.innerHTML = '<div class="empty">No student or teacher accounts yet.</div>';
      return;
    }

    container.innerHTML = nonLibrarians.map(u => `
      <div class="entry">
        <div class="entry-main">
          <p class="entry-title">${escapeHtml(u.name)}</p>
          <p class="entry-meta">
            <span class="type-tag">${u.role}</span>${escapeHtml(u.email)} · ${u.active ? 'active' : 'access revoked'}
          </p>
        </div>
        <div class="entry-actions">
          ${u.active
            ? `<button class="btn btn-small btn-danger" data-revoke="${u.id}">Revoke access</button>`
            : `<button class="btn btn-small btn-outline" data-restore="${u.id}">Restore access</button>`}
        </div>
      </div>
    `).join('');

    container.querySelectorAll('[data-revoke]').forEach(btn => {
      btn.addEventListener('click', () => setUserAccess(btn.getAttribute('data-revoke'), false));
    });
    container.querySelectorAll('[data-restore]').forEach(btn => {
      btn.addEventListener('click', () => setUserAccess(btn.getAttribute('data-restore'), true));
    });
  } catch (err) {
    container.innerHTML = `<div class="empty">Could not load users: ${escapeHtml(err.message)}</div>`;
  }
}

document.getElementById('addResourceForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('addResMsg');
  const title = document.getElementById('resTitle').value.trim();
  const author = document.getElementById('resAuthor').value.trim();
  const type = document.getElementById('resType').value;
  const totalCopies = parseInt(document.getElementById('resCopies').value, 10) || 1;

  try {
    await Api.post('/api/resources', { title, author, type, totalCopies });
    msg.className = 'form-msg show ok';
    msg.textContent = 'Added to catalogue.';
    document.getElementById('addResourceForm').reset();
    document.getElementById('resCopies').value = 1;
    await loadCatalogue();
  } catch (err) {
    msg.className = 'form-msg show error';
    msg.textContent = err.message;
  }
});

async function removeResource(id) {
  if (!confirm('Remove this item from the catalogue?')) return;
  try {
    await Api.del('/api/resources?id=' + encodeURIComponent(id));
    await loadCatalogue();
  } catch (err) {
    alert(err.message);
  }
}

async function setUserAccess(id, active) {
  const path = (active ? '/api/admin/users/restore' : '/api/admin/users/revoke') + '?id=' + encodeURIComponent(id);
  try {
    await Api.put(path);
    await loadUsers();
  } catch (err) {
    alert(err.message);
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : String(str);
  return div.innerHTML;
}

loadCatalogue();
loadUsers();
