/* /admin/roles — CRUD de roles (nombre + tipo). */
(function () {
  const user = Layout.init({ area: 'admin', menu: 'roles' });
  if (!user) return;

  const API = '/api/admin/roles';
  const DESCRIPTIONS = {
    1: 'Todo en su empresa: perfil, empleados, instalaciones y reservas.',
    2: 'Solo las instalaciones asignadas: ubicaciones, productos, días cerrados y reservas.',
  };
  const rows = document.getElementById('rows');
  const form = document.getElementById('role-form');
  const modal = Ui.modal('role-modal');
  let roles = [];

  async function load() {
    rows.innerHTML = Ui.loadingRow(5);
    try {
      roles = await Api.get(API);
      rows.innerHTML = roles.length ? roles.map((r) => `
        <tr>
          <td><strong>${Ui.esc(r.name)}</strong></td>
          <td><span class="badge badge-rol-${r.type}">${Ui.esc(Ui.roleTypeName(r.type))}</span></td>
          <td class="muted">${DESCRIPTIONS[r.type] || ''}</td>
          <td class="num">${r.employeesCount}</td>
          <td class="acciones">
            <button class="btn btn-sm btn-secondary" data-edit="${r.id}">Editar</button>
            <button class="btn btn-sm btn-danger" data-delete="${r.id}">Eliminar</button>
          </td>
        </tr>`).join('') : Ui.emptyRow(5, 'No hay roles registrados.');
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(5, err.message);
    }
  }

  function openForm(role) {
    Ui.resetForm(form);
    form.querySelector('.modal-title').textContent = role ? 'Editar Rol' : 'Nuevo Rol';
    if (role) Ui.fillForm(form, role);
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(roles.find((r) => String(r.id) === edit));
    if (del) {
      const r = roles.find((x) => String(x.id) === del);
      const ok = await Ui.confirm(`¿Eliminar el rol "${r.name}"?`, { title: 'Eliminar rol', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Rol eliminado.');
        load();
      } catch (err) {
        Ui.handleError(err);
      }
    }
  });

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = Ui.formData(form);
    const btn = form.querySelector('button[type=submit]');
    Ui.busy(btn, true);
    try {
      if (data.id) await Api.put(`${API}/${data.id}`, data);
      else await Api.post(API, data);
      modal.hide();
      Ui.flash(data.id ? 'Rol actualizado.' : 'Rol registrado.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  load();
})();
