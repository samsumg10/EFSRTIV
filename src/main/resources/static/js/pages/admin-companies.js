/* /admin/companies — CRUD de empresas + representante. */
(function () {
  const user = Layout.init({ area: 'admin', menu: 'companies' });
  if (!user) return;

  const API = '/api/admin/companies';
  const rows = document.getElementById('rows');
  const search = document.getElementById('search');
  const form = document.getElementById('company-form');
  const modal = Ui.modal('company-modal');
  let companies = [];

  async function load() {
    rows.innerHTML = Ui.loadingRow(7);
    try {
      companies = await Api.get(API);
      render();
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(7, err.message);
    }
  }

  function render() {
    const q = search.value.trim().toLowerCase();
    const list = companies.filter((c) => !q || [c.name, c.email, c.representativeName, c.representativeEmail]
      .some((v) => (v || '').toLowerCase().includes(q)));
    if (!list.length) {
      rows.innerHTML = Ui.emptyRow(7, 'No se encontraron empresas.');
      return;
    }
    rows.innerHTML = list.map((c) => `
      <tr>
        <td><strong>${Ui.esc(c.name)}</strong><br><code class="uuid">${Ui.esc(c.uuid)}</code></td>
        <td>${Ui.esc(c.email)}</td>
        <td>${Ui.esc(c.phone)}</td>
        <td>${c.representativeName
          ? `${Ui.esc(c.representativeName)}<br><span class="muted">${Ui.esc(c.representativeEmail)}</span>`
          : '<span class="muted">Sin representante</span>'}</td>
        <td><span class="badge ${c.status === 1 ? 'badge-activo' : 'badge-inactivo'}">${c.status === 1 ? 'Activa' : 'Inactiva'}</span></td>
        <td>${Ui.date(c.createdAt)}</td>
        <td class="acciones">
          <button class="btn btn-sm btn-secondary" data-edit="${c.id}">Editar</button>
          <button class="btn btn-sm btn-danger" data-delete="${c.id}">Eliminar</button>
        </td>
      </tr>`).join('');
  }

  function openForm(company) {
    Ui.resetForm(form);
    const isNew = !company;
    form.querySelector('.modal-title').textContent = isNew ? 'Nueva Empresa' : 'Editar Empresa';
    document.getElementById('password-required').hidden = !isNew;
    document.getElementById('password-hint').textContent = isNew
      ? 'Mínimo 8 caracteres.' : 'Déjala vacía para no cambiarla.';
    if (company) Ui.fillForm(form, company);
    else form.status.value = '1';
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));
  search.addEventListener('input', render);

  rows.addEventListener('click', async (e) => {
    const editId = e.target.dataset.edit;
    const deleteId = e.target.dataset.delete;
    if (editId) {
      openForm(companies.find((c) => String(c.id) === editId));
    }
    if (deleteId) {
      const c = companies.find((x) => String(x.id) === deleteId);
      const ok = await Ui.confirm(`¿Eliminar la empresa "${c.name}"? Sus usuarios ya no podrán ingresar.`,
        { title: 'Eliminar empresa', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${deleteId}`);
        Ui.flash('Empresa eliminada.');
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
      Ui.flash(data.id ? 'Empresa actualizada.' : 'Empresa registrada.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  load();
})();
