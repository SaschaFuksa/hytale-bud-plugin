(function () {
  "use strict";

  var STORAGE_KEY = "bud-plugin-theme";
  var root = document.documentElement;
  var toggle = document.getElementById("theme-toggle");

  function systemPrefersDark() {
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  }

  function apply(theme) {
    if (theme === "light" || theme === "dark") {
      root.setAttribute("data-theme", theme);
    } else {
      root.removeAttribute("data-theme");
    }
    if (toggle) {
      var isDark = theme === "dark" || (theme !== "light" && systemPrefersDark());
      toggle.textContent = isDark ? "☀️" : "🌙";
      toggle.setAttribute("aria-label", isDark ? "Switch to light theme" : "Switch to dark theme");
    }
  }

  var stored = null;
  try {
    stored = window.localStorage.getItem(STORAGE_KEY);
  } catch (err) {
    stored = null;
  }
  apply(stored);

  if (toggle) {
    toggle.addEventListener("click", function () {
      var current = root.getAttribute("data-theme");
      var currentlyDark = current === "dark" || (!current && systemPrefersDark());
      var next = currentlyDark ? "light" : "dark";
      apply(next);
      try {
        window.localStorage.setItem(STORAGE_KEY, next);
      } catch (err) {
        toggle.dataset.persisted = "false";
      }
    });
  }
})();
