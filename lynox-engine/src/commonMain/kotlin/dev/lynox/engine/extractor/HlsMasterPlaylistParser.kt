package dev.lynox.engine.extractor

import dev.lynox.engine.model.EngineError
import dev.lynox.engine.model.EngineResult

/**
 * Parses an HLS master playlist supplied by the caller.
 *
 * This parser performs no network requests and does not discover protected
 * content. The caller is responsible for obtaining the manifest through an
 * access method permitted by its source.
 */
class HlsMasterPlaylistParser {

    fun parse(manifestUrl: String, content: String): EngineResult<ResolvedMedia> {
        val baseUrl = manifestUrl.trim()
        if (!isHttpUrl(baseUrl)) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("manifestUrl must be an absolute HTTP or HTTPS URL")
            )
        }
        if (content.length > MAX_MANIFEST_CHARS) {
            return EngineResult.Failure(EngineError.InvalidRequest("HLS manifest exceeds the supported size"))
        }

        val lines = content.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.firstOrNull() != "#EXTM3U") {
            return EngineResult.Failure(EngineError.InvalidRequest("Invalid HLS manifest header"))
        }

        val audioRenditions = mutableMapOf<String, MutableList<AudioRendition>>()
        val variants = mutableListOf<Variant>()
        var pendingVariant: Map<String, String>? = null

        for (line in lines.drop(1)) {
            if (line.startsWith("#EXT-X-MEDIA:", ignoreCase = true)) {
                val attrs = parseAttributes(line.substringAfter(':'))
                if (attrs["TYPE"].equals("AUDIO", ignoreCase = true)) {
                    val group = attrs["GROUP-ID"]
                    val uri = attrs["URI"]
                    if (!group.isNullOrBlank() && !uri.isNullOrBlank()) {
                        val resolvedUri = resolveUrl(baseUrl, uri)
                        if (resolvedUri != null) {
                            audioRenditions.getOrPut(group) { mutableListOf() } += AudioRendition(
                                url = resolvedUri,
                                codecs = attrs["CODECS"],
                                isDefault = attrs["DEFAULT"].equals("YES", ignoreCase = true)
                            )
                        }
                    }
                }
            } else if (line.startsWith("#EXT-X-STREAM-INF:", ignoreCase = true)) {
                pendingVariant = parseAttributes(line.substringAfter(':'))
            } else if (!line.startsWith("#") && pendingVariant != null) {
                val attributes = pendingVariant
                val variantUrl = resolveUrl(baseUrl, line)
                if (attributes != null && variantUrl != null) {
                    variants += Variant(attributes, variantUrl)
                }
                pendingVariant = null
            }
        }

        val streams = mutableListOf<MediaStream>()
        variants.forEach { variant ->
            val attrs = variant.attributes
            val resolution = attrs["RESOLUTION"]?.split('x', 'X')
            val width = resolution?.getOrNull(0)?.toIntOrNull()?.takeIf { it > 0 }
            val height = resolution?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
            val bandwidth = attrs["AVERAGE-BANDWIDTH"]?.toLongOrNull()
                ?: attrs["BANDWIDTH"]?.toLongOrNull()
            val renditions = attrs["AUDIO"]?.let(audioRenditions::get).orEmpty()
            streams += MediaStream(
                url = variant.url,
                kind = if (renditions.isEmpty()) MediaStreamKind.AUDIO_VIDEO else MediaStreamKind.VIDEO,
                mimeType = "application/vnd.apple.mpegurl",
                codecs = attrs["CODECS"],
                bitrate = bandwidth?.takeIf { it > 0 },
                width = width,
                height = height,
                frameRate = attrs["FRAME-RATE"]?.toFloatOrNull()?.takeIf { it > 0f },
                isDefault = streams.none { it.kind == MediaStreamKind.VIDEO || it.kind == MediaStreamKind.AUDIO_VIDEO }
            )
            renditions.forEach { rendition ->
                streams += MediaStream(
                    url = rendition.url,
                    kind = MediaStreamKind.AUDIO,
                    mimeType = "application/vnd.apple.mpegurl",
                    codecs = rendition.codecs ?: attrs["CODECS"]?.split(',')
                        ?.firstOrNull { it.trim().startsWith("mp4a", ignoreCase = true) }?.trim(),
                    isDefault = rendition.isDefault
                )
            }
        }

        if (streams.isEmpty()) {
            return EngineResult.Failure(
                EngineError.InvalidRequest("Manifest contains no supported HLS master variants")
            )
        }

        return EngineResult.Success(ResolvedMedia(sourceUrl = baseUrl, streams = streams.distinctBy { it.url to it.kind }))
    }

    private data class AudioRendition(
        val url: String,
        val codecs: String?,
        val isDefault: Boolean
    )

    private data class Variant(
        val attributes: Map<String, String>,
        val url: String
    )

    private fun parseAttributes(value: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        val token = StringBuilder()
        val parts = mutableListOf<String>()
        var quoted = false
        value.forEach { char ->
            when (char) {
                '"' -> quoted = !quoted
                ',' -> if (quoted) token.append(char) else {
                    parts += token.toString()
                    token.setLength(0)
                }
                else -> token.append(char)
            }
        }
        if (token.isNotEmpty()) parts += token.toString()
        parts.forEach { part ->
            val key = part.substringBefore('=', "").trim().uppercase()
            val raw = part.substringAfter('=', "").trim()
            if (key.isNotEmpty() && raw.isNotEmpty()) result[key] = raw
        }
        return result
    }

    private fun resolveUrl(base: String, reference: String): String? {
        val ref = reference.trim()
        if (ref.isEmpty()) return null
        if (isHttpUrl(ref)) return ref
        if (ref.startsWith("//")) {
            val scheme = base.substringBefore("://")
            return "$scheme:$ref"
        }

        val baseWithoutFragment = base.substringBefore('#')
        val baseWithoutQueryOrFragment = baseWithoutFragment.substringBefore('?')
        if (ref.startsWith('#')) return baseWithoutFragment + ref
        if (ref.startsWith('?')) return baseWithoutQueryOrFragment + ref

        val authority = base.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
        val origin = base.substringBefore("://") + "://" + authority
        val referencePath = ref.substringBefore('?').substringBefore('#')
        val suffix = ref.substring(referencePath.length)
        if (referencePath.isEmpty()) return baseWithoutQueryOrFragment + suffix

        val basePath = baseWithoutQueryOrFragment.substringAfter("://").substringAfter('/', "")
        val combinedPath = if (referencePath.startsWith('/')) {
            referencePath
        } else {
            val directory = basePath.substringBeforeLast('/', "").let {
                if (it.isEmpty()) "/" else "/$it/"
            }
            directory + referencePath
        }
        return origin + normalizePath(combinedPath) + suffix
    }

    private fun normalizePath(path: String): String {
        val stack = mutableListOf<String>()
        path.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                else -> stack += segment
            }
        }
        return "/" + stack.joinToString("/")
    }

    private fun isHttpUrl(url: String): Boolean {
        val http = url.startsWith("https://", true) || url.startsWith("http://", true)
        if (!http) return false
        val authority = url.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#')
        return authority.isNotBlank() && !authority.any(Char::isWhitespace)
    }

    private companion object {
        const val MAX_MANIFEST_CHARS = 1_000_000
    }
}
