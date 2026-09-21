/*
 * /company/facilities/{id}/booking/getBookingsByFacility — lista de reservas con filtros,
 * estadísticas y paginación (BookingController@getBookingsByFacility del sistema Laravel).
 * Los filtros viajan en la query string para que sobrevivan a un refresco.
 */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const facilityId = Ui.pathParam(/^\/company\/facilities\/(\d+)\/booking\/getBookingsByFacility/);
  const filterForm = document.getElementById('filter-form');
  const rows = document.getElementById('rows');
  let page = 0;
  let totalPages = 0;

  /** Filtros iniciales: los de la URL, o reservas desde hoy. */
  function initialFilters() {
    const q = new URLSearchParams(location.search);
    if (![...q.keys()].length) return { resStart: Ui.todayIso() };
    page = Math.max(0, Number(q.get('page')) || 0);
    return Object.fromEntries([...q.entries()].filter(([k]) => k !== 'page'));
  }

  async function loadHeader() {
    try {
      const [facilities, locations] = await Promise.all([
        Api.get('/api/company/facilities'),
        Api.get(`/api/company/facilities/${facilityId}/locations`)]);
      const f = facilities.find((x) => String(x.id) === String(facilityId));
      if (f) {
        document.getElementById('title').textContent = `Reservas · ${f.name}`;
        document.getElementById('crumbs').innerHTML = Ui.crumbs([
          { text: 'Instalaciones', href: '/company/facilities' }, { text: f.name }, { text: 'Reservas' }]);
      }
      const facilitySelect = document.getElementById('facility-select');
      facilitySelect.innerHTML = facilities.map((x) =>
        `<option value="${x.id}" ${String(x.id) === String(facilityId) ? 'selected' : ''}>🏕️ ${Ui.esc(x.name)}</option>`).join('');
      facilitySelect.addEventListener('change', () => {
        location.href = `/company/facilities/${facilitySelect.value}/booking/getBookingsByFacility`;
      });
      const locationSelect = document.getElementById('location-select');
      const selected = new URLSearchParams(location.search).get('locationId');
      locationSelect.innerHTML = '<option value="">Todas</option>' + locations.map((l) =>
        `<option value="${l.id}" ${String(l.id) === selected ? 'selected' : ''}>${Ui.esc(l.name)}</option>`).join('');
    } catch (err) {
      Ui.flash(err.message, 'error');
    }
  }

  async function load() {
    const filters = Ui.formData(filterForm);
    history.replaceState(null, '', location.pathname + Api.qs({ ...filters, page: page || '' }));
    rows.innerHTML = Ui.loadingRow(9);
    try {
      const r = await Api.get(`/api/company/facilities/${facilityId}/bookings${Api.qs({ ...filters, page })}`);
      totalPages = r.totalPages;
      renderStats(r.stats);
      rows.innerHTML = r.items.length ? r.items.map((b) => `
        <tr>
          <td><code class="uuid" title="${Ui.esc(b.uuid)}">${Ui.esc(b.uuid.slice(0, 8).toUpperCase())}</code></td>
          <td><strong>${Ui.esc(b.name)}</strong><br><span class="muted">${Ui.esc(b.email)} · ${Ui.esc(b.phone)}</span></td>
          <td>${Ui.esc(b.locationName)}</td>
          <td>${Ui.date(b.date)}</td>
          <td class="num">${b.numberPersons}</td>
          <td class="num">${Ui.money(b.total)}</td>
          <td>${Ui.saleBadge(b.status)}</td>
          <td>${Ui.dateTime(b.createdAt)}<br><span class="muted">${Ui.esc(b.origin)}</span></td>
          <td class="acciones"><button class="btn btn-sm btn-secondary" data-view="${b.id}">Ver</button></td>
        </tr>`).join('') : Ui.emptyRow(9, 'No hay reservas con esos filtros.');
      document.getElementById('page-info').textContent = r.totalElements
        ? `Página ${r.page + 1} de ${r.totalPages} · ${r.totalElements} reserva(s)` : '';
      document.getElementById('btn-prev').disabled = page <= 0;
      document.getElementById('btn-next').disabled = page >= totalPages - 1;
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(9, err.message);
    }
  }

  function renderStats(s) {
    document.getElementById('kpi-total').textContent = s.totalBookings;
    document.getElementById('kpi-amount').textContent = Ui.money(s.totalAmount);
    document.getElementById('kpi-paid').textContent = Ui.money(s.paidAmount);
    document.getElementById('kpi-paid-label').textContent = `Pagadas (${s.paidCount})`;
    document.getElementById('kpi-pending').textContent = Ui.money(s.pendingAmount);
    document.getElementById('kpi-pending-label').textContent = `Pendientes de pago (${s.pendingCount})`;
  }

  filterForm.addEventListener('submit', (e) => { e.preventDefault(); page = 0; load(); });
  document.getElementById('btn-clear').addEventListener('click', () => {
    filterForm.reset();
    filterForm.resStart.value = Ui.todayIso();
    page = 0;
    load();
  });
  document.getElementById('btn-prev').addEventListener('click', () => { if (page > 0) { page--; load(); } });
  document.getElementById('btn-next').addEventListener('click', () => { if (page < totalPages - 1) { page++; load(); } });
  rows.addEventListener('click', (e) => {
    if (e.target.dataset.view) BookingDetail.open(e.target.dataset.view, { onChange: load });
  });

  // Primero se llena el select de ubicaciones (toma locationId de la URL) y luego se busca
  Ui.fillForm(filterForm, initialFilters());
  loadHeader().then(load);
})();
