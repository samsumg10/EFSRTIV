/* /company/profile — datos de la empresa + mi cuenta. */
(function () {
  const user = Layout.init({ area: 'company', menu: 'profile', adminOnly: true });
  if (!user) return;

  const API = '/api/company/profile';
  const form = document.getElementById('profile-form');
  const loading = document.getElementById('loading');

  function show(profile) {
    Ui.fillForm(form, profile);
    form.accountPassword.value = '';
    form.accountPasswordConfirmation.value = '';
    document.getElementById('uuid-line').innerHTML =
      `Código de la empresa: <code class="uuid">${Ui.esc(profile.uuid)}</code>`;
  }

  async function load() {
    try {
      show(await Api.get(API));
      loading.hidden = true;
      form.hidden = false;
    } catch (err) {
      loading.textContent = err.message;
    }
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    Ui.clearErrors(form);
    const btn = form.querySelector('button[type=submit]');
    Ui.busy(btn, true);
    try {
      show(await Api.put(API, Ui.formData(form)));
      Ui.flash('Perfil actualizado.');
    } catch (err) {
      Ui.showErrors(form, err);
    } finally {
      Ui.busy(btn, false);
    }
  });

  load();
})();
