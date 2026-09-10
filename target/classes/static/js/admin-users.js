(function () {
  const userRows = document.getElementById('userRows');
  const userEmpty = document.getElementById('userEmpty');
  const roles = ['PENDING', 'VISITOR', 'ADMIN'];
  let currentUsername = null;

  function roleClass(role) {
    if (role === 'ADMIN') return 'role-pill role-pill--admin';
    if (role === 'VISITOR') return 'role-pill role-pill--visitor';
    return 'role-pill role-pill--pending';
  }

  function renderUsers(users) {
    userRows.innerHTML = '';
    if (users.length === 0) {
      userEmpty.style.display = 'block';
      return;
    }
    userEmpty.style.display = 'none';

    users.forEach(function (u) {
      const tr = document.createElement('tr');

      const tdUsername = document.createElement('td');
      tdUsername.textContent = u.username;
      tdUsername.className = 'mono';

      const tdEmail = document.createElement('td');
      tdEmail.textContent = u.email || '—';

      const tdProvider = document.createElement('td');
      tdProvider.textContent = u.authProvider;

      const tdRole = document.createElement('td');
      const pill = document.createElement('span');
      pill.className = roleClass(u.role);
      pill.textContent = u.role;
      tdRole.appendChild(pill);

      const tdAction = document.createElement('td');
      const tdDelete = document.createElement('td');

      if (u.username === currentUsername) {
        const note = document.createElement('span');
        note.className = 'helper-text';
        note.textContent = "That's you";
        tdAction.appendChild(note);
      } else {
        const row = document.createElement('div');
        row.className = 'role-select-row';

        const select = document.createElement('select');
        roles.forEach(function (r) {
          const opt = document.createElement('option');
          opt.value = r;
          opt.textContent = r;
          if (r === u.role) opt.selected = true;
          select.appendChild(opt);
        });

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn-primary';
        btn.textContent = 'Save';
        btn.addEventListener('click', function () {
          hideBanner('banner');
          btn.disabled = true;
          btn.textContent = 'Saving…';

          Api.put('/api/admin/users/' + u.id + '/role', { role: select.value })
            .then(function () {
              showBanner('banner', u.username + ' is now ' + select.value + '.', 'success');
              loadUsers();
            })
            .catch(function (err) {
              showBanner('banner', err.message, 'error');
              btn.disabled = false;
              btn.textContent = 'Save';
            });
        });

        row.appendChild(select);
        row.appendChild(btn);
        tdAction.appendChild(row);

        const deleteBtn = document.createElement('button');
        deleteBtn.type = 'button';
        deleteBtn.className = 'btn-danger-sm';
        deleteBtn.textContent = 'Delete';
        deleteBtn.addEventListener('click', function () {
          if (!window.confirm('Delete the account "' + u.username + '"? This can\'t be undone.')) {
            return;
          }
          hideBanner('banner');
          deleteBtn.disabled = true;

          Api.delete('/api/admin/users/' + u.id)
            .then(function () {
              showBanner('banner', u.username + ' has been deleted.', 'success');
              loadUsers();
            })
            .catch(function (err) {
              showBanner('banner', err.message, 'error');
              deleteBtn.disabled = false;
            });
        });
        tdDelete.appendChild(deleteBtn);
      }

      tr.appendChild(tdUsername);
      tr.appendChild(tdEmail);
      tr.appendChild(tdProvider);
      tr.appendChild(tdRole);
      tr.appendChild(tdAction);
      tr.appendChild(tdDelete);
      userRows.appendChild(tr);
    });
  }

  function loadUsers() {
    Api.get('/api/admin/users')
      .then(renderUsers)
      .catch(function (err) { showBanner('banner', err.message, 'error'); });
  }

  initTopNav('/admin/users').then(function (me) {
    if (me) currentUsername = me.username;
    loadUsers();
  });
})();
