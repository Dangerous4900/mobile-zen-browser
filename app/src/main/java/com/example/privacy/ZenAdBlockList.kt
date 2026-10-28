package com.example.privacy

import android.content.Context
import org.json.JSONObject
import kotlin.concurrent.thread

/**
 * Large ad/tracker/popup blocklist (~46k domains) plus generic cosmetic hiding rules, generated
 * from the public EasyList / EasyPrivacy / uBlock Origin filter lists (see assets/adblock/).
 *
 * Host lookups are O(labels) hash probes, which is also far cheaper per request than the old
 * hundreds of substring checks.
 */
object ZenAdBlockList {
    @Volatile private var domains: HashSet<String>? = null
    @Volatile private var cosmeticScript: String = ""

    fun load(context: Context) {
        if (domains != null) return
        val app = context.applicationContext
        thread(name = "zen-adblock-load", isDaemon = true) {
            try {
                val set = HashSet<String>(60_000)
                app.assets.open("adblock/domains.txt").bufferedReader().useLines { lines ->
                    lines.forEach { l -> if (l.isNotBlank()) set.add(l.trim()) }
                }
                val css = app.assets.open("adblock/cosmetic.css").bufferedReader().use { it.readText() }
                cosmeticScript = buildCosmeticScript(css)
                domains = set
            } catch (_: Exception) {
                // Falls back to the built-in short lists in ZenPrivacyEngine.
            }
        }
    }

    /** True when [host] or any parent domain is on the list (e.g. ads.x.example.com -> example.com). */
    fun isBlockedHost(host: String?): Boolean {
        val set = domains ?: return false
        if (host.isNullOrEmpty()) return false
        var h = host.lowercase()
        while (true) {
            if (set.contains(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0 || h.indexOf('.', dot + 1) < 0) return false
            h = h.substring(dot + 1)
        }
    }

    fun cosmeticScript(): String = cosmeticScript

    private fun buildCosmeticScript(css: String): String = """
        (function() {
            try {
                if (window.__zenCosmeticApplied) return;
                window.__zenCosmeticApplied = true;
                var css = ${JSONObject.quote(css)};
                if (typeof CSSStyleSheet !== 'undefined' && document.adoptedStyleSheets !== undefined) {
                    var sh = new CSSStyleSheet();
                    sh.replaceSync(css);
                    document.adoptedStyleSheets = document.adoptedStyleSheets.concat([sh]);
                } else {
                    var st = document.createElement('style');
                    st.textContent = css;
                    (document.head || document.documentElement).appendChild(st);
                }
            } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Runtime cleanup that static lists cannot do: unsolicited overlays/pop-ups, popunder anchor
     * tricks, alert/confirm spam without a user gesture, and ad-sized third-party iframes.
     */
    val ANNOYANCE_SCRIPT: String = """
        (function() {
            if (window.__zenAnnoyanceHooked) return;
            window.__zenAnnoyanceHooked = true;
            var lastGesture = 0;
            ['click', 'touchstart', 'keydown', 'pointerdown'].forEach(function(ev) {
                window.addEventListener(ev, function() { lastGesture = Date.now(); }, true);
            });
            function hasGesture() { return Date.now() - lastGesture < 1500; }

            // window.open never opens a window (the app also refuses onCreateWindow)
            try { window.open = function() { return null; }; } catch (e) {}

            // Popunder trick: script builds a detached <a target=_blank> and clicks it
            try {
                var origClick = HTMLAnchorElement.prototype.click;
                HTMLAnchorElement.prototype.click = function() {
                    if (!this.isConnected && !hasGesture()) return;
                    return origClick.apply(this, arguments);
                };
            } catch (e) {}

            // alert/confirm/prompt spam with no user interaction
            try {
                var oa = window.alert, oc = window.confirm, op = window.prompt;
                window.alert = function() { if (hasGesture()) return oa.apply(this, arguments); };
                window.confirm = function() { return hasGesture() ? oc.apply(this, arguments) : false; };
                window.prompt = function() { return hasGesture() ? op.apply(this, arguments) : null; };
            } catch (e) {}

            var AD_WORDS = /(^|[-_ ])(ad|ads|adv|advert|advertisement|sponsor|sponsored|promo|popup|pop-up|interstitial|takeover|modal-ad)([-_ ]|$)/i;
            var SAFE_HOSTS = /(youtube|youtu\.be|vimeo|twitch|spotify|soundcloud|google\.com\/maps|recaptcha|hcaptcha|stripe|paypal|disqus|facebook\.com\/plugins)/i;
            var AD_SIZES = ['300x250', '728x90', '320x50', '160x600', '300x600', '970x250', '970x90', '336x280', '320x100', '468x60', '250x250', '120x600', '300x50'];

            function isBigFixed(el) {
                var cs = getComputedStyle(el);
                if (cs.position !== 'fixed' && cs.position !== 'sticky') return false;
                var r = el.getBoundingClientRect();
                var vw = window.innerWidth, vh = window.innerHeight;
                var area = (Math.max(0, Math.min(r.right, vw) - Math.max(r.left, 0))) * (Math.max(0, Math.min(r.bottom, vh) - Math.max(r.top, 0)));
                return area > vw * vh * 0.5 && (parseInt(cs.zIndex, 10) || 0) >= 100;
            }
            function nameOf(el) { return (el.id || '') + ' ' + (typeof el.className === 'string' ? el.className : ''); }

            function sweep() {
                if (hasGesture()) return; // things the user just opened stay
                var removed = false;
                var cands = document.body ? document.body.querySelectorAll('div, section, aside, iframe, a') : [];
                for (var i = 0; i < cands.length; i++) {
                    var el = cands[i];
                    try {
                        if (el.tagName === 'IFRAME') {
                            var src = el.src || '';
                            var w = Math.round(el.getBoundingClientRect().width), h = Math.round(el.getBoundingClientRect().height);
                            var cross = false;
                            try { cross = !!src && new URL(src, location.href).host !== location.host; } catch (x) {}
                            if (cross && !SAFE_HOSTS.test(src) && AD_SIZES.indexOf(w + 'x' + h) !== -1) {
                                el.style.setProperty('display', 'none', 'important'); removed = true;
                            }
                            continue;
                        }
                        if (!isBigFixed(el)) continue;
                        if (el.querySelector('video, input, textarea, select, form, canvas')) continue;
                        if ((el.innerText || '').length > 600) continue;
                        var looksAd = AD_WORDS.test(nameOf(el)) || el.querySelector('iframe[src], ins.adsbygoogle');
                        if (looksAd) { el.style.setProperty('display', 'none', 'important'); removed = true; }
                    } catch (e) {}
                }
                if (removed && document.body) {
                    var bs = getComputedStyle(document.body);
                    if (bs.overflow === 'hidden') document.body.style.setProperty('overflow', 'auto', 'important');
                    var hs = getComputedStyle(document.documentElement);
                    if (hs.overflow === 'hidden') document.documentElement.style.setProperty('overflow', 'auto', 'important');
                }
            }
            var pending = false;
            function schedule() {
                if (pending) return;
                pending = true;
                setTimeout(function() { pending = false; sweep(); }, 700);
            }
            try {
                new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });
            } catch (e) {}
            schedule();
        })();
    """.trimIndent()

    /** YouTube: strip ad payloads from player responses, hide ad UI, skip any ad that slips through. */
    val YOUTUBE_SCRIPT: String = """
        (function() {
            if (!/(^|\.)youtube\.com${'$'}/.test(location.hostname)) return;
            if (window.__zenYtHooked) return;
            window.__zenYtHooked = true;

            function clean(o) {
                if (!o || typeof o !== 'object') return;
                if (o.adPlacements) o.adPlacements = [];
                if (o.playerAds) o.playerAds = [];
                if (o.adSlots) o.adSlots = [];
                if (o.playerResponse) clean(o.playerResponse);
                if (o.response) clean(o.response);
            }
            try {
                var origParse = JSON.parse;
                JSON.parse = function() { var r = origParse.apply(this, arguments); try { clean(r); } catch (e) {} return r; };
            } catch (e) {}
            try {
                var origJson = Response.prototype.json;
                Response.prototype.json = function() { return origJson.apply(this, arguments).then(function(r) { try { clean(r); } catch (e) {} return r; }); };
            } catch (e) {}
            try {
                var v = window.ytInitialPlayerResponse;
                Object.defineProperty(window, 'ytInitialPlayerResponse', {
                    configurable: true,
                    get: function() { return v; },
                    set: function(x) { try { clean(x); } catch (e) {} v = x; }
                });
            } catch (e) {}

            var st = document.createElement('style');
            st.textContent = '#player-ads,#masthead-ad,ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-banner-promo-renderer,' +
                'ytd-promoted-sparkles-web-renderer,ytd-display-ad-renderer,ytd-promoted-video-renderer,.ytp-ad-overlay-container,' +
                '.ytp-ad-text-overlay,ytm-promoted-sparkles-web-renderer,ytm-companion-ad-renderer,ytm-promoted-video-renderer,' +
                'ytd-player-legacy-desktop-watch-ads-renderer,.ytd-companion-slot-renderer,ytd-statement-banner-renderer,' +
                'ytm-statement-banner-renderer,.ad-container,.ytp-paid-content-overlay{display:none!important}';
            (document.head || document.documentElement).appendChild(st);

            var adMuted = false;
            setInterval(function() {
                try {
                    var p = document.querySelector('.html5-video-player');
                    var vid = document.querySelector('video.html5-main-video') || document.querySelector('video');
                    var showing = p && (p.classList.contains('ad-showing') || p.classList.contains('ad-interrupting'));
                    if (showing && vid) {
                        if (!adMuted) { adMuted = true; }
                        vid.muted = true;
                        if (isFinite(vid.duration) && vid.duration > 0) { vid.currentTime = vid.duration; }
                        vid.playbackRate = 16;
                    } else if (adMuted && vid) {
                        adMuted = false;
                        vid.muted = !!window.__zenIsMuted;
                        vid.playbackRate = 1;
                    }
                    var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-container button');
                    if (skip) skip.click();
                    var closeBtn = document.querySelector('.ytp-ad-overlay-close-button');
                    if (closeBtn) closeBtn.click();
                } catch (e) {}
            }, 400);
        })();
    """.trimIndent()
}
