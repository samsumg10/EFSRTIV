/*
 * /location/{uuid} — el cliente elige día, personas y productos (PortalController@showBooking + addToCart).
 * "Continuar" valida y cotiza en el servidor, guarda el carrito y pasa a /booking-details/{uuid}.
 */
(async function () {
  const uuid = Ui.pathParam(/^\/location\/([^/]+)/);
  const API = `/api/public/locations/${encodeURIComponent(uuid)}`;
  const form = document.getElementById('booking-form');
  const fields = document.getElementById('form-fields');
  const productRows = document.getElementById('product-rows');
  const hint = document.getElementById('capacity-hint');
  let l;
  let cal;

  try {
    l = await Api.get(API);
  } catch (err) {
    Portal.showError(err.message);
    return;
  }

  Portal.setHeader(l.companyName, l.facilityName);
  document.getElementById('title').textContent = l.name;
  document.getElementById('subtitle').textContent =
    `${Portal.capacityText(l.maxPerson)} · ${Ui.money(l.price)} por reserva${l.facilityAddress ? ' · 📍 ' + l.facilityAddress : ''}`;
  document.getElementById('crumbs').innerHTML = Ui.crumbs([
    { text: l.facilityName, href: `/facility/${encodeURIComponent(l.facilityUuid)}` }, { text: l.name }]);
  document.getElementById('legend').innerHTML = BbqCalendar.legendHtml(false);

  productRows.innerHTML = l.products.length ? l.products.map((p) => `
    <div class="product-row">
      <div><div class="product-name">${Ui.esc(p.name)}</div><div class="product-price">${Ui.money(p.price)} c/u</div></div>
      <input type="number" min="0" max="999" step="1" value="0" data-product-id="${p.id}" data-price="${p.price}" aria-label="Cantidad de ${Ui.esc(p.name)}">
    </div>`).join('') : '<p class="muted">No hay productos adicionales.</p>';

  function productInputs() { return [...productRows.querySelectorAll('input[data-product-id]')]; }

  function updateTotals() {
    const products = productInputs().reduce((s, i) => s + Number(i.dataset.price) * (Number(i.value) || 0), 0);
    document.getElementById('sum-location').textContent = Ui.money(l.price);
    document.getElementById('sum-products').textContent = Ui.money(products);
    document.getElementById('sum-total').textContent = Ui.money(Number(l.price) + products);
  }

  async function selectDate(dateStr) {
    form.date.value = dateStr;
    Ui.clearErrors(form);
    cal.select(dateStr);
    document.getElementById('selected-date').textContent = `📅 ${Ui.longDate(dateStr)}`;
    fields.disabled = false;
    hint.textContent = 'Consultando disponibilidad...';
    try {
      const a = await Api.get(`${API}/availability${Api.qs({ date: dateStr })}`);
      if (!a.available) {
        hint.textContent = a.message;
        fields.disabled = true;
      } else if (a.exclusive) {
        hint.textContent = 'Tendrás la ubicación solo para tu grupo ese día.';
        form.numberPersons.removeAttribute('max');
      } else {
        hint.textContent = `Quedan ${a.remainingPersons} lugares para ese día.`;
        form.numberPersons.max = a.remainingPersons;
      }
    } catch (err) {
      hint.textContent = err.message;
    }
  }

  cal = BbqCalendar.create(document.getElementById('calendar'), {
    loadDays: (start, end) => Api.get(`${API}/calendar-days${Api.qs({ start, end })}`),
    onDayClick: (dateStr, state) => {
      if (state === 'available') selectDate(dateStr);
      else Ui.flash(BbqCalendar.stateMessage(state), 'info');
    },
  });

  // Si el cliente vuelve desde el resumen para modificar, se recupera lo que llenó
  const cart = Portal.getCart();
  if (cart && cart.locationUuid === uuid && cart.request) {
    const r = cart.request;
    Ui.fillForm(form, r);
    (r.products || []).forEach((p) => {
      const input = productRows.querySelector(`input[data-product-id="${p.productId}"]`);
      if (input) input.value = p.quantity;
    });
    if (r.date) {
      cal.calendar.gotoDate(r.date);
      selectDate(r.date);
    }
  }
  updateTotals();
  productRows.addEventListener('input', updateTotals);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const data = Ui.formData(form);
    data.products = productInputs()
      .map((i) => ({ productId: Number(i.dataset.productId), quantity: Number(i.value) || 0 }))
      .filter((p) => p.quantity > 0);
    const btn = form.querySelector('button[type=submit]');
    Ui.busy(btn, true, 'Verificando...');
    try {
      const quote = await Api.post(`${API}/quote`, data);
      Portal.saveCart({ locationUuid: uuid, request: data, quote });
      location.href = `/booking-details/${encodeURIComponent(uuid)}`;
    } catch (err) {
      Ui.showErrors(form, err);
      Ui.busy(btn, false);
    }
  });
})();
