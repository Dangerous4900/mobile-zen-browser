package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PrivacyConfig
import com.example.privacy.ZenPrivacyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zen Browser", appName)
    }

    @Test
    fun `zen privacy engine strips tracking params and upgrades https`() {
        val config = PrivacyConfig(
            httpsEverywhere = true,
            stripTrackingParams = true
        )
        val rawUrl = "http://example.com/article?id=42&utm_source=newsletter&fbclid=xyz987"
        val result = ZenPrivacyEngine.sanitizeUrl(rawUrl, config)

        assertTrue(result.upgradedToHttps)
        assertEquals(2, result.strippedParamsCount)
        assertEquals("https://example.com/article?id=42", result.finalUrl)
    }

    @Test
    fun `zen privacy engine intercepts known tracker domains`() {
        val blocked = ZenPrivacyEngine.matchTrackerRequest("https://www.google-analytics.com/collect?v=1")
        assertNotNull(blocked)
        assertEquals("google-analytics.com", blocked?.domain)

        val allowed = ZenPrivacyEngine.matchTrackerRequest("https://en.m.wikipedia.org/wiki/Zen")
        assertNull(allowed)
    }

    @Test
    fun `sidebar config defaults new tab position to bottom`() {
        val config = com.example.data.SidebarConfig()
        assertEquals(com.example.data.NewTabPosition.BOTTOM, config.newTabPosition)
    }

    @Test
    fun `history nav entry formats title and steps back correctly`() {
        val entry = com.example.data.HistoryNavEntry(
            title = "Example Domain",
            url = "https://example.com",
            stepsBack = 2
        )
        assertEquals("Example Domain", entry.title)
        assertEquals("https://example.com", entry.url)
        assertEquals(2, entry.stepsBack)
    }

    @Test
    fun `sidebar config enables archive box and removes legacy library and download manager tools`() {
        val config = com.example.data.SidebarConfig()
        val toolIds = com.example.data.SidebarToolItem.entries.map { it.id }
        assertTrue(toolIds.contains("archive_box"))
        assertTrue(!toolIds.contains("library"))
        assertTrue(!toolIds.contains("download_manager"))
        assertTrue(config.enabledSidebarTools.contains(com.example.data.SidebarToolItem.ARCHIVE_BOX))
    }

    @Test
    fun `sidebar box tabs contain offline downloads bookmarks spaces and history`() {
        val boxTabNames = com.example.data.SidebarBoxTab.entries.map { it.name }
        assertEquals(
            listOf("OFFLINE", "DOWNLOADS", "BOOKMARKS", "SPACES", "HISTORY"),
            boxTabNames
        )
    }

    @Test
    fun `zen adblock loads extension assets and blocks ad domains and popup redirects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.privacy.ZenAdBlockList.load(context)
        assertTrue(com.example.privacy.ZenAdBlockList.isBlockedHost("pagead2.googlesyndication.com"))
        assertTrue(com.example.privacy.ZenAdBlockList.cosmeticScript().isNotBlank())
        assertTrue(ZenPrivacyEngine.isAdOrPopupRedirect("https://example.org/out?zoneid=12345", userInitiated = false))
        assertTrue(ZenPrivacyEngine.isLikelyPopupRedirectQuery("https://track.site/go?clickid=abc"))
        assertTrue(context.assets.list("extensions/zen_adblock")?.toSet()?.containsAll(
            listOf("manifest.json", "background.js", "content.js", "page_shield.js", "cosmetic.css", "domains.txt")
        ) == true)
    }

    @Test
    fun `sidebar tool item excludes compact mode button`() {
        val toolIds = com.example.data.SidebarToolItem.entries.map { it.id }
        assertTrue(!toolIds.contains("compact_mode"))
        assertTrue(com.example.data.SidebarConfig().enabledSidebarTools.none { it.id == "compact_mode" })
    }

    @Test
    fun `zen downloader supports pause resume and cancel actions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val blockingStream = object : java.io.InputStream() {
            @Volatile
            private var closed = false
            override fun read(): Int {
                if (closed) throw java.io.IOException("closed")
                Thread.sleep(25)
                if (closed) throw java.io.IOException("closed")
                return 0x41
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (closed) throw java.io.IOException("closed")
                Thread.sleep(25)
                if (closed) throw java.io.IOException("closed")
                b[off] = 0x41
                return 1
            }
            override fun close() {
                closed = true
            }
        }
        val states = java.util.Collections.synchronizedList(mutableListOf<String>())
        val latch = java.util.concurrent.CountDownLatch(1)
        val id = "dl_test_ctrl"
        com.example.downloads.ZenDownloader.save(
            context = context,
            fileName = "test.bin",
            mimeType = "application/octet-stream",
            input = blockingStream,
            downloadId = id,
            url = "https://example.com/test.bin",
            totalBytes = 1024L,
            onDone = { _, _ -> latch.countDown() },
            onStateChange = { state, _, _, _ -> states.add(state) }
        )

        com.example.downloads.ZenDownloader.pause(context, id)
        assertTrue(com.example.downloads.ZenDownloader.isPaused(id))
        com.example.downloads.ZenDownloader.resume(context, id)
        assertTrue(!com.example.downloads.ZenDownloader.isPaused(id))
        com.example.downloads.ZenDownloader.cancel(context, id)
        assertTrue(com.example.downloads.ZenDownloader.wasCancelled(id))
        assertTrue(latch.await(10, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals(listOf("paused", "downloading", "cancelled"), states)
    }

    @Test
    fun `navigation back edge detection opens sidebar on left edge and goes back on right edge`() {
        val leftSwipeRightAction = com.example.ui.resolveNavigationBackAction(
            swipeEdge = androidx.activity.BackEventCompat.EDGE_LEFT,
            touchX = 12f,
            viewportWidthPx = 1080,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
            canGoBackInPage = true
        )
        assertEquals(com.example.ui.NavigationBackAction.OPEN_SIDEBAR, leftSwipeRightAction)

        val rightSwipeLeftAction = com.example.ui.resolveNavigationBackAction(
            swipeEdge = androidx.activity.BackEventCompat.EDGE_RIGHT,
            touchX = 1068f,
            viewportWidthPx = 1080,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
            canGoBackInPage = true
        )
        assertEquals(com.example.ui.NavigationBackAction.GO_BACK_PAGE, rightSwipeLeftAction)

        val rightSwipeAtFirstPage = com.example.ui.resolveNavigationBackAction(
            swipeEdge = androidx.activity.BackEventCompat.EDGE_RIGHT,
            touchX = 1068f,
            viewportWidthPx = 1080,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
            canGoBackInPage = false
        )
        assertEquals(com.example.ui.NavigationBackAction.NO_OP_AT_FIRST_PAGE, rightSwipeAtFirstPage)

        val fallbackTouchLeft = com.example.ui.resolveNavigationBackAction(
            swipeEdge = com.example.ui.BACK_EDGE_NONE,
            touchX = 16f,
            viewportWidthPx = 1080,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
            canGoBackInPage = true
        )
        assertEquals(com.example.ui.NavigationBackAction.OPEN_SIDEBAR, fallbackTouchLeft)

        val fallbackCancelledRight = com.example.ui.resolveNavigationBackAction(
            swipeEdge = com.example.ui.BACK_EDGE_NONE,
            touchX = null,
            viewportWidthPx = 1080,
            recentCancelledEdge = androidx.activity.BackEventCompat.EDGE_RIGHT,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
            canGoBackInPage = true
        )
        assertEquals(com.example.ui.NavigationBackAction.GO_BACK_PAGE, fallbackCancelledRight)

        val swappedModeRightEdge = com.example.ui.resolveNavigationBackAction(
            swipeEdge = androidx.activity.BackEventCompat.EDGE_RIGHT,
            touchX = 1068f,
            viewportWidthPx = 1080,
            backSwipeMode = com.example.data.BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK,
            horizontalSwipeMode = com.example.data.HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK,
            canGoBackInPage = true
        )
        assertEquals(com.example.ui.NavigationBackAction.OPEN_SIDEBAR, swappedModeRightEdge)
    }

    @Test
    fun `colorful gradients are included by default and pip is enabled by default`() {
        val colorfulPresets = com.example.data.StartPageGradientPreset.entries.filter { it.isColorful }
        assertTrue(colorfulPresets.size >= 8)
        assertEquals(
            com.example.data.StartPageGradientPreset.CYBERWAVE_NEON,
            com.example.data.StartPageGradientPreset.fromId("cyberwave_neon")
        )
        assertEquals(
            com.example.data.StartPageGradientPreset.PITCH_BLACK_AMOLED,
            com.example.data.StartPageGradientPreset.fromId("unknown_id")
        )
        val cfg = com.example.data.SidebarConfig()
        assertTrue(cfg.playInWindowPipEnabled)
        assertTrue(cfg.backgroundPlayEnabled)
    }

    @Test
    fun `zen scroll store normalizes urls saves restores and clears`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.gecko.ZenScrollStore.clearAll(context)

        assertNull(com.example.gecko.ZenScrollStore.normalizeUrl("zen://home"))
        assertNull(com.example.gecko.ZenScrollStore.normalizeUrl("about:blank"))
        assertEquals(
            "https://example.com/articles/1",
            com.example.gecko.ZenScrollStore.normalizeUrl("https://example.com/articles/1/#comments")
        )

        com.example.gecko.ZenScrollStore.saveScrollY(context, "https://example.com/articles/1/", 840)
        assertEquals(
            840,
            com.example.gecko.ZenScrollStore.getScrollY(context, "https://example.com/articles/1#top")
        )

        com.example.gecko.ZenScrollStore.clearAll(context)
        assertEquals(0, com.example.gecko.ZenScrollStore.getScrollY(context, "https://example.com/articles/1"))
    }

    @Test
    fun `zen extension compatibility resolves mobile firefox amo handles large xpi and rejects chrome crx`() {
        val mobileAmo = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://addons.mozilla.org/en-US/android/addon/ublock-origin/"
        )
        assertTrue(mobileAmo is com.example.extensions.ExtensionInstallTarget.AmoAddon)
        assertEquals("ublock-origin", (mobileAmo as com.example.extensions.ExtensionInstallTarget.AmoAddon).slug)

        val directAmoFile = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://addons.mozilla.org/firefox/downloads/file/4391000/ublock_origin-1.61.2.xpi"
        )
        assertTrue(directAmoFile is com.example.extensions.ExtensionInstallTarget.DirectXpiUrl)

        val cws = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://chromewebstore.google.com/detail/dark-reader/eimadpbcbfnmbkopoojfekhnkhdbieeh"
        )
        assertTrue(cws is com.example.extensions.ExtensionInstallTarget.Unsupported)

        val crxUrl = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://example.com/extension/helper.crx"
        )
        assertTrue(crxUrl is com.example.extensions.ExtensionInstallTarget.Unsupported)

        val userscript = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://greasyfork.org/scripts/1234-test/code/test.user.js"
        )
        assertTrue(userscript is com.example.extensions.ExtensionInstallTarget.UserscriptUrl)

        // Build synthetic Firefox .xpi ZIP with > 2,200 entries (like real uBlock Origin / Dark Reader packages)
        val zipOut = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(zipOut).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write(
                """
                {
                  "manifest_version": 2,
                  "name": "__MSG_extName__",
                  "version": "1.61.2",
                  "description": "__MSG_extDesc__",
                  "browser_specific_settings": {
                    "gecko": { "id": "ublock-test@example.org" }
                  },
                  "content_scripts": [
                    {
                      "matches": ["https://*/*"],
                      "css": ["styles/site.css"],
                      "js": ["scripts/content.js"],
                      "run_at": "document_end"
                    }
                  ],
                  "background": { "scripts": ["bg.js"] }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("_locales/en/messages.json"))
            zos.write(
                """
                {
                  "extName": { "message": "uBlock Origin Mobile" },
                  "extDesc": { "message": "Efficient wide-spectrum blocker" }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("styles/site.css"))
            zos.write("body { scroll-behavior: smooth; }".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("scripts/content.js"))
            zos.write("window.__zenExtLoaded = true;".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            for (i in 1..2200) {
                zos.putNextEntry(java.util.zip.ZipEntry("assets/filters/list_$i.txt"))
                zos.write("filter".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        val parsed = com.example.extensions.ZenExtensionCompatibility.extractPackageMetadataAndScripts(
            fileName = "ublock-origin.xpi",
            rawBytes = zipOut.toByteArray()
        )
        assertTrue(parsed.supportsNativeGecko)
        assertEquals("uBlock Origin Mobile", parsed.entity.name)
        assertEquals("1.61.2", parsed.entity.version)
        assertEquals("document_end", parsed.entity.runAt)
        assertTrue(parsed.entity.cssPayload.contains("scroll-behavior: smooth"))
        assertTrue(parsed.entity.jsPayload.contains("__zenExtLoaded"))
        assertTrue(parsed.warnings.contains("background/service_worker"))
    }

    @Test
    fun `zen page ux built-in extension assets are packaged with native messaging`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val files = context.assets.list("extensions/zen_page_ux")?.toSet().orEmpty()
        assertTrue(files.containsAll(listOf("manifest.json", "background.js", "content.js")))
        val manifest = context.assets.open("extensions/zen_page_ux/manifest.json").bufferedReader().use { it.readText() }
        assertTrue(manifest.contains("nativeMessaging"))
        assertTrue(manifest.contains("nativeMessagingFromContent"))
        val contentJs = context.assets.open("extensions/zen_page_ux/content.js").bufferedReader().use { it.readText() }
        assertTrue(contentJs.contains("setupExtensionStoreBridge"))
        assertTrue(contentJs.contains("installAmoExtension"))
        assertTrue(contentJs.contains("withinPipTransition"))
    }

    @Test
    fun `native firefox xpi without static content scripts is accepted when background or gecko id exists`() {
        val zipOut = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(zipOut).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            zos.write(
                """
                {
                  "manifest_version": 2,
                  "name": "Privacy Guardian",
                  "version": "1.6.2",
                  "description": "Blocks trackers natively in Gecko",
                  "browser_specific_settings": {
                    "gecko": { "id": "privacy-guardian@example.org" }
                  },
                  "permissions": ["webRequest", "webRequestBlocking", "<all_urls>"],
                  "background": { "scripts": ["background.js"] }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            zos.closeEntry()
            zos.putNextEntry(java.util.zip.ZipEntry("background.js"))
            zos.write("console.log('bg');".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val parsed = com.example.extensions.ZenExtensionCompatibility.extractPackageMetadataAndScripts(
            fileName = "privacy-guardian.xpi",
            rawBytes = zipOut.toByteArray(),
            allowNativeBackgroundOnly = true
        )
        assertTrue(parsed.supportsNativeGecko)
        assertEquals("Privacy Guardian", parsed.entity.name)
        assertEquals("1.6.2", parsed.entity.version)
        assertTrue(parsed.warnings.contains("background/service_worker"))
    }

    @Test
    fun `amo slug resolves cleanly without full url`() {
        val bareSlug = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget("ublock-origin")
        assertTrue(bareSlug is com.example.extensions.ExtensionInstallTarget.AmoAddon)
        assertEquals("ublock-origin", (bareSlug as com.example.extensions.ExtensionInstallTarget.AmoAddon).slug)
    }

    @Test
    fun `default sidebar config includes extensions icon and biometric grace period rules`() {
        val defaultCfg = com.example.data.SidebarConfig()
        assertTrue(defaultCfg.enabledTopToolbarTools.contains(com.example.data.TopToolbarToolItem.EXTENSIONS))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.ZenDatabase::class.java
        ).allowMainThreadQueries().build()
        val repo = com.example.data.ZenRepository(db.zenDao(), context)
        val vm = com.example.ui.ZenBrowserViewModel(repo)

        try {
            vm.updateBiometricLockEnabled(true)
            vm.setAppLocked(false)

            // UNTIL_CLOSED must never re-lock during same process session
            vm.updateBiometricGracePeriod(com.example.data.BiometricGracePeriod.UNTIL_CLOSED)
            vm.onAppBackgroundedOrClosed()
            vm.evaluateBiometricLockOnForeground(isInPipNow = false)
            assertTrue(!vm.isAppLockedNow())

            // IMMEDIATE locks immediately on foreground after non-PiP background
            vm.updateBiometricGracePeriod(com.example.data.BiometricGracePeriod.IMMEDIATE)
            vm.onAppBackgroundedOrClosed()
            vm.evaluateBiometricLockOnForeground(isInPipNow = false)
            assertTrue(vm.isAppLockedNow())

            // Unlock and verify PiP does not trigger re-lock
            vm.setAppLocked(false)
            vm.setPictureInPictureMode(true)
            vm.evaluateBiometricLockOnForeground(isInPipNow = true)
            assertTrue(!vm.isAppLockedNow())
        } finally {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            db.close()
        }
    }

    @Test
    fun `pip mode closes sidebar blocks opening it and preserves auto resume unless user paused`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.ZenDatabase::class.java
        ).allowMainThreadQueries().build()
        val repo = com.example.data.ZenRepository(db.zenDao(), context)
        val vm = com.example.ui.ZenBrowserViewModel(repo)

        try {
            vm.setSidebarOpen(true)
            assertTrue(vm.isSidebarOpenNow())

            vm.updateMediaPlaybackState(
                isActive = true,
                isPlaying = true,
                title = "Live Stream",
                domain = "youtube.com",
                tabId = "tab_1"
            )
            assertTrue(vm.shouldAutoResumeMediaOnPip())

            vm.setPictureInPictureMode(true)
            assertTrue(vm.isInPictureInPictureModeNow())
            assertTrue(!vm.isSidebarOpenNow())

            // Attempts to open sidebar during PiP must be rejected
            vm.setSidebarOpen(true)
            assertTrue(!vm.isSidebarOpenNow())
            vm.toggleSidebar()
            assertTrue(!vm.isSidebarOpenNow())

            // Transient pause callback during PiP entry still permits auto-resume
            vm.updateMediaPlaybackState(
                isActive = true,
                isPlaying = false,
                title = "Live Stream",
                domain = "youtube.com",
                tabId = "tab_1"
            )
            assertTrue(vm.shouldAutoResumeMediaOnPip())

            // Intentional user pause disables auto-resume
            vm.noteUserMediaPause()
            assertTrue(!vm.shouldAutoResumeMediaOnPip())

            // Exiting PiP sets transition suppression cooldown so stray edge swipes don't open sidebar
            vm.setPictureInPictureMode(false)
            vm.setSidebarOpen(true)
            assertTrue(!vm.isSidebarOpenNow())
        } finally {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            db.close()
        }
    }

    @Test
    fun `context menu is suppressed when sidebar is open or near screen edges`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.gecko.ZenGeckoManager.init(context)

        // When sidebar is open, context menu is strictly suppressed everywhere
        com.example.gecko.ZenGeckoManager.onSidebarStateChanged(true)
        assertTrue(com.example.gecko.ZenGeckoManager.isSidebarOpen)
        assertTrue(com.example.gecko.ZenGeckoManager.isContextMenuSuppressed(540))
        assertTrue(com.example.gecko.ZenGeckoManager.isContextMenuSuppressed(50))

        // When cooldown is applied, context menu is suppressed
        com.example.gecko.ZenGeckoManager.onSidebarStateChanged(false)
        com.example.gecko.ZenGeckoManager.suppressContextMenuFor(2500L)
        assertTrue(com.example.gecko.ZenGeckoManager.isContextMenuSuppressed(540))

        // Edge touches (within edge margins) are suppressed
        assertTrue(com.example.gecko.ZenGeckoManager.isContextMenuSuppressed(20))
    }

    @Test
    fun `mobile firefox extensions normalize xpi slugs and support evalScript bridge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.gecko.ZenGeckoManager.init(context)

        val directTarget = com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(
            "https://addons.mozilla.org/firefox/downloads/file/4391000/ublock_origin-1.61.2.xpi"
        )
        assertTrue(directTarget is com.example.extensions.ExtensionInstallTarget.DirectXpiUrl)
        assertEquals(
            "ublock-origin",
            (directTarget as com.example.extensions.ExtensionInstallTarget.DirectXpiUrl).slug
        )

        val contentJs = context.assets.open("extensions/zen_page_ux/content.js").bufferedReader().use { it.readText() }
        assertTrue(contentJs.contains("evalScript"))
        assertTrue(contentJs.contains("lastAmoInstallAt"))
        assertTrue(contentJs.contains("MutationObserver"))

        val shim = ZenPrivacyEngine.buildDesktopWebExtensionShimScript()
        assertTrue(shim.contains("Zen Mobile Firefox Add-ons"))

        assertTrue(!com.example.gecko.ZenGeckoManager.hasNativeExtension("non-existent-addon"))
        assertTrue(!com.example.gecko.ZenGeckoManager.hasNativePopupOrOptions("non-existent-addon"))
        assertNull(com.example.gecko.ZenGeckoManager.getNativeOptionsUrl("non-existent-addon"))
    }

    @Test
    fun `ai side panel toolbar option toggles cleanly and manages provider state`() {
        val defaultCfg = com.example.data.SidebarConfig()
        assertTrue(!defaultCfg.enabledSidebarTools.contains(com.example.data.SidebarToolItem.AI_SIDE_PANEL))

        val providers = com.example.data.AiSidePanelProvider.entries
        assertTrue(providers.any { it.id == "chatgpt" && it.url == "https://chatgpt.com/" })
        assertTrue(providers.any { it.id == "claude" && it.url == "https://claude.ai/" })
        assertTrue(providers.any { it.id == "gemini" && it.url == "https://gemini.google.com/" })
        assertEquals(com.example.data.AiSidePanelProvider.CHATGPT, com.example.data.AiSidePanelProvider.fromId("unknown"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val contentJs = context.assets.open("extensions/zen_page_ux/content.js").bufferedReader().use { it.readText() }
        assertTrue(contentJs.contains("checkAiSidePanel"))
        assertTrue(contentJs.contains("__zen_ai_sidepanel_fit_css"))

        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.ZenDatabase::class.java
        ).allowMainThreadQueries().build()
        val repo = com.example.data.ZenRepository(db.zenDao(), context)
        val vm = com.example.ui.ZenBrowserViewModel(repo)

        try {
            Thread.sleep(150)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()

            vm.toggleSidebarToolItem(com.example.data.SidebarToolItem.AI_SIDE_PANEL)
            Thread.sleep(220)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            assertTrue(vm.enabledSidebarToolsNow().contains(com.example.data.SidebarToolItem.AI_SIDE_PANEL))

            vm.setSidebarBoxMode(true)
            assertTrue(vm.isSidebarBoxModeNow())

            vm.toggleAiSidePanel()
            assertTrue(vm.isAiSidePanelOpenNow())
            assertTrue(!vm.isSidebarBoxModeNow())

            vm.selectAiProvider("claude")
            assertEquals("claude", vm.activeAiProviderIdNow())
            assertTrue(vm.isAiSidePanelOpenNow())

            // Disabling the toolbar option closes active AI side panel
            vm.toggleSidebarToolItem(com.example.data.SidebarToolItem.AI_SIDE_PANEL)
            Thread.sleep(220)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            assertTrue(!vm.enabledSidebarToolsNow().contains(com.example.data.SidebarToolItem.AI_SIDE_PANEL))
            assertTrue(!vm.isAiSidePanelOpenNow())
        } finally {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            db.close()
        }
    }
}

