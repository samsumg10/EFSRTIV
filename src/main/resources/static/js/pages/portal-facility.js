/* /facility/{uuid} — instalación y sus ubicaciones reservables (PortalController@showFacility). */
(async function () {
  const uuid = Ui.pathParam(/^\/facility\/([^/]+)/);
  const content = document.getElementById('content');

  let f;
  try {
    f = await Api.get(`/api/public/facilities/${encodeURIComponent(uuid)}`);
  } catch (err) {
    Portal.showError(err.message);
    return;
  }
  Portal.setHeader(f.companyName, f.name);

  const cards = f.locations.length ? f.locations.map((l) => `
    <div class="location-card">
      <h3>${Ui.esc(l.name)}</h3>
      <p>${Ui.esc(l.description || 'Zona reservable para tu parrillada.')}</p>
      <div class="muted">👥 ${Ui.esc(Portal.capacityText(l.maxPerson))}</div>
      <div class="location-meta">
        <span><span class="price">${Ui.money(l.price)}</span> <span class="muted">por reserva</span></span>
        <a class="btn btn-primary" href="/location/${encodeURIComponent(l.uuid)}">Reservar</a>
      </div>
    </div>`).join('') : '<div class="card empty">Por ahora no hay ubicaciones disponibles.</div>';

  content.innerHTML = `
    <section class="hero">
      <h1>${Ui.esc(f.name)}</h1>
      ${f.description ? `<p>${Ui.esc(f.description)}</p>` : ''}
      <div class="hero-meta">
        ${f.address ? `<span>📍 ${Ui.esc(f.address)}</span>` : ''}
        ${f.phone ? `<span>📞 ${Ui.esc(f.phone)}</span>` : ''}
        ${f.email ? `<span>✉️ ${Ui.esc(f.email)}</span>` : ''}
        ${f.companyUrl ? `<span>🌐 <a href="${Ui.esc(f.companyUrl)}" target="_blank" rel="noopener" style="color:#fff">${Ui.esc(f.companyName)}</a></span>` : ''}
      </div>
    </section>
    <div class="page-header"><h2>Elige dónde quieres reservar</h2></div>
    <div class="cards-grid">${cards}</div>`;
})();
