// src/main/resources/static/js/unscheduled.js
(() => {
  const root = document.getElementById('unscheduled-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $loading = document.getElementById('unscheduled-loading');
  const $error = document.getElementById('unscheduled-error');
  const $empty = document.getElementById('unscheduled-empty');
  const $card = document.getElementById('unscheduled-card');
  const $list = document.getElementById('unscheduled-list');
  const $count = document.getElementById('unscheduled-count');

  function getTpl(id) {
    const t = document.getElementById(id);
    return t && t.content ? t : null;
  }

  function hideAll() {
    $loading.classList.add('hidden');
    $error.classList.add('hidden');
    $empty.classList.add('hidden');
    $card.classList.add('hidden');
  }

  function showError(msg) {
    hideAll();
    $error.textContent = msg;
    $error.classList.remove('hidden');
  }

  function showEmpty() {
    hideAll();
    $empty.classList.remove('hidden');
  }

  function showList() {
    hideAll();
    $card.classList.remove('hidden');
  }

  function escapeHtml(s) {
    return String(s ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
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

  function renderRow(t) {
    const tpl = getTpl('taskRowTpl');
    if (!tpl) throw new Error('taskRowTpl(template)이 base.html에 없습니다.');

    const node = tpl.content.firstElementChild.cloneNode(true);

    node.setAttribute('data-task-id', t.id);

    const $leftBar = node.querySelector('[data-field="leftBar"]');
    if ($leftBar) $leftBar.setAttribute('style', 'background: rgba(99, 102, 241, 0.55);');

    const $title = node.querySelector('[data-field="title"]');
    if ($title) $title.textContent = t.title ?? '';

    const $cat = node.querySelector('[data-field="category"]');
    if ($cat) {
      const c = (t.category ?? '').trim();
      if (c) { $cat.textContent = c; $cat.classList.remove('hidden'); }
    }

    const $desc = node.querySelector('[data-field="description"]');
    if ($desc) {
      const d = (t.description ?? '').trim();
      if (d) { $desc.textContent = d; $desc.classList.remove('hidden'); }
    }

    const created = (t.createdAt && typeof t.createdAt === 'string') ? t.createdAt.split('T')[0] : '';
    const $meta = node.querySelector('[data-field="meta"]');
    if ($meta && created) {
      $meta.textContent = `등록일 · ${created}`;
      $meta.classList.remove('hidden');
    }

    const $right = node.querySelector('[data-field="right"]');
    if ($right) $right.textContent = '미정';

    return node;
  }

  async function load() {
    try {
      $loading.classList.remove('hidden');
      $error.classList.add('hidden');

      const res = await fetch('/api/tasks/unscheduled', { headers: { 'Accept': 'application/json' } });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const body = await res.json();
      if (!body) throw new Error('응답이 비어있음');
      if (body.success === false) throw new Error(body.message || 'API 실패');

      const raw = body.data ?? [];
      const only = raw.filter(x => x && x.unscheduled === true);
      const sorted = sortTasks(only);

      if (!sorted.length) {
        showEmpty();
        return;
      }

      showList();

      if ($count) {
        $count.textContent = `${sorted.length}개`;
        $count.classList.remove('hidden');
      }

      $list.innerHTML = '';
      for (const t of sorted) $list.appendChild(renderRow(t));
    } catch (e) {
      showError(`렌더 실패: ${e.message}`);
    } finally {
      $loading.classList.add('hidden');
    }
  }

  load();
})();
