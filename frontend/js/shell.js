/**
 * Injects the sidebar/topbar shell used by every authenticated screen and
 * wraps the page's own <main id="gg-page"> content. Call GGShell.render('dashboard')
 * (or 'problems' / 'search' / 'settings') near the top of each page's script.
 */
const GGShell = {
  NAV_ITEMS: [
    { key: 'dashboard', label: 'Dashboard', href: 'dashboard.html', icon: 'grid' },
    { key: 'add-problem', label: 'Add Problem', href: 'problem-form.html', icon: 'plus' },
    { key: 'search', label: 'Search', href: 'search.html', icon: 'search' },
    { key: 'settings', label: 'Settings', href: 'settings.html', icon: 'settings' },
  ],

  ICONS: {
    grid: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg>',
    plus: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>',
    search: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
    settings: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 00.3 1.9l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.9-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1-1.6 1.7 1.7 0 00-1.9.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.9 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1 1.7 1.7 0 00-.3-1.9l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.9.3H9a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.9-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.9V9a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z"/></svg>',
    logout: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/></svg>',
    menu: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18M3 6h18M3 18h18"/></svg>',
  },

  render(activeKey) {
    GGAuth.requireAuth();
    const user = GGAuth.getUser();
    const theme = user ? undefined : null;

    document.documentElement.setAttribute('data-theme', localStorage.getItem('gg_theme') || 'dark');

    const navHtml = this.NAV_ITEMS.map(item => `
      <a href="${item.href}" class="gg-nav-link ${item.key === activeKey ? 'active' : ''}">
        ${this.ICONS[item.icon]}<span>${item.label}</span>
      </a>
    `).join('');

    const shell = document.createElement('div');
    shell.className = 'gg-shell';
    shell.innerHTML = `
      <aside class="gg-sidebar" id="gg-sidebar">
        <div class="gg-logo">
          <span class="gg-logo-mark"><span></span><span></span><span></span><span></span></span>
          GreenGrid
        </div>
        <nav>${navHtml}</nav>
        <div class="gg-sidebar-footer">
          <div class="d-flex align-items-center gap-2 px-2 py-2">
            <div style="width:28px;height:28px;border-radius:7px;background:var(--accent-soft);color:var(--accent);display:flex;align-items:center;justify-content:center;font-size:.75rem;font-weight:700;">
              ${(user?.displayName || '?').charAt(0).toUpperCase()}
            </div>
            <div class="flex-grow-1 overflow-hidden">
              <div style="font-size:.82rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${user?.displayName || ''}</div>
            </div>
            <button class="btn-gg-icon" id="gg-logout-btn" title="Log out">${this.ICONS.logout}</button>
          </div>
        </div>
      </aside>
      <main class="gg-main" id="gg-main-content"></main>
    `;

    const pageContent = document.getElementById('gg-page');
    const contentHtml = pageContent ? pageContent.innerHTML : '';
    document.body.innerHTML = '';
    document.body.appendChild(shell);
    document.getElementById('gg-main-content').innerHTML = contentHtml;
    document.getElementById('gg-logout-btn').addEventListener('click', () => GGAuth.logout());
  },
};
