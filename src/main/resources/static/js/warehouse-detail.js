(function () {
  const warehouseId = document.body.getAttribute('data-warehouse-id');

  const floorEl = document.getElementById('floor');
  const receiveForm = document.getElementById('receiveForm');
  const receiveBtn = document.getElementById('receiveBtn');
  const receiveSlotLabel = document.getElementById('receiveSlotLabel');
  const receiveSlotClear = document.getElementById('receiveSlotClear');
  const pickBtn = document.getElementById('pickBtn');
  const pickEmpty = document.getElementById('pickEmpty');
  const pickSelected = document.getElementById('pickSelected');
  const pickQtyInput = document.getElementById('pickQtyInput');

  let selectedReceiveSlotId = null;
  let selectedPickSlotId = null;

  function loadMe() {
    initTopNav('/warehouses');
  }

  function loadProductOptions() {
    const select = document.getElementById('sku');
    Api.get('/api/products').then(function (products) {
      select.innerHTML = '';
      if (products.length === 0) {
        select.innerHTML = '<option value="" disabled selected>No products yet</option>';
        receiveBtn.disabled = true;
        return;
      }
      products.forEach(function (p, i) {
        const opt = document.createElement('option');
        opt.value = p.sku;
        opt.textContent = p.sku + ' — ' + p.name + ' (' + p.unit + ')';
        if (i === 0) opt.selected = true;
        select.appendChild(opt);
      });
      receiveBtn.disabled = false;
    }).catch(function (err) {
      select.innerHTML = '<option value="" disabled selected>Couldn\'t load products</option>';
      showBanner('banner', err.message, 'error');
    });
  }

  function loadWarehouseInfo() {
    Api.get('/api/warehouses/' + warehouseId).then(function (wh) {
      document.getElementById('whName').textContent = wh.name;
      document.getElementById('whNameCrumb').textContent = wh.name;
      document.getElementById('whDims').textContent =
        wh.totalAisles + ' aisles × ' + wh.totalTiers + ' tiers · ' + (wh.totalAisles * wh.totalTiers) + ' slots total';
    }).catch(function (err) {
      showBanner('banner', err.message, 'error');
    });
  }

  function loadReport() {
    Api.get('/api/warehouses/' + warehouseId + '/report/occupancy').then(function (report) {
      const pct = Math.round(report.utilizationPercent);

      document.getElementById('statTotal').textContent = report.totalSlots;
      document.getElementById('statFilled').textContent = report.filledSlots;
      document.getElementById('statAvailable').textContent = report.availableSlots;
      document.getElementById('hpCaption').textContent = pct + '% full';
      document.getElementById('nearFullBadge').style.display = pct >= 85 ? 'inline-flex' : 'none';

      renderHpBar(pct);
    }).catch(function (err) {
      showBanner('banner', err.message, 'error');
    });
  }

  function renderHpBar(pct) {
    const bar = document.getElementById('hpBar');
    bar.innerHTML = '';
    const fill = document.createElement('div');
    fill.className = 'hp-bar__fill';
    fill.style.width = Math.max(0, Math.min(100, pct)) + '%';
    bar.appendChild(fill);
  }

  // --- Receive: choosing a destination slot --------------------------------

  function clearReceiveSelection() {
    selectedReceiveSlotId = null;
    receiveSlotLabel.textContent = 'Auto-assign (first available)';
    receiveSlotClear.style.display = 'none';
    document.querySelectorAll('.slot.is-selected-receive').forEach(function (el) {
      el.classList.remove('is-selected-receive');
    });
  }

  function selectReceiveSlot(slot, cellEl) {
    selectedReceiveSlotId = slot.id;
    document.querySelectorAll('.slot.is-selected-receive').forEach(function (el) {
      el.classList.remove('is-selected-receive');
    });
    cellEl.classList.add('is-selected-receive');

    receiveSlotLabel.textContent = 'A' + slot.aisle + ' · T' + slot.tier;
    receiveSlotClear.style.display = 'inline-flex';
  }

  receiveSlotClear.addEventListener('click', clearReceiveSelection);

  // --- Pick: choosing a source slot + quantity ------------------------------

  function clearPickSelection() {
    selectedPickSlotId = null;
    pickEmpty.style.display = 'block';
    pickSelected.style.display = 'none';
    document.querySelectorAll('.slot.is-selected-pick').forEach(function (el) {
      el.classList.remove('is-selected-pick');
    });
  }

  function selectPickSlot(slot, cellEl) {
    selectedPickSlotId = slot.id;
    document.querySelectorAll('.slot.is-selected-pick').forEach(function (el) {
      el.classList.remove('is-selected-pick');
    });
    cellEl.classList.add('is-selected-pick');

    document.getElementById('pickCoord').textContent = 'A' + slot.aisle + ' · T' + slot.tier;
    document.getElementById('pickSku').textContent = slot.productSku;
    document.getElementById('pickAvailable').textContent = slot.quantity;
    pickQtyInput.max = slot.quantity;
    pickQtyInput.value = slot.quantity; // defaults to picking everything; user can lower it
    pickEmpty.style.display = 'none';
    pickSelected.style.display = 'block';
  }

  // --- Floor plan rendering --------------------------------------------------

  function renderFloor(slots) {
    floorEl.innerHTML = '';

    if (slots.length === 0) {
      floorEl.innerHTML = '<div class="floor-empty">This warehouse has no slots.</div>';
      return;
    }

    const maxAisle = Math.max.apply(null, slots.map(function (s) { return s.aisle; }));
    const maxTier = Math.max.apply(null, slots.map(function (s) { return s.tier; }));

    const byCoord = {};
    slots.forEach(function (s) { byCoord[s.aisle + '-' + s.tier] = s; });

    const table = document.createElement('div');
    table.className = 'floor__table';
    table.style.gridTemplateColumns = 'auto repeat(' + maxAisle + ', 64px)';

    // Corner + column headers (aisle numbers)
    table.appendChild(document.createElement('div'));
    for (let a = 1; a <= maxAisle; a++) {
      const label = document.createElement('div');
      label.className = 'floor__axis-label';
      label.textContent = 'A' + a;
      table.appendChild(label);
    }

    // Rows: highest tier at the top, like physical racking
    for (let t = maxTier; t >= 1; t--) {
      const rowLabel = document.createElement('div');
      rowLabel.className = 'floor__axis-label';
      rowLabel.textContent = 'T' + t;
      table.appendChild(rowLabel);

      for (let a = 1; a <= maxAisle; a++) {
        const slot = byCoord[a + '-' + t];
        const cell = document.createElement('div');

        if (!slot) {
          cell.className = 'slot';
          cell.style.visibility = 'hidden';
          table.appendChild(cell);
          continue;
        }

        const isOccupied = slot.status === 'OCCUPIED';
        cell.className = 'slot' + (isOccupied ? ' is-occupied' : '');
        cell.innerHTML =
          '<span class="slot__coord">A' + slot.aisle + '·T' + slot.tier + '</span>' +
          (isOccupied
            ? '<span class="slot__sku">' + slot.productSku + ' ×' + slot.quantity + '</span>'
            : '<span class="slot__sku">empty</span>');

        cell.addEventListener('click', function () {
          if (isOccupied) {
            clearReceiveSelection();
            selectPickSlot(slot, cell);
          } else {
            clearPickSelection();
            selectReceiveSlot(slot, cell);
          }
        });

        table.appendChild(cell);
      }
    }

    floorEl.appendChild(table);
  }

  function loadFloor() {
    Api.get('/api/warehouses/' + warehouseId + '/grid').then(function (slots) {
      renderFloor(slots);
    }).catch(function (err) {
      showBanner('banner', err.message, 'error');
    });
  }

  // --- Activity log (audit trail of receives/picks for this warehouse) ------

  function fmtWhen(iso) {
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' ' +
      d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
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
        '<td class="mono">A' + m.aisle + '-T' + m.tier + '</td>' +
        '<td class="cell-sku">' + m.productSku + '</td>' +
        '<td>' + m.quantity + '</td>' +
        '<td class="cell-type cell-type--' + m.type.toLowerCase() + '">' + m.type + '</td>' +
        '<td>' + m.performedBy + '</td>';
      rows.appendChild(tr);
    });
  }

  function loadActivity() {
    Api.get('/api/warehouses/' + warehouseId + '/movements').then(renderActivity);
  }

  function refreshAll() {
    loadFloor();
    loadReport();
    loadActivity();
  }

  // --- Actions -----------------------------------------------------------

  receiveForm.addEventListener('submit', function (e) {
    e.preventDefault();
    hideBanner('banner');
    receiveBtn.disabled = true;
    receiveBtn.textContent = 'Receiving…';

    Api.post('/api/warehouses/' + warehouseId + '/receive', {
      productSku: document.getElementById('sku').value,
      quantity: Number(document.getElementById('qty').value),
      slotId: selectedReceiveSlotId, // null = let the server auto-assign
    })
      .then(function (result) {
        showBanner('banner', 'Received into slot A' + result.aisle + ' · T' + result.tier + '.', 'success');
        document.getElementById('qty').value = 1;
        clearReceiveSelection();
        refreshAll();
      })
      .catch(function (err) {
        showBanner('banner', err.message, 'error');
      })
      .finally(function () {
        receiveBtn.disabled = false;
        receiveBtn.textContent = 'Receive →';
      });
  });

  pickBtn.addEventListener('click', function () {
    if (!selectedPickSlotId) return;
    const quantity = Number(pickQtyInput.value);
    if (!quantity || quantity < 1) {
      showBanner('banner', 'Enter a quantity of at least 1.', 'error');
      return;
    }

    hideBanner('banner');
    pickBtn.disabled = true;
    pickBtn.textContent = 'Picking…';

    Api.post('/api/warehouses/' + warehouseId + '/pick', { slotId: selectedPickSlotId, quantity: quantity })
      .then(function (result) {
        showBanner('banner', 'Picked ' + quantity + ' from slot A' + result.aisle + ' · T' + result.tier + '.', 'success');
        clearPickSelection();
        refreshAll();
      })
      .catch(function (err) {
        showBanner('banner', err.message, 'error');
      })
      .finally(function () {
        pickBtn.disabled = false;
        pickBtn.textContent = '← Pick from this slot';
      });
  });

  loadMe();
  loadWarehouseInfo();
  loadProductOptions();
  refreshAll();
})();
