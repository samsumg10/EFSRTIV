/* /company/locations/{id}/bookings — calendario de reservas de una ubicación + reserva manual. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const locationId = Ui.pathParam(/^\/company\/locations\/(\d+)\/bookings/);
  const API = `/api/company/locations/${locationId}`;
  const form = document.getElementById('booking-form');
  const modal = Ui.modal('booking-modal');
  const showCancelled = document.getElementById('show-cancelled');
  const productRows = document.getElementById('product-rows');
  let info = null;
  let cal = null;

  document.getElementById('btn-closeddays').href = `/company/locations/${locationId}/closeddays`;
  document.getElementById('legend').innerHTML = BbqCalendar.legendHtml(true);

  async function init() {
    try {
      info = await Api.get(`${API}/calendar-info`);
    } catch (err) {
      Ui.flash(err.message, 'error');
      return;
    }
    const l = info.location;
    document.getElementById('title').textContent = `Reservas · ${l.name}`;
    document.getElementById('subtitle').textContent =
      `${l.maxPerson ? `Capacidad: ${l.maxPerson} personas por día` : 'Exclusiva: una reserva por día'} · Precio por reserva: ${Ui.money(l.price)}`;
    document.getElementById('crumbs').innerHTML = Ui.crumbs([
      { text: 'Instalaciones', href: '/company/facilities' },
      { text: l.facilityName, href: `/company/facilities/${l.facilityId}/locations` },
      { text: l.name }, { text: 'Reservas' }]);
    document.getElementById('btn-list').href = `/company/facilities/${l.facilityId}/booking/getBookingsByFacility`;

    productRows.innerHTML = info.products.length ? info.products.map((p) => `
      <div class="product-row">
        <div><div class="product-name">${Ui.esc(p.name)}</div><div class="product-price">${Ui.money(p.price)} c/u</div></div>
        <input type="number" min="0" max="999" step="1" value="0" data-product-id="${p.id}" data-price="${p.price}">
      </div>`).join('') : '<p class="muted">Esta instalación no tiene productos.</p>';

    cal = BbqCalendar.create(document.getElementById('calendar'), {
      withList: true,
      loadDays: (start, end) => Api.get(`${API}/calendar-days${Api.qs({ start, end })}`),
      loadEvents: async (start, end) => {
        const events = await Api.get(`${API}/bookings${Api.qs({ start, end })}`);
        return showCancelled.checked ? events : events.filter((e) => e.status === 1 || e.status === 2);
      },
      onDayClick: (dateStr, state) => {
        if (state === 'available') openForm(dateStr);
        else Ui.flash(BbqCalendar.stateMessage(state), 'info');
      },
      onEventClick: (event) => BookingDetail.open(event.id, { onChange: () => cal.refresh() }),
    });
  }

  function productsPayload() {
    return [...productRows.querySelectorAll('input[data-product-id]')]
      .map((i) => ({ productId: Number(i.dataset.productId), quantity: Number(i.value) || 0 }))
      .filter((p) => p.quantity > 0);
  }

  function updateTotals() {
    const products = [...productRows.querySelectorAll('input[data-product-id]')]
      .reduce((sum, i) => sum + Number(i.dataset.price) * (Number(i.value) || 0), 0);
    const location = Number(info.location.price);
    document.getElementById('sum-location').textContent = Ui.money(location);
    document.getElementById('sum-products').textContent = Ui.money(products);
    document.getElementById('sum-total').textContent = Ui.money(location + products);
  }

  async function openForm(dateStr) {
    Ui.resetForm(form);
    productRows.querySelectorAll('input').forEach((i) => { i.value = 0; });
    form.date.value = dateStr;
    form.numberPersons.value = 1;
    document.getElementById('selected-date').textContent = `📅 ${Ui.longDate(dateStr)}`;
    const hint = document.getElementById('capacity-hint');
    hint.textContent = 'Consultando disponibilidad...';
    cal.select(dateStr);
    updateTotals();
    modal.show();
    try {
      const a = await Api.get(`${API}/availability${Api.qs({ date: dateStr })}`);
      if (!a.available) hint.textContent = a.message;
      else if (a.exclusive) hint.textContent = 'Ubicación exclusiva: el día queda reservado por completo.';
      else {
        hint.textContent = `Quedan ${a.remainingPersons} de ${a.maxPerson} lugares.`;
        form.numberPersons.max = a.remainingPersons;
      }
    } catch (err) {
      hint.textContent = err.message;
    }
  }

  productRows.addEventListener('input', updateTotals);
  showCancelled.addEventListener('change', () => cal && cal.calendar.refetchEvents());
  document.getElementById('booking-modal').addEventListener('hidden.bs.modal', () => cal && cal.select(null));

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = Ui.formData(form);
    data.products = productsPayload();
    const btn = form.querySelector('button[type=submit]');
    Ui.busy(btn, true);
    try {
      const booking = await Api.post(`${API}/bookings`, data);
      modal.hide();
      Ui.flash(`Reserva registrada para ${booking.name} (${Ui.money(booking.total)}).`);
      cal.refresh();
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  init();
})();
