package com.marsguide.vzd

import com.marsguide.vzd.util.VisualQuality
import org.junit.Assert.*
import org.junit.Test

class SpacecraftExperienceTest {

    @Test
    fun visualQuality_hasDistinctLabelsAndMultipliers() {
        val high = VisualQuality.HIGH
        val medium = VisualQuality.MEDIUM
        val low = VisualQuality.LOW

        assertEquals("HIGH", high.label)
        assertEquals("MEDIUM", medium.label)
        assertEquals("LOW", low.label)

        assertTrue(high.particleMultiplier > medium.particleMultiplier)
        assertTrue(medium.particleMultiplier > low.particleMultiplier)
        assertTrue(high.enableParallax)
        assertTrue(medium.enableParallax)
        assertFalse(low.enableParallax)
    }

    @Test
    fun visualQuality_fromPreferencesFallback() {
        assertEquals(VisualQuality.HIGH, VisualQuality.valueOf("HIGH"))
        assertEquals(VisualQuality.MEDIUM, VisualQuality.valueOf("MEDIUM"))
        assertEquals(VisualQuality.LOW, VisualQuality.valueOf("LOW"))
    }
}
