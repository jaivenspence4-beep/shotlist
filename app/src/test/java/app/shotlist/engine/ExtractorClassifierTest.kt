package app.shotlist.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorClassifierTest {

    private val flyer = """
        SUMMER NIGHTS FESTIVAL
        Live music · food trucks
        Sep 14 · Doors 8:30 PM
        123 Harbor Blvd
        Tickets $25 at the door · RSVP now
    """.trimIndent()

    @Test
    fun `flyer produces an event finding with when and address`() {
        val s = Extractor.extract(flyer)
        val findings = Classifier.classify(1L, flyer, s)
        val event = findings.first { it.type == "EVENT" }
        assertNotNull(event.whenAt)
        assertTrue(event.payload.contains("Harbor", ignoreCase = true))
        assertTrue(event.confidence >= 0.8f)
        assertEquals("SUMMER NIGHTS FESTIVAL", event.title)
    }

    @Test
    fun `wifi card extracts ssid and password`() {
        val text = "Guest WiFi: BlueHouse\nPassword: sunfl0wer99"
        val s = Extractor.extract(text)
        assertEquals("BlueHouse" to "sunfl0wer99", s.wifi)
        val f = Classifier.classify(1L, text, s).first { it.type == "WIFI" }
        assertEquals("sunfl0wer99", f.payload)
    }

    @Test
    fun `gate code is found and not confused with wifi password`() {
        val text = "Gate code #4821 — building B"
        val s = Extractor.extract(text)
        assertEquals(listOf("4821"), s.codes)
    }

    @Test
    fun `product screenshot needs both price and shop words`() {
        val product = "Trail Runner XT\n$129.99\nAdd to cart · Free shipping"
        val s = Extractor.extract(product)
        val f = Classifier.classify(1L, product, s).first { it.type == "PRODUCT" }
        assertEquals(12999L, f.amountCents)

        val notProduct = "I owe you $20 lol"
        val none = Classifier.classify(1L, notProduct, Extractor.extract(notProduct))
        assertTrue(none.none { it.type == "PRODUCT" })
    }

    @Test
    fun `deadline text classifies as deadline`() {
        val text = "Registration closes: payment due 9/14/26"
        val f = Classifier.classify(1L, text, Extractor.extract(text))
            .first { it.whenAt != null }
        assertEquals("DEADLINE", f.type)
    }

    @Test
    fun `meme text produces no findings`() {
        val meme = "bro really said that 😭😭 no way lmaooo"
        assertTrue(Classifier.classify(1L, meme, Extractor.extract(meme)).isEmpty())
    }

    @Test
    fun `ups tracking number is detected`() {
        val s = Extractor.extract("Shipped! 1Z999AA10123456784")
        assertEquals(listOf("1Z999AA10123456784"), s.tracking)
    }

    @Test
    fun `status bar clock text is never an event`() {
        // The exact false positive from the S26 field test.
        val statusBar = "10:34 Mon, Aug 31 M•\nsome app content with no dates"
        val findings = Classifier.classify(1L, statusBar, Extractor.extract(statusBar))
        assertTrue("clock text produced $findings", findings.none { it.whenAt != null })
    }

    @Test
    fun `a bare date with no semantic anchor produces no event`() {
        val text = "Jaçobian\nAugust 27\n2026 2:15"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.type == "EVENT" || it.type == "DEADLINE" })
    }

    @Test
    fun `one screenshot yields at most one finding per type`() {
        val text = "Gate code: 1111\nDoor code: 2222\nAccess code: 3333"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertEquals(1, findings.count { it.type == "CODE" })
    }

    // ---- Round-2 regressions: exact false positives from the S26 field test ----

    @Test
    fun `social profile with Follow and a date is not an event`() {
        val text = "7 Follow\nAug 12 00:48\nLive replay"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.type == "EVENT" || it.type == "DEADLINE" })
    }

    @Test
    fun `chat timestamp today is not an event without strong anchors`() {
        val text = "EONON\ntoday 00:49\nok sounds good"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.whenAt != null })
    }

    @Test
    fun `masked password fields do not become wifi cards`() {
        val text = "Sign in\nNetwork: CoffeeShop5G\nPassword: ••••••••"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.type == "WIFI" })
    }

    @Test
    fun `a search url with one shop word is not a product`() {
        val text = "google.com/search?q=deals\n$49.99 order today"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.type == "PRODUCT" })
    }

    @Test
    fun `titles are never urls or ui fragments`() {
        val text = "% zillow.com/homedet\nOpen house Sep 14 2:00 PM\nRSVP for a tour\n123 Harbor Blvd"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        val event = findings.firstOrNull { it.type == "EVENT" }
        if (event != null) {
            assertTrue("bad title: ${event.title}", !event.title.contains("zillow.com"))
        }
    }

    // ---- Round-3 regressions: exact junk rows from the build-29 device DB ----

    @Test
    fun `social counter lines never become titles or findings`() {
        val text = "7 Follow\nexpires Aug 30\nsale ends order checkout % off"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.title == "7 Follow" })
    }

    @Test
    fun `mangled domain fragments are not titles`() {
        val text = "o der.littlecaesars.com +\ndue 9/2 payment due"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.title.contains(".com") })
    }

    @Test
    fun `link-heavy pages need a third product anchor`() {
        val text = "25 tiktok.comview/prod\nhttps://a.co/x\nwww.shop.com/y\n" +
            "$19.99 order · free shipping"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.type == "PRODUCT" })
    }

    @Test
    fun `explicit past dates never become cards`() {
        // Build-36 device row: a letter dated with an explicit year in the past.
        val text = "Dear Stacey, Brian, Jasmine, and Flint,\n" +
            "payment due August 23, 2020\nregister by then"
        val findings = Classifier.classify(1L, text, Extractor.extract(text))
        assertTrue(findings.none { it.whenAt != null })
    }

    @Test
    fun `snippets read like sentences not raw matches`() {
        val s = Extractor.extract(flyer)
        val event = Classifier.classify(1L, flyer, s).first { it.type == "EVENT" }
        assertTrue("snippet was: ${event.snippet}",
            event.snippet.contains("Sep") && event.snippet.contains("Harbor"))
    }
}
