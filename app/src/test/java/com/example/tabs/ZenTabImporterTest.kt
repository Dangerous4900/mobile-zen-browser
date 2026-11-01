package com.example.tabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZenTabImporterTest {

    @Test
    fun `parse html bookmarks from chrome or firefox export`() {
        val html = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
            <TITLE>Bookmarks</TITLE>
            <H1>Bookmarks</H1>
            <DL><p>
                <DT><A HREF="https://github.com/zen-browser" ADD_DATE="1600000000">Zen Browser &amp; Community</A>
                <DT><A HREF="https://en.wikipedia.org/wiki/Kotlin_(programming_language)">Kotlin &lt;Language&gt;</A>
                <DT><A HREF="javascript:void(0)">Invalid Bookmark</A>
            </DL><p>
        """.trimIndent()

        val results = ZenTabImporter.parse(html, "Chrome")
        assertEquals(2, results.size)
        assertEquals("https://github.com/zen-browser", results[0].url)
        assertEquals("Zen Browser & Community", results[0].title)
        assertEquals("https://en.wikipedia.org/wiki/Kotlin_(programming_language)", results[1].url)
        assertEquals("Kotlin <Language>", results[1].title)
    }

    @Test
    fun `parse json session tabs export`() {
        val json = """
            [
              {"url": "https://news.ycombinator.com", "title": "Hacker News"},
              {"url": "https://developer.android.com/jetpack/compose", "title": "Jetpack Compose Docs"}
            ]
        """.trimIndent()

        val results = ZenTabImporter.parse(json, "Session Buddy")
        assertEquals(2, results.size)
        assertEquals("https://news.ycombinator.com", results[0].url)
        assertEquals("Hacker News", results[0].title)
        assertEquals("https://developer.android.com/jetpack/compose", results[1].url)
        assertEquals("Jetpack Compose Docs", results[1].title)
    }

    @Test
    fun `parse plain text and onetab list`() {
        val text = """
            https://google.com | Google Search
            https://reddit.com/r/android, Reddit Android
            https://www.youtube.com
        """.trimIndent()

        val results = ZenTabImporter.parse(text, "OneTab")
        assertEquals(3, results.size)
        assertEquals("https://google.com", results[0].url)
        assertEquals("Google Search", results[0].title)
        assertEquals("https://reddit.com/r/android", results[1].url)
        assertEquals("Reddit Android", results[1].title)
        assertEquals("https://www.youtube.com", results[2].url)
    }
}
