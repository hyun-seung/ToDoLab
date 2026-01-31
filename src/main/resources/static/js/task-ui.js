// src/main/resources/static/js/task-ui.js
(() => {
  const TaskUI = {};

  TaskUI.escapeHtml = (s) => {
    return String(s ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  };

  TaskUI.badgeCategory = (cat) => {
    if (!cat) return '';
    const safe = TaskUI.escapeHtml(cat);
    return `<span class="task-badge">${safe}</span>`;
  };

  /**
   * 공통 카드(행) 렌더
   * @param {Object} p
   * @param {number|string} p.id
   * @param {string} p.title
   * @param {string|null} p.description
   * @param {string|null} p.category
   * @param {string|null} p.rightText   // 오른쪽(시간/종일/미정)
   * @param {string|null} p.metaText    // 하단 작은 메타(등록일 등)
   */
  TaskUI.renderTaskCard = (p) => {
    const title = TaskUI.escapeHtml(p.title);
    const desc = TaskUI.escapeHtml(p.description);
    const categoryHtml = TaskUI.badgeCategory(p.category);

    const right = TaskUI.escapeHtml(p.rightText);
    const meta = TaskUI.escapeHtml(p.metaText);

    return `
      <div class="task-card task-card-clickable"
           data-task-id="${TaskUI.escapeHtml(p.id)}">
        <div class="task-row">
          <div class="task-left-bar" style="background: rgba(99, 102, 241, 0.55);"></div>
          <div class="check-box">✓</div>

          <div class="min-w-0 flex-1">
            <div class="flex items-center gap-2 min-w-0">
              <div class="task-title truncate">${title}</div>
              ${categoryHtml}
            </div>

            ${desc ? `<div class="task-desc mt-1 line-clamp-2">${desc}</div>` : ``}
            ${meta ? `<div class="task-meta mt-2">${meta}</div>` : ``}
          </div>

          ${right ? `<div class="task-right">${right}</div>` : ``}
        </div>
      </div>
    `;
  };

  window.TaskUI = TaskUI;
})();
