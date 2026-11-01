package com.example.tabs

import android.net.Uri

data class ImportedTab(
    val url: String,
    val title: String,
    val source: String = "External"
)

object ZenTabImporter {

    private val HTML_ANCHOR_REGEX = Regex(
        """<a\s+[^>]*?href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
        RegexOption.IGNORE_CASE
    )

    private val URL_IN_TEXT_REGEX = Regex(
        """https?://[^\s<>"]+""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses tabs from raw content which can be:
     * 1. HTML Bookmark / Export file (Chrome, Firefox, Brave, Safari, Edge)
     * 2. JSON array / object with url & title (Session Buddy, TabCopy, OneTab)
     * 3. Plain text / CSV with URLs or "URL | Title" format
     */
    fun parse(rawContent: String, sourceHint: String = "External"): List<ImportedTab> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        return when {
            trimmed.contains("<a", ignoreCase = true) || trimmed.contains("<!DOCTYPE", ignoreCase = true) -> {
                parseHtml(trimmed, sourceHint)
            }
            trimmed.startsWith("{") || trimmed.startsWith("[") -> {
                parseJsonOrFallBack(trimmed, sourceHint)
            }
            else -> {
                parsePlainText(trimmed, sourceHint)
            }
        }.filter { it.url.isNotBlank() && isValidUrl(it.url) }
            .distinctBy { it.url.trimEnd('/') }
    }

    private fun parseHtml(html: String, sourceHint: String): List<ImportedTab> {
        val results = mutableListOf<ImportedTab>()
        val matches = HTML_ANCHOR_REGEX.findAll(html)
        for (match in matches) {
            val href = match.groupValues[1].trim()
            val rawTitle = match.groupValues[2].trim()

            // Skip javascript:, place:, or internal bookmark anchors
            if (href.startsWith("javascript:", ignoreCase = true) ||
                href.startsWith("place:", ignoreCase = true) ||
                href.startsWith("#")
            ) {
                continue
            }

            val cleanTitle = cleanHtmlText(rawTitle)
            val fallbackTitle = extractHostFromUrl(href)
            results.add(
                ImportedTab(
                    url = href,
                    title = cleanTitle.ifBlank { fallbackTitle },
                    source = sourceHint
                )
            )
        }
        return results
    }

    private fun parseJsonOrFallBack(jsonStr: String, sourceHint: String): List<ImportedTab> {
        val results = mutableListOf<ImportedTab>()
        // Match JSON patterns: {"url": "...", "title": "..."} in any order
        val urlRegex = Regex(""""url"\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        val titleRegex = Regex(""""title"\s*:\s*"([^"]*)"""", RegexOption.IGNORE_CASE)

        // Try extracting objects
        val objectRegex = Regex("""\{[^{}]*\}""")
        val objects = objectRegex.findAll(jsonStr).toList()

        if (objects.isNotEmpty()) {
            for (objMatch in objects) {
                val block = objMatch.value
                val urlMatch = urlRegex.find(block)
                if (urlMatch != null) {
                    val rawUrl = unescapeJsonString(urlMatch.groupValues[1].trim())
                    val titleMatch = titleRegex.find(block)
                    val rawTitle = titleMatch?.let { unescapeJsonString(it.groupValues[1].trim()) }.orEmpty()
                    val fallbackTitle = extractHostFromUrl(rawUrl)
                    results.add(
                        ImportedTab(
                            url = rawUrl,
                            title = rawTitle.ifBlank { fallbackTitle },
                            source = sourceHint
                        )
                    )
                }
            }
        }

        if (results.isEmpty()) {
            // Fallback to text matching
            return parsePlainText(jsonStr, sourceHint)
        }

        return results
    }

    private fun parsePlainText(text: String, sourceHint: String): List<ImportedTab> {
        val results = mutableListOf<ImportedTab>()
        val lines = text.lineSequence()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) continue

            // OneTab style: "https://example.com | Page Title"
            if (trimmedLine.contains(" | ")) {
                val parts = trimmedLine.split(" | ", limit = 2)
                val urlPart = parts[0].trim()
                val titlePart = parts.getOrNull(1)?.trim().orEmpty()
                if (isValidUrl(urlPart)) {
                    results.add(
                        ImportedTab(
                            url = normalizeUrl(urlPart),
                            title = titlePart.ifBlank { extractHostFromUrl(urlPart) },
                            source = sourceHint
                        )
                    )
                    continue
                }
            }

            // CSV style: "https://example.com, Page Title"
            if (trimmedLine.contains(",")) {
                val commaIndex = trimmedLine.indexOf(',')
                val first = trimmedLine.substring(0, commaIndex).trim()
                val rest = trimmedLine.substring(commaIndex + 1).trim().removeSurrounding("\"")
                if (isValidUrl(first)) {
                    results.add(
                        ImportedTab(
                            url = normalizeUrl(first),
                            title = rest.ifBlank { extractHostFromUrl(first) },
                            source = sourceHint
                        )
                    )
                    continue
                }
            }

            // Find all URLs in the line
            val urlMatches = URL_IN_TEXT_REGEX.findAll(trimmedLine).toList()
            if (urlMatches.isNotEmpty()) {
                for (um in urlMatches) {
                    val foundUrl = um.value.trim().trimEnd('.', ',', ';', ')')
                    val title = trimmedLine.replace(um.value, "").trim().removePrefix("-").trim()
                    results.add(
                        ImportedTab(
                            url = foundUrl,
                            title = title.ifBlank { extractHostFromUrl(foundUrl) },
                            source = sourceHint
                        )
                    )
                }
            } else if (trimmedLine.startsWith("www.") || (trimmedLine.contains(".") && !trimmedLine.contains(" "))) {
                // Bare domain
                val normalized = "https://$trimmedLine"
                results.add(
                    ImportedTab(
                        url = normalized,
                        title = trimmedLine,
                        source = sourceHint
                    )
                )
            }
        }
        return results
    }

    private fun cleanHtmlText(raw: String): String {
        return raw.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }

    private fun unescapeJsonString(s: String): String {
        return s.replace("\\/", "/")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    private fun normalizeUrl(url: String): String {
        return if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            "https://$url"
        } else url
    }

    private fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.length < 4) return false
        if (trimmed.startsWith("javascript:", ignoreCase = true)) return false
        if (trimmed.startsWith("about:blank", ignoreCase = true)) return false
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            (trimmed.contains(".") && !trimmed.contains(" "))
    }

    fun extractHostFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(normalizeUrl(url))
            uri.host?.removePrefix("www.")?.ifBlank { url } ?: url
        } catch (_: Exception) {
            url
        }
    }
}
