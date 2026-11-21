(function () {
  "use strict";
  if (window.__zenContentShieldReady) return;
  window.__zenContentShieldReady = true;

  // 1. Inject page-world shield immediately before page scripts run.
  try {
    var s = document.createElement("script");
    s.src = browser.runtime.getURL("page_shield.js");
    s.async = false;
    (document.head || document.documentElement).appendChild(s);
    s.onload = function () {
      if (s.parentNode) s.parentNode.removeChild(s);
    };
  } catch (e) {}

  // 2. CSP-proof wrappedJSObject overrides (works even if script-src blocks external scripts).
  try {
    if (typeof window.wrappedJSObject !== "undefined" && typeof exportFunction === "function") {
      var pageWin = window.wrappedJSObject;
      var dummyStub = cloneInto(
        {
          closed: true,
          opener: null
        },
        pageWin
      );
      dummyStub.focus = exportFunction(function () {}, pageWin);
      dummyStub.blur = exportFunction(function () {}, pageWin);
      dummyStub.close = exportFunction(function () {}, pageWin);
      dummyStub.postMessage = exportFunction(function () {}, pageWin);
      exportFunction(
        function () {
          return dummyStub;
        },
        pageWin,
        { defineAs: "open" }
      );
      exportFunction(
        function () {
          return null;
        },
        pageWin,
        { defineAs: "showModalDialog" }
      );
      pageWin.canRunAds = true;
      pageWin.isAdBlockActive = false;
      pageWin.adblock = false;
      pageWin.isAdBlocked = false;
    }
  } catch (e) {}

  // 3. Click-jacking / invisible full-screen overlay & popup anchor killer (capture phase).
  var POP_HREF_RE = /(popads|popcash|propeller|onclickads|exoclick|exosrv|realsrv|juicyads|trafficjunky|adsterra|monetag|hilltopads|popunder|adserver|adclick|clickad|outbrain|taboola|mgid|zoneid=|bannerid=|clickid=|sub_id=|aff_id=)/i;

  function isTransparentClickShield(el) {
    if (!el || el.nodeType !== 1) return false;
    var tag = el.tagName;
    if (tag === "VIDEO" || tag === "AUDIO" || tag === "INPUT" || tag === "BUTTON" || tag === "TEXTAREA" || tag === "SELECT") {
      return false;
    }
    try {
      var cs = getComputedStyle(el);
      if (cs.position !== "fixed" && cs.position !== "absolute") return false;
      var r = el.getBoundingClientRect();
      var vw = window.innerWidth || 1;
      var vh = window.innerHeight || 1;
      var coversLarge = r.width >= vw * 0.5 && r.height >= vh * 0.4;
      if (!coversLarge) return false;
      var z = parseInt(cs.zIndex, 10) || 0;
      var opacity = parseFloat(cs.opacity || "1");
      var noVisibleText = (el.innerText || "").trim().length === 0;
      var hasMediaOrForm = !!el.querySelector("video, input, textarea, select, form, canvas");
      if (hasMediaOrForm) return false;
      var transparentBg = cs.backgroundColor === "rgba(0, 0, 0, 0)" || cs.backgroundColor === "transparent";
      return (z >= 100 && noVisibleText && (transparentBg || opacity < 0.1)) || (tag === "A" && coversLarge);
    } catch (e) {
      return false;
    }
  }

  document.addEventListener(
    "click",
    function (e) {
      var el = e.target;
      if (isTransparentClickShield(el)) {
        e.preventDefault();
        e.stopImmediatePropagation();
        try {
          el.style.setProperty("display", "none", "important");
          el.remove();
        } catch (err) {}
        return false;
      }
      while (el && el !== document.body && el !== document.documentElement) {
        if (el.tagName === "A") {
          var target = (el.getAttribute("target") || "").toLowerCase();
          var href = el.getAttribute("href") || "";
          if (
            POP_HREF_RE.test(href) ||
            (target === "_blank" && isTransparentClickShield(el)) ||
            (target === "_blank" && href.trim().toLowerCase().startsWith("javascript:"))
          ) {
            e.preventDefault();
            e.stopImmediatePropagation();
            return false;
          }
        }
        el = el.parentElement;
      }
    },
    true
  );

  // 4. DOM cosmetic & annoyance sweeper + anti-adblock modal removal.
  var AD_WORDS = /(^|[-_ ])(ad|ads|adv|advert|advertisement|sponsor|sponsored|promo|popup|pop-up|popunder|interstitial|takeover|modal-ad)([-_ ]|$)/i;
  var SAFE_IFRAME_HOSTS = /(youtube|youtu\.be|vimeo|twitch|spotify|soundcloud|google\.com\/maps|recaptcha|hcaptcha|stripe|paypal|disqus|facebook\.com\/plugins|challenges\.cloudflare\.com)/i;
  var AD_SIZES = ["300x250", "728x90", "320x50", "160x600", "300x600", "970x250", "970x90", "336x280", "320x100", "468x60", "250x250", "120x600", "300x50"];

  function nameOf(el) {
    return (el.id || "") + " " + (typeof el.className === "string" ? el.className : "");
  }

  function isBigFixedAdOverlay(el) {
    try {
      var cs = getComputedStyle(el);
      if (cs.position !== "fixed" && cs.position !== "sticky") return false;
      var r = el.getBoundingClientRect();
      var vw = window.innerWidth || 1;
      var vh = window.innerHeight || 1;
      var area = Math.max(0, Math.min(r.right, vw) - Math.max(r.left, 0)) * Math.max(0, Math.min(r.bottom, vh) - Math.max(r.top, 0));
      return area > vw * vh * 0.4 && (parseInt(cs.zIndex, 10) || 0) >= 50;
    } catch (e) {
      return false;
    }
  }

  var AB_SELECTORS = [
    'div[id*="adblock"]', 'div[class*="adblock"]',
    'div[id*="ad-block"]', 'div[class*="ad-block"]',
    ".sp-message-open", "#sp-message-container", ".fc-ab-root",
    ".adblock-overlay", ".adblock-modal", ".ad-blocker-warning",
    ".anti-adblock", "#anti-adblock-modal",
    ".popup-overlay", ".ad-popup", ".popunder",
    'div[class*="overlay-ad"]', 'div[id*="overlay-ad"]',
    'ins.adsbygoogle'
  ];

  function sweepDom() {
    for (var i = 0; i < AB_SELECTORS.length; i++) {
      try {
        var matched = document.querySelectorAll(AB_SELECTORS[i]);
        for (var j = 0; j < matched.length; j++) {
          var node = matched[j];
          if (node.querySelector('input[type="text"], input[type="search"], video')) continue;
          node.style.setProperty("display", "none", "important");
          node.remove();
        }
      } catch (e) {}
    }

    if (document.body) {
      var cands = document.body.querySelectorAll("div, section, aside, iframe, a");
      var limit = Math.min(cands.length, 400);
      for (var k = 0; k < limit; k++) {
        var el = cands[k];
        try {
          if (el.tagName === "IFRAME") {
            var src = el.src || "";
            var rect = el.getBoundingClientRect();
            var w = Math.round(rect.width);
            var h = Math.round(rect.height);
            var cross = false;
            try {
              cross = !!src && new URL(src, location.href).host !== location.host;
            } catch (x) {}
            if (cross && !SAFE_IFRAME_HOSTS.test(src) && (AD_SIZES.indexOf(w + "x" + h) !== -1 || AD_WORDS.test(nameOf(el)))) {
              el.style.setProperty("display", "none", "important");
              el.remove();
            }
            continue;
          }
          if (isTransparentClickShield(el)) {
            el.style.setProperty("display", "none", "important");
            el.remove();
            continue;
          }
          if (!isBigFixedAdOverlay(el)) continue;
          if (el.querySelector("video, input, textarea, select, form, canvas")) continue;
          if ((el.innerText || "").length > 600) continue;
          if (AD_WORDS.test(nameOf(el)) || el.querySelector("iframe[src], ins.adsbygoogle")) {
            el.style.setProperty("display", "none", "important");
            el.remove();
          }
        } catch (e) {}
      }

      try {
        var bs = getComputedStyle(document.body);
        if (bs.overflow === "hidden" && document.body.classList.contains("adblock-active")) {
          document.body.style.setProperty("overflow", "auto", "important");
        }
        document.body.classList.remove("sp-message-open", "adblock-active");
      } catch (e) {}
    }
  }

  var sweepPending = false;
  function scheduleSweep() {
    if (sweepPending) return;
    sweepPending = true;
    setTimeout(function () {
      sweepPending = false;
      sweepDom();
    }, 180);
  }

  try {
    new MutationObserver(scheduleSweep).observe(document.documentElement, {
      childList: true,
      subtree: true
    });
  } catch (e) {}
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", scheduleSweep);
  } else {
    scheduleSweep();
  }

  // 5. YouTube CSS hiding + auto-skip & fast-forward fallback in DOM
  if (/(^|\.)youtube\.com$/.test(location.hostname)) {
    try {
      var ytStyle = document.createElement("style");
      ytStyle.textContent =
        "#player-ads,#masthead-ad,ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-banner-promo-renderer," +
        "ytd-promoted-sparkles-web-renderer,ytd-display-ad-renderer,ytd-promoted-video-renderer,.ytp-ad-overlay-container," +
        ".ytp-ad-text-overlay,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ytm-promoted-video-renderer," +
        "ytd-player-legacy-desktop-watch-ads-renderer,.ytd-companion-slot-renderer,ytd-statement-banner-renderer," +
        "ytm-statement-banner-renderer,.ad-container,.ytp-paid-content-overlay,.video-ads,.ytp-ad-module{display:none!important}";
      (document.head || document.documentElement).appendChild(ytStyle);
    } catch (e) {}

    var adMuted = false;
    var prevMuted = false;
    setInterval(function () {
      try {
        var player = document.querySelector(".html5-video-player, #movie_player");
        var video = document.querySelector("video.html5-main-video") || document.querySelector("video");
        var adShowing = player && (player.classList.contains("ad-showing") || player.classList.contains("ad-interrupting"));
        if (adShowing && video) {
          if (!adMuted) {
            prevMuted = !!video.muted;
            adMuted = true;
          }
          video.muted = true;
          if (isFinite(video.duration) && video.duration > 0) {
            video.currentTime = video.duration;
          }
          video.playbackRate = 16;
        } else if (adMuted && video) {
          adMuted = false;
          video.muted = prevMuted;
          video.playbackRate = 1;
        }
        var skips = document.querySelectorAll(
          ".ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-container button, .videoAdUiSkipButton"
        );
        for (var i = 0; i < skips.length; i++) {
          skips[i].click();
        }
        var closeOverlay = document.querySelector(".ytp-ad-overlay-close-button");
        if (closeOverlay) closeOverlay.click();
      } catch (e) {}
    }, 250);
  }
})();
