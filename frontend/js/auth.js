// If already logged in, skip straight to the right dashboard.
if (Api.isLoggedIn()) {
  const user = Api.currentUser();
  window.location.href = (user && user.role === 'LIBRARIAN') ? 'admin.html' : 'dashboard.html';
}

const tabLogin = document.getElementById('tabLogin');
const tabSignup = document.getElementById('tabSignup');
const loginForm = document.getElementById('loginForm');
const signupForm = document.getElementById('signupForm');

tabLogin.addEventListener('click', () => {
  tabLogin.classList.add('active');
  tabSignup.classList.remove('active');
  loginForm.style.display = 'block';
  signupForm.style.display = 'none';
});

tabSignup.addEventListener('click', () => {
  tabSignup.classList.add('active');
  tabLogin.classList.remove('active');
  signupForm.style.display = 'block';
  loginForm.style.display = 'none';
});

function showMsg(el, text, ok) {
  el.textContent = text;
  el.className = 'form-msg show ' + (ok ? 'ok' : 'error');
}

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('loginMsg');
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;

  try {
    const data = await Api.post('/api/auth/login', { email, password });
    Api.setSession(data.token, { id: data.id, name: data.name, role: data.role });
    window.location.href = data.role === 'LIBRARIAN' ? 'admin.html' : 'dashboard.html';
  } catch (err) {
    showMsg(msg, err.message, false);
  }
});

signupForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const msg = document.getElementById('signupMsg');
  const name = document.getElementById('signupName').value.trim();
  const email = document.getElementById('signupEmail').value.trim();
  const password = document.getElementById('signupPassword').value;
  const role = document.getElementById('signupRole').value;

  try {
    await Api.post('/api/auth/signup', { name, email, password, role });
    showMsg(msg, 'Account created — you can sign in now.', true);
    signupForm.reset();
    setTimeout(() => tabLogin.click(), 900);
  } catch (err) {
    showMsg(msg, err.message, false);
  }
});
