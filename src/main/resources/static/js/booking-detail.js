/*
 * Modal de detalle de una reserva (calendario y lista de reservas del panel de empresa).
 *   BookingDetail.open(bookingId, { onChange: () => recargar() });
 */
const BookingDetail = (() => {
  let modalEl = null;
  let current = null;
  let onChange = null;

  function ensureModal() {
    if (modalEl) return;
    const wrap = document.createElement('div');
    wrap.innerHTML = `
      <div class="modal fade" id="booking-detail-modal" tabindex="-1">
        <div class="modal-dialog modal-lg modal-dialog-scrollable">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">Detalle de la reserva</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
            </div>
            <div class="modal-body" id="booking-detail-body"></div>
            <div class="modal-footer" id="booking-detail-actions"></div>
          </div>
        </div>
      </div>`;
    modalEl = wrap.firstElementChild;
    document.body.appendChild(modalEl);
    modalEl.querySelector('#booking-detail-actions').addEventListener('click', onAction);
  }

  function render(b) {
    const lines = b.lines.length
      ? b.lines.map((l) => `
          <tr><td>${Ui.esc(l.name)}</td><td class="num">${l.quantity}</td>
              <td class="num">${Ui.money(l.price)}</td><td class="num">${Ui.money(l.total)}</td></tr>`).join('')
      : '<tr><td colspan="4" class="muted">Sin productos adicionales.</td></tr>';
    document.getElementById('booking-detail-body').innerHTML = `
      <div class="recibo-header">
        <span class="recibo-logo">🔥</span>
        <div>
          <h1>${Ui.esc(b.locationName)}</h1>
          <p>${Ui.esc(b.facilityName)}</p>
        </div>
        <div class="recibo-num">
          <span>Código ${Ui.esc(b.uuid.slice(0, 8).toUpperCase())}</span>
          ${Ui.saleBadge(b.status)}
        </div>
      </div>
      <div class="recibo-datos">
        <div class="dato"><span>Cliente</span><strong>${Ui.esc(b.name)}</strong></div>
        <div class="dato"><span>Fecha de la reserva</span><strong>${Ui.esc(Ui.longDate(b.date))}</strong></div>
        <div class="dato"><span>Correo</span><strong>${Ui.esc(b.email)}</strong></div>
        <div class="dato"><span>Teléfono</span><strong>${Ui.esc(b.phone)}</strong></div>
        <div class="dato"><span>Personas</span><strong>${b.numberPersons}</strong></div>
        <div class="dato"><span>Registrada por</span><strong>${Ui.esc(b.origin)} · ${Ui.dateTime(b.createdAt)}</strong></div>
        ${b.status === 1 ? `<div class="dato"><span>Plazo de pago</span><strong>${Ui.date(b.paymentDeadlineDate)}</strong></div>` : ''}
        ${b.paidAt ? `<div class="dato"><span>Pagada el</span><strong>${Ui.dateTime(b.paidAt)}</strong></div>` : ''}
      </div>
      <table class="recibo-tabla">
        <thead><tr><th>Concepto</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Total</th></tr></thead>
        <tbody>
          <tr><td>Reserva de ubicación</td><td class="num">1</td>
              <td class="num">${Ui.money(b.locationPrice)}</td><td class="num">${Ui.money(b.locationPrice)}</td></tr>
          ${lines}
        </tbody>
      </table>
      <div class="recibo-total"><span>Total</span><strong>${Ui.money(b.total)}</strong></div>
      <p class="muted mt-2 mb-0">Código completo: <code class="uuid">${Ui.esc(b.uuid)}</code></p>`;

    renderActions(b);
  }

  function renderActions(b) {
    document.getElementById('booking-detail-actions').innerHTML = `
      ${b.status === 1 ? '<button type="button" class="btn btn-success" data-action="pay">✔ Marcar como pagada</button>' : ''}
      ${b.status === 1 || b.status === 2 ? '<button type="button" class="btn btn-danger" data-action="cancel">Cancelar reserva</button>' : ''}
      <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cerrar</button>`;
  }

  /** Confirmación dentro del mismo modal (Bootstrap 5 no maneja bien dos modales abiertos). */
  function renderConfirm(action) {
    const pay = action === 'pay';
    document.getElementById('booking-detail-actions').innerHTML = `
      <span class="me-auto fw-semibold">${pay ? '¿Confirmas que el cliente ya pagó?' : '¿Cancelar la reserva? El cupo quedará libre.'}</span>
      <button type="button" class="btn ${pay ? 'btn-success' : 'btn-danger'}" data-action="confirm-${action}">Sí, ${pay ? 'está pagada' : 'cancelar'}</button>
      <button type="button" class="btn btn-secondary" data-action="back">No</button>`;
  }

  async function onAction(e) {
    const action = e.target.dataset.action;
    if (!action || !current) return;
    if (action === 'pay' || action === 'cancel') { renderConfirm(action); return; }
    if (action === 'back') { renderActions(current); return; }
    const status = action === 'confirm-pay' ? 2 : 3;
    Ui.busy(e.target, true, 'Guardando...');
    try {
      current = await Api.patch(`/api/company/bookings/${current.id}/status`, { status });
      render(current);
      Ui.flash(status === 2 ? 'Pago registrado.' : 'Reserva cancelada.');
      if (onChange) onChange(current);
    } catch (err) {
      renderActions(current);
      Ui.flash(err.message, 'error');
    }
  }

  async function open(bookingId, options = {}) {
    ensureModal();
    onChange = options.onChange || null;
    document.getElementById('booking-detail-body').innerHTML = '<div class="loading">Cargando...</div>';
    document.getElementById('booking-detail-actions').innerHTML = '';
    bootstrap.Modal.getOrCreateInstance(modalEl).show();
    try {
      current = await Api.get(`/api/company/bookings/${bookingId}`);
      render(current);
    } catch (err) {
      document.getElementById('booking-detail-body').innerHTML = `<div class="alert alert-error">${Ui.esc(err.message)}</div>`;
    }
  }

  return { open };
})();
