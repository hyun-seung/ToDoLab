// src/main/resources/static/js/modal-create.js

window.AppModal = (() => {
  const modal = () => document.getElementById("appModal");
  const content = () => document.getElementById("appModalContent");

  function open() {
    modal()?.classList.remove("hidden");
    document.body.style.overflow = "hidden";
  }

  function close() {
    modal()?.classList.add("hidden");
    document.body.style.overflow = "";
    if (content()) content().innerHTML = "";
  }

  async function loadCreate(date) {
    const url = date ? `/tasks/create?date=${encodeURIComponent(date)}` : `/tasks/create`;

    const res = await fetch(url, { headers: { "X-Requested-With": "fetch" }});
    const html = await res.text();

    content().innerHTML = html;
    open();

    initCreatePickersIfPresent(date);
    bindCreateSubmitIfPresent();
  }

  function initCreatePickersIfPresent(dateParam) {
    const startDate = document.getElementById("startDateInput");
    const endDate   = document.getElementById("endDateInput");
    const startTime = document.getElementById("startTimeInput");
    const endTime   = document.getElementById("endTimeInput");

    const unscheduledToggle = document.getElementById("unscheduledToggle");
    const allDayToggle      = document.getElementById("allDayToggle");
    const scheduleFields    = document.getElementById("scheduleFields");
    const allDayRow         = document.getElementById("allDayRow");

    if (!startDate || !startTime || !unscheduledToggle || !allDayToggle || !scheduleFields) return;

    [startDate, endDate, startTime, endTime].forEach(el => {
      if (el && el._flatpickr) el._flatpickr.destroy();
    });

    flatpickr("#startDateInput", {
      dateFormat: "Y-m-d",
      defaultDate: dateParam ?? "today",
      locale: "ko"
    });

    if (endDate) {
      flatpickr("#endDateInput", { dateFormat: "Y-m-d", locale: "ko" });
    }

    flatpickr("#startTimeInput", {
      enableTime: true,
      noCalendar: true,
      dateFormat: "H:i",
      time_24hr: true,
      minuteIncrement: 30
    });

    if (endTime) {
      flatpickr("#endTimeInput", {
        enableTime: true,
        noCalendar: true,
        dateFormat: "H:i",
        time_24hr: true,
        minuteIncrement: 30
      });
    }

    const applyState = () => {
      const isUnscheduled = !!unscheduledToggle.checked;
      const isAllDay = !!allDayToggle.checked;

      scheduleFields.style.opacity = isUnscheduled ? "0.5" : "1";
      [startDate, endDate, startTime, endTime].forEach(el => {
        if (!el) return;
        el.disabled = isUnscheduled;
        if (isUnscheduled) el.value = "";
      });

      if (allDayRow) allDayRow.style.opacity = isUnscheduled ? "0.5" : "1";

      allDayToggle.disabled = isUnscheduled;
      if (isUnscheduled) allDayToggle.checked = false;

      const disableTime = (!isUnscheduled && isAllDay);
      if (startTime) {
        startTime.disabled = isUnscheduled || disableTime;
        if (disableTime) startTime.value = "";
      }
      if (endTime) {
        endTime.disabled = isUnscheduled || disableTime;
        if (disableTime) endTime.value = "";
      }
    };

    unscheduledToggle.addEventListener("change", applyState);
    allDayToggle.addEventListener("change", applyState);
    applyState();
  }

  function bindCreateSubmitIfPresent() {
    const submitBtn = document.getElementById("submitBtn");
    const form = document.getElementById("taskForm");
    const errorBox = document.getElementById("error-box");
    if (!submitBtn || !form || !errorBox) return;

    submitBtn.onclick = () => form.requestSubmit();

    form.onsubmit = async (e) => {
      e.preventDefault();
      errorBox.classList.add("hidden");

      const originalText = submitBtn.innerHTML;
      submitBtn.disabled = true;
      submitBtn.innerHTML = "⏳ 등록 중...";

      const unscheduled = !!form.unscheduled?.checked;
      const allDay = !!form.allDay?.checked;

      const startDate = form.startDate?.value?.trim() || null;
      const startTime = form.startTime?.value?.trim() || null;
      const endDate   = form.endDate?.value?.trim() || null;
      const endTime   = form.endTime?.value?.trim() || null;

      let startAt = null;
      let endAt = null;
      let finalAllDay = allDay;

      if (unscheduled) {
        startAt = null;
        endAt = null;
        finalAllDay = false;
      } else {
        if (allDay) {
          if (!startDate) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            errorBox.innerText = "종일 일정은 시작 날짜가 필요합니다.";
            errorBox.classList.remove("hidden");
            return;
          }

          startAt = toIsoLocalDateTime(startDate, "00:00");

          if (endDate) {
            const endExclusive = addDays(endDate, 1);
            endAt = toIsoLocalDateTime(endExclusive, "00:00");
          } else {
            endAt = null;
          }
        } else {
          if (!startDate || !startTime) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            errorBox.innerText = "일정은 시작 날짜와 시간을 입력해주세요. (또는 종일/미정 선택)";
            errorBox.classList.remove("hidden");
            return;
          }

          startAt = toIsoLocalDateTime(startDate, startTime);

          if (endDate || endTime) {
            if (!endDate || !endTime) {
              submitBtn.disabled = false;
              submitBtn.innerHTML = originalText;
              errorBox.innerText = "종료를 입력하려면 종료 날짜와 시간을 모두 입력해주세요.";
              errorBox.classList.remove("hidden");
              return;
            }
            endAt = toIsoLocalDateTime(endDate, endTime);
          } else {
            endAt = null;
          }
        }
      }

      const data = {
        title: form.title?.value,
        description: (form.description?.value || "").trim() || null,
        startAt: startAt,
        endAt: endAt,
        category: (form.category?.value || "").trim() || null,
        allDay: finalAllDay
      };

      let response;
      try {
        response = await fetch("/api/tasks", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data)
        });
      } catch (err) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
        errorBox.innerText = "서버와의 통신에 실패했습니다.";
        errorBox.classList.remove("hidden");
        return;
      }

      if (response.ok) {
        submitBtn.innerHTML = "✔ 등록 완료";
        showToast("일정이 등록되었습니다!");
        openModal();

        form.reset();
        close();

        setTimeout(() => window.location.reload(), 150);

        setTimeout(() => {
          submitBtn.disabled = false;
          submitBtn.innerHTML = originalText;
        }, 500);
      } else {
        const result = await response.json().catch(() => null);
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;

        errorBox.innerText = result?.message || "등록 실패";
        errorBox.classList.remove("hidden");
      }
    };
  }

  function bindClose() {
    document.getElementById("appModalCloseBtn")?.addEventListener("click", close);
    document.getElementById("appModalBackdrop")?.addEventListener("click", (e) => {
      if (e.target?.id === "appModalBackdrop") close();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !modal()?.classList.contains("hidden")) close();
    });
  }

  return { bindClose, loadCreate, close };
})();

document.addEventListener("DOMContentLoaded", () => {
  window.AppModal?.bindClose();

  const createBtn = document.getElementById("floatingCreateBtn");
  createBtn?.addEventListener("click", async (e) => {
    e.preventDefault();
    const dayPage = document.getElementById("day-page");
    const date = dayPage?.dataset?.date || null;
    await window.AppModal.loadCreate(date);
  });
});
