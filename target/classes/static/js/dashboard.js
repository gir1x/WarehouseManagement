(function () {
  const pendingNotice = document.getElementById('pendingNotice');
  const mainPanels = document.getElementById('mainPanels');

  function fmtPercent(n) { return n.toFixed(1) + '%'; }

  function fmtWhen(iso) {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' ' +
      d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }

  function renderKpis(summary) {
    document.getElementById('kpiWarehouses').textContent = summary.totalWarehouses;
    document.getElementById('kpiProducts').textContent = summary.totalProducts;
    document.getElementById('kpiFilled').textContent = summary.filledSlots;
    document.getElementById('kpiFilledSub').textContent = summary.totalSlots + ' total slots';
    document.getElementById('kpiUtilization').textContent = fmtPercent(summary.utilizationPercent);
    document.getElementById('kpiAvailableSub').textContent = summary.availableSlots + ' available';
  }

  function renderNearFull(warehouses) {
    const list = document.getElementById('nearFullList');
    const empty = document.getElementById('nearFullEmpty');
    const nearFull = warehouses.filter(function (w) { return w.nearFull; });

    list.innerHTML = '';
    if (nearFull.length === 0) {
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';

    nearFull.forEach(function (w) {
      const li = document.createElement('li');
      const name = document.createElement('a');
      name.className = 'alert-list__name';
      name.href = '/warehouses/' + w.id;
      name.textContent = w.name;

      const meta = document.createElement('span');
      meta.className = 'badge-pill badge-pill--warning';
      meta.textContent = fmtPercent(w.utilizationPercent) + ' full';

      li.appendChild(name);
      li.appendChild(meta);
      list.appendChild(li);
    });
  }

  function renderLowStock(products) {
    const list = document.getElementById('lowStockList');
    const empty = document.getElementById('lowStockEmpty');

    list.innerHTML = '';
    if (products.length === 0) {
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';

    products.forEach(function (p) {
      const li = document.createElement('li');
      const name = document.createElement('span');
      name.className = 'alert-list__name';
      name.textContent = p.name;

      const meta = document.createElement('span');
      meta.className = 'badge-pill badge-pill--danger';
      meta.textContent = p.totalOnHand + ' ' + p.unit + ' on hand (reorder at ' + p.reorderThreshold + ')';

      li.appendChild(name);
      li.appendChild(meta);
      list.appendChild(li);
    });
  }

  function renderActivity(movements) {
    const rows = document.getElementById('activityRows');
    const empty = document.getElementById('activityEmpty');

    rows.innerHTML = '';
    if (movements.length === 0) {
      empty.style.display = 'block';
      return;
    }
    empty.style.display = 'none';

    movements.forEach(function (m) {
      const tr = document.createElement('tr');
      tr.innerHTML =
        '<td>' + fmtWhen(m.occurredAt) + '</td>' +
        '<td>' + m.warehouseName + '</td>' +
        '<td class="mono">A' + m.aisle + '-T' + m.tier + '</td>' +
        '<td class="cell-sku">' + m.productSku + '</td>' +
        '<td>' + m.quantity + '</td>' +
        '<td class="cell-type cell-type--' + m.type.toLowerCase() + '">' + m.type + '</td>' +
        '<td>' + m.performedBy + '</td>';
      rows.appendChild(tr);
    });
  }

  function loadSummary() {
    Api.get('/api/dashboard/summary').then(function (summary) {
      renderKpis(summary);
      renderNearFull(summary.warehouses);
      renderLowStock(summary.lowStockProducts);
      renderActivity(summary.recentActivity);
    });
  }

  initTopNav('/dashboard').then(function (me) {
    if (!me) return;
    if (me.role === 'PENDING') {
      pendingNotice.style.display = 'block';
    } else {
      mainPanels.style.display = 'block';
      loadSummary();
    }
  });
})();
