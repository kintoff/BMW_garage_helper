package pl.garage.bmwassistant.ui.screens

import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

internal fun parseCzescidobmwResults(
    html: String,
    searchUrl: String,
    searchedOemPartNumber: String,
): List<MockPartLookupResult> {
    val analyticsJson = Regex(
        pattern = "var googleAnalyticsData = (\\{.*?\\});",
        option = RegexOption.DOT_MATCHES_ALL
    ).find(html)?.groupValues?.getOrNull(1) ?: return emptyList()

    val items = JSONObject(analyticsJson).optJSONArray("items") ?: return emptyList()
    val imageUrlsByArticleCode = productImageUrlsByArticleCode(html)
    val productUrlsByArticleCode = productPageUrlsByArticleCode(html)
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val itemId = decodeHtmlCompat(item.optString("item_id"))
            if (itemId.isBlank()) continue
            val itemName = decodeHtmlCompat(item.optString("item_name"))
            val name = itemName.ifBlank { itemId }
            val brand = decodeHtmlCompat(item.optString("item_brand")).ifBlank { "Nieznany producent" }
            val price = item.optDouble("price", Double.NaN)
            val priceLabel = if (price.isNaN()) {
                "do sprawdzenia"
            } else {
                "%.2f PLN".format(Locale.US, price)
            }
            val imageUrl = imageUrlsByArticleCode[itemId]

            add(
                MockPartLookupResult(
                    oemPartNumber = searchedOemPartNumber,
                    manufacturerPartNumber = itemId,
                    name = name,
                    manufacturer = brand,
                    realOemPrice = "do sprawdzenia",
                    shopPrice = priceLabel,
                    diagram = "do uzupelnienia z RealOEM",
                    realOemUrl = "https://www.realoem.com/bmw/partxref?q=$itemId",
                    shopUrl = productUrlsByArticleCode[itemId] ?: searchUrl,
                    imageSource = if (imageUrl == null) "brak zdjecia w czescidobmw.pl" else "czescidobmw.pl",
                    imageUrl = imageUrl,
                    imageSearchUrl = imageSearchUrlFor(itemId, brand)
                )
            )
        }
    }
}

internal fun productPageUrlsByArticleCode(html: String): Map<String, String> {
    val productPanels = Regex(
        pattern = "<div class=\"c-product__panel[\\s\\S]*?(?=<div class=\"c-product__panel|<div id=\"hook_seodescriptionbottomhook)",
    ).findAll(html)

    return buildMap {
        productPanels.forEach { panelMatch ->
            val panel = panelMatch.value
            val articleCode = Regex("data-article-code=\"([^\"]+)\"")
                .find(panel)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::decodeHtmlCompat)
                ?: return@forEach

            val productUrl = Regex("href=\"([^\"]+)\"")
                .findAll(panel)
                .mapNotNull { match -> match.groupValues.getOrNull(1) }
                .map(::decodeHtmlCompat)
                .map(::absoluteCzescidobmwUrl)
                .firstOrNull { url -> url.isLikelyCzescidobmwProductUrl() }

            if (!productUrl.isNullOrBlank()) {
                put(articleCode, productUrl)
            }
        }
    }
}

internal fun productImageUrlsByArticleCode(html: String): Map<String, String> {
    val productPanels = Regex(
        pattern = "<div class=\"c-product__panel[\\s\\S]*?(?=<div class=\"c-product__panel|<div id=\"hook_seodescriptionbottomhook)",
    ).findAll(html)

    return buildMap {
        productPanels.forEach { panelMatch ->
            val panel = panelMatch.value
            val articleCode = Regex("data-article-code=\"([^\"]+)\"")
                .find(panel)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::decodeHtmlCompat)
                ?: return@forEach

            val imageUrl = Regex("class=\"c-product-image__link\" href=\"([^\"]+)\"")
                .find(panel)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::decodeHtmlCompat)
                ?: Regex("CustomLazySrc: '([^']+)'")
                    .find(panel)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let(::decodeHtmlCompat)

            if (!imageUrl.isNullOrBlank() && !imageUrl.endsWith("BMW.svg")) {
                put(articleCode, absoluteCzescidobmwUrl(imageUrl))
            }
        }
    }
}

internal fun absoluteCzescidobmwUrl(url: String): String =
    when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "https://czescidobmw.pl$url"
        else -> "https://czescidobmw.pl/$url"
    }.replace("&amp;", "&")

internal fun String.isLikelyCzescidobmwProductUrl(): Boolean {
    val normalized = lowercase(Locale.ROOT)
    return normalized.contains("czescidobmw.pl") &&
        !normalized.contains("/wyniki-wyszukiwania") &&
        !normalized.contains("/szukaj") &&
        !normalized.endsWith(".jpg") &&
        !normalized.endsWith(".jpeg") &&
        !normalized.endsWith(".png") &&
        !normalized.endsWith(".svg") &&
        !normalized.endsWith(".webp")
}

internal fun imageSearchUrlFor(
    partNumber: String,
    manufacturer: String,
): String {
    val query = URLEncoder.encode("$partNumber BMW $manufacturer czesc zdjecie", "UTF-8")
    return "https://www.google.com/search?tbm=isch&q=$query"
}

internal fun parsePartLabelText(rawText: String): ParsedPartLabel {
    val normalizedText = normalizeLabelText(rawText)

    val manufacturer = when {
        normalizedText.contains("FEBI") || normalizedText.contains("BILSTEIN") -> "FEBI"
        normalizedText.contains("LEMFORDER") || normalizedText.contains("LEMF") -> "LEMFORDER"
        normalizedText.contains("BMW") -> "BMW"
        else -> null
    }

    val oemPartNumber = findBmwOemPartNumber(normalizedText)
    val manufacturerPartNumber = findManufacturerPartNumber(
        normalizedText = normalizedText,
        manufacturer = manufacturer,
        oemPartNumber = oemPartNumber
    )

    return ParsedPartLabel(
        oemPartNumber = oemPartNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        manufacturer = manufacturer
    )
}

internal fun normalizeLabelText(rawText: String): String =
    rawText
        .uppercase(Locale.ROOT)
        .replace("Ö", "O")
        .replace("Ó", "O")
        .replace("Ł", "L")
        .replace(Regex("[^A-Z0-9/ .:-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun findBmwOemPartNumber(text: String): String? {
    val spacedBmwPattern = Regex("\\b[0-9OQIIL]{2}\\s*[0-9OQIIL]{2}\\s*[0-9OQIIL]\\s*[0-9OQIIL]{3}\\s*[0-9OQIIL]{3}\\b")
    val compactBmwPattern = Regex("\\b[0-9OQIIL]{11}\\b")

    return spacedBmwPattern.findAll(text)
        .map { it.value.normalizeOcrNumber() }
        .firstOrNull { it.length == 11 }
        ?: compactBmwPattern.findAll(text)
            .map { it.value.normalizeOcrNumber() }
            .firstOrNull { it.length == 11 }
}

internal fun findManufacturerPartNumber(
    normalizedText: String,
    manufacturer: String?,
    oemPartNumber: String?,
): String? {
    if (manufacturer == "FEBI") {
        Regex("\\b(?:NR|NO)\\.?\\s*(\\d{4,6})\\b")
            .find(normalizedText)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it }
    }

    if (manufacturer == "LEMFORDER") {
        Regex("\\b(\\d{5}\\s*\\d{2})(?:\\s+\\d{3})?\\b")
            .find(normalizedText)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.let { return it }
    }

    return Regex("\\b\\d{4,8}\\b")
        .findAll(normalizedText)
        .map { it.value.onlyDigits() }
        .firstOrNull { candidate ->
            candidate != oemPartNumber &&
                candidate.length in 4..8 &&
                candidate != "000000"
        }
}

internal fun String.onlyDigits(): String = filter { it.isDigit() }

internal fun String.normalizeOcrNumber(): String =
    uppercase(Locale.ROOT)
        .mapNotNull { character ->
            when (character) {
                in '0'..'9' -> character
                'O', 'Q' -> '0'
                'I', 'L' -> '1'
                else -> null
            }
        }
        .joinToString("")

internal fun decodeHtmlCompat(value: String): String =
    value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .trim()
