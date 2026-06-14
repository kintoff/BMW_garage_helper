package pl.garage.bmwassistant.ui.screens

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehiclePartsStorageParsingTest {

    @Test
    fun parsePriceAmountHandlesPolishFormats() {
        assertEquals(1234.56, parsePriceAmount("1 234,56 PLN")!!, 0.001)
        assertEquals(99.0, parsePriceAmount("99 zl")!!, 0.001)
        assertNull(parsePriceAmount("brak ceny"))
    }

    @Test
    fun normalizeOfferPriceFormatsNumericValueToPln() {
        assertEquals("1234,50 PLN", normalizeOfferPrice("1234,5"))
        assertEquals("brak PLN", normalizeOfferPrice("brak"))
    }

    @Test
    fun normalizeAllegroOfferInputAcceptsShortOfferPath() {
        assertEquals(
            "https://test-oferty-123",
            normalizeAllegroOfferInput("test-oferty-123")
        )
        assertEquals(
            "https://allegro.pl/oferta/test-oferty-123",
            normalizeAllegroOfferInput("/oferta/test-oferty-123")
        )
        assertEquals(
            "https://m.allegro.pl/oferta/test-oferty-123",
            normalizeAllegroOfferInput("m.allegro.pl/oferta/test-oferty-123")
        )
    }

    @Test
    fun parseAllegroOfferDetailsReadsJsonLdProduct() {
        val html = """
            <html><head>
            <script type="application/ld+json">
            {
              "@type":"Product",
              "name":"Wahacz tylny BMW",
              "image":["https://img.example.com/1.jpg"],
              "offers":{
                "price":"199.99",
                "priceCurrency":"PLN",
                "url":"https://allegro.pl/oferta/wahacz-123"
              }
            }
            </script>
            </head></html>
        """.trimIndent()

        val details = parseAllegroOfferDetails(html, "https://fallback.example.com")

        assertNotNull(details)
        assertEquals("Wahacz tylny BMW", details?.title)
        assertEquals("199,99 PLN", details?.price)
        assertEquals("https://img.example.com/1.jpg", details?.imageUrl)
        assertEquals("https://allegro.pl/oferta/wahacz-123", details?.offerUrl)
    }

    @Test
    fun parseAllegroOfferDetailsFallsBackToMetaTags() {
        val html = """
            <html><head>
            <meta property="og:title" content="Tuleja wahacza BMW"/>
            <meta property="product:price:amount" content="249.90"/>
            <meta property="og:image" content="https://img.example.com/tuleja.jpg"/>
            </head></html>
        """.trimIndent()

        val details = parseAllegroOfferDetails(html, "https://fallback.example.com/oferta")

        assertNotNull(details)
        assertEquals("Tuleja wahacza BMW", details?.title)
        assertEquals("249,90 PLN", details?.price)
        assertEquals("https://img.example.com/tuleja.jpg", details?.imageUrl)
        assertEquals("https://fallback.example.com/oferta", details?.offerUrl)
    }

    @Test
    fun jsonArrayOfferParsingReturnsFirstValidOffer() {
        val array = JSONArray()
            .put(JSONObject().put("@type", "BreadcrumbList"))
            .put(
                JSONObject()
                    .put("@type", "Offer")
                    .put("name", "Amortyzator BMW")
                    .put("price", "350")
                    .put("priceCurrency", "PLN")
                    .put("url", "https://allegro.pl/oferta/amortyzator-1")
            )

        val details = array.toAllegroOfferDetails("https://fallback.example.com")

        assertNotNull(details)
        assertEquals("Amortyzator BMW", details?.title)
        assertEquals("350,00 PLN", details?.price)
    }

    @Test
    fun extractHtmlMetaContentDecodesHtmlEntities() {
        val html = """<meta property="og:title" content="BMW &amp; Lemforder"/>"""

        assertEquals("BMW & Lemforder", extractHtmlMetaContent(html, "property", "og:title"))
    }

    @Test
    fun allegroUrlDetectionRecognizesValidOfferLinks() {
        assertTrue("https://allegro.pl/oferta/test".isAllegroOfferUrl())
        assertTrue("/oferta/test".isAllegroOfferUrl())
    }
}
