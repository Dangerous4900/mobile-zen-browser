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
@Config(sdk = [36])
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
}
