/* Login de admin y de empresa (el área viene de <body data-area>). */
(function () {
  const area = document.body.dataset.area;
  const HOME = { admin: '/admin/companies', company: '/company/facilities' };

  if (Layout.currentUser(area)) {
    location.replace(HOME[area]);
    return;
  }

  const form = document.getElementById('login-form');
  const btn = form.querySelector('button[type=submit]');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    Ui.clearErrors(form);
    Ui.busy(btn, true, 'Ingresando...');
    try {
      const res = await Api.post(`/api/${area}/login`, Ui.formData(form));
      Api.setToken(area, res.token);
      location.replace(HOME[area]);
    } catch (err) {
      Ui.showErrors(form, err);
      Ui.busy(btn, false);
    }
  });
})();
