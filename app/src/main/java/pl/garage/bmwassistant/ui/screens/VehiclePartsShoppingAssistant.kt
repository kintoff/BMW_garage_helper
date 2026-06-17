package pl.garage.bmwassistant.ui.screens

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pl.garage.bmwassistant.BuildConfig
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

internal enum class MarketSearchSource(
    val displayName: String,
    val badgeLabel: String,
) {
    Allegro("Allegro", "al"),
    Ceneo("Ceneo", "ce"),
    IParts("iParts", "ip"),
}

internal data class AiPartComparisonRequest(
    val oem: String,
    val partName: String,
    val vehicle: String?,
    val partnerPrice: Double?,
)

internal data class AiPartOffer(
    val source: String,
    val price: Double,
    val currency: String,
    val url: String,
    val note: String,
)

internal data class AiBestPrice(
    val source: String,
    val price: Double,
)

internal data class AiBestSafeChoice(
    val source: String,
    val reason: String,
)

internal data class AiSaving(
    val amount: Double,
    val percentage: Double,
)

internal data class AiPartComparisonResult(
    val offers: List<AiPartOffer>,
    val bestPrice: AiBestPrice?,
    val bestSafeChoice: AiBestSafeChoice?,
    val saving: AiSaving?,
    val recommendation: String,
)

internal class AiAssistantRequestException(
    val userMessage: String,
    cause: Throwable? = null,
) : IllegalStateException(userMessage, cause)

internal interface ShoppingAssistantProvider {
    suspend fun comparePart(request: AiPartComparisonRequest): AiPartComparisonResult
}

internal fun shoppingAssistantProvider(): ShoppingAssistantProvider {
    val firebaseEnabled = BuildConfig.USE_FIREBASE_AI_LOGIC
    val firebaseConfigured = BuildConfig.HAS_FIREBASE_CONFIG

    return if (firebaseEnabled && firebaseConfigured) {
        Log.d(AI_ASSISTANT_LOG_TAG, "Selected provider: Firebase AI Logic")
        FirebaseAiShoppingAssistantProvider()
    } else {
        val reason = buildString {
            append("Selected provider: backend")
            append(" (USE_FIREBASE_AI_LOGIC=")
            append(firebaseEnabled)
            append(", HAS_FIREBASE_CONFIG=")
            append(firebaseConfigured)
            append(")")
        }
        Log.d(AI_ASSISTANT_LOG_TAG, reason)
        BackendShoppingAssistantProvider()
    }
}

internal class BackendShoppingAssistantProvider(
    private val baseUrl: String = BuildConfig.AI_ASSISTANT_BASE_URL,
) : ShoppingAssistantProvider {

    override suspend fun comparePart(request: AiPartComparisonRequest): AiPartComparisonResult =
        withContext(Dispatchers.IO) {
            val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
            val endpoint = "$normalizedBaseUrl/parts/compare"
            val requestJson = request.toJson().toString()

            Log.d(AI_ASSISTANT_LOG_TAG, "Base URL: $normalizedBaseUrl")
            Log.d(AI_ASSISTANT_LOG_TAG, "HTTP method: POST")
            Log.d(AI_ASSISTANT_LOG_TAG, "Endpoint: $endpoint")
            Log.d(AI_ASSISTANT_LOG_TAG, "Sending request: $requestJson")

            if (normalizedBaseUrl.isBlank()) {
                throw AiAssistantRequestException(
                    "Nie udało się połączyć z asystentem AI.\nSprawdź konfigurację backendu."
                )
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            try {
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestJson)
                }

                val responseCode = connection.responseCode
                Log.d(AI_ASSISTANT_LOG_TAG, "Response status: $responseCode")

                val responseStream = if (responseCode >= 400) {
                    connection.errorStream ?: connection.inputStream
                } else {
                    connection.inputStream
                }

                val responseBody = responseStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                Log.d(AI_ASSISTANT_LOG_TAG, "Raw response: $responseBody")

                if (responseCode >= 400) {
                    throw AiAssistantRequestException(
                        "Nie udało się połączyć z asystentem AI.\nSprawdź konfigurację backendu."
                    )
                }

                val parsed = responseBody.toAiPartComparisonResponse()
                Log.d(AI_ASSISTANT_LOG_TAG, "Parsed response: $parsed")
                return@withContext parsed
            } catch (exception: Exception) {
                Log.e(AI_ASSISTANT_LOG_TAG, "Request failed", exception)
                when (exception) {
                    is AiAssistantRequestException -> throw exception
                    else -> throw AiAssistantRequestException(
                        "Nie udało się połączyć z asystentem AI.\nSprawdź konfigurację backendu.",
                        exception
                    )
                }
            }
        }
}

internal class FirebaseAiShoppingAssistantProvider : ShoppingAssistantProvider {

    override suspend fun comparePart(request: AiPartComparisonRequest): AiPartComparisonResult =
        withContext(Dispatchers.IO) {
            val prompt = buildGeminiPartComparePrompt(request)
            Log.d(AI_ASSISTANT_LOG_TAG, "Firebase AI Logic prompt: $prompt")

            try {
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(FIREBASE_AI_MODEL)

                val response = model.generateContent(prompt)
                val rawText = response.text.orEmpty()
                Log.d(AI_ASSISTANT_LOG_TAG, "Raw Gemini text response: $rawText")

                val parsed = rawText.toAiPartComparisonResponse(
                    fallbackRecommendation = INVALID_JSON_RECOMMENDATION
                )
                Log.d(AI_ASSISTANT_LOG_TAG, "Parsed Variant C response: $parsed")
                return@withContext parsed
            } catch (exception: Exception) {
                Log.e(AI_ASSISTANT_LOG_TAG, "Firebase AI Logic request failed", exception)
                return@withContext safeEmptyVariantCResponse(
                    recommendation = INVALID_JSON_RECOMMENDATION
                )
            }
        }
}

internal fun buildGeminiPartComparePrompt(request: AiPartComparisonRequest): String {
    val oemValue = request.oem.ifBlank { "brak pewnego numeru OEM" }
    val vehicleValue = request.vehicle.orEmpty().ifBlank { "brak danych o aucie" }
    val partnerPriceValue = request.partnerPrice?.let(::formatCurrency) ?: "brak ceny partnera OEM"

    return """
Jestes prostym asystentem zakupowym dla aplikacji Garage Assistant.

Znajdz alternatywne oferty dla tej konkretnej czesci samochodowej.

Dane:
- OEM: $oemValue
- Nazwa czesci: ${request.partName}
- Auto: $vehicleValue
- Cena partnera OEM: $partnerPriceValue

Zasady:
- Szukaj alternatywnych ofert dla tego dokladnego numeru OEM.
- Preferuj tylko dokladne dopasowania po OEM.
- Jesli nie masz pewnosci co do zgodnosci OEM, napisz to w polu "note".
- Odpowiedz tylko poprawnym JSON.
- Nie uzywaj markdown.
- Nie dodawaj zadnych wyjasnien poza JSON.
- Jesli nie znajdziesz wiarygodnych ofert, zwroc pusty zestaw ofert i zostaw OEM Partner jako najbezpieczniejszy wybor.
- Odpowiedz po polsku.

Wymagany format JSON:
{
  "offers": [
    {
      "source": "Allegro",
      "price": 59.90,
      "currency": "PLN",
      "url": "https://...",
      "note": "Found by exact OEM number"
    }
  ],
  "best_price": {
    "source": "Allegro",
    "price": 59.90
  },
  "best_safe_choice": {
    "source": "OEM Partner",
    "reason": "Confirmed compatibility and OEM diagrams"
  },
  "saving": {
    "amount": 7.23,
    "percentage": 10.77
  },
  "recommendation": "Short recommendation in Polish."
}

Jesli nie znajdziesz pewnych ofert, zwroc:
{
  "offers": [],
  "best_price": null,
  "best_safe_choice": {
    "source": "OEM Partner",
    "reason": "Brak pewnych alternatywnych ofert. Partner OEM pozostaje najbezpieczniejszym wyborem."
  },
  "saving": null,
  "recommendation": "Nie znaleziono wiarygodnych ofert dla tego numeru OEM. Użyj przycisków Allegro, Ceneo lub iParts, aby sprawdzić rynek ręcznie."
}
""".trimIndent()
}

internal fun ShoppingListItem.marketSearchUrlFor(source: MarketSearchSource): String {
    val query = marketSearchQuery()
    val encoded = query.urlEncode()
    return when (source) {
        MarketSearchSource.Allegro -> "https://allegro.pl/listing?string=$encoded"
        MarketSearchSource.Ceneo -> "https://www.ceneo.pl/;szukaj-$encoded"
        MarketSearchSource.IParts -> "https://www.iparts.pl/szukaj/$encoded.html"
    }
}

internal fun ShoppingListItem.marketSearchQuery(): String {
    val normalizedOem = normalizedOemCandidate()
    if (normalizedOem != null) return normalizedOem

    return listOf(manufacturer.trim(), name.trim())
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { name.trim().ifBlank { "BMW czesci" } }
}

internal fun ShoppingListItem.hasKnownOemNumber(): Boolean = normalizedOemCandidate() != null

internal fun ShoppingListItem.partnerCompatibilityLabel(): String =
    if (hasKnownOemNumber()) {
        "Pewne dopasowanie po OEM"
    } else {
        "Sprawdz numer OEM na schemacie"
    }

internal fun ShoppingListItem.toAiComparisonRequest(vehicle: Vehicle): AiPartComparisonRequest =
    AiPartComparisonRequest(
        oem = normalizedOemCandidate().orEmpty(),
        partName = name.trim(),
        vehicle = listOf(vehicle.displayName, vehicle.engine.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() },
        partnerPrice = parsePriceAmount(price)
    )

internal fun aiSavingLabel(partnerPrice: Double?, alternativePrice: Double): String? {
    if (partnerPrice == null || partnerPrice <= alternativePrice) return null
    return formatCurrency(partnerPrice - alternativePrice)
}

internal fun formatCurrency(value: Double, currency: String = "PLN"): String =
    String.format(Locale.US, "%.2f %s", value, currency).replace('.', ',')

internal fun formatPercentage(value: Double): String =
    String.format(Locale.US, "%.1f%%", value).replace('.', ',')

internal fun safeEmptyVariantCResponse(
    recommendation: String = "",
): AiPartComparisonResult =
    AiPartComparisonResult(
        offers = emptyList(),
        bestPrice = null,
        bestSafeChoice = null,
        saving = null,
        recommendation = recommendation
    )

private fun ShoppingListItem.normalizedOemCandidate(): String? {
    val normalized = partNumber.trim()
    if (normalized.isBlank()) return null
    val blockedValues = setOf(
        "do ustalenia",
        "do uzupelnienia",
        "brak",
        "unknown"
    )
    return normalized.takeIf { candidate ->
        blockedValues.none { candidate.equals(it, ignoreCase = true) }
    }
}

private fun AiPartComparisonRequest.toJson(): JSONObject = JSONObject().apply {
    put("oem", oem)
    put("part_name", partName)
    if (vehicle.isNullOrBlank()) {
        put("vehicle", JSONObject.NULL)
    } else {
        put("vehicle", vehicle)
    }
    if (partnerPrice == null) {
        put("partner_price", JSONObject.NULL)
    } else {
        put("partner_price", partnerPrice)
    }
}

private fun String.toAiPartComparisonResponse(
    fallbackRecommendation: String = "",
): AiPartComparisonResult {
    val jsonText = extractJsonObjectText(this) ?: return safeEmptyVariantCResponse(fallbackRecommendation)
    return runCatching {
        val json = JSONObject(jsonText)
        val bestPriceJson = json.optJSONObject("best_price")
        val bestSafeChoiceJson = json.optJSONObject("best_safe_choice")
        val savingJson = json.optJSONObject("saving")

        AiPartComparisonResult(
            offers = json.optJSONArray("offers").toAiOffers(),
            bestPrice = bestPriceJson?.toAiBestPrice(),
            bestSafeChoice = bestSafeChoiceJson?.toAiBestSafeChoice(),
            saving = savingJson?.toAiSaving(),
            recommendation = json.optString("recommendation").ifBlank { fallbackRecommendation }
        )
    }.getOrElse { error ->
        Log.e(AI_ASSISTANT_LOG_TAG, "Failed to parse Variant C JSON", error)
        safeEmptyVariantCResponse(fallbackRecommendation)
    }
}

private fun extractJsonObjectText(raw: String): String? {
    val normalized = raw
        .replace("```json", "", ignoreCase = true)
        .replace("```", "")
        .trim()

    val firstBrace = normalized.indexOf('{')
    val lastBrace = normalized.lastIndexOf('}')
    if (firstBrace == -1 || lastBrace == -1 || lastBrace <= firstBrace) return null
    return normalized.substring(firstBrace, lastBrace + 1)
}

private fun JSONArray?.toAiOffers(): List<AiPartOffer> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val offer = optJSONObject(index) ?: continue
            val source = offer.optString("source").ifBlank { "Rynek" }
            val price = offer.optDouble("price", Double.NaN)
            if (price.isNaN()) continue
            add(
                AiPartOffer(
                    source = source,
                    price = price,
                    currency = offer.optString("currency").ifBlank { "PLN" },
                    url = offer.optString("url"),
                    note = offer.optString("note").ifBlank { "Oferta znaleziona przez AI." }
                )
            )
        }
    }
}

private fun String.toBackendErrorMessage(): String =
    runCatching {
        JSONObject(this).optString("message")
            .ifBlank { JSONObject(this).optString("error") }
            .ifBlank { this }
    }.getOrDefault(this)
        .ifBlank { "Backend AI nie odpowiedzial poprawnie." }

private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

private fun JSONObject.toAiBestPrice(): AiBestPrice? {
    val source = optString("source").trim()
    val price = optDouble("price", Double.NaN)
    if (source.isBlank() || price.isNaN()) return null
    return AiBestPrice(source = source, price = price)
}

private fun JSONObject.toAiBestSafeChoice(): AiBestSafeChoice? {
    val source = optString("source").trim()
    val reason = optString("reason").trim()
    if (source.isBlank() && reason.isBlank()) return null
    return AiBestSafeChoice(
        source = source.ifBlank { "Brak danych" },
        reason = reason
    )
}

private fun JSONObject.toAiSaving(): AiSaving? {
    val amount = optDouble("amount", Double.NaN)
    val percentage = optDouble("percentage", Double.NaN)
    if (amount.isNaN() && percentage.isNaN()) return null
    return AiSaving(
        amount = if (amount.isNaN()) 0.0 else amount,
        percentage = if (percentage.isNaN()) 0.0 else percentage
    )
}

private const val AI_ASSISTANT_LOG_TAG = "AI_ASSISTANT"
private const val FIREBASE_AI_MODEL = "gemini-2.5-flash"
private const val INVALID_JSON_RECOMMENDATION =
    "AI nie zwróciło poprawnych danych. Spróbuj ponownie później lub użyj ręcznego wyszukiwania."
