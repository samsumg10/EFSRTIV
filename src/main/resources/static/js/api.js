/*
 * Cliente HTTP de la API (/api/**).
 * Guarda el JWT en localStorage (igual que asociacion-frontend), un token por área.
 */
const Api = (() => {
  const TOKEN_KEYS = { admin: 'bbq_admin_token', company: 'bbq_company_token' };

  class ApiError extends Error {
    constructor(status, message, errors) {
      super(message);
      this.status = status;
      this.errors = errors || {};
    }
  }

  /** Área según la URL de la página: admin, company o public (portal). */
  function area() {
    const p = location.pathname;
    if (p === '/admin' || p.startsWith('/admin/')) return 'admin';
    if (p === '/company' || p.startsWith('/company/')) return 'company';
    return 'public';
  }

  function getToken(a = area()) {
    try { return TOKEN_KEYS[a] ? localStorage.getItem(TOKEN_KEYS[a]) : null; } catch { return null; }
  }
  function setToken(a, token) {
    try { localStorage.setItem(TOKEN_KEYS[a], token); } catch { /* sin almacenamiento */ }
  }
  function clearToken(a) {
    try { localStorage.removeItem(TOKEN_KEYS[a]); } catch { /* sin almacenamiento */ }
  }

  async function request(method, url, body) {
    const headers = { Accept: 'application/json' };
    const a = area();
    const token = a === 'public' ? null : getToken(a);
    if (token) headers.Authorization = 'Bearer ' + token;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    let res;
    try {
      res = await fetch(url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
    } catch {
      throw new ApiError(0, 'No se pudo conectar con el servidor.', {});
    }

    let data = null;
    const text = await res.text();
    if (text) {
      try { data = JSON.parse(text); } catch { data = null; }
    }

    if (res.status === 401 && a !== 'public' && !url.endsWith('/login')) {
      clearToken(a);
      location.href = `/${a}/login`;
      throw new ApiError(401, (data && data.message) || 'Sesión expirada.', {});
    }
    if (!res.ok) {
      throw new ApiError(res.status, (data && data.message) || 'Ocurrió un error inesperado.', data && data.errors);
    }
    return data;
  }

  /** Arma un query string ignorando valores vacíos. */
  function qs(params) {
    const q = new URLSearchParams();
    Object.entries(params || {}).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') q.append(k, v);
    });
    const s = q.toString();
    return s ? '?' + s : '';
  }

  return {
    get: (url) => request('GET', url),
    post: (url, body) => request('POST', url, body ?? {}),
    put: (url, body) => request('PUT', url, body ?? {}),
    patch: (url, body) => request('PATCH', url, body ?? {}),
    del: (url) => request('DELETE', url),
    qs, area, getToken, setToken, clearToken, ApiError,
  };
})();
