// src/main/resources/static/js/today.js
(() => {
  const root = document.getElementById('today-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const date = root.dataset.date; // yyyy-MM-dd

  const $loading  = document.getElementById('today-loading');
  const $error    = document.getElementById('today-error');
  const $empty    = document.getElementById('today-empty');
  const $card     = document.getElementById('today-card');
  const $list     = document.getElementById('today-list');
  const $count    = document.getElementById('today-count');
  const $dateText = document.getElementById('todayDateText');

  function hideAll() {
    $loading?.classList.add('hidden');
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
    $card?.classList.add('hidden');
  }

  function setCount(n) {
    if (!$count) return;
    if (n <= 0) {
      $count.classList.add('hidden');
      $count.textContent = '';
      return;
    }
    $count.textContent = `${n}개`;
    $count.classList.remove('hidden');
  }

  function showError(msg) {
    hideAll();
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
    setCount(0);
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
    setCount(0);
  }

  function showList(n) {
    hideAll();
    $card?.classList.remove('hidden');
    setCount(n);
  }

  function fmtDowKorean(yyyyMmDd) {
    try {
      const [y, m, d] = yyyyMmDd.split('-').map(Number);
      const dt = new Date(y, m - 1, d);
      const map = ['일', '월', '화', '수', '목', '금', '토'];
      return map[dt.getDay()];
    } catch {
      return '';
    }
  }

  function render(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderTodayCard !== 'function') {
      showError('렌더 실패: TaskUI.renderTodayCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    showList(tasks.length);
    $list.innerHTML = tasks.map(TaskUI.renderTodayCard).join('');
  }

  async function load() {
    try {
      // 로딩 시작
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');

      // 날짜 라벨에 요일 붙이기
      if ($dateText && date) {
        const dow = fmtDowKorean(date);
        $dateText.textContent = dow ? `${date} (${dow})` : date;
      }

      const url = `/api/tasks?type=DAY&date=${encodeURIComponent(date)}`;
      const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const body = await res.json();
      if (!body) throw new Error('응답이 비어있음');
      if (body.success === false) throw new Error(body.message || 'API 실패');

      render(body.data ?? []);
    } catch (e) {
      showError(`Today 로딩 실패: ${e.message}`);
    } finally {
      // ✅ 항상 로딩 종료
      $loading?.classList.add('hidden');
    }
  }

  load();
})();
