(function() {
  'use strict';
  try {
    const defineVisibility = function(target) {
      if (!target) return;
      try {
        Object.defineProperty(target, 'hidden', {
          get: function() { return false; },
          set: function() {},
          enumerable: true,
          configurable: true
        });
      } catch (e) {}
      try {
        Object.defineProperty(target, 'visibilityState', {
          get: function() { return 'visible'; },
          set: function() {},
          enumerable: true,
          configurable: true
        });
      } catch (e) {}
      try {
        Object.defineProperty(target, 'webkitHidden', {
          get: function() { return false; },
          set: function() {},
          enumerable: true,
          configurable: true
        });
      } catch (e) {}
      try {
        Object.defineProperty(target, 'webkitVisibilityState', {
          get: function() { return 'visible'; },
          set: function() {},
          enumerable: true,
          configurable: true
        });
      } catch (e) {}
    };

    if (typeof Document !== 'undefined' && Document.prototype) {
      defineVisibility(Document.prototype);
    }
    if (typeof document !== 'undefined') {
      defineVisibility(document);
    }

    // Prevent web apps (YouTube, Twitch, Spotify, etc.) from registering visibility/freeze listeners that pause playback on screen-off
    const blockedEvents = new Set(['visibilitychange', 'webkitvisibilitychange', 'freeze']);
    if (typeof EventTarget !== 'undefined' && EventTarget.prototype && !EventTarget.prototype.__zenBgPlayPatched) {
      const origAdd = EventTarget.prototype.addEventListener;
      EventTarget.prototype.addEventListener = function(type, listener, options) {
        if (typeof type === 'string' && blockedEvents.has(type.toLowerCase())) {
          if (this === window || this === document) {
            return;
          }
        }
        return origAdd.call(this, type, listener, options);
      };
      EventTarget.prototype.__zenBgPlayPatched = true;
    }

    const stopPropagation = function(e) {
      if (e) {
        e.stopImmediatePropagation();
        e.stopPropagation();
      }
    };

    window.addEventListener('visibilitychange', stopPropagation, true);
    document.addEventListener('visibilitychange', stopPropagation, true);
    window.addEventListener('webkitvisibilitychange', stopPropagation, true);
    document.addEventListener('webkitvisibilitychange', stopPropagation, true);

    try {
      if (typeof Document !== 'undefined' && Document.prototype) {
        Document.prototype.hasFocus = function() { return true; };
      }
      if (typeof document !== 'undefined') {
        document.hasFocus = function() { return true; };
      }
    } catch (e) {}

    window.addEventListener('blur', function(e) {
      if (e && (e.target === window || e.target === document)) {
        e.stopImmediatePropagation();
      }
    }, true);

  } catch (err) {}
})();
