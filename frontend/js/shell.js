/**
 * GreenGrid Shell
 * ──────────────
 * Injects Bootstrap 5 (needed by all authenticated pages), renders the
 * sidebar, mobile topbar, and wraps the page's own content.
 *
 * Usage: GGShell.render('dashboard')  ('add-problem' / 'search' / 'settings')
 */
const GGShell = {
  NAV_ITEMS: [
    { key: 'dashboard',   label: 'Dashboard',   href: 'dashboard.html',    icon: 'grid'     },
    { key: 'add-problem', label: 'Add Problem',  href: 'problem-form.html', icon: 'plus'     },
    { key: 'search',      label: 'Search',       href: 'search.html',       icon: 'search'   },
    { key: 'friends',     label: 'Friends',      href: 'friends.html',      icon: 'users'    },
    { key: 'settings',    label: 'Settings',     href: 'settings.html',     icon: 'settings' },
  ],

  ICONS: {
    grid: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>`,
    plus: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>`,
    search: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>`,
    users: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
    settings: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>`,
    logout: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/></svg>`,
    menu: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12h18M3 6h18M3 18h18"/></svg>`,
    logo_mark: ``,
  },

  /** Inject Bootstrap CSS once per page (avoids duplicate tags). */
  _ensureBootstrap() {
    if (document.getElementById('gg-bs-css')) return;
    const link = document.createElement('link');
    link.id   = 'gg-bs-css';
    link.rel  = 'stylesheet';
    link.href = 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css';
    // Insert before the page's own stylesheet so GG tokens win
    const ggCss = document.querySelector('link[href*="styles.css"]');
    if (ggCss) document.head.insertBefore(link, ggCss);
    else document.head.appendChild(link);
  },

  render(activeKey) {
    GGAuth.requireAuth();
    this._ensureBootstrap();

    const user = GGAuth.getUser();
    const theme = localStorage.getItem('gg_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', theme);

    const initial = (user?.displayName || '?').charAt(0).toUpperCase();
    const displayName = user?.displayName || '';

    const navHtml = this.NAV_ITEMS.map(item => `
      <a href="${item.href}" class="gg-nav-link ${(item.key === activeKey || (item.key === 'add-problem' && activeKey === 'edit-problem')) ? 'active' : ''}" id="nav-${item.key}">
        ${this.ICONS[item.icon]}<span>${item.label}</span>
      </a>
    `).join('');


    // Capture page content before wiping body
    const pageContent = document.getElementById('gg-page');
    const contentHtml = pageContent ? pageContent.innerHTML : '';

    const shell = document.createElement('div');
    shell.innerHTML = `
      <!-- Mobile topbar -->
      <div class="gg-topbar" id="gg-topbar">
        <button class="btn-gg-icon" id="gg-menu-btn" aria-label="Open menu" style="border:none;background:transparent;">
          ${this.ICONS.menu}
        </button>
        <div class="gg-logo" style="padding:0; font-size:.92rem;">
          <span class="gg-logo-mark"><span></span><span></span><span></span><span></span></span>
          GreenGrid
        </div>
        <div style="width:32px;"></div><!-- spacer -->
      </div>

      <!-- Sidebar backdrop (mobile) -->
      <div class="gg-sidebar-backdrop" id="gg-backdrop"></div>

      <div class="gg-shell">
        <!-- Sidebar -->
        <aside class="gg-sidebar" id="gg-sidebar">
          <a href="dashboard.html" class="gg-logo">
            <span class="gg-logo-mark"><span></span><span></span><span></span><span></span></span>
            GreenGrid
          </a>
          <nav>${navHtml}</nav>
          <div class="gg-sidebar-footer">
            <div class="gg-user-chip">
              <div class="gg-avatar">${initial}</div>
              <div style="flex:1;min-width:0;">
                <div class="gg-user-name">${displayName}</div>
              </div>
              <button class="btn-gg-icon" id="gg-logout-btn" title="Log out" style="flex-shrink:0;">
                ${this.ICONS.logout}
              </button>
            </div>
          </div>
        </aside>

        <!-- Main content area -->
        <main class="gg-main" id="gg-main-content"></main>
      </div>
    `;

    document.body.innerHTML = '';
    document.body.appendChild(shell);

    // Restore page content
    document.getElementById('gg-main-content').innerHTML = contentHtml;

    // Logout
    document.getElementById('gg-logout-btn')
      .addEventListener('click', () => GGAuth.logout());

    // Mobile sidebar toggle
    const sidebar  = document.getElementById('gg-sidebar');
    const backdrop = document.getElementById('gg-backdrop');
    const menuBtn  = document.getElementById('gg-menu-btn');

    function openSidebar()  { sidebar.classList.add('open');  backdrop.classList.add('open'); }
    function closeSidebar() { sidebar.classList.remove('open'); backdrop.classList.remove('open'); }

    menuBtn && menuBtn.addEventListener('click', openSidebar);
    backdrop.addEventListener('click', closeSidebar);

    // Close on nav click (mobile)
    sidebar.querySelectorAll('.gg-nav-link').forEach(link => {
      link.addEventListener('click', () => {
        if (window.innerWidth <= 900) closeSidebar();
      });
    });
  },

  /** Show a slim top-of-page progress bar for async operations. */
  progress: {
    _el: null,
    _timer: null,
    show() {
      if (!this._el) {
        this._el = document.createElement('div');
        this._el.className = 'gg-progress-bar';
        this._el.style.cssText = 'position:fixed;top:0;left:0;height:2px;z-index:9999;transition:width .3s,opacity .4s;border-radius:0 2px 2px 0;box-shadow:0 0 8px var(--accent-glow);';
        this._el.style.background = 'var(--accent)';
        document.body.appendChild(this._el);
      }
      this._el.style.width   = '0%';
      this._el.style.opacity = '1';
      // Animate to 80% quickly, hold there until done()
      clearTimeout(this._timer);
      requestAnimationFrame(() => { this._el.style.width = '75%'; });
    },
    done() {
      if (!this._el) return;
      this._el.style.width = '100%';
      this._timer = setTimeout(() => { if (this._el) this._el.style.opacity = '0'; }, 300);
    },
  },
};
