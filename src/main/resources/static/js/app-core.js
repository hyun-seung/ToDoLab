// src/main/resources/static/js/app-core.js

function openModal() {
  document.getElementById("successModal")?.classList.remove("hidden");
}
function closeModal() {
  document.getElementById("successModal")?.classList.add("hidden");
}

function showToast(message, type = "success") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  const normalizedType = type === "error" ? "error" : "success";
  toast.className = `app-toast app-toast-${normalizedType}`;
  toast.setAttribute("role", normalizedType === "error" ? "alert" : "status");

  const icon = document.createElement("span");
  icon.className = "app-toast-icon";
  icon.setAttribute("aria-hidden", "true");
  icon.textContent = normalizedType === "error" ? "!" : "✓";

  const text = document.createElement("span");
  text.className = "app-toast-message";
  text.textContent = String(message || "");

  toast.append(icon, text);

  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("app-toast-leaving");
    setTimeout(() => toast.remove(), 180);
  }, normalizedType === "error" ? 3600 : 2200);
}

window.AppFeedback = {
  success(message) {
    showToast(message, "success");
  },
  error(message) {
    showToast(message, "error");
  }
};

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
