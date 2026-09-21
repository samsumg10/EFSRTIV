/* /company/facilities/{id}/locations — CRUD de ubicaciones de una instalación. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const facilityId = Ui.pathParam(/^\/company\/facilities\/(\d+)\/locations/);
  const API = `/api/company/facilities/${facilityId}/locations`;
  const rows = document.getElementById('rows');
  const form = document.getElementById('location-form');
  const modal = Ui.modal('location-modal');
  let locations = [];

  async function loadFacility() {
    try {
      const f = await Api.get(`/api/company/facilities/${facilityId}`);
      document.getElementById('title').textContent = `Ubicaciones · ${f.name}`;
      document.getElementById('crumbs').innerHTML = Ui.crumbs([
        { text: 'Instalaciones', href: '/company/facilities' }, { text: f.name }, { text: 'Ubicaciones' }]);
    } catch (err) {
      Ui.flash(err.message, 'error');
    }
  }

  async function load() {
    rows.innerHTML = Ui.loadingRow(4);
    try {
      locations = await Api.get(API);
      rows.innerHTML = locations.length ? locations.map((l) => `
        <tr>
          <td><strong>${Ui.esc(l.name)}</strong>${l.description ? `<br><span class="muted">${Ui.esc(l.description)}</span>` : ''}</td>
          <td>${l.maxPerson ? `${l.maxPerson} personas` : '<span class="badge badge-rol-1">Exclusiva</span> <span class="muted">1 reserva/día</span>'}</td>
          <td class="num">${Ui.money(l.price)}</td>
          <td class="acciones">
            <a class="btn btn-sm btn-primary" href="/company/locations/${l.id}/bookings">📅 Reservas</a>
            <a class="btn btn-sm btn-secondary" href="/company/locations/${l.id}/closeddays">🚫 Días cerrados</a>
            <a class="btn btn-sm btn-secondary" href="/location/${encodeURIComponent(l.uuid)}" target="_blank" rel="noopener">🔗 Portal</a>
            <button class="btn btn-sm btn-warning" data-edit="${l.id}">Editar</button>
            <button class="btn btn-sm btn-danger" data-delete="${l.id}">Eliminar</button>
          </td>
        </tr>`).join('') : Ui.emptyRow(4, 'Esta instalación aún no tiene ubicaciones.');
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(4, err.message);
    }
  }

  function openForm(location) {
    Ui.resetForm(form);
    form.querySelector('.modal-title').textContent = location ? 'Editar Ubicación' : 'Nueva Ubicación';
    if (location) Ui.fillForm(form, location);
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(locations.find((l) => String(l.id) === edit));
    if (del) {
      const l = locations.find((x) => String(x.id) === del);
      const ok = await Ui.confirm(`¿Eliminar la ubicación "${l.name}"? Ya no se podrá reservar.`,
        { title: 'Eliminar ubicación', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Ubicación eliminada.');
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
      Ui.flash(data.id ? 'Ubicación actualizada.' : 'Ubicación registrada.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  loadFacility();
  load();
})();
