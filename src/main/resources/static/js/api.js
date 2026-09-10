/**
 * Small fetch wrapper shared by every page. The browser's session cookie
 * (set by Spring Security on form login) is sent automatically for
 * same-origin requests, and /api/** has CSRF protection disabled (see
 * SecurityConfig), so no token handling is needed here.
 */
const Api = {
  async request(method, url, body) {
    const res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });

    let payload = null;
    const text = await res.text();
    if (text) {
      try { payload = JSON.parse(text); } catch (e) { payload = text; }
    }

    if (!res.ok) {
      const message = (payload && payload.message) ? payload.message : `Request failed (${res.status})`;
      const err = new Error(message);
      err.status = res.status;
      throw err;
    }

    return payload;
  },

  get(url) { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body); },
  put(url, body) { return this.request('PUT', url, body); },
  delete(url) { return this.request('DELETE', url); },
};

/** Shows a banner (success/error) inside the given container element id. */
function showBanner(elId, message, kind) {
  const el = document.getElementById(elId);
  if (!el) return;
  el.textContent = message;
  el.className = 'banner is-visible ' + (kind === 'error' ? 'banner-error' : 'banner-success');
}

function hideBanner(elId) {
  const el = document.getElementById(elId);
  if (!el) return;
  el.className = 'banner';
}

/**
 * Fills in the user badge + top nav links on any authenticated page, and
 * returns the /api/me payload so the calling page can react to the role
 * (e.g. hide an admin-only form, or show the "pending approval" notice).
 * Expects a <span id="userLabel"> and a <nav id="topNav"> in the page.
 */
const NAV_ICONS = {
  '/dashboard': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></svg>',
  '/warehouses': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9.5 12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z"/></svg>',
  '/products': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21 8-9-5-9 5 9 5 9-5Z"/><path d="M3 8v8l9 5 9-5V8"/><path d="M12 13v8"/></svg>',
  '/admin/users': '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
};

function initTopNav(activePath) {
  return Api.get('/api/me').then(function (me) {
    const userLabel = document.getElementById('userLabel');
    if (userLabel) {
      userLabel.textContent = me.username;
      const badge = document.createElement('span');
      badge.className = 'role-badge';
      badge.textContent = me.role;
      userLabel.parentElement.appendChild(badge);
    }

    const avatar = document.getElementById('userAvatar');
    if (avatar) {
      avatar.textContent = (me.username || '?').charAt(0);
    }

    const nav = document.getElementById('topNav');
    if (nav) {
      const links = [{ href: '/dashboard', label: 'Dashboard' }];
      // PENDING has no access to warehouses/products at all — don't dangle a link
      // to a page that will just 403 on every data call.
      if (me.role === 'ADMIN' || me.role === 'VISITOR') {
        links.push({ href: '/warehouses', label: 'Warehouses' });
        links.push({ href: '/products', label: 'Products' });
      }
      if (me.role === 'ADMIN') {
        links.push({ href: '/admin/users', label: 'Manage users' });
      }
      nav.innerHTML = '';
      links.forEach(function (link) {
        const a = document.createElement('a');
        a.href = link.href;
        a.innerHTML = (NAV_ICONS[link.href] || '') + '<span>' + link.label + '</span>';
        if (link.href === activePath) a.className = 'is-active';
        nav.appendChild(a);
      });
    }

    return me;
  }).catch(function () { return null; });
}
