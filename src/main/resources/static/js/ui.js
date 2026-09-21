/* Utilidades de interfaz compartidas por todas las páginas. */
const Ui = (() => {
  const CURRENCY = 'S/';
  const moneyFmt = new Intl.NumberFormat('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const SALE_STATUS = {
    1: { text: 'Pendiente', cls: 'badge-pendiente', color: '#f9a825' },
    2: { text: 'Pagado', cls: 'badge-pagado', color: '#2e7d32' },
    3: { text: 'Cancelado', cls: 'badge-anulado', color: '#ad1457' },
    4: { text: 'Expirado', cls: 'badge-expirado', color: '#78909c' },
  };
  const ROLE_TYPES = { 1: 'Administrador de empresa', 2: 'Empleado' };

  /** Escapa texto antes de insertarlo con innerHTML. */
  function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  function money(n) { return `${CURRENCY} ${moneyFmt.format(Number(n || 0))}`; }

  /** 'YYYY-MM-DD' → 'DD/MM/YYYY' */
  function date(iso) {
    if (!iso) return '—';
    const [y, m, d] = String(iso).slice(0, 10).split('-');
    return `${d}/${m}/${y}`;
  }
  function dateTime(iso) {
    if (!iso) return '—';
    return `${date(iso)} ${String(iso).slice(11, 16)}`;
  }
  /** 'YYYY-MM-DD' → 'sábado, 5 de octubre de 2026' */
  function longDate(iso) {
    const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString('es-PE',
      { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }
  function isoDate(dt) {
    const p = (n) => String(n).padStart(2, '0');
    return `${dt.getFullYear()}-${p(dt.getMonth() + 1)}-${p(dt.getDate())}`;
  }
  function todayIso() { return isoDate(new Date()); }

  function saleBadge(status) {
    const s = SALE_STATUS[status] || { text: '—', cls: '' };
    return `<span class="badge ${s.cls}">${s.text}</span>`;
  }
  function roleTypeName(type) { return ROLE_TYPES[type] || '—'; }

  /** Id numérico o uuid de la URL: Ui.pathParam(/\/facilities\/(\d+)\//) */
  function pathParam(regex) {
    const m = location.pathname.match(regex);
    return m ? decodeURIComponent(m[1]) : null;
  }

  function flash(message, type = 'success') {
    const box = document.getElementById('flash');
    if (!box) { window.alert(message); return; }
    const el = document.createElement('div');
    el.className = `alert alert-${type}`;
    el.textContent = message;
    box.prepend(el);
    setTimeout(() => el.remove(), 5000);
  }

  function clearErrors(form) {
    form.querySelectorAll('.invalid').forEach((e) => e.classList.remove('invalid'));
    form.querySelectorAll('.field-error').forEach((e) => e.remove());
    const general = form.querySelector('.form-error');
    if (general) { general.hidden = true; general.textContent = ''; }
  }

  /** Pinta los errores 422 {errors: {campo: [..]}} bajo cada campo del formulario. */
  function showErrors(form, err) {
    clearErrors(form);
    const errors = (err && err.errors) || {};
    let shown = 0;
    Object.entries(errors).forEach(([field, msgs]) => {
      const input = form.querySelector(`[data-error-for="${field}"]`) || form.querySelector(`[name="${field}"]`);
      if (!input) return;
      shown++;
      input.classList.add('invalid');
      const small = document.createElement('small');
      small.className = 'field-error';
      small.textContent = [].concat(msgs).join(' ');
      (input.closest('.form-group') || input.parentElement).appendChild(small);
    });
    if (shown) return;
    const general = form.querySelector('.form-error');
    if (general) {
      general.textContent = err.message;
      general.hidden = false;
    } else {
      flash(err.message, 'error');
    }
  }

  /** Muestra el error en el formulario (si hay) o como alerta. */
  function handleError(err, form) {
    if (form) showErrors(form, err);
    else flash(err.message || 'Ocurrió un error inesperado.', 'error');
  }

  /**
   * Lee los campos [name] del formulario.
   * Vacío → null; type=number → Number; checkbox con data-array → lista de valores marcados.
   */
  function formData(form) {
    const data = {};
    form.querySelectorAll('[name]').forEach((el) => {
      const name = el.name;
      if (el.type === 'checkbox' && el.dataset.array !== undefined) {
        data[name] = data[name] || [];
        if (el.checked) data[name].push(isNaN(el.value) ? el.value : Number(el.value));
        return;
      }
      if (el.type === 'checkbox') { data[name] = el.checked; return; }
      if (el.type === 'radio') { if (el.checked) data[name] = el.value; return; }
      const v = el.type === 'password' ? el.value : el.value.trim();
      if (v === '') { data[name] = null; return; }
      data[name] = (el.type === 'number' || el.dataset.type === 'number') ? Number(v) : v;
    });
    return data;
  }

  function fillForm(form, obj) {
    form.querySelectorAll('[name]').forEach((el) => {
      const v = obj[el.name];
      if (el.type === 'checkbox' && el.dataset.array !== undefined) {
        el.checked = Array.isArray(v) && v.map(String).includes(String(el.value));
      } else if (el.type === 'checkbox') {
        el.checked = !!v;
      } else if (el.type !== 'password') {
        el.value = v ?? '';
      }
    });
  }

  function resetForm(form) {
    form.reset();
    form.querySelectorAll('input[type=hidden]').forEach((el) => { el.value = ''; });
    clearErrors(form);
  }

  function modal(id) {
    return bootstrap.Modal.getOrCreateInstance(document.getElementById(id));
  }

  /** Modal de confirmación. Devuelve Promise<boolean>. */
  function confirm(message, { title = 'Confirmar', okText = 'Aceptar', danger = false } = {}) {
    return new Promise((resolve) => {
      const wrap = document.createElement('div');
      wrap.innerHTML = `
        <div class="modal fade" tabindex="-1">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title">${esc(title)}</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
              </div>
              <div class="modal-body"><p class="m-0">${esc(message)}</p></div>
              <div class="modal-footer">
                <button type="button" class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-ok>${esc(okText)}</button>
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
              </div>
            </div>
          </div>
        </div>`;
      const el = wrap.firstElementChild;
      document.body.appendChild(el);
      const m = new bootstrap.Modal(el);
      let ok = false;
      el.querySelector('[data-ok]').addEventListener('click', () => { ok = true; m.hide(); });
      el.addEventListener('hidden.bs.modal', () => { el.remove(); resolve(ok); });
      m.show();
    });
  }

  /** Deshabilita un botón mientras corre una petición. */
  function busy(btn, isBusy, text = 'Guardando...') {
    if (!btn) return;
    if (isBusy) {
      btn.dataset.originalText = btn.innerHTML;
      btn.textContent = text;
      btn.disabled = true;
    } else {
      if (btn.dataset.originalText) btn.innerHTML = btn.dataset.originalText;
      btn.disabled = false;
    }
  }

  function emptyRow(colspan, text) {
    return `<tr><td colspan="${colspan}" class="empty">${esc(text)}</td></tr>`;
  }
  function loadingRow(colspan) {
    return `<tr><td colspan="${colspan}" class="loading">Cargando...</td></tr>`;
  }

  /** Migas de pan: [{text, href}] (el último sin enlace). */
  function crumbs(items) {
    return items.map((it) => it.href
      ? `<a href="${esc(it.href)}">${esc(it.text)}</a>`
      : `<span>${esc(it.text)}</span>`).join('<span class="sep">›</span>');
  }

  return {
    CURRENCY, SALE_STATUS, esc, money, date, dateTime, longDate, isoDate, todayIso,
    saleBadge, roleTypeName, pathParam, flash, clearErrors, showErrors, handleError,
    formData, fillForm, resetForm, modal, confirm, busy, emptyRow, loadingRow, crumbs,
  };
})();
