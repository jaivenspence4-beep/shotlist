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
}
