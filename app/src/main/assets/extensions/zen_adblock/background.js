(function () {
  "use strict";

  const blockedDomains = new Set([
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "adservice.google.com",
    "pagead2.googlesyndication.com",
    "tpc.googlesyndication.com",
    "ads.youtube.com",
    "adnxs.com",
    "rubiconproject.com",
    "pubmatic.com",
    "openx.net",
    "criteo.com",
    "criteo.net",
    "taboola.com",
    "outbrain.com",
    "popads.net",
    "popcash.net",
    "propellerads.com",
    "propellerclick.com",
    "exoclick.com",
    "exosrv.com",
    "realsrv.com",
    "juicyads.com",
    "trafficjunky.com",
    "trafficjunky.net",
    "adsterra.com",
    "highperformanceformat.com",
    "monetag.com",
    "hilltopads.net",
    "onclickads.net",
    "mgid.com",
    "amazon-adsystem.com",
    "moatads.com",
    "doubleverify.com",
    "scorecardresearch.com",
    "quantserve.com",
    "adform.net",
    "smartadserver.com",
    "adsafeprotected.com",
    "aniview.com",
    "connatix.com",
    "revcontent.com",
    "zergnet.com",
    "33across.com",
    "casalemedia.com",
    "indexww.com",
    "bidswitch.net",
    "sharethrough.com",
    "adcolony.com",
    "applovin.com",
    "vungle.com",
    "unityads.unity3d.com",
    "inmobi.com",
    "chartboost.com",
    "advertising.com",
    "serving-sys.com",
    "media.net",
    "adroll.com",
    "buysellads.com",
    "carbonads.com",
    "adcash.com",
    "bidvertiser.com"
  ]);

  const SAFE_ALLOW_HOSTS = [
    "fonts.googleapis.com",
    "fonts.gstatic.com",
    "ajax.googleapis.com",
    "www.recaptcha.net",
    "www.gstatic.com",
    "hcaptcha.com",
    "js.stripe.com",
    "www.paypal.com",
    "accounts.google.com",
    "challenges.cloudflare.com"
  ];

  const SUBRESOURCE_AD_REGEX = /(^|\/|\.)(pagead|adsbygoogle|gpt\.js|pubads_|amp4ads|prebid|hb_bidder=|vast\.xml|vmap\.xml|ima3\.js|imasdk|show_ads\.js|adserver|adservice|popunder|popads|popcash|propellerads|exoclick|adsterra|monetag|onclickads|juicyads|trafficjunky|mgid|outbrain|taboola|criteo|pubmatic|rubiconproject|openx\.net|adnxs|amazon-adsystem|moatads|doubleverify|scorecardresearch|quantserve|adform\.net|smartadserver|aniview|connatix|revcontent|zergnet|adsafeprotected|ad-banner|ad_banner|banner_ad|ads\/banner)/i;

  const POPUP_AD_URL_REGEX = /(popads|popcash|propellerads|propellerclick|onclickads|exoclick|exosrv|realsrv|juicyads|trafficjunky|adsterra|monetag|hilltopads|popunder|ad-maven|adcash|highperformanceformat|clck\.ru|clickid=|zoneid=|bannerid=)/i;

  // Load full ~46k EasyList/EasyPrivacy/uBO domain blocklist bundled with the extension.
  fetch(browser.runtime.getURL("domains.txt"))
    .then(function (resp) {
      return resp.text();
    })
    .then(function (text) {
      const lines = text.split(/\r?\n/);
      for (let i = 0; i < lines.length; i++) {
        const domain = lines[i].trim().toLowerCase();
        if (domain) {
          blockedDomains.add(domain);
        }
      }
    })
    .catch(function () {});

  function isSafeHost(host) {
    for (let i = 0; i < SAFE_ALLOW_HOSTS.length; i++) {
      const safe = SAFE_ALLOW_HOSTS[i];
      if (host === safe || host.endsWith("." + safe)) return true;
    }
    return false;
  }

  function isBlockedHost(host) {
    if (!host || isSafeHost(host)) return false;
    let h = host;
    while (true) {
      if (blockedDomains.has(h)) return true;
      const dot = h.indexOf(".");
      if (dot < 0 || h.indexOf(".", dot + 1) < 0) return false;
      h = h.substring(dot + 1);
    }
  }

  function extractHost(url) {
    try {
      return new URL(url).hostname.toLowerCase();
    } catch (e) {
      return "";
    }
  }

  function isYouTubeAdRequest(host, lowerUrl) {
    if (host === "youtube.com" || host.endsWith(".youtube.com") || host.endsWith(".youtube-nocookie.com")) {
      return (
        lowerUrl.includes("/api/stats/ads") ||
        lowerUrl.includes("/pagead/") ||
        lowerUrl.includes("/ptracking") ||
        lowerUrl.includes("/get_midroll_info") ||
        lowerUrl.includes("/pcs/activeview") ||
        lowerUrl.includes("/youtubei/v1/player/ad_break") ||
        lowerUrl.includes("&adformat=") ||
        lowerUrl.includes("?adformat=")
      );
    }
    if (host === "googlevideo.com" || host.endsWith(".googlevideo.com")) {
      return (
        lowerUrl.includes("&oad=") ||
        lowerUrl.includes("?oad=") ||
        lowerUrl.includes("adformat=")
      );
    }
    return false;
  }

  browser.webRequest.onBeforeRequest.addListener(
    function (details) {
      const url = details.url;
      if (!url) return {};
      const lower = url.toLowerCase();
      if (!lower.startsWith("http://") && !lower.startsWith("https://")) return {};

      const type = details.type || "";
      if (type === "ping" || type === "csp_report") {
        return { cancel: true };
      }

      const host = extractHost(url);
      if (!host || isSafeHost(host)) return {};

      if (isYouTubeAdRequest(host, lower)) {
        return { cancel: true };
      }

      if (type === "main_frame") {
        if (isBlockedHost(host) || POPUP_AD_URL_REGEX.test(lower)) {
          return { cancel: true };
        }
        return {};
      }

      if (isBlockedHost(host) || SUBRESOURCE_AD_REGEX.test(lower)) {
        return { cancel: true };
      }

      return {};
    },
    { urls: ["<all_urls>"] },
    ["blocking"]
  );

  // Close pop-up tabs immediately if any script triggers a pop-up tab to an ad or blank URL.
  if (browser.tabs && browser.tabs.onCreated) {
    browser.tabs.onCreated.addListener(function (tab) {
      if (!tab || tab.openerTabId == null || tab.id == null) return;
      const targetUrl = (tab.pendingUrl || tab.url || "").toLowerCase();
      if (!targetUrl || targetUrl === "about:blank") return;
      const host = extractHost(targetUrl);
      if (isBlockedHost(host) || POPUP_AD_URL_REGEX.test(targetUrl)) {
        try {
          browser.tabs.remove(tab.id);
        } catch (e) {}
      }
    });
  }
})();
