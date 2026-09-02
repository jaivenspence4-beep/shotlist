package app.shotlist.entitlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementTest {
    private val day = 24L * 60L * 60L * 1_000L
    private val now = 100L * day

    @Test
    fun `free Recall includes exactly 30 days and rejects older shots`() {
        assertTrue(Entitlement.FREE.includesRecallShot(now - 30L * day, now))
        assertFalse(Entitlement.FREE.includesRecallShot(now - 30L * day - 1L, now))
    }

    @Test
    fun `pro Recall includes full history`() {
        assertTrue(Entitlement.PRO.includesRecallShot(Long.MIN_VALUE, now))
    }

    @Test
    fun `free vault stops before fourth item while pro has no limit`() {
        assertTrue(Entitlement.FREE.canAddVaultItem(2))
        assertFalse(Entitlement.FREE.canAddVaultItem(3))
        assertTrue(Entitlement.PRO.canAddVaultItem(Int.MAX_VALUE))
    }

    @Test
    fun `stored entitlement parsing fails closed to free`() {
        assertEquals(Entitlement.PRO, Entitlement.fromStored("PRO"))
        assertEquals(Entitlement.FREE, Entitlement.fromStored("FREE"))
        assertEquals(Entitlement.FREE, Entitlement.fromStored("unknown"))
        assertEquals(Entitlement.FREE, Entitlement.fromStored(null))
    }

    @Test
    fun `release stays unlocked until billing is live`() {
        assertFalse(BILLING_LIVE)
        assertEquals(Entitlement.PRO, Entitlement.releaseDefault())
        assertEquals(Entitlement.PRO, Entitlement.releaseDefault(billingLive = false))
    }

    @Test
    fun `release fails closed to free when billing goes live`() {
        assertEquals(Entitlement.FREE, Entitlement.releaseDefault(billingLive = true))
    }
}
