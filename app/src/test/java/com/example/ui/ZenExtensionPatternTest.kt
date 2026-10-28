package com.example.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenExtensionPatternTest {
    private val state = ZenBrowserUiState()

    @Test
    fun exactHostAndPathMustMatch() {
        assertTrue(state.matchesExtensionUrlPattern(
            "https://example.com/articles/one",
            "https://example.com/articles/*"
        ))
        assertFalse(state.matchesExtensionUrlPattern(
            "https://example.com/account",
            "https://example.com/articles/*"
        ))
    }

    @Test
    fun wildcardHostMatchesRootAndSubdomainsOnly() {
        assertTrue(state.matchesExtensionUrlPattern(
            "https://example.com/page",
            "*://*.example.com/*"
        ))
        assertTrue(state.matchesExtensionUrlPattern(
            "http://news.example.com/page",
            "*://*.example.com/*"
        ))
        assertFalse(state.matchesExtensionUrlPattern(
            "https://notexample.com/page",
            "*://*.example.com/*"
        ))
    }

    @Test
    fun patternsDoNotMatchHostSubstrings() {
        assertFalse(state.matchesExtensionUrlPattern(
            "https://example.com.attacker.test/",
            "https://example.com/*"
        ))
    }

    @Test
    fun internalAndNonHttpUrlsAreNeverMatched() {
        assertFalse(state.matchesExtensionUrlPattern("zen://home", "*://*/*"))
        assertFalse(state.matchesExtensionUrlPattern("about:blank", "<all_urls>"))
        assertFalse(state.matchesExtensionUrlPattern("file:///tmp/page.html", "<all_urls>"))
    }

    @Test
    fun userscriptUrlGlobIsSupported() {
        assertTrue(state.matchesExtensionUrlPattern(
            "https://example.com/path/123",
            "https://example.com/path/*"
        ))
        assertFalse(state.matchesExtensionUrlPattern(
            "https://example.com/other",
            "https://example.com/path/*"
        ))
    }
}
