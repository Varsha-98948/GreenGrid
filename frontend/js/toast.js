const GGToast = {
  container() {
    let el = document.getElementById('gg-toast-container');
    if (!el) {
      el = document.createElement('div');
      el.id = 'gg-toast-container';
      el.className = 'gg-toast-container';
      document.body.appendChild(el);
    }
    return el;
  },

  show(message, type = 'success', timeoutMs = 4000) {
    const toast = document.createElement('div');
    toast.className = `gg-toast ${type}`;
    toast.textContent = message;
    this.container().appendChild(toast);
    setTimeout(() => toast.remove(), timeoutMs);
  },

  success(message) { this.show(message, 'success'); },

  error(err) {
    const message = err?.body?.message || err?.message || 'Something went wrong. Please try again.';
    this.show(message, 'error');
  },
};
