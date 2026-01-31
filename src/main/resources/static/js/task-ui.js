// src/main/resources/static/js/task-ui.js
(() => {
  const TaskUI = {};

  /* -----------------------------
   * utils (기존 페이지들이 기대할 수 있는 API)
   * ----------------------------- */
  TaskUI.escapeHtml = (v) => {
    if (v === null || v === undefined) return '';
    return String(v)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  };

  TaskUI.toDate = (iso) => (iso ? String(iso).split('T')[0] : null);

  TaskUI.toTimeHM = (iso) => {
    if (!iso) return null;
    const t = String(iso).split('T')[1] || '';
    return t.substring(0, 5) || null;
  };

  TaskUI.formatDateTime = (task) => {
    if (!task) return '';
    if (task.unscheduled) return '미정';

    if (task.allDay) {
      const d = TaskUI.toDate(task.startAt) || TaskUI.toDate(task.endAt);
      return d ? `${d} · 종일` : '종일';
    }

    const sd = TaskUI.toDate(task.startAt);
    const ed = TaskUI.toDate(task.endAt);
    const st = TaskUI.toTimeHM(task.startAt);
    const et = TaskUI.toTimeHM(task.endAt);

    if (sd && ed && sd === ed) {
      if (st && et) return `${sd} · ${st} ~ ${et}`;
      if (st) return `${sd} · ${st}`;
      return `${sd}`;
    }

    if (sd && ed) return `${sd} ~ ${ed}`;
    if (sd) return `${sd}`;
    if (ed) return `${ed}`;
    return '';
  };

  /* -----------------------------
   * renderTaskCard (unscheduled.js가 요구)
   * - 반환: HTML string
   * - data-task-id 필수 (클릭 상세 연결)
   * ----------------------------- */
  TaskUI.renderTaskCard = (task, options = {}) => {
    if (!task) return '';

    const title = TaskUI.escapeHtml(task.title || '(제목 없음)');
    const desc = TaskUI.escapeHtml((task.description || '').trim());
    const cat = TaskUI.escapeHtml((task.category || '').trim());
    const timeText = TaskUI.escapeHtml(TaskUI.formatDateTime(task) || '');

    const barColor = options.barColor || 'rgba(99, 102, 241, 0.55)';

    return `
<div class="task-card cursor-pointer hover:bg-gray-50 active:scale-[0.995]"
     data-task-id="${TaskUI.escapeHtml(task.id)}">
  <div class="task-row">
    <div class="task-left-bar" style="background:${barColor};"></div>
    <div class="check-box">✓</div>

    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2">
        <div class="text-[16px] font-black text-gray-900 truncate">${title}</div>
        ${cat ? `<span class="text-[11px] px-2 py-[2px] rounded-full bg-gray-100 text-gray-700 font-bold">${cat}</span>` : ``}
      </div>

      ${timeText ? `<div class="mt-1 text-[12px] text-gray-500 font-semibold">${timeText}</div>` : ``}

      ${desc ? `<div class="mt-2 text-[13px] text-gray-600 leading-snug whitespace-pre-wrap break-words">${desc}</div>` : ``}
    </div>
  </div>
</div>`.trim();
  };

  /* -----------------------------
   * open helpers
   * ----------------------------- */
  TaskUI.openCreate = (preset = {}) => {
    if (window.TaskModal?.openCreate) {
      window.TaskModal.openCreate(preset);
      return true;
    }
    console.warn('[TaskUI] TaskModal not loaded');
    return false;
  };

  TaskUI.openDetail = (taskId) => {
    if (!taskId) return false;
    if (window.TaskModal?.openDetail) {
      window.TaskModal.openDetail(taskId);
      return true;
    }
    console.warn('[TaskUI] TaskModal not loaded');
    return false;
  };

  /* -----------------------------
   * 클릭 위임: 카드 클릭 → 상세
   * ----------------------------- */
  document.addEventListener('click', (e) => {
    const card = e.target.closest('[data-task-id]');
    if (!card) return;

    // 내부 버튼/링크/폼 요소 클릭은 제외
    if (e.target.closest('button,a,input,textarea,select,label')) return;

    const id = card.getAttribute('data-task-id');
    if (!id) return;

    TaskUI.openDetail(id);
  });

  /* -----------------------------
   * 생성 버튼 트리거(플로팅 버튼/헤더 버튼 등)
   * - 아래 중 아무거나 붙이면 작동:
   *   data-action="open-create"
   *   id="openCreateBtn"
   * ----------------------------- */
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-action="open-create"], #openCreateBtn');
    if (!btn) return;
    e.preventDefault();
    TaskUI.openCreate();
  });

  window.TaskUI = TaskUI;
})();
