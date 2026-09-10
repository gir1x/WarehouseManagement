(function () {
  const productList = document.getElementById('productList');
  const productEmpty = document.getElementById('productEmpty');
  const createPanel = document.getElementById('createPanel');
  const createForm = document.getElementById('createForm');
  const createBtn = document.getElementById('createBtn');

  let lowStockSkus = new Set();
  let isAdmin = false;

  function renderProducts(products) {
    productList.innerHTML = '';
    if (products.length === 0) {
      productEmpty.style.display = 'block';
      return;
    }
    productEmpty.style.display = 'none';
    products.forEach(function (p) {
      const li = document.createElement('li');

      const name = document.createElement('span');
      name.textContent = p.name;
      name.style.fontFamily = 'var(--font-display)';
      name.style.fontSize = 'var(--text-lg)';

      const meta = document.createElement('span');
      meta.className = 'row-meta';

      const sku = document.createElement('span');
      sku.className = 'dims mono';
      sku.textContent = p.sku + ' · ' + p.unit;
      meta.appendChild(sku);

      if (lowStockSkus.has(p.sku)) {
        const badge = document.createElement('span');
        badge.className = 'badge-pill badge-pill--danger';
        badge.textContent = 'Low stock';
        meta.appendChild(badge);
      }

      if (isAdmin) {
        const deleteBtn = document.createElement('button');
        deleteBtn.type = 'button';
        deleteBtn.className = 'btn-danger-sm';
        deleteBtn.textContent = 'Delete';
        deleteBtn.addEventListener('click', function () { deleteProduct(p); });
        meta.appendChild(deleteBtn);
      }

      li.appendChild(name);
      li.appendChild(meta);
      productList.appendChild(li);
    });
  }

  function deleteProduct(product) {
    if (!window.confirm('Delete "' + product.name + '" (' + product.sku + ')? This can\'t be undone.')) {
      return;
    }
    hideBanner('banner');
    Api.delete('/api/products/' + product.id)
      .then(function () {
        showBanner('banner', product.sku + ' deleted.', 'success');
        loadProducts();
      })
      // Most likely failure here is the "still stocked somewhere" guard in
      // ProductService — show that message rather than a generic one.
      .catch(function (err) { showBanner('banner', err.message, 'error'); });
  }

  function loadProducts() {
    Api.get('/api/products')
      .then(renderProducts)
      .catch(function (err) { showBanner('banner', err.message, 'error'); });
  }

  initTopNav('/products').then(function (me) {
    if (!me) return;
    isAdmin = me.role === 'ADMIN';
    if (isAdmin) {
      createPanel.style.display = 'block';
    }
    if (me.role === 'ADMIN' || me.role === 'VISITOR') {
      // Cross-reference against the dashboard's low-stock list so this page
      // can flag the same products without duplicating the aggregation logic.
      Api.get('/api/dashboard/summary').then(function (summary) {
        lowStockSkus = new Set(summary.lowStockProducts.map(function (p) { return p.sku; }));
        loadProducts();
      }).catch(function () { loadProducts(); });
    } else {
      loadProducts();
    }
  });

  createForm.addEventListener('submit', function (e) {
    e.preventDefault();
    hideBanner('banner');
    createBtn.disabled = true;
    createBtn.textContent = 'Adding…';

    Api.post('/api/products', {
      sku: document.getElementById('sku').value,
      name: document.getElementById('name').value,
      reorderThreshold: Number(document.getElementById('reorderThreshold').value),
      unit: document.getElementById('unit').value,
    })
      .then(function () {
        document.getElementById('sku').value = '';
        document.getElementById('name').value = '';
        document.getElementById('reorderThreshold').value = 5;
        document.getElementById('unit').value = 'pcs';
        showBanner('banner', 'Product added.', 'success');
        loadProducts();
      })
      .catch(function (err) {
        showBanner('banner', err.message, 'error');
      })
      .finally(function () {
        createBtn.disabled = false;
        createBtn.textContent = 'Add product';
      });
  });
})();
