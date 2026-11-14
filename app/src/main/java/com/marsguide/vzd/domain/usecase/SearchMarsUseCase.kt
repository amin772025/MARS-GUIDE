package com.marsguide.vzd.domain.usecase

import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.domain.model.DataClassification

enum class SearchResultType {
    LOCATION,
    MISSION,
    ROVER,
    SCIENCE_TARGET,
    COORDINATE
}

data class SearchResultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SearchResultType,
    val classification: DataClassification,
    val details: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rawItem: Any? = null
)

class SearchMarsUseCase {
    operator fun invoke(query: String, filterType: String = "All"): List<SearchResultItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResultItem>()

        // 1. Check if user entered coordinate pair: e.g. "18.44, 77.45" or "18.44 N, 77.45 E"
        val coordPattern = Regex("""^(-?\d+(?:\.\d+)?)\s*[, ]\s*(-?\d+(?:\.\d+)?)$""")
        val coordMatch = coordPattern.find(trimmed)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1].toDoubleOrNull()
            val lon = coordMatch.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0) {
                var normLon = lon % 360.0
                if (normLon < 0) normLon += 360.0
                results.add(
                    SearchResultItem(
                        id = "coord_${lat}_${normLon}",
                        title = "Coordinate: ${String.format(java.util.Locale.US, "%.4f°", lat)}, ${String.format(java.util.Locale.US, "%.4f°", normLon)}",
                        subtitle = "Direct Martian surface coordinate",
                        type = SearchResultType.COORDINATE,
                        classification = DataClassification.APPLICATION_CALCULATED,
                        details = "User input planetary surface coordinate. Tap to center map.",
                        latitude = lat,
                        longitude = normLon
                    )
                )
            }
        }

        // 2. Search Locations
        if (filterType == "All" || filterType == "Locations") {
            AuthenticMarsDatasets.LOCATIONS.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.type.contains(trimmed, ignoreCase = true) ||
                it.quadrangle?.contains(trimmed, ignoreCase = true) == true ||
                it.geologicalContext.contains(trimmed, ignoreCase = true)
            }.forEach { loc ->
                results.add(
                    SearchResultItem(
                        id = loc.id,
                        title = loc.name,
                        subtitle = "${loc.type} • ${loc.quadrangle ?: "Mars"}",
                        type = SearchResultType.LOCATION,
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        details = loc.description,
                        latitude = loc.coordinate.latitude,
                        longitude = loc.coordinate.normalizedLongitude,
                        rawItem = loc
                    )
                )
            }
        }

        // 3. Search Missions
        if (filterType == "All" || filterType == "Missions") {
            AuthenticMarsDatasets.MISSIONS.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.type.contains(trimmed, ignoreCase = true) ||
                it.status.contains(trimmed, ignoreCase = true)
            }.forEach { mission ->
                results.add(
                    SearchResultItem(
                        id = mission.id,
                        title = mission.name,
                        subtitle = "${mission.type} • Status: ${mission.status}",
                        type = SearchResultType.MISSION,
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        details = mission.description,
                        latitude = mission.landingCoordinate?.latitude,
                        longitude = mission.landingCoordinate?.normalizedLongitude,
                        rawItem = mission
                    )
                )
            }
        }

        // 4. Search Rovers
        if (filterType == "All" || filterType == "Rovers") {
            AuthenticMarsDatasets.ROVERS.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.mission.contains(trimmed, ignoreCase = true) ||
                it.landingLocation.contains(trimmed, ignoreCase = true)
            }.forEach { rover ->
                results.add(
                    SearchResultItem(
                        id = rover.id,
                        title = rover.name,
                        subtitle = "Rover • ${rover.operationalStatus}",
                        type = SearchResultType.ROVER,
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        details = "Landed: ${rover.landingDate} at ${rover.landingLocation}. Traverse: ${rover.totalDistanceTraveledKm ?: 0.0} km.",
                        latitude = rover.landingCoordinate.latitude,
                        longitude = rover.landingCoordinate.normalizedLongitude,
                        rawItem = rover
                    )
                )
            }
        }

        // 5. Search Science Targets
        if (filterType == "All" || filterType == "Science") {
            AuthenticMarsDatasets.SCIENCE_TARGETS.filter {
                it.name.contains(trimmed, ignoreCase = true) ||
                it.targetType.contains(trimmed, ignoreCase = true) ||
                it.mission.contains(trimmed, ignoreCase = true) ||
                it.description.contains(trimmed, ignoreCase = true)
            }.forEach { target ->
                results.add(
                    SearchResultItem(
                        id = target.id,
                        title = target.name,
                        subtitle = "${target.targetType} • Mission: ${target.mission}",
                        type = SearchResultType.SCIENCE_TARGET,
                        classification = DataClassification.STATIC_SOURCE_DERIVED,
                        details = target.description,
                        latitude = target.latitude,
                        longitude = target.longitude,
                        rawItem = target
                    )
                )
            }
        }

        return results
    }
}
