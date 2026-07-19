// Use an absolute backend URL when the API and static site have different
// origins. Leaving this empty supports same-origin/reverse-proxy deployments.
window.GG_CONFIG = window.GG_CONFIG || {
  apiBaseUrl: '',
};
