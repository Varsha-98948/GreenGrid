/**
 * GreenGrid shared constants — single source of truth for values
 * used across multiple pages (forms, filters, cards).
 *
 * To add a new language, append it to GG_LANGUAGES.options.
 * The value is stored verbatim in the database (varchar 50) so keep it
 * consistent with existing rows.  "Other" is intentionally last so it
 * acts as the extensibility catch-all.
 */
const GG_LANGUAGES = {
  // Ordered list rendered as <option> elements.
  options: [
    { value: 'Java',       label: 'Java' },
    { value: 'Python',     label: 'Python' },
    { value: 'C',          label: 'C' },
    { value: 'C++',        label: 'C++' },
    { value: 'SQL',        label: 'SQL' },
    { value: 'JavaScript', label: 'JavaScript' },
    { value: 'Other',      label: 'Other' },
  ],

  /**
   * Renders <option> elements into a <select> element.
   * @param {HTMLSelectElement} selectEl
   * @param {boolean} [includeAny=false]  Prepend an "Any" / empty option (for filters)
   * @param {string}  [selected]          Pre-select this value
   */
  populate(selectEl, { includeAny = false, selected = '' } = {}) {
    selectEl.innerHTML = '';
    if (includeAny) {
      const any = document.createElement('option');
      any.value = '';
      any.textContent = 'Any language';
      selectEl.appendChild(any);
    }
    for (const lang of this.options) {
      const opt = document.createElement('option');
      opt.value = lang.value;
      opt.textContent = lang.label;
      if (lang.value === selected) opt.selected = true;
      selectEl.appendChild(opt);
    }
  },

  /** Monaco editor language identifiers for each supported language. */
  monacoMap: {
    'Java':       'java',
    'Python':     'python',
    'C':          'c',
    'C++':        'cpp',
    'SQL':        'sql',
    'JavaScript': 'javascript',
    'Other':      'plaintext',
  },
};

/**
 * Supported platforms.  Same pattern as languages — single source of truth.
 */
const GG_PLATFORMS = [
  'LeetCode', 'HackerRank', 'Codeforces', 'CodeChef', 'AtCoder', 'Other',
];
