(function () {
  const whList = document.getElementById('whList');
  const whEmpty = document.getElementById('whEmpty');
  const createPanel = document.getElementById('createPanel');
  const createForm = document.getElementById('createForm');
  const createBtn = document.getElementById('createBtn');

  function renderWarehouses(warehouses) {
    whList.innerHTML = '';
    if (warehouses.length === 0) {
      whEmpty.style.display = 'block';
      return;
    }
    whEmpty.style.display = 'none';
    warehouses.forEach(function (wh) {
      const li = document.createElement('li');
      const link = document.createElement('a');
      link.href = '/warehouses/' + wh.id;
      link.textContent = wh.name;

      const dims = document.createElement('span');
      dims.className = 'dims';
      dims.textContent = wh.totalAisles + ' aisles × ' + wh.totalTiers + ' tiers';

      li.appendChild(link);
      li.appendChild(dims);
      whList.appendChild(li);
    });
  }

  function loadWarehouses() {
    Api.get('/api/warehouses')
      .then(renderWarehouses)
      .catch(function (err) { showBanner('banner', err.message, 'error'); });
  }

  // Only ADMIN can create a warehouse — hide the form for anyone else.
  // (This is UX only; the server enforces the real rule either way.)
  initTopNav('/warehouses').then(function (me) {
    if (me && me.role === 'ADMIN') {
      createPanel.style.display = 'block';
    }
  });

  createForm.addEventListener('submit', function (e) {
    e.preventDefault();
    hideBanner('banner');
    createBtn.disabled = true;
    createBtn.textContent = 'Creating…';

    Api.post('/api/warehouses', {
      name: document.getElementById('name').value,
      aisles: Number(document.getElementById('aisles').value),
      tiers: Number(document.getElementById('tiers').value),
    })
      .then(function (wh) {
        window.location.href = '/warehouses/' + wh.id;
      })
      .catch(function (err) {
        showBanner('banner', err.message, 'error');
        createBtn.disabled = false;
        createBtn.textContent = 'Create warehouse';
      });
  });

  loadWarehouses();
})();
