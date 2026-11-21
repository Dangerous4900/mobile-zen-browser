(function () {
  "use strict";
  if (window.__zenPageUxInitialized) return;
  window.__zenPageUxInitialized = true;

  var SCROLL_KEY_PREFIX = "__zen_scroll_v1:";
  var userInteracted = false;
  var restoredDone = false;
  var currentMuted = false;
  var currentPip = false;
  var pipEnteredAt = 0;
  var pipTransitionAt = 0;
  var lastUserPauseClickAt = 0;
  var lastReportedTop = true;
  var bgPort = null;
  var injectedJsHashSet = new Set();

  function normalizeUrl(raw) {
    try {
      var u = new URL(raw || window.location.href);
      var clean = u.origin + u.pathname + u.search;
      if (clean.length > 8 && clean.endsWith("/")) clean = clean.slice(0, -1);
      return clean;
    } catch (e) {
      return String(raw || window.location.href).split("#")[0];
    }
  }

  function storageKey() {
    return SCROLL_KEY_PREFIX + normalizeUrl(window.location.href);
  }

  function connectPort() {
    try {
      if (typeof browser !== "undefined" && browser.runtime && browser.runtime.connect) {
        bgPort = browser.runtime.connect({ name: "zen-page-ux-content" });
        bgPort.onMessage.addListener(handleHostMessage);
      }
    } catch (e) {}
  }

  function sendToHost(msg) {
    try {
      if (bgPort) {
        bgPort.postMessage(msg);
        return;
      }
      if (typeof browser !== "undefined" && browser.runtime && browser.runtime.sendMessage) {
        browser.runtime.sendMessage(msg).catch(function () {});
      }
    } catch (e) {}
  }

  function readLocalScrollY() {
    try {
      var v = parseInt(localStorage.getItem(storageKey()) || "0", 10);
      return isNaN(v) || v < 0 ? 0 : v;
    } catch (e) {
      return 0;
    }
  }

  function saveScrollNow() {
    try {
      var y = Math.round(window.scrollY || document.documentElement.scrollTop || 0);
      if (y < 0) y = 0;
      localStorage.setItem(storageKey(), String(y));
      sendToHost({ type: "saveScroll", url: window.location.href, y: y });
      var atTop = y <= 4;
      if (atTop !== lastReportedTop) {
        lastReportedTop = atTop;
        sendToHost({ type: "topState", atTop: atTop, url: window.location.href });
      }
    } catch (e) {}
  }

  function attemptRestoreTo(targetY) {
    if (userInteracted || targetY <= 2) return;
    if (window.location.hash && window.location.hash.length > 1) return;
    try {
      var delays = [0, 60, 180, 420, 850, 1500, 2400];
      delays.forEach(function (ms) {
        setTimeout(function () {
          if (userInteracted || restoredDone) return;
          var cur = window.scrollY || document.documentElement.scrollTop || 0;
          if (Math.abs(cur - targetY) <= 8) {
            restoredDone = true;
            return;
          }
          var maxScroll = Math.max(0, (document.documentElement.scrollHeight || 0) - window.innerHeight);
          if (maxScroll >= targetY - 12 || ms >= 1500) {
            window.scrollTo({ top: Math.min(targetY, maxScroll > 0 ? maxScroll : targetY), behavior: "instant" });
          }
        }, ms);
      });
    } catch (e) {}
  }

  function queryAndRestoreScroll() {
    var localY = readLocalScrollY();
    if (localY > 2) attemptRestoreTo(localY);
    try {
      if (typeof browser !== "undefined" && browser.runtime && browser.runtime.sendMessage) {
        browser.runtime.sendMessage({ type: "getScroll", url: window.location.href })
          .then(function (resp) {
            var nativeY = resp && typeof resp.y === "number" ? resp.y : 0;
            if (nativeY > 2 && !userInteracted) {
              attemptRestoreTo(nativeY);
            }
          })
          .catch(function () {});
      }
    } catch (e) {}
  }

  // Mark manual user scroll so automatic restoration stops immediately when the user interacts
  function markUserInteract() {
    userInteracted = true;
  }
  window.addEventListener("touchstart", markUserInteract, { passive: true, capture: true });
  window.addEventListener("wheel", markUserInteract, { passive: true, capture: true });
  window.addEventListener("keydown", function (e) {
    if (["ArrowDown", "ArrowUp", "PageDown", "PageUp", "Space", "Home", "End"].includes(e.code)) {
      markUserInteract();
    }
  }, { passive: true, capture: true });

  var scrollDebounce = null;
  window.addEventListener("scroll", function () {
    var atTop = (window.scrollY || document.documentElement.scrollTop || 0) <= 4;
    if (atTop !== lastReportedTop) {
      lastReportedTop = atTop;
      sendToHost({ type: "topState", atTop: atTop, url: window.location.href });
    }
    clearTimeout(scrollDebounce);
    scrollDebounce = setTimeout(saveScrollNow, 180);
  }, { passive: true, capture: true });

  window.addEventListener("pagehide", saveScrollNow, true);
  window.addEventListener("beforeunload", saveScrollNow, true);
  document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "hidden") saveScrollNow();
  }, true);

  // AudioContext master gain tracking for complete WebAudio muting
  var zenAudioGains = [];
  try {
    var OrigAC = window.AudioContext || window.webkitAudioContext;
    if (OrigAC && !OrigAC.__zenWrapped) {
      var WrappedAC = function () {
        var ctx = new OrigAC();
        try {
          var masterGain = ctx.createGain();
          masterGain.gain.value = currentMuted ? 0 : 1;
          var origDest = ctx.destination;
          masterGain.connect(origDest);
          Object.defineProperty(ctx, "__zenMasterGain", { value: masterGain });
          zenAudioGains.push({ ctx: ctx, gain: masterGain });
        } catch (e) {}
        return ctx;
      };
      WrappedAC.prototype = OrigAC.prototype;
      WrappedAC.__zenWrapped = true;
      if (window.AudioContext) window.AudioContext = WrappedAC;
      if (window.webkitAudioContext) window.webkitAudioContext = WrappedAC;
    }
  } catch (e) {}

  function applyMuted(muted) {
    currentMuted = !!muted;
    try {
      var elems = document.querySelectorAll("video, audio");
      for (var i = 0; i < elems.length; i++) {
        var el = elems[i];
        if (muted) {
          if (el.__zenOrigVolume === undefined) el.__zenOrigVolume = el.volume;
          el.muted = true;
          el.volume = 0;
        } else {
          el.muted = false;
          if (el.__zenOrigVolume !== undefined) el.volume = el.__zenOrigVolume;
        }
      }
      var ytPlayer = document.getElementById("movie_player");
      if (ytPlayer) {
        if (muted && typeof ytPlayer.mute === "function") ytPlayer.mute();
        else if (!muted && typeof ytPlayer.unMute === "function") ytPlayer.unMute();
      }
      zenAudioGains.forEach(function (entry) {
        try {
          if (entry && entry.gain && entry.gain.gain) {
            entry.gain.gain.value = muted ? 0 : 1;
          }
        } catch (e) {}
      });
    } catch (e) {}
  }

  function applyPipMode(enabled) {
    currentPip = !!enabled;
    pipTransitionAt = Date.now();
    if (currentPip) {
      pipEnteredAt = pipTransitionAt;
    }
    try {
      var styleId = "__zen_pip_video_style";
      var existing = document.getElementById(styleId);
      if (enabled) {
        if (!existing) {
          existing = document.createElement("style");
          existing.id = styleId;
          existing.textContent =
            "video.__zen_pip_active { position: fixed !important; inset: 0 !important; width: 100vw !important; height: 100vh !important; object-fit: contain !important; z-index: 2147483647 !important; background: #000 !important; }" +
            "html.__zen_pip_host, body.__zen_pip_host { overflow: hidden !important; background: #000 !important; }";
          (document.head || document.documentElement).appendChild(existing);
        }
        var vids = document.querySelectorAll("video");
        var best = null;
        for (var i = 0; i < vids.length; i++) {
          if (!vids[i].paused && !vids[i].ended) { best = vids[i]; break; }
        }
        if (!best && vids.length > 0) best = vids[0];
        if (best) {
          best.classList.add("__zen_pip_active");
          document.documentElement.classList.add("__zen_pip_host");
          if (document.body) document.body.classList.add("__zen_pip_host");
          if (best.paused && !best.ended && (Date.now() - lastUserPauseClickAt > 2500)) {
            best.play().catch(function () {});
          }
        }
      } else {
        if (existing) existing.remove();
        var wasPlayingEl = document.querySelector("video.__zen_pip_active");
        document.querySelectorAll("video.__zen_pip_active").forEach(function (v) {
          v.classList.remove("__zen_pip_active");
        });
        document.documentElement.classList.remove("__zen_pip_host");
        if (document.body) document.body.classList.remove("__zen_pip_host");
        if (wasPlayingEl && wasPlayingEl.paused && !wasPlayingEl.ended && (Date.now() - lastUserPauseClickAt > 2500)) {
          wasPlayingEl.play().catch(function () {});
        }
      }
    } catch (e) {}
  }

  function applyExtensionPayload(cssText, jsText, shimText) {
    try {
      if (shimText) {
        var shimHash = "shim:" + shimText.length;
        if (!injectedJsHashSet.has(shimHash)) {
          injectedJsHashSet.add(shimHash);
          (0, eval)(shimText);
        }
      }
      var styleId = "__zen_ext_dynamic_css";
      var styleEl = document.getElementById(styleId);
      if (cssText && cssText.trim().length > 0) {
        if (!styleEl) {
          styleEl = document.createElement("style");
          styleEl.id = styleId;
          (document.head || document.documentElement).appendChild(styleEl);
        }
        if (styleEl.textContent !== cssText) styleEl.textContent = cssText;
      } else if (styleEl) {
        styleEl.remove();
      }
      if (jsText && jsText.trim().length > 0) {
        var jsHash = jsText.length + ":" + jsText.slice(0, 64);
        if (!injectedJsHashSet.has(jsHash)) {
          injectedJsHashSet.add(jsHash);
          var fn = new Function(jsText);
          fn();
        }
      }
    } catch (e) {}
  }

  function handleHostMessage(msg) {
    if (!msg || typeof msg !== "object") return;
    if (msg.type === "setMuted") {
      applyMuted(msg.muted);
    } else if (msg.type === "setPipMode") {
      applyPipMode(msg.enabled);
    } else if (msg.type === "extensionPayload") {
      applyExtensionPayload(msg.css || "", msg.js || "", msg.shim || "");
    } else if (msg.type === "evalScript" && typeof msg.js === "string" && msg.js.trim().length > 0) {
      try {
        var execFn = new Function(msg.js);
        execFn();
      } catch (e) {}
    } else if (msg.type === "restoreScroll" && typeof msg.y === "number") {
      attemptRestoreTo(msg.y);
    } else if (msg.type === "saveScrollNow") {
      saveScrollNow();
    } else if (msg.type === "mediaCommand") {
      var m = document.querySelector("video, audio");
      if (m) {
        if (msg.action === "play") {
          lastUserPauseClickAt = 0;
          m.play().catch(function () {});
        } else if (msg.action === "pause") {
          lastUserPauseClickAt = Date.now();
          m.pause();
        } else if (msg.action === "seekBy" && typeof msg.deltaMs === "number") {
          try {
            var nextTime = Math.max(0, (m.currentTime || 0) + (msg.deltaMs / 1000));
            if (m.duration && isFinite(m.duration)) nextTime = Math.min(m.duration, nextTime);
            m.currentTime = nextTime;
          } catch (e) {}
        }
      }
    }
  }

  // Track user clicks on video controls so user-requested pause isn't overridden
  document.addEventListener("click", function (ev) {
    try {
      var t = ev.target;
      if (t && t.closest && t.closest("video, .ytp-play-button, [aria-label*='Pause'], [aria-label*='Play']")) {
        lastUserPauseClickAt = Date.now();
        sendToHost({ type: "userMediaPauseIntent" });
      }
    } catch (e) {}
  }, true);

  // Fallback media detector for sites without navigator.mediaSession
  var lastMediaSig = "";
  function reportMediaState() {
    try {
      var list = document.querySelectorAll("video, audio");
      if (!list || list.length === 0) return;
      var activeEl = null;
      var playing = false;
      for (var i = 0; i < list.length; i++) {
        var m = list[i];
        if (!m.paused && !m.ended && m.readyState > 0) {
          activeEl = m;
          playing = true;
          break;
        }
        if (!activeEl && (m.currentTime > 0 || !m.paused)) activeEl = m;
      }
      if (!activeEl) return;
      var title = (document.title || window.location.hostname || "Media").trim();
      var sig = playing + "|" + title;
      if (sig === lastMediaSig) return;
      lastMediaSig = sig;
      sendToHost({
        type: "mediaState",
        isActive: true,
        isPlaying: playing,
        title: title,
        url: window.location.href
      });
    } catch (e) {}
  }

  document.addEventListener("play", function () {
    if (currentMuted) applyMuted(true);
    reportMediaState();
  }, true);
  document.addEventListener("pause", function (ev) {
    var targetEl = ev && ev.target;
    var now = Date.now();
    var withinPipTransition = (currentPip && (now - pipEnteredAt < 3800)) || (now - pipTransitionAt < 3800);
    var userPaused = (now - lastUserPauseClickAt) < 2500;
    if (withinPipTransition && !userPaused && targetEl && !targetEl.ended && typeof targetEl.play === "function") {
      setTimeout(function () {
        if (((Date.now() - pipTransitionAt < 4000) || currentPip) && !targetEl.ended && (Date.now() - lastUserPauseClickAt > 2500)) {
          targetEl.play().catch(function () {});
        }
      }, 90);
      return;
    }
    reportMediaState();
  }, true);
  document.addEventListener("ended", reportMediaState, true);

  // Mozilla Add-ons Store (addons.mozilla.org) + Userscript store direct install hook
  function setupExtensionStoreBridge() {
    try {
      var host = (window.location.hostname || "").toLowerCase();
      if (host === "addons.mozilla.org" || host.endsWith(".addons.mozilla.org")) {
        var lastAmoInstallAt = 0;
        function unlockAmoButtons() {
          try {
            var notices = document.querySelectorAll(".Notice-warning, .Notice--warning, .InstallButtonWrapper-notice, .GetFirefoxButton, .AMOPrompt");
            notices.forEach(function (n) {
              var txt = (n.textContent || "").toLowerCase();
              if (txt.indexOf("firefox") !== -1 || txt.indexOf("android") !== -1 || txt.indexOf("download") !== -1 || txt.indexOf("compatible") !== -1) {
                n.style.display = "none";
              }
            });
            var btns = document.querySelectorAll(
              ".AMInstallButton-button, a.AMInstallButton-button, .InstallButtonWrapper .Button, a.InstallButton, .AMOPrompt .Button, [data-testid*='install'] a, [data-testid*='install'] button, a[href*='.xpi'], a[href*='/downloads/file/']"
            );
            btns.forEach(function (btn) {
              btn.removeAttribute("disabled");
              btn.removeAttribute("aria-disabled");
              btn.classList.remove("Button--disabled", "disabled", "AMInstallButton-loading");
              var label = (btn.textContent || "").toLowerCase();
              if (label.indexOf("only with firefox") !== -1 || label.indexOf("download firefox") !== -1 || label.indexOf("get firefox") !== -1) {
                btn.textContent = "Add to Firefox";
              }
            });
          } catch (e) {}
        }
        document.addEventListener("click", function (e) {
          try {
            var target = e.target && e.target.closest
              ? e.target.closest('a.AMInstallButton-button, button.AMInstallButton-button, a[href*=".xpi"], a[href*="/downloads/file/"], .InstallButtonWrapper a, .InstallButtonWrapper button, .AMInstallButton a, .AMInstallButton button, [data-testid*="install"] a, [data-testid*="install"] button, a.InstallButton')
              : null;
            if (!target && e.target && e.target.closest) {
              var maybeBtn = e.target.closest("a, button");
              var txt = maybeBtn ? (maybeBtn.textContent || "").toLowerCase().trim() : "";
              if (
                txt === "add to firefox" ||
                txt === "add to zen" ||
                txt === "install" ||
                txt.indexOf("add to firefox") !== -1 ||
                txt.indexOf("add to android") !== -1 ||
                txt.indexOf("install anyway") !== -1
              ) {
                target = maybeBtn;
              }
            }
            if (!target) return;
            var href = target.getAttribute("href") || "";
            if (!href) {
              var altLink = document.querySelector('a.AMInstallButton-button[href], a[href*=".xpi"], a[href*="/downloads/file/"]');
              if (altLink) href = altLink.getAttribute("href") || "";
            }
            if (href && href.indexOf("/") === 0) href = window.location.origin + href;
            var targetUrl = (href.indexOf(".xpi") !== -1 || href.indexOf("/downloads/file/") !== -1) ? href : window.location.href;
            e.preventDefault();
            e.stopPropagation();
            if (typeof e.stopImmediatePropagation === "function") e.stopImmediatePropagation();
            var now = Date.now();
            if (now - lastAmoInstallAt < 1200) return;
            lastAmoInstallAt = now;
            target.textContent = "Installing in Zen...";
            sendToHost({
              type: "installAmoExtension",
              url: targetUrl,
              pageUrl: window.location.href,
              title: document.title || ""
            });
            setTimeout(function () {
              target.textContent = "✓ Added to Zen";
            }, 1200);
          } catch (err) {}
        }, true);
        if (document.readyState === "loading") {
          document.addEventListener("DOMContentLoaded", unlockAmoButtons, true);
        } else {
          unlockAmoButtons();
        }
        try {
          if (typeof MutationObserver !== "undefined" && document.documentElement) {
            var amoObserver = new MutationObserver(function () {
              unlockAmoButtons();
            });
            amoObserver.observe(document.documentElement, { childList: true, subtree: true });
          }
        } catch (obsErr) {}
        setInterval(unlockAmoButtons, 1500);
      } else if (host.indexOf("greasyfork.org") !== -1 || host.indexOf("openuserjs.org") !== -1) {
        document.addEventListener("click", function (e) {
          try {
            var target = e.target && e.target.closest
              ? e.target.closest('a[href*=".user.js"], a.install-link')
              : null;
            if (!target) return;
            e.preventDefault();
            e.stopPropagation();
            var href = target.href || window.location.href;
            target.textContent = "Installing...";
            sendToHost({
              type: "installAmoExtension",
              url: href,
              title: document.title || ""
            });
          } catch (err) {}
        }, true);
      }
    } catch (e) {}
  }

  var aiSidePanelFitActive = false;

  function applyAiSidePanelFitRules() {
    try {
      var styleId = "__zen_ai_sidepanel_fit_css";
      var styleEl = document.getElementById(styleId);
      var css =
        "html, body { max-width: 100% !important; overflow-x: hidden !important; box-sizing: border-box !important; }\n" +
        "main, article, form, section, [role='main'], .prose, .markdown { max-width: 100% !important; min-width: 0 !important; }\n" +
        "pre, code, table { max-width: 100% !important; overflow-x: auto !important; }\n" +
        "pre { white-space: pre-wrap !important; word-break: break-word !important; }\n" +
        "img, video, canvas, iframe { max-width: 100% !important; }\n" +
        "textarea, [contenteditable='true'], input[type='text'], input[type='search'] { max-width: 100% !important; min-width: 0 !important; }\n";
      if (!styleEl) {
        styleEl = document.createElement("style");
        styleEl.id = styleId;
        styleEl.textContent = css;
        (document.head || document.documentElement).appendChild(styleEl);
      } else if (styleEl.textContent !== css) {
        styleEl.textContent = css;
      }

      var containerWidth = window.innerWidth || document.documentElement.clientWidth || 0;
      if (containerWidth > 0 && containerWidth < 355) {
        var meta = document.querySelector('meta[name="viewport"]');
        var desired = "width=360, user-scalable=yes";
        if (!meta) {
          meta = document.createElement("meta");
          meta.setAttribute("name", "viewport");
          meta.setAttribute("content", desired);
          (document.head || document.documentElement).appendChild(meta);
        } else if (meta.getAttribute("content") !== desired) {
          meta.setAttribute("content", desired);
        }
      }
    } catch (e) {}
  }

  function setupAiSidePanelFit() {
    try {
      if (typeof browser === "undefined" || !browser.runtime || !browser.runtime.sendNativeMessage) return;
      browser.runtime.sendNativeMessage("zenPageUx", { type: "checkAiSidePanel" })
        .then(function (resp) {
          if (resp && resp.isAiSidePanel) {
            aiSidePanelFitActive = true;
            applyAiSidePanelFitRules();
            [150, 500, 1200, 2500].forEach(function (ms) {
              setTimeout(applyAiSidePanelFitRules, ms);
            });
            if (typeof MutationObserver !== "undefined" && document.head) {
              try {
                var vpObs = new MutationObserver(function () {
                  if (aiSidePanelFitActive) applyAiSidePanelFitRules();
                });
                vpObs.observe(document.head, { childList: true, subtree: true, attributes: true });
              } catch (obsErr) {}
            }
          }
        })
        .catch(function () {});
    } catch (e) {}
  }

  connectPort();
  queryAndRestoreScroll();
  setupExtensionStoreBridge();
  setupAiSidePanelFit();
  document.addEventListener("DOMContentLoaded", function () {
    if (currentMuted) applyMuted(true);
    if (currentPip) applyPipMode(true);
    if (aiSidePanelFitActive) applyAiSidePanelFitRules();
    queryAndRestoreScroll();
  }, true);
  window.addEventListener("pageshow", function (ev) {
    if (ev && ev.persisted) userInteracted = false;
    if (aiSidePanelFitActive) applyAiSidePanelFitRules();
    queryAndRestoreScroll();
  }, true);
  window.addEventListener("load", function () {
    if (aiSidePanelFitActive) applyAiSidePanelFitRules();
    queryAndRestoreScroll();
    sendToHost({ type: "topState", atTop: (window.scrollY || 0) <= 4, url: window.location.href });
  }, true);
})();

