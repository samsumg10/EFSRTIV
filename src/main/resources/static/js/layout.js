/*
 * Layout de los paneles admin y empresa (sidebar + topbar), como layout.html de asociacion-frontend.
 * Cada página tiene <div id="page" class="page" hidden> y llama:
 *   const user = Layout.init({ area: 'company', menu: 'facilities' });
 *   if (!user) return;   // se está redirigiendo al login
 */
const Layout = (() => {
  const MENUS = {
    admin: [
      { label: 'Plataforma', items: [
        { key: 'companies', icon: '🏢', text: 'Empresas', href: '/admin/companies' },
        { key: 'roles', icon: '🛡️', text: 'Roles', href: '/admin/roles' },
      ] },
    ],
    company: [
      { label: 'Empresa', items: [
        { key: 'profile', icon: '👤', text: 'Perfil', href: '/company/profile', adminOnly: true },
        { key: 'employees', icon: '👥', text: 'Empleados', href: '/company/employees', adminOnly: true },
      ] },
      { label: 'Operación', items: [
        { key: 'facilities', icon: '🏕️', text: 'Instalaciones', href: '/company/facilities' },
      ] },
    ],
  };
  const SIDEBAR_KEY = 'bbq_sidebar_closed';

  /** Lee el payload del JWT (UTF-8). */
  function decodeToken(token) {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(part), (c) => c.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes));
  }

  function currentUser(area) {
    const token = Api.getToken(area);
    if (!token) return null;
    try {
      const payload = decodeToken(token);
      if (!payload.exp || payload.exp * 1000 < Date.now()) return null;
      return payload;
    } catch {
      return null;
    }
  }

  function logout(area) {
    Api.clearToken(area);
    location.href = `/${area}/login`;
  }

  function roleLabel(area, user) {
    if (area === 'admin') return 'Administrador de plataforma';
    return `${Ui.roleTypeName(user.roleType)} · ${user.companyName || ''}`;
  }

  function init({ area, menu, adminOnly = false }) {
    const user = currentUser(area);
    if (!user) {
      Api.clearToken(area);
      location.replace(`/${area}/login`);
      return null;
    }
    const isCompanyAdmin = area === 'admin' || user.roleType === 1;
    if (adminOnly && !isCompanyAdmin) {
      location.replace('/company/facilities');
      return null;
    }

    const nav = MENUS[area].map((group) => {
      const items = group.items.filter((it) => !it.adminOnly || isCompanyAdmin);
      if (!items.length) return '';
      return `<div class="nav-group">
        <span class="nav-label">${Ui.esc(group.label)}</span>
        ${items.map((it) => `
          <a href="${it.href}" class="nav-item ${it.key === menu ? 'active' : ''}" title="${Ui.esc(it.text)}">
            <span class="nav-icon">${it.icon}</span><span class="nav-text">${Ui.esc(it.text)}</span>
          </a>`).join('')}
      </div>`;
    }).join('');

    let closed = false;
    try { closed = localStorage.getItem(SIDEBAR_KEY) === '1'; } catch { /* sin almacenamiento */ }
    if (window.innerWidth <= 768) closed = true;

    const shell = document.createElement('div');
    shell.className = 'app-shell' + (closed ? ' sidebar-cerrado' : '');
    shell.innerHTML = `
      <aside class="sidebar">
        <div class="sidebar-header">
          <span class="sidebar-logo">🔥</span>
          <span class="sidebar-title">BBQ Reservas</span>
        </div>
        <nav class="sidebar-nav">${nav}</nav>
        <div class="sidebar-footer">
          <button class="btn-logout" type="button"><span>🚪</span><span class="nav-text">Cerrar Sesión</span></button>
        </div>
      </aside>
      <div class="main-content">
        <header class="topbar">
          <button class="btn-toggle" type="button" aria-label="Menú">☰</button>
          <div class="topbar-user">
            <span class="user-name">${Ui.esc(user.name)}</span>
            <span class="user-role">${Ui.esc(roleLabel(area, user))}</span>
          </div>
        </header>
        <main class="page-content"><div id="flash"></div></main>
      </div>`;

    const page = document.getElementById('page');
    document.body.prepend(shell);
    shell.querySelector('.page-content').appendChild(page);
    page.hidden = false;

    shell.querySelector('.btn-toggle').addEventListener('click', () => {
      const isClosed = shell.classList.toggle('sidebar-cerrado');
      try { localStorage.setItem(SIDEBAR_KEY, isClosed ? '1' : '0'); } catch { /* sin almacenamiento */ }
    });
    shell.querySelector('.btn-logout').addEventListener('click', () => logout(area));

    return user;
  }

  return { init, currentUser, logout };
})();
