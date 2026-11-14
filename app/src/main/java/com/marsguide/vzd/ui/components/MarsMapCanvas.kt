package com.marsguide.vzd.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.marsguide.vzd.domain.model.*
import com.marsguide.vzd.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Vibrant NASA MOLA Topographic Map Canvas.
 * Implements high-resolution colorized elevation tiles and tactical HUD overlays.
 */
@Composable
fun MarsMapCanvas(
    locations: List<MarsLocation>,
    rovers: List<MarsRover>,
    scienceTargets: List<ScienceTarget> = emptyList(),
    missions: List<MarsMission> = emptyList(),
    activeRoute: MarsRoute? = null,
    selectedCoordinate: MarsCoordinate?,
    selectedLocation: MarsLocation?,
    onLocationSelected: (MarsLocation) -> Unit,
    onCoordinateSelected: (MarsCoordinate) -> Unit,
    showBaseLayer: Boolean = true,
    showTopographyLayer: Boolean = true,
    showGridLayer: Boolean = true,
    showFeaturesLayer: Boolean = true,
    showLandingSitesLayer: Boolean = true,
    showRoversLayer: Boolean = true,
    showScienceTargetsLayer: Boolean = true,
    showRouteLayer: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var centerLat by remember { mutableDoubleStateOf(selectedCoordinate?.latitude ?: 0.0) }
    var centerLon by remember { mutableDoubleStateOf(selectedCoordinate?.longitude ?: 0.0) }
    var currentZoom by remember { mutableDoubleStateOf(3.0) }

    val latestOnLocationSelected by rememberUpdatedState(onLocationSelected)
    val latestOnCoordinateSelected by rememberUpdatedState(onCoordinateSelected)

    remember {
        val prefs = context.getSharedPreferences("osmdroid_mars", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().cacheMapTileCount = 1000
        Configuration.getInstance().cacheMapTileOvershoot = 50
    }

    val overlayKey = remember(
        locations, rovers, scienceTargets, missions, activeRoute, selectedCoordinate, selectedLocation,
        showTopographyLayer, showFeaturesLayer, showLandingSitesLayer, showRoversLayer,
        showScienceTargetsLayer, showRouteLayer
    ) {
        listOf(
            locations.size, rovers.size, scienceTargets.size, missions.size,
            activeRoute?.id, selectedCoordinate?.latitude, selectedCoordinate?.longitude,
            selectedLocation?.id, showTopographyLayer, showFeaturesLayer,
            showLandingSitesLayer, showRoversLayer, showScienceTargetsLayer, showRouteLayer
        )
    }

    Box(modifier = modifier.fillMaxSize().background(SpaceBlack)) {
        // 1. OSMDROID MAP VIEWPORT
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setUseDataConnection(showBaseLayer)
                    tileProvider.tileCache.setAutoEnsureCapacity(true)

                    setTileSource(
                        object : XYTileSource(
                            "NASA_Mars_MOLA_Color",
                            0, 8, 256, ".png",
                            arrayOf("https://trek.nasa.gov/tiles/Mars/EQ/mola_color/1.0.0/default/default0/")
                        ) {
                            override fun getTileURLString(pTileIndex: Long): String {
                                val zoom = MapTileIndex.getZoom(pTileIndex)
                                val x = MapTileIndex.getX(pTileIndex)
                                val y = MapTileIndex.getY(pTileIndex)
                                return "$baseUrl$zoom/$y/$x.png"
                            }
                        }
                    )

                    controller.setZoom(currentZoom)
                    controller.setCenter(GeoPoint(centerLat, if (centerLon > 180.0) centerLon - 360.0 else centerLon))
                    isVerticalMapRepetitionEnabled = false
                    isHorizontalMapRepetitionEnabled = false

                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            centerLat = mapCenter.latitude
                            centerLon = ((mapCenter.longitude % 360.0) + 360.0) % 360.0
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                            currentZoom = zoomLevelDouble
                            return true
                        }
                    })
                }
            },
            update = { mapView ->
                mapView.overlayManager.tilesOverlay.setColorFilter(
                    if (showTopographyLayer) null else android.graphics.ColorMatrixColorFilter(
                        android.graphics.ColorMatrix().apply { setSaturation(0.2f) }
                    )
                )

                if (mapView.tag == overlayKey) return@AndroidView
                mapView.tag = overlayKey
                mapView.overlays.clear()

                // Tap on flat map -> select coordinate
                mapView.overlays.add(
                    MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            val normLon = ((p.longitude % 360.0) + 360.0) % 360.0
                            latestOnCoordinateSelected(MarsCoordinate(p.latitude.coerceIn(-89.9, 89.9), normLon))
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean {
                            val normLon = ((p.longitude % 360.0) + 360.0) % 360.0
                            latestOnCoordinateSelected(MarsCoordinate(p.latitude.coerceIn(-89.9, 89.9), normLon))
                            return true
                        }
                    })
                )

                // Active Geodesic Route Polyline
                if (showRouteLayer && activeRoute != null && activeRoute.waypoints.size >= 2) {
                    val polyline = Polyline(mapView).apply {
                        setPoints(activeRoute.waypoints.map { wp ->
                            val lon = if (wp.normalizedLongitude > 180.0) wp.normalizedLongitude - 360.0 else wp.normalizedLongitude
                            GeoPoint(wp.latitude, lon)
                        })
                        outlinePaint.color = AndroidColor.rgb(255, 112, 67)
                        outlinePaint.strokeWidth = 6f
                        title = activeRoute.name
                    }
                    mapView.overlays.add(polyline)
                }

                // Geological Features
                if (showFeaturesLayer) {
                    locations.forEach { loc ->
                        val lon = if (loc.coordinate.normalizedLongitude > 180.0) loc.coordinate.normalizedLongitude - 360.0 else loc.coordinate.normalizedLongitude
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(loc.coordinate.latitude, lon)
                            title = "${loc.name} (${loc.type})"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { _, _ ->
                                latestOnLocationSelected(loc)
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Historic Landing Sites
                if (showLandingSitesLayer) {
                    missions.forEach { mission ->
                        val coord = mission.landingCoordinate ?: return@forEach
                        val lon = if (coord.normalizedLongitude > 180.0) coord.normalizedLongitude - 360.0 else coord.normalizedLongitude
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(coord.latitude, lon)
                            title = "${mission.name} — ${mission.landingSiteName ?: mission.target}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Active & Historic Rovers
                if (showRoversLayer) {
                    rovers.forEach { rover ->
                        val lon = if (rover.landingCoordinate.normalizedLongitude > 180.0) rover.landingCoordinate.normalizedLongitude - 360.0 else rover.landingCoordinate.normalizedLongitude
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(rover.landingCoordinate.latitude, lon)
                            title = "${rover.name} (${rover.operationalStatus})"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(marker)
                    }
                }

                // Curated Science Targets
                if (showScienceTargetsLayer) {
                    scienceTargets.forEach { target ->
                        val normLon = ((target.longitude % 360.0) + 360.0) % 360.0
                        val lon = if (normLon > 180.0) normLon - 360.0 else normLon
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(target.latitude, lon)
                            title = "${target.name} (${target.instrument})"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mapView.overlays.add(marker)
                    }
                }

                mapView.invalidate()
            }
        )

        // 2. TACTICAL HUD OVERLAY (Digital Graticule & Scan Lines)
        if (showGridLayer) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val cx = width / 2f
                val cy = height / 2f

                // Subtle Global Scan Lines
                for (y in 0 until height.toInt() step 10) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.025f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }

                // Digital Graticule (Corner Brackets)
                val bracketSize = 30.dp.toPx()
                val margin = 16.dp.toPx()

                drawLine(ScienceTeal.copy(0.4f), Offset(margin, margin), Offset(margin + bracketSize, margin), 2f)
                drawLine(ScienceTeal.copy(0.4f), Offset(margin, margin), Offset(margin, margin + bracketSize), 2f)

                drawLine(ScienceTeal.copy(0.4f), Offset(width - margin, margin), Offset(width - margin - bracketSize, margin), 2f)
                drawLine(ScienceTeal.copy(0.4f), Offset(width - margin, margin), Offset(width - margin, margin + bracketSize), 2f)

                drawLine(ScienceTeal.copy(0.4f), Offset(margin, height - margin), Offset(margin + bracketSize, height - margin), 2f)
                drawLine(ScienceTeal.copy(0.4f), Offset(margin, height - margin), Offset(margin, height - margin - bracketSize), 2f)

                drawLine(ScienceTeal.copy(0.4f), Offset(width - margin, height - margin), Offset(width - margin - bracketSize, height - margin), 2f)
                drawLine(ScienceTeal.copy(0.4f), Offset(width - margin, height - margin), Offset(width - margin, height - margin - bracketSize), 2f)

                // Central Tactical Reticle
                drawCircle(
                    color = ScienceTeal.copy(alpha = 0.6f),
                    radius = 24.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = MarsOrangeBright.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy)
                )

                val gap = 12.dp.toPx()
                val len = 48.dp.toPx()
                drawLine(ScienceTeal.copy(alpha = 0.5f), Offset(cx - len, cy), Offset(cx - gap, cy), 1.dp.toPx())
                drawLine(ScienceTeal.copy(alpha = 0.5f), Offset(cx + gap, cy), Offset(cx + len, cy), 1.dp.toPx())
                drawLine(ScienceTeal.copy(alpha = 0.5f), Offset(cx, cy - len), Offset(cx, cy - gap), 1.dp.toPx())
                drawLine(ScienceTeal.copy(alpha = 0.5f), Offset(cx, cy + gap), Offset(cx, cy + len), 1.dp.toPx())
            }
        }

        // 3. LIVE COORDINATE HUD
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(8.dp)
        ) {
            Text(
                text = "PLANETOCENTRIC NAV SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                color = ScienceTeal,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text("LATITUDE", color = Color(0xFF94A3B8), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${"%.4f".format(centerLat)}°",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("LONGITUDE", color = Color(0xFF94A3B8), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${"%.4f".format(centerLon)}°",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MAGNIFICATION: ${"%.2f".format(currentZoom)}x",
                color = MarsOrangeBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
