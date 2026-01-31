// src/main/resources/static/js/unscheduled.js
(() => {
  const root = document.getElementById('unscheduled-page');
  if (!root) return;

  // 중복 바인딩 방지
  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $loading = document.getElementById('unscheduled-loading');
  const $error   = document.getElementById('unscheduled-error');
  const $empty   = document.getElementById('unscheduled-empty');
  const $card    = document.getElementById('unscheduled-card');
  const $list    = document.getElementById('unscheduled-list');
  const $count   = document.getElementById('unscheduled-count');

  // TaskUI가 없으면 렌더 불가
  if (!window.TaskUI || typeof window.TaskUI.renderTaskCard !== 'function') {
    if ($error) {
      $error.textContent = '렌더 실패: TaskUI.renderTaskCard를 찾을 수 없습니다. (task-ui.js 로드 확인)';
      $error.classList.remove('hidden');
    }
    if ($loading) $loading.classList.add('hidden');
    return;
  }

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
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
  }

  function showList() {
    hideAll();
    $card?.classList.remove('hidden');
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

  function toDateOnly(iso) {
    return (iso && typeof iso === 'string') ? iso.split('T')[0] : '';
  }

  function render(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    showList();

    if ($count) {
      $count.textContent = `${tasks.length}개`;
      $count.classList.remove('hidden');
    }

    // ✅ 템플릿 clone 제거 → TaskUI로 문자열 렌더링
    $list.innerHTML = tasks.map(t => {
      const created = toDateOnly(t.createdAt);

      return TaskUI.renderTaskCard({
        id: t.id,
        title: t.title ?? '',
        description: (t.description || '').trim() || null,
        category: (t.category || '').trim() || null,

        // 여기서 '미정' → 나중에 '씨앗'으로 바꾸면 UI 용어 변경 끝
        rightText: '미정',

        metaText: created ? `등록일 · ${created}` : null
      });
    }).join('');
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
      showError(`미정 일정 로딩 실패: ${e.message}`);
    } finally {
      $loading?.classList.add('hidden');
    }
  }

  load();
})();
