// src/main/resources/static/js/unscheduled.js
(() => {
  const root = document.getElementById('unscheduled-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $loading = document.getElementById('unscheduled-loading');
  const $error   = document.getElementById('unscheduled-error');
  const $empty   = document.getElementById('unscheduled-empty');
  const $card    = document.getElementById('unscheduled-card');
  const $list    = document.getElementById('unscheduled-list');
  const $count   = document.getElementById('unscheduled-count');

  function hideAll() {
    $loading?.classList.add('hidden');
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
    $card?.classList.add('hidden');
  }

  function showError(msg) {
    hideAll();
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
    if ($count) $count.classList.add('hidden');
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
    if ($count) $count.classList.add('hidden');
  }

  function showList(n) {
    hideAll();
    $card?.classList.remove('hidden');

    if ($count) {
      $count.textContent = `${n}개`;
      $count.classList.remove('hidden');
    }
  }

  function sortTasks(tasks) {
    return [...tasks].sort((a, b) => {
      const ca = a.createdAt ?? '';
      const cb = b.createdAt ?? '';
      if (ca !== cb) return ca < cb ? 1 : -1;

      const at = (a.title ?? '').toLowerCase();
      const bt = (b.title ?? '').toLowerCase();
      return at.localeCompare(bt);
    });
  }

  function render(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderSeedCard !== 'function') {
      showError('렌더 실패: TaskUI.renderSeedCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    showList(tasks.length);
    $list.innerHTML = tasks.map(TaskUI.renderSeedCard).join('');
  }

  async function load() {
    try {
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');

      const res = await fetch('/api/tasks/unscheduled', {
        headers: { 'Accept': 'application/json' }
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const body = await res.json();
      if (!body) throw new Error('응답이 비어있음');
      if (body.success === false) throw new Error(body.message || 'API 실패');

      const raw = body.data ?? [];
      const only = raw.filter(t => t && t.unscheduled === true);
      render(sortTasks(only));
    } catch (e) {
      showError(`씨앗 로딩 실패: ${e.message}`);
    } finally {
      $loading?.classList.add('hidden');
    }
  }

  load();
})();
