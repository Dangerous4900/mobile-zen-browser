package com.example.privacy

import android.net.Uri
import com.example.data.BlockedTrackerEvent
import com.example.data.CustomSearchEngineEntity
import com.example.data.PrivacyConfig
import com.example.data.SearchEngine

object ZenPrivacyEngine {

    private val TRACKER_DOMAINS = setOf(
        "doubleclick.net",
        "google-analytics.com",
        "googletagmanager.com",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "adservice.google.",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "ade.googlesyndication.com",
        "ads.youtube.com",
        "youtube.com/api/stats/ads",
        "youtube.com/pagead/",
        "youtube.com/ptracking",
        "s0.2mdn.net",
        "2mdn.net",
        "ima3vpaid",
        "imasdk.googleapis.com",
        "connect.facebook.net",
        "facebook.com/tr",
        "pixel.facebook.com",
        "an.facebook.com",
        "analytics.tiktok.com",
        "ads.tiktok.com",
        "ads-twitter.com",
        "static.ads-twitter.com",
        "ads-api.twitter.com",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "c.amazon-adsystem.com",
        "scorecardresearch.com",
        "quantserve.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "taboola.com",
        "trc.taboola.com",
        "cdn.taboola.com",
        "criteo.com",
        "criteo.net",
        "adnxs.com",
        "ib.adnxs.com",
        "rubiconproject.com",
        "fastlane.rubiconproject.com",
        "pubmatic.com",
        "ads.pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "indexww.com",
        "smartadserver.com",
        "advertising.com",
        "adform.net",
        "serving-sys.com",
        "moatads.com",
        "adsafeprotected.com",
        "doubleverify.com",
        "teads.tv",
        "sharethrough.com",
        "triplelift.com",
        "33across.com",
        "sovrn.com",
        "lijit.com",
        "bidswitch.net",
        "RichAudience.com".lowercase(),
        "medianet.com",
        "media.net",
        "contextual.media.net",
        "revcontent.com",
        "mgid.com",
        "adblade.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "onclickads.net",
        "exoclick.com",
        "Syndication.exoclick.com".lowercase(),
        "juicyads.com",
        "trafficjunky.net",
        "adroll.com",
        "zemanta.com",
        " nativo.com".trim(),
        "yieldmo.com",
        "gumgum.com",
        "kargo.com",
        "undertone.com",
        "adcolony.com",
        "unityads.unity3d.com",
        "applovin.com",
        "vungle.com",
        "chartboost.com",
        "inmobi.com",
        "smaato.net",
        "mopub.com",
        "ironsrc.com",
        "hotjar.com",
        "mouseflow.com",
        "fullstory.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "newrelic.com",
        "nr-data.net",
        "clarity.ms",
        "bat.bing.com",
        "ads.linkedin.com",
        "px.ads.linkedin.com",
        "ct.pinterest.com",
        "ads.pinterest.com",
        "sc-static.net/scevent",
        "tr.snapchat.com",
        "ads.yahoo.com",
        "gemini.yahoo.com",
        "yandex.ru/metrika",
        "mc.yandex.ru",
        "an.yandex.ru",
        "braze.com",
        "appsflyer.com",
        "adjust.com",
        "branch.io",
        "cleverpush.com",
        "onesignal.com"
    )

    private val AD_URL_PATTERNS = listOf(
        "/pagead/",
        "/ads/ga-audiences",
        "/adview?",
        "/adserver",
        "/adiframe",
        "/adscript",
        "/ads.js",
        "/ad.js",
        "/adsbygoogle",
        "/show_ads.js",
        "/gpt.js",
        "/pubads_impl",
        "/prebid",
        "/api/stats/ads",
        "&ad_type=",
        "?ad_type=",
        "&adformat=",
        "/get_midroll_",
        "/adservice",
        "/adsystem",
        "partner.googleadservices.com",
        "securepubads.g.doubleclick.net",
        "googleads.g.doubleclick.net",
        "safeframe.googlesyndication.com"
    )

    private val TRACKING_QUERY_PARAMS = setOf(
        "utm_source",
        "utm_medium",
        "utm_campaign",
        "utm_term",
        "utm_content",
        "utm_id",
        "fbclid",
        "gclid",
        "dclid",
        "msclkid",
        "twclid",
        "igshid",
        "mc_cid",
        "mc_eid",
        "_ga",
        "_gl",
        "yclid",
        "vero_id",
        "oly_enc_id",
        "oly_anon_id",
        "rb_clickid",
        "s_kwcid"
    )

    data class SanitizedUrlResult(
        val finalUrl: String,
        val strippedParamsCount: Int,
        val upgradedToHttps: Boolean
    )

    fun resolveSearchEngineLabel(
        engineId: String,
        customEngines: List<CustomSearchEngineEntity>
    ): String {
        SearchEngine.entries.find { it.id.equals(engineId, ignoreCase = true) || it.name.equals(engineId, ignoreCase = true) }
            ?.let { return it.label }
        customEngines.find { it.id == engineId }?.let { return it.name }
        return SearchEngine.DUCKDUCKGO.label
    }

    private fun resolveSearchTemplate(
        engineId: String,
        customEngines: List<CustomSearchEngineEntity>
    ): String {
        SearchEngine.entries.find { it.id.equals(engineId, ignoreCase = true) || it.name.equals(engineId, ignoreCase = true) }
            ?.let { return it.searchUrlTemplate }
        customEngines.find { it.id == engineId }?.let { return it.searchUrlTemplate }
        return SearchEngine.DUCKDUCKGO.searchUrlTemplate
    }

    fun resolveInputToUrl(
        rawInput: String,
        privacyConfig: PrivacyConfig,
        isPrivateContext: Boolean = false,
        customEngines: List<CustomSearchEngineEntity> = emptyList()
    ): SanitizedUrlResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty() || trimmed.equals("zen://home", ignoreCase = true)) {
            return SanitizedUrlResult("zen://home", 0, false)
        }
        if (trimmed.startsWith("zen://", ignoreCase = true)) {
            return SanitizedUrlResult(trimmed.lowercase(), 0, false)
        }

        // Easter Egg: "? : who is the main character"
        val isMainCharacterSearch = trimmed.startsWith("?") &&
            trimmed.contains("main character", ignoreCase = true)
        if (isMainCharacterSearch) {
            return SanitizedUrlResult("zen://search?q=" + Uri.encode(trimmed), 0, false)
        }

        val isLikelyUrl = !trimmed.contains(" ") && (
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.contains(".")
            )

        val candidateUrl = if (isLikelyUrl) {
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
        } else {
            val encoded = Uri.encode(trimmed)
            val activeEngineId = if (isPrivateContext) {
                privacyConfig.privateSearchEngineId
            } else {
                privacyConfig.normalSearchEngineId
            }
            val template = resolveSearchTemplate(activeEngineId, customEngines)
            if (template.contains("%s")) {
                template.replace("%s", encoded)
            } else {
                "$template$encoded"
            }
        }

        return sanitizeUrl(candidateUrl, privacyConfig)
    }

    fun sanitizeUrl(
        url: String,
        privacyConfig: PrivacyConfig
    ): SanitizedUrlResult {
        if (url.startsWith("zen://")) {
            return SanitizedUrlResult(url, 0, false)
        }

        var workingUrl = url
        var upgradedHttps = false

        if (privacyConfig.httpsEverywhere && workingUrl.startsWith("http://", ignoreCase = true)) {
            val host = runCatching { Uri.parse(workingUrl).host ?: "" }.getOrDefault("")
            if (host != "localhost" && !host.startsWith("127.") && !host.startsWith("192.168.")) {
                workingUrl = "https://" + workingUrl.substring(7)
                upgradedHttps = true
            }
        }

        if (!privacyConfig.stripTrackingParams) {
            return SanitizedUrlResult(workingUrl, 0, upgradedHttps)
        }

        return try {
            val uri = Uri.parse(workingUrl)
            val queryNames = uri.queryParameterNames
            if (queryNames.isEmpty()) {
                SanitizedUrlResult(workingUrl, 0, upgradedHttps)
            } else {
                var strippedCount = 0
                val builder = uri.buildUpon().clearQuery()
                for (name in queryNames) {
                    if (TRACKING_QUERY_PARAMS.contains(name.lowercase())) {
                        strippedCount++
                    } else {
                        for (value in uri.getQueryParameters(name)) {
                            builder.appendQueryParameter(name, value)
                        }
                    }
                }
                SanitizedUrlResult(builder.build().toString(), strippedCount, upgradedHttps)
            }
        } catch (_: Exception) {
            SanitizedUrlResult(workingUrl, 0, upgradedHttps)
        }
    }

    fun matchTrackerRequest(requestUrl: String): BlockedTrackerEvent? {
        val lower = requestUrl.lowercase()
        for (tracker in TRACKER_DOMAINS) {
            val cleanTracker = tracker.trim()
            if (lower.contains(cleanTracker)) {
                val category = when {
                    cleanTracker.contains("ads") || cleanTracker.contains("doubleclick") ||
                        cleanTracker.contains("syndication") || cleanTracker.contains("criteo") ||
                        cleanTracker.contains("taboola") || cleanTracker.contains("outbrain") ||
                        cleanTracker.contains("adnxs") || cleanTracker.contains("rubicon") ||
                        cleanTracker.contains("pubmatic") || cleanTracker.contains("openx") ||
                        cleanTracker.contains("popads") || cleanTracker.contains("exoclick") ||
                        cleanTracker.contains("pagead") -> "Ad Network"
                    cleanTracker.contains("facebook") || cleanTracker.contains("tiktok") ||
                        cleanTracker.contains("twitter") || cleanTracker.contains("linkedin") ||
                        cleanTracker.contains("pinterest") || cleanTracker.contains("snapchat") -> "Social Pixel"
                    cleanTracker.contains("hotjar") || cleanTracker.contains("fullstory") ||
                        cleanTracker.contains("clarity") || cleanTracker.contains("mouseflow") -> "Session Replay"
                    else -> "Analytics Tracker"
                }
                return BlockedTrackerEvent(domain = cleanTracker, category = category)
            }
        }
        for (pattern in AD_URL_PATTERNS) {
            if (lower.contains(pattern)) {
                val host = extractHostDisplay(requestUrl).ifBlank { "ad-server" }
                return BlockedTrackerEvent(domain = host, category = "Ad Network")
            }
        }
        return null
    }

    fun isAdOrTrackerUrl(requestUrl: String): Boolean {
        if (matchTrackerRequest(requestUrl) != null) return true
        val lower = requestUrl.lowercase()
        return lower.contains("doubleclick") ||
            lower.contains("googlesyndication") ||
            lower.contains("googleadservices") ||
            lower.contains("adservice.google") ||
            lower.contains("/pagead/") ||
            lower.contains("adnxs") ||
            lower.contains("rubiconproject") ||
            lower.contains("criteo") ||
            lower.contains("taboola") ||
            lower.contains("outbrain") ||
            lower.contains("pubmatic") ||
            lower.contains("openx.net") ||
            lower.contains("smartadserver") ||
            lower.contains("advertising.com") ||
            lower.contains("adform.net") ||
            lower.contains("serving-sys") ||
            lower.contains("moatads") ||
            lower.contains("doubleverify") ||
            lower.contains("scorecardresearch") ||
            lower.contains("quantserve") ||
            lower.contains("popads") ||
            lower.contains("popcash") ||
            lower.contains("propellerads") ||
            lower.contains("exoclick") ||
            lower.contains("juicyads") ||
            lower.contains("trafficjunky") ||
            lower.contains("amazon-adsystem") ||
            lower.contains("adcolony") ||
            lower.contains("applovin") ||
            lower.contains("vungle") ||
            lower.contains("chartboost") ||
            lower.contains("inmobi") ||
            lower.contains("unityads") ||
            lower.contains("ironsource") ||
            lower.contains("adservice") ||
            lower.contains("adsystem") ||
            lower.contains("/show_ads.js") ||
            lower.contains("/adsbygoogle") ||
            lower.contains("/gpt.js") ||
            lower.contains("&adformat=") ||
            lower.contains("youtube.com/api/stats/ads") ||
            lower.contains("youtube.com/pagead")
    }

    fun buildFingerprintProtectionScript(
        fingerprintProtection: Boolean,
        readerModeActive: Boolean,
        customCssPayload: String,
        customJsPayload: String = ""
    ): String {
        val sb = StringBuilder()
        sb.append("(function() { try {")
        if (fingerprintProtection) {
            sb.append(
                """
                Object.defineProperty(navigator, 'globalPrivacyControl', { value: true, configurable: false });
                Object.defineProperty(navigator, 'doNotTrack', { value: '1', configurable: false });
                Object.defineProperty(navigator, 'hardwareConcurrency', { value: 4, configurable: false });
                Object.defineProperty(navigator, 'deviceMemory', { value: 8, configurable: false });
                """.trimIndent()
            )
        }

        // Firefox WebExtension (`browser.*`, `chrome.*`) & Greasemonkey (`GM_*`) Runtime Polyfill
        sb.append(
            """
            if (!window.__zenWebExtApiReady) {
                window.__zenWebExtApiReady = true;
                var zenStorageArea = {
                    get: function(keys, cb) {
                        return new Promise(function(resolve) {
                            var out = {};
                            try {
                                if (typeof keys === 'string') {
                                    var v = localStorage.getItem('__zen_ext_' + keys);
                                    if (v !== null) out[keys] = JSON.parse(v);
                                } else if (Array.isArray(keys)) {
                                    keys.forEach(function(k) {
                                        var v = localStorage.getItem('__zen_ext_' + k);
                                        if (v !== null) out[k] = JSON.parse(v);
                                    });
                                } else if (keys && typeof keys === 'object') {
                                    Object.keys(keys).forEach(function(k) {
                                        var v = localStorage.getItem('__zen_ext_' + k);
                                        out[k] = v !== null ? JSON.parse(v) : keys[k];
                                    });
                                }
                            } catch(e) {}
                            if (typeof cb === 'function') cb(out);
                            resolve(out);
                        });
                    },
                    set: function(items, cb) {
                        return new Promise(function(resolve) {
                            try {
                                if (items && typeof items === 'object') {
                                    Object.keys(items).forEach(function(k) {
                                        localStorage.setItem('__zen_ext_' + k, JSON.stringify(items[k]));
                                    });
                                }
                            } catch(e) {}
                            if (typeof cb === 'function') cb();
                            resolve();
                        });
                    },
                    remove: function(keys, cb) {
                        return new Promise(function(resolve) {
                            try {
                                var arr = Array.isArray(keys) ? keys : [keys];
                                arr.forEach(function(k) { localStorage.removeItem('__zen_ext_' + k); });
                            } catch(e) {}
                            if (typeof cb === 'function') cb();
                            resolve();
                        });
                    }
                };
                var zenBrowserPolyfill = {
                    runtime: {
                        id: 'zen-mobile-webextension@zen.browser',
                        getManifest: function() { return { name: 'Zen Firefox Extension Engine', manifest_version: 3, version: '1.0' }; },
                        getURL: function(path) { return path || ''; },
                        sendMessage: function(msg, cb) {
                            if (typeof cb === 'function') cb({ ok: true });
                            return Promise.resolve({ ok: true });
                        },
                        onMessage: { addListener: function() {}, removeListener: function() {} }
                    },
                    storage: {
                        local: zenStorageArea,
                        sync: zenStorageArea
                    },
                    tabs: {
                        query: function(q, cb) {
                            var res = [{ id: 1, active: true, url: location.href, title: document.title }];
                            if (typeof cb === 'function') cb(res);
                            return Promise.resolve(res);
                        }
                    }
                };
                if (typeof window.browser === 'undefined') { window.browser = zenBrowserPolyfill; }
                if (typeof window.chrome === 'undefined' || !window.chrome.storage) { window.chrome = zenBrowserPolyfill; }
                window.GM_addStyle = function(cssText) {
                    try {
                        var s = document.createElement('style');
                        s.textContent = cssText;
                        (document.head || document.documentElement).appendChild(s);
                        return s;
                    } catch(e) { return null; }
                };
                window.GM_setValue = function(k, v) { try { localStorage.setItem('__zen_gm_' + k, JSON.stringify(v)); } catch(e) {} };
                window.GM_getValue = function(k, defVal) {
                    try {
                        var raw = localStorage.getItem('__zen_gm_' + k);
                        return raw !== null ? JSON.parse(raw) : defVal;
                    } catch(e) { return defVal; }
                };
            }
            """.trimIndent()
        )

        val combinedCss = buildString {
            if (customCssPayload.isNotBlank()) {
                append(customCssPayload)
                append(" ")
            }
            if (readerModeActive) {
                append(
                    """
                    header, nav, footer, aside, .sidebar, .ads, .ad-banner, .social-share, .comments { display: none !important; }
                    body {
                        max-width: 680px !important;
                        margin: 0 auto !important;
                        padding: 28px 20px 80px 20px !important;
                        background-color: #121217 !important;
                        color: #ECECF4 !important;
                        font-family: Georgia, 'Times New Roman', serif !important;
                        font-size: 19px !important;
                        line-height: 1.78 !important;
                    }
                    h1, h2, h3 {
                        color: #FFFFFF !important;
                        font-family: sans-serif !important;
                        line-height: 1.3 !important;
                    }
                    a { color: #D4D4D8 !important; text-decoration: underline !important; }
                    img, video { max-width: 100% !important; height: auto !important; border-radius: 8px !important; }
                    """.trimIndent()
                )
            }
        }

        if (combinedCss.isNotBlank()) {
            val jsonCss = org.json.JSONObject.quote(combinedCss)
            sb.append(
                """
                function __zenApplyCss() {
                    var parent = document.head || document.documentElement || document.body;
                    if (!parent) return;
                    var existing = document.getElementById('zen-browser-injected-style');
                    if (!existing) {
                        var style = document.createElement('style');
                        style.id = 'zen-browser-injected-style';
                        style.textContent = $jsonCss;
                        parent.appendChild(style);
                    } else {
                        existing.textContent = $jsonCss;
                    }
                }
                __zenApplyCss();
                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', __zenApplyCss);
                }
                """.trimIndent()
            )
        } else {
            sb.append(
                """
                var existing = document.getElementById('zen-browser-injected-style');
                if (existing) { existing.remove(); }
                """.trimIndent()
            )
        }

        if (customJsPayload.isNotBlank()) {
            sb.append(" try { ")
            sb.append(customJsPayload)
            sb.append(" } catch(extErr) {} ")
        }

        sb.append("} catch(e) {} })();")
        return sb.toString()
    }

    fun extractHostDisplay(url: String): String {
        if (url.startsWith("zen://search")) return "Google Search"
        if (url.startsWith("zen://")) return "New Tab"
        return try {
            val host = Uri.parse(url).host ?: return url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url
        }
    }
}
