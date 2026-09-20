if (!Api.isLoggedIn()) {
  window.location.href = 'index.html';
}

const user = Api.currentUser();
if (user && user.role === 'LIBRARIAN') {
  window.location.href = 'admin.html';
}

document.getElementById('welcomeText').textContent = 'Hi, ' + (user ? user.name : '');
document.getElementById('roleTag').textContent = user ? user.role.toLowerCase() : '';

document.getElementById('logoutBtn').addEventListener('click', async () => {
  try { await Api.post('/api/auth/logout'); } catch (e) { /* ignore */ }
  Api.clearSession();
  window.location.href = 'index.html';
});

let resourceTitleById = {};

async function loadMyBorrows() {
  const container = document.getElementById('myBorrowsList');
  try {
    const records = await Api.get('/api/my-borrows');
    const active = records.filter(r => r.active);

    if (active.length === 0) {
      container.innerHTML = '<div class="empty">You have nothing borrowed right now.</div>';
      return;
    }

    container.innerHTML = active.map(r => `
      <div class="entry">
        <div class="entry-main">
          <p class="entry-title">${escapeHtml(resourceTitleById[r.resourceId] || r.resourceId)}</p>
          <p class="entry-meta">Due ${r.dueDate}</p>
        </div>
        <div class="entry-actions">
          <button class="btn btn-small btn-outline" data-return="${r.resourceId}">Return</button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('[data-return]').forEach(btn => {
      btn.addEventListener('click', () => returnResource(btn.getAttribute('data-return')));
    });
  } catch (err) {
    container.innerHTML = `<div class="empty">Could not load your loans: ${escapeHtml(err.message)}</div>`;
  }
}

async function loadCatalogue() {
  const container = document.getElementById('catalogueList');
  try {
    const resources = await Api.get('/api/resources');
    resourceTitleById = {};
    resources.forEach(r => { resourceTitleById[r.id] = r.title; });

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
          <button class="btn btn-small" data-borrow="${r.id}" ${r.availableCopies === 0 ? 'disabled' : ''}>
            Borrow
          </button>
        </div>
      </div>
    `).join('');

    container.querySelectorAll('[data-borrow]').forEach(btn => {
      btn.addEventListener('click', () => borrowResource(btn.getAttribute('data-borrow')));
    });
  } catch (err) {
    container.innerHTML = `<div class="empty">Could not load the catalogue: ${escapeHtml(err.message)}</div>`;
  }
}

async function borrowResource(resourceId) {
  try {
    await Api.post('/api/borrow', { resourceId });
    await Promise.all([loadCatalogue(), loadMyBorrows()]);
  } catch (err) {
    alert(err.message);
  }
}

async function returnResource(resourceId) {
  try {
    await Api.post('/api/return', { resourceId });
    await Promise.all([loadCatalogue(), loadMyBorrows()]);
  } catch (err) {
    alert(err.message);
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : String(str);
  return div.innerHTML;
}

loadCatalogue().then(loadMyBorrows);
