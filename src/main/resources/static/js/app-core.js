// src/main/resources/static/js/app-core.js

function openModal() {
  document.getElementById("successModal")?.classList.remove("hidden");
}
function closeModal() {
  document.getElementById("successModal")?.classList.add("hidden");
}

function showToast(message) {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = "bg-black text-white px-4 py-2 rounded shadow-lg opacity-90 transition transform text-sm";
  toast.innerHTML = "✔️ " + message;

  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("opacity-0");
    setTimeout(() => toast.remove(), 300);
  }, 2000);
}

// util
function toIsoLocalDateTime(dateStr, timeStr) {
  if (!dateStr) return null;
  const t = (timeStr && timeStr.trim()) ? timeStr.trim() : "00:00";
  return `${dateStr}T${t}:00`;
}

function addDays(dateStr, days) {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + days);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

// 전역 상세 열기 (data-task-id)
document.addEventListener("DOMContentLoaded", () => {
  // 상세 열기: data-task-id 클릭
  document.addEventListener("click", async (e) => {
    const el = e.target?.closest?.("[data-task-id]");
    if (!el) return;

    const taskId = el.getAttribute("data-task-id");
    if (!taskId) return;

    // DetailModal은 modal-detail.js에서 제공
    await window.DetailModal?.loadDetail(taskId);
  });
});
