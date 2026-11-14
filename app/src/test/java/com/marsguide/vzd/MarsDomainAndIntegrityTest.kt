package com.marsguide.vzd

import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.domain.model.DataClassification
import com.marsguide.vzd.domain.model.ElevationSource
import com.marsguide.vzd.domain.model.MarsCoordinate
import com.marsguide.vzd.domain.usecase.SearchMarsUseCase
import com.marsguide.vzd.domain.usecase.SearchResultType
import com.marsguide.vzd.util.MarsMath
import org.junit.Assert.*
import org.junit.Test

class MarsDomainAndIntegrityTest {

    private val searchUseCase = SearchMarsUseCase()

    @Test
    fun search_findsLocationByName() {
        val results = searchUseCase("Jezero")
        assertTrue("Should find Jezero crater", results.any { it.title.contains("Jezero", ignoreCase = true) })
        val jezeroResult = results.first { it.title.contains("Jezero", ignoreCase = true) }
        assertEquals(SearchResultType.LOCATION, jezeroResult.type)
        assertEquals(DataClassification.STATIC_SOURCE_DERIVED, jezeroResult.classification)
    }

    @Test
    fun search_parsesDirectCoordinateInput() {
        val results = searchUseCase("18.44, 77.45")
        assertTrue("Should produce a coordinate search result", results.any { it.type == SearchResultType.COORDINATE })
        val coordResult = results.first { it.type == SearchResultType.COORDINATE }
        assertNotNull(coordResult.latitude)
        assertNotNull(coordResult.longitude)
        assertEquals(18.44, coordResult.latitude!!, 0.001)
        assertEquals(77.45, coordResult.longitude!!, 0.001)
    }

    @Test
    fun search_filtersRoversProperly() {
        val results = searchUseCase("Perseverance", filterType = "Rovers")
        assertTrue("Should find Perseverance rover", results.any { it.title.contains("Perseverance") })
        val rover = results.first { it.title.contains("Perseverance") }
        assertEquals(SearchResultType.ROVER, rover.type)
    }

    @Test
    fun search_filtersMissionsProperly() {
        val results = searchUseCase("Curiosity", filterType = "Missions")
        assertTrue("Should find Curiosity/MSL mission", results.isNotEmpty())
    }

    @Test
    fun search_filtersScienceTargets() {
        val results = searchUseCase("Kodiak", filterType = "Science")
        assertTrue("Should find Kodiak delta target", results.any { it.title.contains("Kodiak") })
        assertEquals(SearchResultType.SCIENCE_TARGET, results.first().type)
    }

    @Test
    fun authenticDatasets_locationsHaveValidCoordinates() {
        val locations = AuthenticMarsDatasets.LOCATIONS
        assertTrue("Must contain curated scientific locations", locations.size >= 10)
        locations.forEach { loc ->
            assertTrue("Location ${loc.name} latitude must be in [-90, 90]", loc.coordinate.latitude in -90.0..90.0)
            assertTrue("Location ${loc.name} longitude must be in [0, 360]", loc.coordinate.normalizedLongitude in 0.0..360.0)
            assertNotNull("Location ${loc.name} must have a valid NASA/USGS source", loc.source)
            assertTrue("Source name must not be blank", loc.source.name.isNotBlank())
        }
    }

    @Test
    fun authenticDatasets_roversHavePositiveTraverseDistance() {
        val rovers = AuthenticMarsDatasets.ROVERS
        assertTrue("Must have known historical and active Mars rovers", rovers.isNotEmpty())
        rovers.forEach { rover ->
            assertTrue("Rover ${rover.name} landing latitude must be valid", rover.landingCoordinate.latitude in -90.0..90.0)
            val dist = rover.totalDistanceTraveledKm ?: 0.0
            assertTrue("Rover ${rover.name} traverse distance must be >= 0", dist >= 0.0)
        }
    }

    @Test
    fun dataClassification_labelsAndIntegrity() {
        // Verify all 4 honest classifications have distinct labels
        val live = DataClassification.REAL_LIVE_API
        val staticSource = DataClassification.STATIC_SOURCE_DERIVED
        val calculated = DataClassification.APPLICATION_CALCULATED
        val estimated = DataClassification.ESTIMATED_APPROXIMATION

        assertEquals("REAL LIVE DATA", live.badgeLabel)
        assertEquals("STATIC SOURCE-DERIVED", staticSource.badgeLabel)
        assertEquals("APPLICATION-CALCULATED", calculated.badgeLabel)
        assertEquals("ESTIMATED APPROXIMATION", estimated.badgeLabel)
    }

    @Test
    fun bearing_withinZeroToThreeSixtyDegrees() {
        val p1 = MarsCoordinate(latitude = 45.0, longitude = 270.0)
        val p2 = MarsCoordinate(latitude = -30.0, longitude = 45.0)
        val bearing = MarsMath.calculateInitialBearingDegrees(p1, p2)
        assertTrue("Bearing $bearing must be in [0, 360)", bearing >= 0.0 && bearing < 360.0)
    }
}
