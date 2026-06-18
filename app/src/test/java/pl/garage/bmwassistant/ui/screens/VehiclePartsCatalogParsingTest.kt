package pl.garage.bmwassistant.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehiclePartsCatalogParsingTest {

    @Test
    fun parseCzescidobmwResultsReadsAnalyticsItemsAndImages() {
        val html = """
            <script>
            var googleAnalyticsData = {
              "items": [
                {
                  "item_id": "33326763092",
                  "item_name": "Tuleja wahacza tylnego",
                  "item_brand": "Lemforder",
                  "price": 249.99
                }
              ]
            };
            </script>
            <div class="c-product__panel" data-article-code="33326763092">
              <a class="c-product__name" href="/produkt/tuleja-wahacza-tylnego-33326763092">Produkt</a>
              <a class="c-product-image__link" href="/images/tuleja.jpg"></a>
            </div>
            <div id="hook_seodescriptionbottomhook"></div>
        """.trimIndent()

        val results = parseCzescidobmwResults(
            html = html,
            searchUrl = "https://czescidobmw.pl/wyniki-wyszukiwania?q=33326763092",
            searchedOemPartNumber = "33326763092"
        )

        assertEquals(1, results.size)
        val item = results.single()
        assertEquals("33326763092", item.manufacturerPartNumber)
        assertEquals("Tuleja wahacza tylnego", item.name)
        assertEquals("Lemforder", item.manufacturer)
        assertEquals("249,99 PLN".replace(',', '.'), item.shopPrice.replace(',', '.'))
        assertEquals("https://czescidobmw.pl/produkt/tuleja-wahacza-tylnego-33326763092", item.shopUrl)
        assertEquals("https://czescidobmw.pl/images/tuleja.jpg", item.imageUrl)
    }

    @Test
    fun parseCzescidobmwResultsReturnsEmptyWhenAnalyticsMissing() {
        assertTrue(
            parseCzescidobmwResults(
                html = "<html>brak danych</html>",
                searchUrl = "https://czescidobmw.pl",
                searchedOemPartNumber = "33326763092"
            ).isEmpty()
        )
    }

    @Test
    fun productImageUrlsByArticleCodeSkipsBmwPlaceholderSvg() {
        val html = """
            <div class="c-product__panel" data-article-code="33326763092">
              <a class="c-product-image__link" href="/images/BMW.svg"></a>
            </div>
            <div id="hook_seodescriptionbottomhook"></div>
        """.trimIndent()

        assertTrue(productImageUrlsByArticleCode(html).isEmpty())
    }

    @Test
    fun productImageUrlsByArticleCodeFallsBackToLazyImageSource() {
        val html = """
            <div class="c-product__panel" data-article-code="33326763092">
              <script>CustomLazySrc: '/images/lazy.jpg'</script>
            </div>
            <div id="hook_seodescriptionbottomhook"></div>
        """.trimIndent()

        assertEquals(
            "https://czescidobmw.pl/images/lazy.jpg",
            productImageUrlsByArticleCode(html)["33326763092"]
        )
    }

    @Test
    fun absoluteCzescidobmwUrlBuildsAbsolutePaths() {
        assertEquals("https://czescidobmw.pl/images/tuleja.jpg", absoluteCzescidobmwUrl("/images/tuleja.jpg"))
        assertEquals("https://czescidobmw.pl/images/tuleja.jpg", absoluteCzescidobmwUrl("images/tuleja.jpg"))
        assertEquals("https://cdn.example.com/tuleja.jpg", absoluteCzescidobmwUrl("https://cdn.example.com/tuleja.jpg"))
    }

    @Test
    fun parsePartLabelTextDetectsBmwOemAndLemforderNumber() {
        val parsed = parsePartLabelText(
            """
            BMW
            33 32 6 763 092
            LEMFORDER
            34567 01
            """.trimIndent()
        )

        assertEquals("33326763092", parsed.oemPartNumber)
        assertEquals("34567 01", parsed.manufacturerPartNumber)
        assertEquals("LEMFORDER", parsed.manufacturer)
    }

    @Test
    fun parsePartLabelTextDetectsFebiNumber() {
        val parsed = parsePartLabelText("FEBI BILSTEIN NR. 12345 BMW 31 12 6 789 111")

        assertEquals("31126789111", parsed.oemPartNumber)
        assertEquals("12345", parsed.manufacturerPartNumber)
        assertEquals("FEBI", parsed.manufacturer)
    }

    @Test
    fun parsePartLabelTextFallsBackToGenericManufacturerNumber() {
        val parsed = parsePartLabelText("BMW 31 12 6 789 111 Sachs 998877")

        assertEquals("31126789111", parsed.oemPartNumber)
        assertEquals("998877", parsed.manufacturerPartNumber)
        assertEquals("BMW", parsed.manufacturer)
    }

    @Test
    fun findBmwOemPartNumberNormalizesCommonOcrMistakes() {
        assertEquals("33326763092", findBmwOemPartNumber("33 32 6 763 O92"))
        assertNull(findBmwOemPartNumber("brak numeru"))
    }

    @Test
    fun normalizeLabelTextRemovesNoiseAndPolishCharacters() {
        assertEquals(
            "ZA OL BMW 33 32 6 763 092",
            normalizeLabelText("Zażółć BMW 33 32 6 763 092")
        )
    }

    @Test
    fun helperFunctionsNormalizeNumbersAndHtmlEntities() {
        assertEquals("123456", "12-34/56".onlyDigits())
        assertEquals("10021", "IOO2L".normalizeOcrNumber())
        assertEquals("BMW \"Lemforder\" & test", decodeHtmlCompat("BMW &quot;Lemforder&quot; &amp; test"))
    }

    @Test
    fun imageSearchUrlForEncodesQuery() {
        val url = imageSearchUrlFor("33326763092", "Lemforder")

        assertTrue(url.contains("tbm=isch"))
        assertTrue(url.contains("33326763092+BMW+Lemforder"))
    }
}
