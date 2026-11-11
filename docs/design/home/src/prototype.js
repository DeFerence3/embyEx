// Prototype helper (not part of the product): keeps overlays opened inside a scrolled device frame in view.
document.addEventListener("click", (e) => {
  const t = e.target.closest("[data-open]");
  if (!t) return;
  const f = t.closest(".v-frame");
  if (!f) return;
  requestAnimationFrame(() => {
    const o = f.querySelector(t.dataset.open);
    if (!o || !o.classList.contains("overlay")) return;
    o.style.top = f.scrollTop + "px";
    o.style.bottom = "auto";
    o.style.height = f.clientHeight + "px";
  });
});
