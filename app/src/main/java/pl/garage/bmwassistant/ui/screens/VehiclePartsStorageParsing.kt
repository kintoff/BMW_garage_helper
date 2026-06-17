package pl.garage.bmwassistant.ui.screens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

internal data class AllegroOfferDetails(
    val title: String,
    val price: String,
    val imageUrl: String?,
    val offerUrl: String,
)

internal fun parsePriceAmount(value: String): Double? {
    val normalized = value
        .replace("PLN", "", ignoreCase = true)
        .replace("zl", "", ignoreCase = true)
        .replace("zł", "", ignoreCase = true)
        .replace(" ", "")
        .replace(",", ".")
    val matched = Regex("""\d+(\.\d+)?""").find(normalized)?.value ?: return null
    return matched.toDoubleOrNull()
}

internal fun String.isAllegroOfferUrl(): Boolean =
    contains("allegro.pl/oferta/", ignoreCase = true) || startsWith("/oferta/", ignoreCase = true)

internal suspend fun fetchAllegroOfferDetails(
    inputUrl: String,
): AllegroOfferDetails = withContext(Dispatchers.IO) {
    val normalizedInput = normalizeAllegroOfferInput(inputUrl)
    val normalizedUrl = normalizedInput.substringBefore("?utm_", missingDelimiterValue = normalizedInput)

    if (!normalizedUrl.isAllegroOfferUrl()) {
        throw IllegalArgumentException("Wklej link do oferty Allegro albo sam koniec adresu oferty.")
    }

    val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        instanceFollowRedirects = true
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
        )
        setRequestProperty("Accept-Language", "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
        setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        setRequestProperty("Cache-Control", "no-cache")
    }

    val responseStream = if (connection.responseCode >= 400) {
        connection.errorStream ?: connection.inputStream
    } else {
        connection.inputStream
    }

    responseStream.bufferedReader().use { reader ->
        val html = reader.readText()
        parseAllegroOfferDetails(html, normalizedUrl)
            ?: throw IllegalStateException(
                if (html.contains("Please enable JS and disable any ad blocker", ignoreCase = true)) {
                    "Allegro zablokowalo automatyczny odczyt tej oferty. Sprobuj innego linku lub zapisz dane recznie."
                } else {
                    "Nie udalo sie odczytac ceny albo zdjecia z tej oferty Allegro."
                }
            )
    }
}

internal fun parseAllegroOfferDetails(
    html: String,
    fallbackUrl: String,
): AllegroOfferDetails? {
    extractAllegroJsonLdDetails(html, fallbackUrl)?.let { return it }

    val title = extractHtmlMetaContent(html, "property", "og:title")
        ?: extractHtmlMetaContent(html, "name", "twitter:title")
        ?: Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.substringBefore(" - Allegro")
            ?.let(::decodeHtmlValue)
            ?.takeIf { it.isNotBlank() }

    val price = extractHtmlMetaContent(html, "property", "product:price:amount")
        ?.let(::normalizeOfferPrice)
        ?: Regex("\"price\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizeOfferPrice)

    val imageUrl = extractHtmlMetaContent(html, "property", "og:image")
        ?: extractHtmlMetaContent(html, "name", "twitter:image")
        ?: Regex("\"image\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeHtmlValue)

    return if (title != null && price != null) {
        AllegroOfferDetails(
            title = title,
            price = price,
            imageUrl = imageUrl,
            offerUrl = fallbackUrl
        )
    } else {
        null
    }
}

internal fun extractAllegroJsonLdDetails(
    html: String,
    fallbackUrl: String,
): AllegroOfferDetails? {
    val scriptRegex = Regex(
        "<script[^>]*type=\"application/ld\\+json\"[^>]*>(.*?)</script>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    scriptRegex.findAll(html).forEach { match ->
        val rawJson = match.groupValues[1]
        val details = runCatching {
            val normalized = rawJson.trim()
            when {
                normalized.startsWith("[") -> JSONArray(normalized).toAllegroOfferDetails(fallbackUrl)
                normalized.startsWith("{") -> JSONObject(normalized).toAllegroOfferDetails(fallbackUrl)
                else -> null
            }
        }.getOrNull()
        if (details != null) return details
    }
    return null
}

internal fun JSONArray.toAllegroOfferDetails(fallbackUrl: String): AllegroOfferDetails? {
    for (index in 0 until length()) {
        val details = optJSONObject(index)?.toAllegroOfferDetails(fallbackUrl)
        if (details != null) return details
    }
    return null
}

internal fun JSONObject.toAllegroOfferDetails(fallbackUrl: String): AllegroOfferDetails? {
    val type = optString("@type")
    if (!type.contains("Product", ignoreCase = true) && !type.contains("Offer", ignoreCase = true)) {
        return null
    }

    val title = optString("name").ifBlank { null }
        ?: optJSONObject("itemOffered")?.optString("name")?.ifBlank { null }
        ?: return null

    val offersObject = optJSONObject("offers")
        ?: optJSONArray("offers")?.optJSONObject(0)
        ?: if (type.contains("Offer", ignoreCase = true)) this else return null

    val rawPrice = offersObject.optString("price").ifBlank { null }
        ?: return null
    val currency = offersObject.optString("priceCurrency").ifBlank { null } ?: "PLN"

    val imageUrl = when (val imageValue = opt("image")) {
        is JSONArray -> imageValue.optString(0).ifBlank { null }
        is String -> imageValue.ifBlank { null }
        else -> null
    }

    val offerUrl = offersObject.optString("url").ifBlank { null }
        ?: optString("url").ifBlank { null }
        ?: fallbackUrl

    return AllegroOfferDetails(
        title = title,
        price = normalizeOfferPrice("$rawPrice $currency"),
        imageUrl = imageUrl,
        offerUrl = offerUrl
    )
}

internal fun extractHtmlMetaContent(
    html: String,
    attributeName: String,
    attributeValue: String,
): String? {
    val patterns = listOf(
        Regex(
            "<meta[^>]*$attributeName=[\"']${Regex.escape(attributeValue)}[\"'][^>]*content=[\"']([^\"']+)[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            "<meta[^>]*content=[\"']([^\"']+)[\"'][^>]*$attributeName=[\"']${Regex.escape(attributeValue)}[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
    )
    return patterns.firstNotNullOfOrNull { regex ->
        regex.find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeHtmlValue)
            ?.takeIf { it.isNotBlank() }
    }
}

internal fun normalizeOfferPrice(value: String): String {
    val parsed = parsePriceAmount(value)
    return if (parsed != null) {
        "%.2f PLN".format(Locale.US, parsed).replace('.', ',')
    } else {
        value
            .replace("PLN", "", ignoreCase = true)
            .trim()
            .let { normalized ->
                if (normalized.contains("PLN", ignoreCase = true)) normalized else "$normalized PLN"
            }
    }
}

internal fun normalizeAllegroOfferInput(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return trimmed

    return when {
        trimmed.contains("allegro.pl/oferta/", ignoreCase = true) -> trimmed.prependHttpsIfMissing()
        trimmed.startsWith("/oferta/", ignoreCase = true) -> "https://allegro.pl$trimmed"
        trimmed.startsWith("oferta/", ignoreCase = true) -> "https://allegro.pl/$trimmed"
        trimmed.startsWith("m.allegro.pl/oferta/", ignoreCase = true) -> "https://$trimmed"
        trimmed.contains("/") && !trimmed.contains(" ") -> "https://allegro.pl/oferta/${trimmed.trimStart('/')}"
        else -> trimmed.prependHttpsIfMissing()
    }
}

private fun String.prependHttpsIfMissing(): String =
    when {
        startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true) -> this
        else -> "https://$this"
    }

private fun decodeHtmlValue(value: String): String =
    value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
