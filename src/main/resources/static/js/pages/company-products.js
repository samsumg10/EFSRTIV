/* /company/facilities/{id}/products — CRUD de productos de una instalación. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'facilities' });
  if (!user) return;

  const facilityId = Ui.pathParam(/^\/company\/facilities\/(\d+)\/products/);
  const API = `/api/company/facilities/${facilityId}/products`;
  const rows = document.getElementById('rows');
  const search = document.getElementById('search');
  const form = document.getElementById('product-form');
  const modal = Ui.modal('product-modal');
  let products = [];

  async function loadFacility() {
    try {
      const f = await Api.get(`/api/company/facilities/${facilityId}`);
      document.getElementById('title').textContent = `Productos · ${f.name}`;
      document.getElementById('crumbs').innerHTML = Ui.crumbs([
        { text: 'Instalaciones', href: '/company/facilities' }, { text: f.name }, { text: 'Productos' }]);
    } catch (err) {
      Ui.flash(err.message, 'error');
    }
  }

  async function load() {
    rows.innerHTML = Ui.loadingRow(3);
    try {
      products = await Api.get(API);
      render();
    } catch (err) {
      rows.innerHTML = Ui.emptyRow(3, err.message);
    }
  }

  function render() {
    const q = search.value.trim().toLowerCase();
    const list = products.filter((p) => !q || p.name.toLowerCase().includes(q));
    rows.innerHTML = list.length ? list.map((p) => `
      <tr>
        <td><strong>${Ui.esc(p.name)}</strong></td>
        <td class="num">${Ui.money(p.price)}</td>
        <td class="acciones">
          <button class="btn btn-sm btn-secondary" data-edit="${p.id}">Editar</button>
          <button class="btn btn-sm btn-danger" data-delete="${p.id}">Eliminar</button>
        </td>
      </tr>`).join('') : Ui.emptyRow(3, products.length ? 'No se encontraron productos.' : 'Esta instalación aún no tiene productos.');
  }

  function openForm(product) {
    Ui.resetForm(form);
    form.querySelector('.modal-title').textContent = product ? 'Editar Producto' : 'Nuevo Producto';
    if (product) Ui.fillForm(form, product);
    modal.show();
  }

  document.getElementById('btn-new').addEventListener('click', () => openForm(null));
  search.addEventListener('input', render);

  rows.addEventListener('click', async (e) => {
    const { edit, delete: del } = e.target.dataset;
    if (edit) openForm(products.find((p) => String(p.id) === edit));
    if (del) {
      const p = products.find((x) => String(x.id) === del);
      const ok = await Ui.confirm(`¿Eliminar el producto "${p.name}"? Las reservas ya hechas lo conservan.`,
        { title: 'Eliminar producto', okText: 'Eliminar', danger: true });
      if (!ok) return;
      try {
        await Api.del(`${API}/${del}`);
        Ui.flash('Producto eliminado.');
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
      Ui.flash(data.id ? 'Producto actualizado.' : 'Producto registrado.');
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
