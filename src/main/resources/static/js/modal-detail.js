// src/main/resources/static/js/modal-detail.js

window.DetailModal = (() => {
  const modal = () => document.getElementById("detailModal");
  const content = () => document.getElementById("detailModalContent");

  function open() {
    modal()?.classList.remove("hidden");
    document.body.style.overflow = "hidden";
  }

  function close() {
    modal()?.classList.add("hidden");
    document.body.style.overflow = "";
    if (content()) content().innerHTML = "";
  }

  async function loadDetail(taskId) {
    if (!taskId) return;

    const url = `/api/tasks/${encodeURIComponent(taskId)}`;
    content().innerHTML = "<div class='text-gray-400 text-sm'>불러오는 중...</div>";

    let res;
    try {
      res = await fetch(url, { headers: { "X-Requested-With": "fetch" }});
    } catch (e) {
      showToast("상세 정보를 불러오지 못했습니다.");
      return;
    }

    if (!res.ok) {
      showToast("상세 정보를 불러오지 못했습니다.");
      return;
    }

    const html = await res.text();
    content().innerHTML = html;
    open();
  }

  function bindClose() {
    document.getElementById("detailModalCloseBtn")?.addEventListener("click", close);
    document.getElementById("detailModalBackdrop")?.addEventListener("click", (e) => {
      if (e.target?.id === "detailModalBackdrop") close();
    });
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && !modal()?.classList.contains("hidden")) close();
    });
  }

  return { bindClose, loadDetail, close };
})();

// detail.html에서 호출
window.closeTaskDetailModal = function () {
  window.DetailModal?.close();
};

document.addEventListener("DOMContentLoaded", () => {
  window.DetailModal?.bindClose();
});
