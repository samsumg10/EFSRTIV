/* /company/locations/{id}/closeddays — días en que la ubicación no acepta reservas. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const locationId = Ui.pathParam(/^\/company\/locations\/(\d+)\/closeddays/);
  const API = `/api/company/locations/${locationId}/closeddays`;
  const rows = document.getElementById('rows');
  const form = document.getElementById('closedday-form');
  const modal = Ui.modal('closedday-modal');
  let closedDays = [];

  document.getElementById('btn-calendar').href = `/company/locations/${locationId}/bookings`;

  function daysBetween(a, b) {
    return Math.round((new Date(b + 'T00:00:00') - new Date(a + 'T00:00:00')) / 86400000) + 1;
  }

  async function loadLocation() {
    try {
      const l = await Api.get(`/api/company/locations/${locationId}`);
      document.getElementById('title').textContent = `Días cerrados · ${l.name}`;
      document.getElementById('crumbs').innerHTML = Ui.crumbs([
        { text: 'Instalaciones', href: '/company/facilities' },
        { text: l.facilityName, href: `/company/facilities/${l.facilityId}/locations` },
        { text: l.name }, { text: 'Días cerrados' }]);
    } catch (err) {
      Ui.flash(err.message, 'error');
    }
  }

  async function load() {
    rows.innerHTML = Ui.loadingRow(5);
    try {
      closedDays = await Api.get(API);
      const today = Ui.todayIso();
      rows.innerHTML = closedDays.length ? closedDays.map((c) => {
        const end = c.endDate || c.startDate;
        const past = end < today;
        return `
          <tr>
            <td>${Ui.date(c.startDate)}${past ? ' <span class="badge badge-expirado">Pasado</span>' : ''}</td>
            <td>${Ui.date(end)}</td>
            <td class="num">${daysBetween(c.startDate, end)}</td>
            <td>${Ui.esc(c.reason || '—')}</td>
            <td class="acciones">
              <button class="btn btn-sm btn-secondary" data-edit="${c.id}">Editar</button>
              <button class="btn btn-sm btn-danger" data-delete="${c.id}">Eliminar</button>
            </td>
          </tr>`;
      }).join('') : Ui.emptyRow(5, 'No hay días cerrados registrados.');
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(5, err.message);
    }
  }

  function openForm(closedDay) {
    Ui.resetForm(form);
    form.querySelector('.modal-title').textContent = closedDay ? 'Editar día cerrado' : 'Nuevo día cerrado';
    if (closedDay) Ui.fillForm(form, closedDay);
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(closedDays.find((c) => String(c.id) === edit));
    if (del) {
      const ok = await Ui.confirm('¿Eliminar este periodo cerrado? Esos días volverán a aceptar reservas.',
        { title: 'Eliminar día cerrado', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Día cerrado eliminado.');
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
      Ui.flash(data.id ? 'Día cerrado actualizado.' : 'Día cerrado registrado.');
      load();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  loadLocation();
  load();
})();
