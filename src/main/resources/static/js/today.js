// src/main/resources/static/js/today.js
(() => {
  const root = document.getElementById('today-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $dateText = document.getElementById('todayDateText');
  const $error = document.getElementById('today-error');
  const $empty = document.getElementById('today-empty');
  const $list = document.getElementById('today-list');

  // 서버에서 date를 내려주는 구조면 그걸 우선 사용
  // 없으면(혹시 누락되면) 클라이언트에서 오늘 날짜로 fallback
  const todayIso = new Date().toISOString().slice(0, 10);
  let currentDate = root.dataset.date || todayIso;

  let inflightAbort = null;
  let requestSeq = 0;

  const cache = new Map();

  function hideAllState() {
    $error.classList.add('hidden');
    $empty.classList.add('hidden');
  }

  function showError(msg) {
    hideAllState();
    $list.innerHTML = '';
    $error.textContent = msg;
    $error.classList.remove('hidden');
  }

  function showEmpty() {
    hideAllState();
    $list.innerHTML = '';
    $empty.classList.remove('hidden');
  }

  function showList(html) {
    hideAllState();
    $list.innerHTML = html;
  }

  function toLocalDatePart(isoLocalDateTime) {
    if (!isoLocalDateTime) return null;
    return isoLocalDateTime.split('T')[0];
  }

  function toLocalTimePartHM(isoLocalDateTime) {
    if (!isoLocalDateTime) return null;
    const t = isoLocalDateTime.split('T')[1] || '';
    return t.substring(0, 5);
  }

  function compareDateStr(a, b) {
    if (a === b) return 0;
    return a < b ? -1 : 1;
  }

  function occursOn(task, targetDate) {
    if (task.unscheduled) return false;

    const s = task.startAt;
    const e = task.endAt;
    if (!s && !e) return false;

    const sd = s ? toLocalDatePart(s) : null;
    const ed = e ? toLocalDatePart(e) : null;

    const endDate = ed ?? sd;
    const startDate = sd ?? endDate;
    if (!startDate || !endDate) return false;

    return compareDateStr(startDate, targetDate) <= 0 &&
           compareDateStr(endDate, targetDate) >= 0;
  }

  function buildTimeText(task, targetDate) {
    if (task.allDay) return '종일';

    const s = task.startAt;
    const e = task.endAt;
    if (!s && !e) return '';

    const sd = s ? toLocalDatePart(s) : null;
    const ed = e ? toLocalDatePart(e) : null;
    const st = s ? toLocalTimePartHM(s) : null;
    const et = e ? toLocalTimePartHM(e) : null;

    if (sd && ed && sd === targetDate && ed === targetDate) {
      if (st && et) return `${st} ~ ${et}`;
      if (st) return `${st}`;
      return '';
    }

    if (sd === targetDate && st) return `${st} ~ …`;
    if (ed === targetDate && et) return `… ~ ${et}`;
    return '계속';
  }

  function sortTasks(tasks, targetDate) {
    function groupKey(t) {
      if (t.allDay) return 1;
      const sd = t.startAt ? toLocalDatePart(t.startAt) : null;
      const ed = t.endAt ? toLocalDatePart(t.endAt) : null;
      if (sd === targetDate && ed === targetDate) return 0;
      return 2;
    }

    function timeKey(t) {
      const st = t.startAt ? toLocalTimePartHM(t.startAt) : null;
      return st ?? '99:99';
    }

    return [...tasks].sort((a, b) => {
      const ga = groupKey(a), gb = groupKey(b);
      if (ga !== gb) return ga - gb;

      if (ga === 0) {
        const ta = timeKey(a), tb = timeKey(b);
        if (ta !== tb) return ta < tb ? -1 : 1;
      }

      const at = (a.title ?? '').toLowerCase();
      const bt = (b.title ?? '').toLowerCase();
      return at.localeCompare(bt);
    });
  }

  function updateHeader(dateStr) {
    root.dataset.date = dateStr;
    currentDate = dateStr;
    if ($dateText) $dateText.textContent = dateStr;
  }

  function buildCardsHtml(tasks, targetDate) {
    return tasks.map(t => {
      return TaskUI.renderTaskCard({
        id: t.id,
        title: t.title,
        description: (t.description || '').trim() || null,
        category: (t.category || '').trim() || null,
        rightText: buildTimeText(t, targetDate) || null,
        metaText: null
      });
    }).join('');
  }

  async function fetchTasks(dateStr) {
    if (cache.has(dateStr)) return cache.get(dateStr);

    if (inflightAbort) inflightAbort.abort();
    inflightAbort = new AbortController();

    const url = `/api/tasks?type=DAY&date=${encodeURIComponent(dateStr)}`;
    const res = await fetch(url, {
      headers: { 'Accept': 'application/json' },
      signal: inflightAbort.signal
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const body = await res.json();
    if (!body) throw new Error('응답이 비어있음');
    if (body.success === false) throw new Error(body.message || 'API 실패');

    const raw = body.data ?? [];
    cache.set(dateStr, raw);
    return raw;
  }

  async function init() {
    const mySeq = ++requestSeq;

    try {
      // Today 페이지니까 항상 "오늘" 기준으로 보여주고 싶다면,
      // 서버 date가 아닌 클라이언트 todayIso로 강제해도 됨.
      // 지금은 server model(date) 우선 + fallback 구조.
      updateHeader(currentDate);

      hideAllState();
      $list.innerHTML = '';

      const raw = await fetchTasks(currentDate);
      if (mySeq !== requestSeq) return;

      const filtered = raw.filter(t => occursOn(t, currentDate));
      const sorted = sortTasks(filtered, currentDate);

      if (!sorted.length) {
        showEmpty();
        return;
      }

      showList(buildCardsHtml(sorted, currentDate));
    } catch (e) {
      if (e.name === 'AbortError') return;
      showError(`일정 로딩 실패: ${e.message}`);
    }
  }

  init();
})();
