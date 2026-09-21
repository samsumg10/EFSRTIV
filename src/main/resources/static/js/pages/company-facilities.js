/* /company/facilities — lista de instalaciones con accesos a ubicaciones, productos, reservas y portal. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const API = '/api/company/facilities';
  const isAdmin = user.roleType === 1;
  const rows = document.getElementById('rows');
  const search = document.getElementById('search');
  const form = document.getElementById('facility-form');
  const modal = Ui.modal('facility-modal');
  let facilities = [];

  document.getElementById('btn-new').hidden = !isAdmin;

  async function load() {
    rows.innerHTML = Ui.loadingRow(5);
    try {
      facilities = await Api.get(API);
      render();
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(5, err.message);
    }
  }

  function render() {
    const q = search.value.trim().toLowerCase();
    const list = facilities.filter((f) => !q || [f.name, f.email, f.address]
      .some((v) => (v || '').toLowerCase().includes(q)));
    if (!list.length) {
      rows.innerHTML = Ui.emptyRow(5, facilities.length ? 'No se encontraron instalaciones.'
        : (isAdmin ? 'Aún no hay instalaciones. Crea la primera con "+ Nueva Instalación".'
                   : 'No tienes instalaciones asignadas.'));
      return;
    }
    rows.innerHTML = list.map((f) => `
      <tr>
        <td><strong>${Ui.esc(f.name)}</strong>${f.description ? `<br><span class="muted">${Ui.esc(f.description.slice(0, 80))}${f.description.length > 80 ? '…' : ''}</span>` : ''}</td>
        <td>${Ui.esc(f.email)}<br><span class="muted">${Ui.esc(f.phone)}</span></td>
        <td>${Ui.esc(f.address || '—')}</td>
        <td class="num">${f.locationsCount}</td>
        <td class="acciones">
          <a class="btn btn-sm btn-primary" href="/company/facilities/${f.id}/locations">📍 Ubicaciones</a>
          <a class="btn btn-sm btn-secondary" href="/company/facilities/${f.id}/products">🍖 Productos</a>
          <a class="btn btn-sm btn-secondary" href="/company/facilities/${f.id}/booking/getBookingsByFacility">📋 Reservas</a>
          <a class="btn btn-sm btn-secondary" href="/facility/${encodeURIComponent(f.uuid)}" target="_blank" rel="noopener">🔗 Portal</a>
          ${isAdmin ? `
            <button class="btn btn-sm btn-warning" data-edit="${f.id}">Editar</button>
            <button class="btn btn-sm btn-danger" data-delete="${f.id}">Eliminar</button>` : ''}
        </td>
      </tr>`).join('');
  }

  function openForm(facility) {
    Ui.resetForm(form);
    form.querySelector('.modal-title').textContent = facility ? 'Editar Instalación' : 'Nueva Instalación';
    if (facility) Ui.fillForm(form, facility);
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));
  search.addEventListener('input', render);

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(facilities.find((f) => String(f.id) === edit));
    if (del) {
      const f = facilities.find((x) => String(x.id) === del);
      const ok = await Ui.confirm(`¿Eliminar la instalación "${f.name}"? Sus ubicaciones dejarán de estar disponibles en el portal.`,
        { title: 'Eliminar instalación', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Instalación eliminada.');
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
      Ui.flash(data.id ? 'Instalación actualizada.' : 'Instalación registrada.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  load();
})();
