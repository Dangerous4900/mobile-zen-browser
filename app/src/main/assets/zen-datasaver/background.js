// Cancels image requests before they hit the network. Installed only while Data Saver is on.
browser.webRequest.onBeforeRequest.addListener(
  function () { return { cancel: true }; },
  { urls: ["<all_urls>"], types: ["image", "imageset"] },
  ["blocking"]
);
