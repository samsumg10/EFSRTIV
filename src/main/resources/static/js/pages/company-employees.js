/* /company/employees — CRUD de empleados con rol e instalaciones asignadas. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'employees', adminOnly: true });
  if (!user) return;

  const API = '/api/company/employees';
  const rows = document.getElementById('rows');
  const search = document.getElementById('search');
  const form = document.getElementById('employee-form');
  const roleSelect = document.getElementById('role-select');
  const facilityChecks = document.getElementById('facility-checks');
  const modal = Ui.modal('employee-modal');
  let employees = [];
  let roles = [];

  function roleType(roleId) {
    const r = roles.find((x) => String(x.id) === String(roleId));
    return r ? r.type : null;
  }

  function toggleFacilities() {
    const isAdmin = roleType(roleSelect.value) === 1;
    document.getElementById('facilities-group').hidden = isAdmin;
    document.getElementById('admin-hint').hidden = !isAdmin;
  }

  async function loadCatalogs() {
    const [r, facilities] = await Promise.all([Api.get('/api/company/roles'), Api.get('/api/company/facilities')]);
    roles = r;
    roleSelect.innerHTML = roles.map((x) =>
      `<option value="${x.id}">${Ui.esc(x.name)} (${Ui.esc(Ui.roleTypeName(x.type))})</option>`).join('');
    facilityChecks.innerHTML = facilities.length
      ? facilities.map((f) => `<label><input type="checkbox" name="facilityIds" data-array value="${f.id}"> ${Ui.esc(f.name)}</label>`).join('')
      : '<span class="muted">Aún no hay instalaciones.</span>';
  }

  async function load() {
    rows.innerHTML = Ui.loadingRow(6);
    try {
      employees = await Api.get(API);
      render();
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(6, err.message);
    }
  }

  function render() {
    const q = search.value.trim().toLowerCase();
    const list = employees.filter((e) => !q || [e.name, e.email].some((v) => (v || '').toLowerCase().includes(q)));
    if (!list.length) {
      rows.innerHTML = Ui.emptyRow(6, 'No se encontraron empleados.');
      return;
    }
    rows.innerHTML = list.map((e) => `
      <tr>
        <td><strong>${Ui.esc(e.name)}</strong>${e.me ? ' <span class="badge badge-activo">Tú</span>' : ''}</td>
        <td>${Ui.esc(e.email)}</td>
        <td>${Ui.esc(e.phone || '—')}</td>
        <td><span class="badge badge-rol-${e.roleType}">${Ui.esc(e.roleName)}</span></td>
        <td>${e.roleType === 1 ? '<span class="muted">Todas</span>'
          : (e.facilityNames.length ? Ui.esc(e.facilityNames.join(', ')) : '<span class="muted">Ninguna</span>')}</td>
        <td class="acciones">
          <button class="btn btn-sm btn-secondary" data-edit="${e.id}">Editar</button>
          ${e.me ? '' : `<button class="btn btn-sm btn-danger" data-delete="${e.id}">Eliminar</button>`}
        </td>
      </tr>`).join('');
  }

  function openForm(employee) {
    Ui.resetForm(form);
    const isNew = !employee;
    form.querySelector('.modal-title').textContent = isNew ? 'Nuevo Empleado' : 'Editar Empleado';
    form.querySelectorAll('.password-required').forEach((el) => { el.hidden = !isNew; });
    document.getElementById('password-hint').textContent = isNew
      ? 'Mínimo 8 caracteres.' : 'Déjala vacía para no cambiarla.';
    if (employee) Ui.fillForm(form, employee);
    else {
      const employeeRole = roles.find((r) => r.type === 2);
      if (employeeRole) roleSelect.value = employeeRole.id;
    }
    toggleFacilities();
    modal.show();
  }

  roleSelect.addEventListener('change', toggleFacilities);
  search.addEventListener('input', render);
  document.getElementById('btn-new').addEventListener('click', () => openForm(null));

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(employees.find((x) => String(x.id) === edit));
    if (del) {
      const emp = employees.find((x) => String(x.id) === del);
      const ok = await Ui.confirm(`¿Eliminar al empleado "${emp.name}"? Ya no podrá ingresar.`,
        { title: 'Eliminar empleado', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Empleado eliminado.');
        load();
      } catch (err) {
        Ui.handleError(err);
      }
    }
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = Ui.formData(form);
    if (roleType(data.roleId) === 1) data.facilityIds = [];
    const btn = form.querySelector('button[type=submit]');
    Ui.busy(btn, true);
    try {
      if (data.id) await Api.put(`${API}/${data.id}`, data);
      else await Api.post(API, data);
      modal.hide();
      Ui.flash(data.id ? 'Empleado actualizado.' : 'Empleado registrado.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  loadCatalogs().then(load).catch((err) => { rows.innerHTML = Ui.emptyRow(6, err.message); });
})();
