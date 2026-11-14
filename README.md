# MARS//GUIDE — Martian Map & Interplanetary Survival Explorer

**2026 NASA International Space Apps Challenge**  
**Challenge Category:** *Interplanetary Survival Guide: Martian Map*  
**Application ID:** `com.marsguide.vzd`  
**Platform:** Native Android (Kotlin, Jetpack Compose, Material Design 3)

---

## 1. Executive Summary & Pitch

**MARS//GUIDE** is a native, offline-first planetary science and cartographic application built for the 2026 NASA Space Apps Challenge. Designed to provide human explorers and planetary scientists with reliable spatial intelligence on the Martian surface, the application combines authentic NASA Planetary Data System (PDS) datasets, USGS Astrogeology gazetteer records, and JPL robotic exploration telemetry into a single, cohesive, high-performance Android experience.

Unlike simple web wrappers or mock applications, **MARS//GUIDE** is built entirely on native Android architecture, implementing real geodesy calculations on the Martian spheroid ($R_{Mars} = 3,389.5\text{ km}$), an open interactive aerocentric map renderer, local Room database caching, and multi-language support including English and Arabic (with complete right-to-left layout alignment).

---

## 2. Planetary Science Context & Cartographic Standards

### Coordinate Reference System (IAU/IAG 2000 Mars Aerocentric Datum)
- **Latitude:** Planetocentric latitude in decimal degrees $[-90.0^\circ, +90.0^\circ]$, where positive values denote the Northern hemisphere and negative values denote the Southern hemisphere.
- **Longitude:** East Longitude convention in decimal degrees $[0.0^\circ, 360.0^\circ\text{ E}]$, referencing the Mars prime meridian passing through Airy-0 crater.
- **Elevation Baseline (Areoid):** Martian elevations are referenced to the zero-elevation gravitational equipotential surface defined by the Mars Orbiter Laser Altimeter (MOLA) aboard the Mars Global Surveyor (MGS) spacecraft. The datum corresponds to an equatorial radius $A = 3,396.19\text{ km}$ at which the mean atmospheric pressure is $610.5\text{ Pa}$ (the triple point of water).

---

## 3. How NASA Data Is Used

The application integrates data directly derived from public NASA and partner planetary science archives:
1. **NASA Planetary Data System (PDS) Geosciences Node:**
   - Mission Experiment Gridded Data Record (MEGDR) topographic elevations (Dataset ID: `MGS-M-MOLA-5-MEGDR-L3-V1.0`).
2. **USGS Astrogeology Science Center / International Astronomical Union (IAU):**
   - Gazetteer of Planetary Nomenclature for verified feature names, crater diameters, central coordinates, and quadrangle designations.
3. **NASA / Jet Propulsion Laboratory (JPL-Caltech):**
   - Mars 2020 (Perseverance & Ingenuity) landing coordinates, traverses, and instruments (`M2020-M-MASTCAMZ-2-EDR-V1.0`).
   - Mars Science Laboratory (Curiosity) operations in Gale Crater (`MSL-M-CHEMCAM-2-EDR-V1.0`).
   - Mars Exploration Rovers (Spirit & Opportunity) traverse records and science discoveries.
4. **MRO HiRISE & CTX (University of Arizona / NASA PDS):**
   - High-resolution orbital imagery and Digital Terrain Models (DTMs).
5. **NASA Image and Video Library REST API:**
   - Asynchronous retrieval of authentic mission photography and observation archives.

---

## 4. Technical Architecture

The codebase strictly adheres to modern Android Clean Architecture and MVVM design patterns:

```
app/src/main/java/com/example/
├── data/
│   ├── local/
│   │   ├── dao/MarsGuideDao.kt          # Room Database Access Objects
│   │   ├── entity/Entities.kt           # SavedLocation, SavedRoute, CachedTarget
│   │   ├── MarsGuideDatabase.kt         # Room Database Provider
│   │   └── NetworkConnectivityMonitor.kt# Reactive StateFlow network monitor
│   ├── remote/
│   │   ├── api/NasaPlanetaryService.kt  # Retrofit API interface
│   │   ├── dto/NasaDtos.kt              # JSON Data Transfer Objects
│   │   ├── AuthenticMarsDatasets.kt     # Verified NASA/PDS science records
│   │   ├── MarsImageRemoteDataSource.kt # Asynchronous image retrieval
│   │   └── NasaHttpClient.kt            # Centralized OkHttp configuration
│   └── repository/
│       └── RepositoriesImpl.kt          # Repositories for locations, routes, saved items
├── domain/
│   ├── model/Models.kt                  # MarsCoordinate, MarsLocation, MarsMission, etc.
│   ├── repository/Repositories.kt       # Clean repository contracts
│   └── usecase/UseCases.kt              # Great-Circle math, Global Search
├── ui/
│   ├── components/                      # MarsMapCanvas, StatusPills, TelemetryChips
│   ├── navigation/NavRoutes.kt          # Compose Navigation Routes
│   ├── screens/                         # Explore, Map, Analysis, Missions, Planner, Saved, About
│   ├── theme/                           # Color.kt, Theme.kt (MarsGuideTheme), Type.kt
│   └── viewmodel/MarsGuideViewModel.kt  # Unidirectional Data Flow ViewModel
└── util/
    └── MarsMath.kt                      # Spherical geodesy, Great-Circle distance, bearing
```

---

## 5. Mathematical Geodesy Engine

Navigation on Mars is modeled using spherical trigonometry on the Mars volumetric mean sphere ($R_{Mars} = 3,389.5\text{ km}$):

- **Great-Circle Distance (Haversine Formula):**
  $$\Delta\phi = \phi_2 - \phi_1, \quad \Delta\lambda = \lambda_2 - \lambda_1$$
  $$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
  $$c = 2 \cdot \text{atan2}(\sqrt{a}, \sqrt{1-a})$$
  $$d = R_{Mars} \cdot c$$

- **Initial True Bearing:**
  $$\theta = \text{atan2}\left(\sin(\Delta\lambda)\cos(\phi_2), \cos(\phi_1)\sin(\phi_2) - \sin(\phi_1)\cos(\phi_2)\cos(\Delta\lambda)\right)$$

- **Intermediate Geodesic Waypoints:**
  Interpolated along the great circle arc using SLERP (Spherical Linear Interpolation) in 3D Cartesian planetary space $(X, Y, Z)$.

> **Navigational Disclaimer:** Trajectories represent approximate geodesic great-circle lines on the Mars reference sphere. They do not account for local escarpments, boulder fields, slope steepness, or human life-support viability.

---

## 6. Offline Support & Local Persistence

- **Room Database:** All user-saved waypoints, planned trajectories, and bookmarks are persistently stored locally using SQLite / Room.
- **Resilience:** The application functions fully offline. When network access is unavailable, a prominent telemetry status pill notifies the user, and all cached USGS/PDS data remains accessible.
- **Zero Inappropriate Permissions:** The app requests only `INTERNET` and `ACCESS_NETWORK_STATE`. No tracking, GPS, camera, or personal data permissions are requested.

---

## 7. Localization & Accessibility

- **Bilingual Support:** Full string resource parity in **English** (`res/values/strings.xml`) and **Arabic** (`res/values-ar/strings.xml`).
- **RTL Layout:** Full support for Right-To-Left bidirectional layouts in Jetpack Compose for Arabic speakers.
- **Accessibility:** Minimum touch targets comply with 48dp Material 3 guidelines; explicit semantic content descriptions for screen readers (TalkBack).

---

## 8. NASA Space Apps Challenge Disclaimer

> **Independent Project Disclaimer:**  
> This application was created for the 2026 NASA International Space Apps Challenge under the track *"Interplanetary Survival Guide: Martian Map"*. It is an independent open-data submission and is not officially affiliated with, endorsed by, or sponsored by NASA or any other space agency.
