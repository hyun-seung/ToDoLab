// src/main/resources/static/js/task-modal.js
window.TaskModal = (() => {
  const $modal = document.getElementById('taskModal');
  if (!$modal) return {};

  const $backdrop = document.getElementById('taskModalBackdrop');
  const $panel = document.getElementById('taskModalPanel');

  const $titleBar = document.getElementById('taskModalTitle');

  const $title = document.getElementById('tmTitle');
  const $desc = document.getElementById('tmDescription');
  const $category = document.getElementById('tmCategory');
  const $unscheduled = document.getElementById('tmUnscheduled');
  const $allDay = document.getElementById('tmAllDay');
  const $startAt = document.getElementById('tmStartAt');
  const $endAt = document.getElementById('tmEndAt');

  const $meta = document.getElementById('tmMeta');
  const $createdAt = document.getElementById('tmCreatedAt');
  const $updatedAt = document.getElementById('tmUpdatedAt');

  const $primary = document.getElementById('tmPrimaryBtn');
  const $delete = document.getElementById('tmDeleteBtn');

  let mode = 'create'; // create | detail | edit
  let currentId = null;

  /* -----------------------------
   * open / close
   * ----------------------------- */
  function open() {
    $modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }

  function close() {
    $modal.classList.add('hidden');
    document.body.style.overflow = '';
    reset();
  }

  // 닫기: X/닫기 버튼(data-action="close"), dim 클릭, ESC
  $modal.addEventListener('click', (e) => {
    if (e.target.closest('[data-action="close"]')) close();
  });

  $backdrop?.addEventListener('click', (e) => {
    if ($panel && !$panel.contains(e.target)) close();
  });

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !$modal.classList.contains('hidden')) close();
  });

  /* -----------------------------
   * helpers
   * ----------------------------- */
  function fmtIso(iso) {
    if (!iso) return '-';
    const [d, t] = String(iso).split('T');
    const hm = (t || '').substring(0, 5);
    return hm ? `${d} ${hm}` : d;
  }

  function toInputLocal(iso) {
    if (!iso) return '';
    const [d, t] = String(iso).split('T');
    const hm = (t || '').substring(0, 5);
    return hm ? `${d}T${hm}` : '';
  }

  function setReadOnly(ro) {
    $title.readOnly = ro;
    $desc.readOnly = ro;
    $category.readOnly = ro;

    // checkbox + datetime-local은 disabled 처리
    $unscheduled.disabled = ro;
    $allDay.disabled = ro;
    $startAt.disabled = ro || $unscheduled.checked;
    $endAt.disabled = ro || $unscheduled.checked;
  }

  function syncDateDisabled() {
    const dis = $unscheduled.checked;
    $startAt.disabled = dis || mode === 'detail';
    $endAt.disabled = dis || mode === 'detail';
    if (dis) {
      $startAt.value = '';
      $endAt.value = '';
    }
  }

  $unscheduled.addEventListener('change', syncDateDisabled);

  function reset() {
    mode = 'create';
    currentId = null;

    $title.value = '';
    $desc.value = '';
    $category.value = '';
    $unscheduled.checked = false;
    $allDay.checked = false;
    $startAt.value = '';
    $endAt.value = '';

    $meta.classList.add('hidden');
    $createdAt.textContent = '-';
    $updatedAt.textContent = '-';

    $delete.classList.add('hidden');
    $primary.textContent = '일정 등록';
    $titleBar.textContent = '일정 등록';

    setReadOnly(false);
    syncDateDisabled();
  }

  function fill(task) {
    $title.value = task.title ?? '';
    $desc.value = task.description ?? '';
    $category.value = task.category ?? '';

    $unscheduled.checked = !!task.unscheduled;
    $allDay.checked = !!task.allDay;

    $startAt.value = toInputLocal(task.startAt);
    $endAt.value = toInputLocal(task.endAt);

    $createdAt.textContent = fmtIso(task.createdAt);
    $updatedAt.textContent = fmtIso(task.updatedAt || task.timestamp);

    syncDateDisabled();
  }

  function payloadFromForm() {
    return {
      title: ($title.value || '').trim(),
      description: $desc.value || '',
      category: $category.value || '',
      unscheduled: !!$unscheduled.checked,
      allDay: !!$allDay.checked,
      startAt: $startAt.value || null,
      endAt: $endAt.value || null,
    };
  }

  function unwrap(body) {
    // { success, data } or { status, data } 등 방어
    if (!body) return null;
    if (body.success === false) throw new Error(body.message || 'API failed');
    if (body.status && body.status !== 'success') throw new Error(body.message || 'API failed');
    return body.data ?? body?.data?.data ?? body;
  }

  async function apiGet(id) {
    const res = await fetch(`/api/tasks/${encodeURIComponent(id)}`, {
      headers: { 'Accept': 'application/json' }
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const body = await res.json();
    return unwrap(body);
  }

  async function apiCreate(payload) {
    const res = await fetch(`/api/tasks`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Requested-With': 'fetch'
      },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const body = await res.json().catch(() => ({}));
    return unwrap(body) || true;
  }

  async function apiUpdate(id, payload) {
    const res = await fetch(`/api/tasks/${encodeURIComponent(id)}`, {
      method: 'PUT', // ✅ 수정은 PUT 고정
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Requested-With': 'fetch'
      },
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const body = await res.json().catch(() => ({}));
    return unwrap(body) || true;
  }

  async function apiDelete(id) {
    const res = await fetch(`/api/tasks/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { 'Accept': 'application/json', 'X-Requested-With': 'fetch' }
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return true;
  }

  /* -----------------------------
   * mode set
   * ----------------------------- */
  function setModeCreate(preset = {}) {
    mode = 'create';
    currentId = null;

    $titleBar.textContent = '일정 등록';
    $primary.textContent = '일정 등록';
    $delete.classList.add('hidden');
    $meta.classList.add('hidden');

    setReadOnly(false);

    // preset 값 반영 (예: 특정 날짜에서 바로 등록)
    if (preset.title != null) $title.value = preset.title;
    if (preset.description != null) $desc.value = preset.description;
    if (preset.category != null) $category.value = preset.category;
    if (preset.unscheduled != null) $unscheduled.checked = !!preset.unscheduled;
    if (preset.allDay != null) $allDay.checked = !!preset.allDay;
    if (preset.startAt != null) $startAt.value = preset.startAt;
    if (preset.endAt != null) $endAt.value = preset.endAt;

    syncDateDisabled();
  }

  function setModeDetail(id, task) {
    mode = 'detail';
    currentId = id;

    $titleBar.textContent = '일정 상세';
    $primary.textContent = '수정';
    $delete.classList.remove('hidden');
    $meta.classList.remove('hidden');

    fill(task);
    setReadOnly(true);
  }

  function setModeEdit(id) {
    mode = 'edit';
    currentId = id;

    $titleBar.textContent = '일정 수정';
    $primary.textContent = '저장';
    $delete.classList.remove('hidden');
    $meta.classList.remove('hidden');

    setReadOnly(false);
    syncDateDisabled();
  }

  /* -----------------------------
   * primary / delete actions
   * ----------------------------- */
  $primary.addEventListener('click', async () => {
    try {
      if (mode === 'create') {
        const payload = payloadFromForm();
        if (!payload.title) return alert('제목은 필수야');
        await apiCreate(payload);
        close();
        location.reload();
        return;
      }

      if (mode === 'detail') {
        // 상세에서 "수정" 누르면 편집모드로 전환
        if (!currentId) return;
        setModeEdit(currentId);
        return;
      }

      if (mode === 'edit') {
        const payload = payloadFromForm();
        if (!payload.title) return alert('제목은 필수야');
        if (!currentId) return;

        await apiUpdate(currentId, payload);
        close();
        location.reload();
      }
    } catch (e) {
      alert(`${e.message || e}`);
    }
  });

  $delete.addEventListener('click', async () => {
    if (!currentId) return;
    if (!confirm('정말 삭제할까?')) return;

    try {
      await apiDelete(currentId);
      close();
      location.reload();
    } catch (e) {
      alert(`${e.message || e}`);
    }
  });

  /* -----------------------------
   * public API
   * ----------------------------- */
  async function openCreate(preset = {}) {
    reset();
    setModeCreate(preset);
    open();
  }

  async function openDetail(id) {
    reset();
    open(); // 로딩 중에도 모달은 띄움

    try {
      const task = await apiGet(id);
      setModeDetail(id, task);
    } catch (e) {
      alert(`상세 로딩 실패: ${e.message || e}`);
      close();
    }
  }

  return { openCreate, openDetail, close };
})();
