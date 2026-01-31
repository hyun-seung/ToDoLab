// src/main/resources/static/js/week.js
(() => {
  function pad2(n){ return String(n).padStart(2, '0'); }
  function parseISO(iso){
    const [y,m,d] = iso.split('-').map(Number);
    return new Date(y, m-1, d);
  }
  function toISO(d){
    return `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}`;
  }
  function addDays(iso, n){
    const d = parseISO(iso);
    d.setDate(d.getDate() + n);
    return toISO(d);
  }

  function initWeek() {
    const root = document.getElementById('week-page');
    if (!root) return;

    // ✅ 중복 바인딩 방지
    if (root.dataset.bound === '1') return;
    root.dataset.bound = '1';

    const rangeEl = document.getElementById('weekRangeText');
    const stripEl = document.getElementById('weekStrip');
    const daysEl  = document.getElementById('weekDays');
    const prevBtn = document.getElementById('weekPrevBtn');
    const nextBtn = document.getElementById('weekNextBtn');
    const dowEl   = document.getElementById('weekDow');

    if (!stripEl || !daysEl) return;

    const todayISO = toISO(new Date());

    const weekStartISO = (anyISO) => {
      const d = parseISO(anyISO);
      const dow = d.getDay(); // 0=일..6=토
      d.setDate(d.getDate() - dow);
      return toISO(d);
    };

    const weekRangeText = (anyISO) => {
      const start = weekStartISO(anyISO);
      const end = addDays(start, 6);
      return `${start} ~ ${end}`;
    };

    let selectedISO = root.getAttribute('data-selected-date');
    let currentISO  = root.getAttribute('data-current-date');

    function setSelected(iso){
      if (!iso) return;

      selectedISO = iso;
      root.setAttribute('data-selected-date', iso);

      // 아래 일정 토글
      daysEl.querySelectorAll('.week-day[data-date]').forEach(box => {
        box.classList.toggle('hidden', box.getAttribute('data-date') !== iso);
      });

      // strip UI 토글
      stripEl.querySelectorAll('a[data-date]').forEach(a => {
        const d = a.getAttribute('data-date');
        const isSel = (d === iso);

        const circle = a.querySelector('div.relative');
        if (!circle) return;

        const ring = circle.querySelector('span.absolute');
        const num  = circle.querySelector('span:not(.absolute)');

        circle.classList.toggle('bg-indigo-500', isSel);
        circle.classList.toggle('shadow-[0_10px_18px_rgba(99,102,241,0.20)]', isSel);
        circle.classList.toggle('-translate-y-[1px]', isSel);

        if (num) {
          if (isSel) { num.classList.add('text-white'); num.classList.remove('text-gray-900'); }
          else { num.classList.remove('text-white'); num.classList.add('text-gray-900'); }
        }

        if (ring) {
          if (isSel) { ring.classList.add('border-white/85'); ring.classList.remove('border-indigo-500/55'); }
          else { ring.classList.remove('border-white/85'); ring.classList.add('border-indigo-500/55'); }
        }
      });
    }

    /**
     * ✅ href 재작성 로직은 "서버가 준 currentISO 기준"으로만 한다.
     * (여기서 curISO를 계산하지 않는다)
     */
    function updateNavHrefs(curISO){
      if (!prevBtn || !nextBtn) return;
      if (!curISO) return;

      const prev = new URL(prevBtn.getAttribute('href'), location.origin);
      const next = new URL(nextBtn.getAttribute('href'), location.origin);

      prev.searchParams.set('date', curISO);
      next.searchParams.set('date', curISO);

      prevBtn.setAttribute('href', prev.pathname + '?' + prev.searchParams.toString());
      nextBtn.setAttribute('href', next.pathname + '?' + next.searchParams.toString());
    }

    function renderLoadingState(){
      daysEl.innerHTML = `
        <div class="flex justify-center mt-8">
          <div class="w-full border border-dashed border-gray-300 rounded-[22px] p-10 text-center bg-white/35">
            <div class="text-2xl">⏳</div>
            <div class="mt-3 text-xl font-extrabold text-gray-800">불러오는 중...</div>
            <div class="mt-1 text-sm text-gray-500 font-semibold">잠시만 기다려주세요</div>
          </div>
        </div>
      `;
    }

    // 최신 응답만 반영
    let reqSeq = 0;

    async function fetchAndCommit(href){
      const mySeq = ++reqSeq;

      try{
        const res = await fetch(href, { headers: { 'X-Requested-With': 'fetch' }});
        if (!res.ok) throw new Error('HTTP ' + res.status);

        const html = await res.text();
        if (mySeq !== reqSeq) return;

        const doc = new DOMParser().parseFromString(html, 'text/html');
        const newRoot = doc.getElementById('week-page');
        if (!newRoot) return;

        const newRange = newRoot.querySelector('#weekRangeText');
        const newStrip = newRoot.querySelector('#weekStrip');
        const newDays  = newRoot.querySelector('#weekDays');

        if (newRange && rangeEl) rangeEl.textContent = newRange.textContent || '';
        if (newStrip) stripEl.innerHTML = newStrip.innerHTML;
        if (newDays)  daysEl.innerHTML  = newDays.innerHTML;

        // ✅ 서버가 내려준 값으로 current/selected를 "확정"
        const cd = newRoot.getAttribute('data-current-date');
        const sd = newRoot.getAttribute('data-selected-date');

        if (cd) {
          currentISO = cd;
          root.setAttribute('data-current-date', cd);
          updateNavHrefs(cd);
          if (rangeEl) rangeEl.textContent = weekRangeText(cd); // 안전하게 보정
        }

        if (sd) {
          selectedISO = sd;
          root.setAttribute('data-selected-date', sd);
        }

        // ✅ 선택일이 strip에 없으면 첫날로 보정
        const exists = !!stripEl.querySelector(`a[data-date="${CSS.escape(selectedISO)}"]`);
        if (!exists) {
          const first = stripEl.querySelector('a[data-date]');
          selectedISO = first ? first.getAttribute('data-date') : selectedISO;
        }
        setSelected(selectedISO);

      } catch(e){
        if (mySeq !== reqSeq) return;
        daysEl.innerHTML = `
          <div class="flex justify-center mt-8">
            <div class="w-full border border-dashed border-gray-300 rounded-[22px] p-10 text-center bg-white/35">
              <div class="text-2xl">⚠️</div>
              <div class="mt-3 text-xl font-extrabold text-gray-800">불러오지 못했어요</div>
              <div class="mt-1 text-sm text-gray-500 font-semibold">다시 시도해보세요</div>
            </div>
          </div>
        `;
        console.warn('[week] fetch failed:', e);
      }
    }

    // 날짜 클릭 (delegation)
    stripEl.addEventListener('click', (e) => {
      const a = e.target.closest('a[data-date]');
      if (!a) return;
      e.preventDefault();
      setSelected(a.getAttribute('data-date'));
    });

    // 요일 클릭
    if (dowEl) {
      dowEl.addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-idx]');
        if (!btn) return;
        const idx = Number(btn.getAttribute('data-idx'));
        const items = Array.from(stripEl.querySelectorAll('a[data-date]'));
        const target = items[idx];
        if (!target) return;
        setSelected(target.getAttribute('data-date'));
      });
    }

    /**
     * ✅ 핵심 수정:
     * - deltaDays 기반 날짜 계산 제거
     * - href 그대로 push + fetch
     * - UI는 로딩만 표시
     */
    function bindNav(btn){
      if (!btn) return;

      btn.onclick = (e) => {
        e.preventDefault();

        const href = btn.getAttribute('href');
        if (!href) return;

        renderLoadingState();
        history.pushState({ href }, '', href);
        fetchAndCommit(href);
      };
    }

    // init
    updateNavHrefs(currentISO);

    if (!selectedISO) {
      const first = stripEl.querySelector('a[data-date]');
      selectedISO = first ? first.getAttribute('data-date') : selectedISO;
    }
    setSelected(selectedISO);

    // ✅ 여기 변경: 인자 제거
    bindNav(prevBtn);
    bindNav(nextBtn);

    window.addEventListener('popstate', () => {
      const href = location.pathname + location.search;
      renderLoadingState();
      fetchAndCommit(href);
    });
  }

  document.addEventListener('DOMContentLoaded', initWeek);
})();
