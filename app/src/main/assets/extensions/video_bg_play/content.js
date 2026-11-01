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
      if (e) e.stopImmediatePropagation();
    }, true);

  } catch (err) {}
})();
