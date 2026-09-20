/* Thin fetch wrapper — every call goes through here so the auth token
   header and JSON handling live in exactly one place. */

const Api = (() => {
  function token() {
    return localStorage.getItem('authToken');
  }

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const t = token();
    if (t) headers['X-Auth-Token'] = t;

    const res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    const text = await res.text();
    const data = text ? JSON.parse(text) : {};

    if (!res.ok) {
      // Sessions live in server memory, so a server restart (or revoked account) invalidates the
      // token stored in this browser. Send the user back to the login page instead of leaving
      // them on a page full of errors. (Not for the login call itself: 401 there = wrong password.)
      if (res.status === 401 && path !== '/api/auth/login') {
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
        window.location.href = 'index.html';
      }
      const message = data.error || `Request failed (${res.status})`;
      throw new Error(message);
    }
    return data;
  }

  return {
    get: (path) => request('GET', path),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    del: (path) => request('DELETE', path),

    setSession(token, user) {
      localStorage.setItem('authToken', token);
      localStorage.setItem('user', JSON.stringify(user));
    },
    clearSession() {
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
    },
    currentUser() {
      const raw = localStorage.getItem('user');
      return raw ? JSON.parse(raw) : null;
    },
    isLoggedIn() {
      return !!token();
    },
  };
})();
