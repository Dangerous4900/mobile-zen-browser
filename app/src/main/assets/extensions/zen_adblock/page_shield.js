(function () {
  "use strict";
  if (window.__zenPageShieldInstalled) return;
  window.__zenPageShieldInstalled = true;

  var lastGesture = 0;
  ["click", "touchstart", "keydown", "pointerdown"].forEach(function (ev) {
    try {
      window.addEventListener(
        ev,
        function (e) {
          if (e && e.isTrusted) {
            lastGesture = Date.now();
          }
        },
        true
      );
    } catch (e) {}
  });
  function hasTrustedGesture() {
    return Date.now() - lastGesture < 1200;
  }

  // 1. Block all JavaScript window.open popups and showModalDialog
  var noop = function () {};
  var dummyWin = {
    focus: noop,
    blur: noop,
    close: noop,
    postMessage: noop,
    closed: true,
    opener: null,
    location: { href: "", replace: noop, assign: noop, reload: noop },
    document: { write: noop, writeln: noop, open: noop, close: noop, body: null }
  };
  try {
    Object.defineProperty(window, "open", {
      value: function () {
        return dummyWin;
      },
      writable: false,
      configurable: false
    });
  } catch (e) {
    try {
      window.open = function () {
        return dummyWin;
      };
    } catch (err) {}
  }
  try {
    Object.defineProperty(window, "showModalDialog", {
      value: function () {
        return null;
      },
      writable: false,
      configurable: false
    });
  } catch (e) {}

  // 2. Block synthetic / detached popunder anchor clicks
  try {
    var origAnchorClick = HTMLAnchorElement.prototype.click;
    Object.defineProperty(HTMLAnchorElement.prototype, "click", {
      value: function () {
        var href = (this.getAttribute("href") || "").toLowerCase();
        var target = (this.getAttribute("target") || "").toLowerCase();
        var isPopKeyword = /(pop|ad|bet|clickid=|zoneid=|bannerid=|affiliate|redirect|casino|spin|monetag|adsterra|exoclick|propeller)/i.test(
          href
        );
        if (!this.isConnected || target === "_blank" && (isPopKeyword || !hasTrustedGesture())) {
          return;
        }
        return origAnchorClick.apply(this, arguments);
      },
      writable: false,
      configurable: false
    });
  } catch (e) {}

  // 3. Block unsolicited alert / confirm / prompt spam popups
  try {
    var origAlert = window.alert;
    var origConfirm = window.confirm;
    var origPrompt = window.prompt;
    var lastAlertAt = 0;
    window.alert = function () {
      var now = Date.now();
      if (!hasTrustedGesture() || now - lastAlertAt < 5000) return;
      lastAlertAt = now;
      return origAlert.apply(window, arguments);
    };
    window.confirm = function () {
      if (!hasTrustedGesture()) return false;
      return origConfirm.apply(window, arguments);
    };
    window.prompt = function () {
      if (!hasTrustedGesture()) return null;
      return origPrompt.apply(window, arguments);
    };
  } catch (e) {}

  // 4. Anti-adblock defusers & scriptlet shims
  try {
    window.canRunAds = true;
    window.isAdBlockActive = false;
    window.adblock = false;
    window.isAdBlocked = false;
    window.google_ad_status = 1;
    window.adsbygoogle = window.adsbygoogle || [];
    window.adsbygoogle.loaded = true;
    window.adsbygoogle.push = function () {
      return 1;
    };

    var noopSelf = function () {
      return this;
    };
    var fakeFab = {
      check: noopSelf,
      clearEvent: noopSelf,
      on: function (isDetected, cb) {
        if (isDetected === false && typeof cb === "function") {
          try {
            cb();
          } catch (e) {}
        }
        return this;
      },
      onDetected: noopSelf,
      onNotDetected: function (cb) {
        if (typeof cb === "function") {
          try {
            cb();
          } catch (e) {}
        }
        return this;
      },
      setOption: noopSelf
    };
    window.fuckAdBlock = fakeFab;
    window.FuckAdBlock = fakeFab;
    window.blockAdBlock = fakeFab;
    window.BlockAdBlock = fakeFab;
    window.snigel = window.snigel || { adblock: false };
  } catch (e) {}

  // 5. YouTube JSON / player response ad stripping
  if (/(^|\.)youtube\.com$/.test(location.hostname)) {
    var cleanYtPayload = function (obj) {
      if (!obj || typeof obj !== "object") return;
      if (Array.isArray(obj.adPlacements)) obj.adPlacements = [];
      if (Array.isArray(obj.playerAds)) obj.playerAds = [];
      if (Array.isArray(obj.adSlots)) obj.adSlots = [];
      if (obj.adBreakHeartbeatParams) delete obj.adBreakHeartbeatParams;
      if (obj.playerResponse) cleanYtPayload(obj.playerResponse);
      if (obj.response) cleanYtPayload(obj.response);
    };

    try {
      var origJsonParse = JSON.parse;
      JSON.parse = function () {
        var res = origJsonParse.apply(this, arguments);
        try {
          cleanYtPayload(res);
        } catch (e) {}
        return res;
      };
    } catch (e) {}

    try {
      var origRespJson = Response.prototype.json;
      Response.prototype.json = function () {
        return origRespJson.apply(this, arguments).then(function (res) {
          try {
            cleanYtPayload(res);
          } catch (e) {}
          return res;
        });
      };
    } catch (e) {}

    try {
      var initialPlayer = window.ytInitialPlayerResponse;
      if (initialPlayer) cleanYtPayload(initialPlayer);
      Object.defineProperty(window, "ytInitialPlayerResponse", {
        configurable: true,
        get: function () {
          return initialPlayer;
        },
        set: function (val) {
          try {
            cleanYtPayload(val);
          } catch (e) {}
          initialPlayer = val;
        }
      });
    } catch (e) {}
  }
})();
