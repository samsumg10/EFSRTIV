/*
 * /booking-details/{uuid} — resumen de la reserva y confirmación (PortalController@showBookingDetails + store).
 * En lugar del pago con tarjeta (fincode) la reserva queda PENDIENTE de pago por transferencia.
 */
(async function () {
  const uuid = Ui.pathParam(/^\/booking-details\/([^/]+)/);
  const API = `/api/public/locations/${encodeURIComponent(uuid)}`;
  const content = document.getElementById('content');
  const backUrl = `/location/${encodeURIComponent(uuid)}`;

  const cart = Portal.getCart();
  if (!cart || cart.locationUuid !== uuid || !cart.request) {
    location.replace(backUrl);
    return;
  }

  let l;
  let quote;
  try {
    l = await Api.get(API);
    Portal.setHeader(l.companyName, l.facilityName);
    // Se vuelve a cotizar: precios y cupos actuales
    quote = await Api.post(`${API}/quote`, cart.request);
  } catch (err) {
    content.innerHTML = `
      <div class="alert alert-error">${Ui.esc(err.message)}</div>
      <a class="btn btn-primary" href="${backUrl}">← Modificar reserva</a>`;
    return;
  }

  function linesHtml(locationPrice, lines) {
    return `
      <tr><td>Reserva de ${Ui.esc(quote.locationName)}</td><td class="num">1</td>
          <td class="num">${Ui.money(locationPrice)}</td><td class="num">${Ui.money(locationPrice)}</td></tr>
      ${lines.map((x) => `
        <tr><td>${Ui.esc(x.name)}</td><td class="num">${x.quantity}</td>
            <td class="num">${Ui.money(x.price)}</td><td class="num">${Ui.money(x.total)}</td></tr>`).join('')}`;
  }

  function summaryHtml(q) {
    return `
      <div class="recibo-container">
        <div class="recibo-header">
          <span class="recibo-logo">🔥</span>
          <div>
            <h1>${Ui.esc(q.locationName)}</h1>
            <p>${Ui.esc(q.facilityName)} · ${Ui.esc(l.companyName)}</p>
          </div>
          <div class="recibo-num"><span>Resumen</span></div>
        </div>
        <div class="recibo-datos">
          <div class="dato"><span>Fecha</span><strong>${Ui.esc(Ui.longDate(q.date))}</strong></div>
          <div class="dato"><span>Personas</span><strong>${q.numberPersons}</strong></div>
          <div class="dato"><span>Nombre</span><strong>${Ui.esc(q.name)}</strong></div>
          <div class="dato"><span>Correo</span><strong>${Ui.esc(q.email)}</strong></div>
          <div class="dato"><span>Teléfono</span><strong>${Ui.esc(q.phone)}</strong></div>
          ${l.facilityAddress ? `<div class="dato"><span>Dirección</span><strong>${Ui.esc(l.facilityAddress)}</strong></div>` : ''}
        </div>
        <table class="recibo-tabla">
          <thead><tr><th>Concepto</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Total</th></tr></thead>
          <tbody>${linesHtml(q.locationPrice, q.lines)}</tbody>
        </table>
        <div class="recibo-total"><span>Total a pagar</span><strong>${Ui.money(q.total)}</strong></div>
        <div class="alert alert-info mt-3 mb-0">
          💳 <strong>Pago por transferencia bancaria.</strong> Al confirmar, tu reserva queda separada y tendrás
          hasta 3 días (o hasta el día anterior a la reserva) para pagar.
        </div>
        <div class="form-actions no-print">
          <button type="button" class="btn btn-primary" id="btn-confirm">✔ Confirmar reserva</button>
          <a class="btn btn-secondary" href="${backUrl}">← Modificar</a>
        </div>
      </div>`;
  }

  function successHtml(b) {
    return `
      <div class="alert alert-success no-print">✅ ¡Tu reserva fue registrada! Guarda tu código de reserva.</div>
      <div class="recibo-container">
        <div class="recibo-header">
          <span class="recibo-logo">🔥</span>
          <div>
            <h1>${Ui.esc(b.locationName)}</h1>
            <p>${Ui.esc(b.facilityName)} · ${Ui.esc(b.companyName)}</p>
          </div>
          <div class="recibo-num">
            <span>Código ${Ui.esc(b.uuid.slice(0, 8).toUpperCase())}</span>
            ${Ui.saleBadge(b.status)}
          </div>
        </div>
        <div class="recibo-datos">
          <div class="dato"><span>Fecha</span><strong>${Ui.esc(Ui.longDate(b.date))}</strong></div>
          <div class="dato"><span>Personas</span><strong>${b.numberPersons}</strong></div>
          <div class="dato"><span>Nombre</span><strong>${Ui.esc(b.name)}</strong></div>
          <div class="dato"><span>Correo</span><strong>${Ui.esc(b.email)}</strong></div>
          <div class="dato"><span>Pagar hasta</span><strong>${Ui.date(b.paymentDeadlineDate)}</strong></div>
          <div class="dato"><span>Código completo</span><strong><code class="uuid">${Ui.esc(b.uuid)}</code></strong></div>
        </div>
        <table class="recibo-tabla">
          <thead><tr><th>Concepto</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Total</th></tr></thead>
          <tbody>${linesHtml(b.locationPrice, b.lines)}</tbody>
        </table>
        <div class="recibo-total"><span>Total a pagar</span><strong>${Ui.money(b.total)}</strong></div>
        <div class="alert alert-warning mt-3 mb-0">
          Realiza una transferencia bancaria por <strong>${Ui.money(b.total)}</strong> antes del
          <strong>${Ui.date(b.paymentDeadlineDate)}</strong> indicando tu código de reserva
          <strong>${Ui.esc(b.uuid.slice(0, 8).toUpperCase())}</strong>. Si no se registra el pago a tiempo, la reserva vence.
          ${l.facilityPhone ? `Consultas: 📞 ${Ui.esc(l.facilityPhone)}.` : ''}
        </div>
        <div class="form-actions no-print">
          <button type="button" class="btn btn-secondary" onclick="window.print()">🖨️ Imprimir</button>
          <a class="btn btn-primary" href="/facility/${encodeURIComponent(l.facilityUuid)}">Volver a ${Ui.esc(b.facilityName)}</a>
        </div>
      </div>`;
  }

  content.innerHTML = summaryHtml(quote);

  document.getElementById('btn-confirm').addEventListener('click', async (e) => {
    Ui.busy(e.target, true, 'Registrando...');
    try {
      const booking = await Api.post(`${API}/bookings`, cart.request);
      Portal.clearCart();
      content.innerHTML = successHtml(booking);
      window.scrollTo(0, 0);
    } catch (err) {
      Ui.busy(e.target, false);
      Ui.flash(`${err.message} Vuelve atrás para elegir otra fecha o cantidad.`, 'error');
      window.scrollTo(0, 0);
    }
  });
})();
