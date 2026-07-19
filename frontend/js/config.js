const GG_DEFAULT_API_BASE_URL =
  window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080'
    : 'https://greengrid-byh0.onrender.com';

window.GG_CONFIG = {
  apiBaseUrl: GG_DEFAULT_API_BASE_URL,
  ...window.GG_CONFIG,
};
