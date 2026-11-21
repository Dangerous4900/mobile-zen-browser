(function () {
  "use strict";

  var nativePort = null;
  var contentPorts = new Set();
  var latestExtensionConfig = { css: "", js: "", shim: "" };
  var latestMuted = false;
  var latestPip = false;

  function connectNative() {
    try {
      if (browser.runtime && browser.runtime.connectNative) {
        nativePort = browser.runtime.connectNative("zenPageUx");
        nativePort.onMessage.addListener(function (msg) {
          if (!msg || typeof msg !== "object") return;
          if (msg.type === "extensionPayload") {
            latestExtensionConfig = {
              css: msg.css || "",
              js: msg.js || "",
              shim: msg.shim || ""
            };
          } else if (msg.type === "setMuted") {
            latestMuted = !!msg.muted;
          } else if (msg.type === "setPipMode") {
            latestPip = !!msg.enabled;
          }
          broadcastToContent(msg);
        });
        nativePort.onDisconnect.addListener(function () {
          nativePort = null;
        });
      }
    } catch (e) {
      nativePort = null;
    }
  }

  function postToNative(msg) {
    try {
      if (nativePort) {
        nativePort.postMessage(msg);
      } else if (browser.runtime && browser.runtime.sendNativeMessage) {
        return browser.runtime.sendNativeMessage("zenPageUx", msg).catch(function () { return null; });
      }
    } catch (e) {}
    return Promise.resolve(null);
  }

  function broadcastToContent(msg) {
    contentPorts.forEach(function (p) {
      try {
        p.postMessage(msg);
      } catch (e) {
        contentPorts.delete(p);
      }
    });
  }

  connectNative();

  if (browser.runtime && browser.runtime.onConnect) {
    browser.runtime.onConnect.addListener(function (port) {
      contentPorts.add(port);
      try {
        if (latestExtensionConfig.css || latestExtensionConfig.js || latestExtensionConfig.shim) {
          port.postMessage({
            type: "extensionPayload",
            css: latestExtensionConfig.css,
            js: latestExtensionConfig.js,
            shim: latestExtensionConfig.shim
          });
        }
        if (latestMuted) {
          port.postMessage({ type: "setMuted", muted: true });
        }
        if (latestPip) {
          port.postMessage({ type: "setPipMode", enabled: true });
        }
      } catch (e) {}
      port.onMessage.addListener(function (msg) {
        if (!msg) return;
        postToNative(msg);
      });
      port.onDisconnect.addListener(function () {
        contentPorts.delete(port);
      });
    });
  }

  if (browser.runtime && browser.runtime.onMessage) {
    browser.runtime.onMessage.addListener(function (msg) {
      if (!msg) return false;
      if (msg.type === "getScroll") {
        if (browser.runtime && browser.runtime.sendNativeMessage) {
          return browser.runtime.sendNativeMessage("zenPageUx", msg).catch(function () {
            return { y: 0 };
          });
        }
        return Promise.resolve({ y: 0 });
      }
      postToNative(msg);
      return false;
    });
  }
})();
