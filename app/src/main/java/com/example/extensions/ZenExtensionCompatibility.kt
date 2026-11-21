package com.example.extensions

import com.example.data.FirefoxExtensionEntity
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.ZipInputStream

sealed class ExtensionInstallTarget {
    data class UserscriptUrl(val url: String) : ExtensionInstallTarget()
    data class AmoAddon(val slug: String, val originalUrl: String) : ExtensionInstallTarget()
    data class DirectXpiUrl(val url: String, val slug: String) : ExtensionInstallTarget()
    data class Unsupported(val reason: String) : ExtensionInstallTarget()
}

data class ParsedWebExtension(
    val entity: FirefoxExtensionEntity,
    val supportsNativeGecko: Boolean,
    val warnings: List<String>
)

/**
 * Unified compatibility helpers for Mobile Firefox Add-ons (.xpi),
 * _locales message localization, and userscripts (.user.js).
 */
object ZenExtensionCompatibility {

    fun resolveInstallTarget(rawUrl: String): ExtensionInstallTarget {
        val trimmedInput = rawUrl.trim()
        if (trimmedInput.isNotEmpty() &&
            !trimmedInput.contains("://") &&
            !trimmedInput.contains('/') &&
            !trimmedInput.contains(' ') &&
            Regex("^[a-zA-Z0-9._-]{2,120}$").matches(trimmedInput)
        ) {
            return ExtensionInstallTarget.AmoAddon(
                slug = trimmedInput,
                originalUrl = "https://addons.mozilla.org/en-US/android/addon/$trimmedInput/"
            )
        }
        val parsed = runCatching { java.net.URI(trimmedInput) }.getOrNull()
            ?: return ExtensionInstallTarget.Unsupported("Extension installs require a valid HTTPS URL")
        val scheme = parsed.scheme?.lowercase()
        val host = parsed.host?.lowercase()
        if (scheme != "https" || host.isNullOrBlank()) {
            return ExtensionInstallTarget.Unsupported("Extension installs require a valid HTTPS URL")
        }

        val lower = trimmedInput.lowercase()
        if (lower.endsWith(".user.js") || lower.contains(".user.js?")) {
            return ExtensionInstallTarget.UserscriptUrl(trimmedInput)
        }

        val path = parsed.path.orEmpty()
        val lowerPath = path.lowercase()
        if (lowerPath.endsWith(".crx") ||
            lower.contains(".crx?") ||
            host == "chromewebstore.google.com" ||
            (host == "chrome.google.com" && lowerPath.contains("/webstore"))
        ) {
            return ExtensionInstallTarget.Unsupported(
                "PC & Chrome extensions are not supported. Use Mobile Firefox Add-ons (addons.mozilla.org), .xpi, or .user.js."
            )
        }

        val isAmo = host == "addons.mozilla.org" || host.endsWith(".addons.mozilla.org")
        val isDirectXpi = lowerPath.endsWith(".xpi") ||
            lower.contains(".xpi?") ||
            (isAmo && lowerPath.contains("/downloads/file/"))
        if (isDirectXpi) {
            val rawName = path.substringAfterLast('/')
                .substringBefore(".xpi")
                .substringBeforeLast('-')
                .replace('_', '-')
                .lowercase()
                .trim('-')
            val slug = rawName.ifBlank {
                path.substringAfterLast('/').substringBefore(".xpi").ifBlank { "firefox-addon" }
            }
            return ExtensionInstallTarget.DirectXpiUrl(trimmedInput, slug)
        }
        if (isAmo) {
            val segments = path.split('/').filter { it.isNotBlank() }
            val addonIndex = segments.indexOfFirst { it.equals("addon", ignoreCase = true) }
            val slug = if (addonIndex >= 0 && addonIndex + 1 < segments.size) segments[addonIndex + 1] else ""
            if (slug.isBlank()) {
                return ExtensionInstallTarget.Unsupported("Open a specific Mozilla Mobile Add-on page before installing")
            }
            return ExtensionInstallTarget.AmoAddon(slug, trimmedInput)
        }

        return ExtensionInstallTarget.Unsupported(
            "Unsupported extension source. Use Mobile Firefox Add-ons (addons.mozilla.org), .xpi, or .user.js."
        )
    }

    fun loadLocaleMessages(entryMap: Map<String, String>): Map<String, String> {
        val candidateKeys = entryMap.keys
            .filter { it.startsWith("_locales/") && it.endsWith("/messages.json") }
            .sortedWith(
                compareBy<String>(
                    { !it.startsWith("_locales/en/") },
                    { !it.startsWith("_locales/en_us/") },
                    { !it.startsWith("_locales/en_gb/") },
                    { it }
                )
            )
        val result = HashMap<String, String>()
        for (localeKey in candidateKeys) {
            val raw = entryMap[localeKey] ?: continue
            runCatching {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val msg = obj.optJSONObject(k)?.optString("message").orEmpty()
                    if (msg.isNotBlank() && !result.containsKey(k.lowercase())) {
                        result[k.lowercase()] = msg
                    }
                }
            }
            if (result.isNotEmpty()) break
        }
        return result
    }

    fun resolveManifestMessage(raw: String, localeMessages: Map<String, String>): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("__MSG_") || !trimmed.endsWith("__") || trimmed.length <= 8) {
            return trimmed
        }
        val key = trimmed.substring(6, trimmed.length - 2).lowercase()
        return localeMessages[key]?.takeIf { it.isNotBlank() } ?: trimmed
    }

    fun extractPackageMetadataAndScripts(
        fileName: String,
        rawBytes: ByteArray,
        allowNativeBackgroundOnly: Boolean = true
    ): ParsedWebExtension {
        val isCrxHeader = rawBytes.size >= 4 &&
            rawBytes[0] == 'C'.code.toByte() &&
            rawBytes[1] == 'r'.code.toByte() &&
            rawBytes[2] == '2'.code.toByte() &&
            rawBytes[3] == '4'.code.toByte()
        if (fileName.lowercase().endsWith(".crx") || isCrxHeader) {
            throw IllegalArgumentException(
                "PC & Chrome (.crx) extensions are not supported. Use Mobile Firefox Add-ons (.xpi) or .user.js."
            )
        }

        val entryMap = mutableMapOf<String, String>()
        var entryCount = 0
        var selectedBytes = 0L
        var totalArchiveBytes = 0L

        ZipInputStream(ByteArrayInputStream(rawBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > 25_000) throw IllegalArgumentException("Archive exceeds safe entry limit")
                val normalizedName = entry.name.replace('\\', '/')
                    .removePrefix("./")
                    .trimStart('/')
                val safePath = normalizedName.split('/').none { it == ".." }
                val lowerName = normalizedName.lowercase()
                val shouldRead = !entry.isDirectory && safePath &&
                    (lowerName == "manifest.json" ||
                        lowerName.endsWith("/messages.json") ||
                        lowerName.endsWith(".js") ||
                        lowerName.endsWith(".css"))
                val buffer = ByteArray(8192)
                if (shouldRead && (entryMap.size < 600 || lowerName == "manifest.json" || lowerName.endsWith("/messages.json"))) {
                    val output = ByteArrayOutputStream()
                    var fileBytes = 0
                    while (true) {
                        val count = zis.read(buffer)
                        if (count < 0) break
                        fileBytes += count
                        selectedBytes += count
                        totalArchiveBytes += count
                        if (fileBytes > 2 * 1024 * 1024 || selectedBytes > 20L * 1024 * 1024 || totalArchiveBytes > 80L * 1024 * 1024) {
                            throw IllegalArgumentException("Extension archive exceeds the import size limit")
                        }
                        output.write(buffer, 0, count)
                    }
                    entryMap[lowerName] = output.toByteArray().toString(Charsets.UTF_8)
                } else {
                    while (true) {
                        val count = zis.read(buffer)
                        if (count < 0) break
                        totalArchiveBytes += count
                        if (totalArchiveBytes > 80L * 1024 * 1024) {
                            throw IllegalArgumentException("Extension archive exceeds the import size limit")
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val manifestRaw = entryMap["manifest.json"]
            ?: throw IllegalArgumentException("Extension package has no root manifest.json")
        val json = JSONObject(manifestRaw)
        val localeMessages = loadLocaleMessages(entryMap)

        val resolvedName = resolveManifestMessage(json.optString("name", ""), localeMessages)
        val extName = resolvedName.takeIf { it.isNotBlank() && !it.startsWith("__MSG_") }
            ?: fileName.substringBeforeLast(".").replace("-", " ").replace("_", " ").trim()
                .ifEmpty { "Firefox Mobile Add-on" }
        val extVersion = json.optString("version", "1.0.0").ifBlank { "1.0.0" }
        val rawDescription = resolveManifestMessage(json.optString("description", ""), localeMessages)
        val extAuthor = json.optString("author", "Mozilla Add-ons")
        val manifestVersion = json.optInt("manifest_version", 2)

        val permsList = mutableListOf<String>()
        listOf("permissions", "host_permissions", "optional_permissions").forEach { key ->
            val arr = json.optJSONArray(key) ?: return@forEach
            for (i in 0 until arr.length()) {
                val p = arr.optString(i).trim()
                if (p.isNotBlank()) permsList.add(p)
            }
        }
        val extPermissions = permsList.distinct().joinToString(", ").ifBlank { "Standard web permissions" }

        val matchesSet = linkedSetOf<String>()
        val collectedCss = StringBuilder()
        val collectedJs = StringBuilder()
        var extRunAt = "document_idle"

        val contentScripts = json.optJSONArray("content_scripts")
        if (contentScripts != null) {
            for (i in 0 until contentScripts.length()) {
                val cs = contentScripts.optJSONObject(i) ?: continue
                when (cs.optString("run_at", "document_idle")) {
                    "document_start" -> extRunAt = "document_start"
                    "document_end" -> if (extRunAt != "document_start") extRunAt = "document_end"
                    else -> if (extRunAt != "document_start" && extRunAt != "document_end") extRunAt = "document_idle"
                }
                val matches = cs.optJSONArray("matches")
                if (matches != null) {
                    for (m in 0 until matches.length()) {
                        val pattern = matches.optString(m).trim()
                        if (pattern.isNotBlank()) matchesSet.add(pattern)
                    }
                }
                val css = cs.optJSONArray("css")
                if (css != null) {
                    for (c in 0 until css.length()) {
                        val pathKey = css.optString(c).replace('\\', '/').removePrefix("./").trimStart('/').lowercase()
                        entryMap[pathKey]?.let { collectedCss.append(it).append('\n') }
                    }
                }
                val js = cs.optJSONArray("js")
                if (js != null) {
                    for (j in 0 until js.length()) {
                        val pathKey = js.optString(j).replace('\\', '/').removePrefix("./").trimStart('/').lowercase()
                        entryMap[pathKey]?.let { collectedJs.append(it).append('\n') }
                    }
                }
            }
        }

        // Fallback: MV3 user_scripts or host_permissions + top-level injected script files
        if (collectedJs.isBlank() && collectedCss.isBlank()) {
            val hostPerms = json.optJSONArray("host_permissions")
            if (hostPerms != null) {
                for (h in 0 until hostPerms.length()) {
                    val p = hostPerms.optString(h).trim()
                    if (p.contains("://") || p == "<all_urls>") matchesSet.add(p)
                }
            }
            if (matchesSet.isEmpty()) matchesSet.add("*://*/*")
            entryMap.entries
                .filter { (k, _) ->
                    (k.endsWith(".js") || k.endsWith(".css")) &&
                        !k.contains("background") &&
                        !k.contains("service_worker") &&
                        !k.contains("popup") &&
                        !k.contains("options")
                }
                .take(8)
                .forEach { (k, v) ->
                    if (k.endsWith(".css")) collectedCss.append(v).append('\n')
                    else collectedJs.append(v).append('\n')
                }
        }

        val hasNativeWebExtLogic = json.has("background") || json.has("action") ||
            json.has("browser_action") || json.has("page_action")
        if (matchesSet.isEmpty()) matchesSet.add("*://*/*")
        if (collectedCss.isBlank() && collectedJs.isBlank() && !allowNativeBackgroundOnly && !hasNativeWebExtLogic) {
            throw IllegalArgumentException("No injectable content scripts or stylesheets were found in this package")
        }

        val warnings = mutableListOf<String>()
        if (json.has("background")) warnings.add("background/service_worker")
        if (json.has("action") || json.has("browser_action") || json.has("page_action")) warnings.add("toolbar popup")
        if (json.has("options_ui") || json.has("options_page")) warnings.add("options page")

        val geckoId = json.optJSONObject("browser_specific_settings")
            ?.optJSONObject("gecko")
            ?.optString("id")
            .orEmpty()
            .ifBlank {
                json.optJSONObject("applications")?.optJSONObject("gecko")?.optString("id").orEmpty()
            }
        val supportsNativeGecko = geckoId.isNotBlank() || hasNativeWebExtLogic

        val modeLabel = buildString {
            append(
                if (collectedCss.isBlank() && collectedJs.isBlank()) {
                    "Firefox Native WebExtension (.xpi) MV$manifestVersion"
                } else {
                    "Firefox Mobile Add-on (.xpi) MV$manifestVersion"
                }
            )
            if (warnings.isNotEmpty()) {
                append(" • Features: ${warnings.joinToString()}")
            }
        }
        val fullDescription = listOfNotNull(
            rawDescription.takeIf { it.isNotBlank() && !it.startsWith("__MSG_") },
            modeLabel
        ).joinToString("\n\n")

        val entity = FirefoxExtensionEntity(
            id = "ff_xpi_${UUID.randomUUID().toString().take(8)}",
            name = extName,
            author = extAuthor,
            version = extVersion,
            description = fullDescription,
            category = "Firefox Mobile Add-on",
            permissions = extPermissions,
            isInstalled = true,
            isEnabled = true,
            allowInPrivate = false,
            runAt = extRunAt,
            matchPatterns = matchesSet.joinToString(", "),
            amoSlug = fileName.substringBeforeLast("."),
            cssPayload = collectedCss.toString().trim(),
            jsPayload = collectedJs.toString().trim()
        )

        return ParsedWebExtension(
            entity = entity,
            supportsNativeGecko = supportsNativeGecko,
            warnings = warnings
        )
    }
}
