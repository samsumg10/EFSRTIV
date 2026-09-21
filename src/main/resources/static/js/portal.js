/*
 * Utilidades del portal público: cabecera y carrito.
 * El carrito (la reserva aún sin confirmar) vive en sessionStorage, en lugar de la sesión PHP del original;
 * el servidor vuelve a validar y a calcular los precios al cotizar y al confirmar.
 */
const Portal = (() => {
  const CART_KEY = 'bbq_cart';

  function setHeader(companyName, subtitle) {
    document.getElementById('brand').textContent = companyName || 'BBQ Reservas';
    document.getElementById('brand-sub').textContent = subtitle || '';
    document.title = `${subtitle || companyName} · Reservas`;
  }

  function getCart() {
    try { return JSON.parse(sessionStorage.getItem(CART_KEY)); } catch { return null; }
  }
  function saveCart(cart) {
    try { sessionStorage.setItem(CART_KEY, JSON.stringify(cart)); } catch { /* sin almacenamiento */ }
  }
  function clearCart() {
    try { sessionStorage.removeItem(CART_KEY); } catch { /* sin almacenamiento */ }
  }

  /** Reemplaza el contenido por un mensaje (instalación inexistente, empresa inactiva...). */
  function showError(message) {
    document.getElementById('content').innerHTML = `
      <div class="card text-center py-5">
        <div style="font-size:3rem">🔥</div>
        <h3 class="mt-2">No disponible</h3>
        <p class="muted mb-0">${Ui.esc(message)}</p>
      </div>`;
  }

  function capacityText(maxPerson) {
    return maxPerson ? `Hasta ${maxPerson} personas por día` : 'Uso exclusivo: una reserva por día';
  }

  return { setHeader, getCart, saveCart, clearCart, showError, capacityText };
})();
