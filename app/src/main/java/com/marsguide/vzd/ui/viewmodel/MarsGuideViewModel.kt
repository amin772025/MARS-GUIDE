package com.marsguide.vzd.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marsguide.vzd.data.local.MarsGuideDatabase
import com.marsguide.vzd.data.local.NetworkConnectivityMonitor
import com.marsguide.vzd.data.local.entity.SavedLocationEntity
import com.marsguide.vzd.data.local.entity.SavedRouteEntity
import com.marsguide.vzd.data.remote.AuthenticMarsDatasets
import com.marsguide.vzd.data.repository.*
import com.marsguide.vzd.domain.model.*
import com.marsguide.vzd.domain.repository.*
import com.marsguide.vzd.domain.usecase.SearchMarsUseCase
import com.marsguide.vzd.domain.usecase.SearchResultItem
import com.marsguide.vzd.util.SoundManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MarsGuideViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MarsGuideDatabase.getInstance(application)
    private val dao = db.marsGuideDao()
    private val connectivityMonitor = NetworkConnectivityMonitor(application)

    private val locationRepository: MarsLocationRepository = MarsLocationRepositoryImpl()
    private val imageRepository: MarsImageRepository = MarsImageRepositoryImpl()
    private val missionRepository: MarsMissionRepository = MarsMissionRepositoryImpl()
    private val routeRepository: MarsRouteRepository = MarsRouteRepositoryImpl(dao)
    private val savedRepository: SavedRepository = SavedRepositoryImpl(dao)
    private val searchUseCase = SearchMarsUseCase()

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val locations: StateFlow<List<MarsLocation>> = locationRepository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthenticMarsDatasets.LOCATIONS)

    val missions: StateFlow<List<MarsMission>> = flowOf(missionRepository.getAllMissions())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthenticMarsDatasets.MISSIONS)

    val rovers: StateFlow<List<MarsRover>> = flowOf(missionRepository.getAllRovers())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthenticMarsDatasets.ROVERS)

    val scienceTargets: StateFlow<List<ScienceTarget>> = flowOf(AuthenticMarsDatasets.SCIENCE_TARGETS)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthenticMarsDatasets.SCIENCE_TARGETS)

    val savedLocations: StateFlow<List<SavedLocationEntity>> = savedRepository.getSavedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRoutes: StateFlow<List<SavedRouteEntity>> = savedRepository.getSavedRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLocation = MutableStateFlow<MarsLocation?>(null)
    val selectedLocation: StateFlow<MarsLocation?> = _selectedLocation.asStateFlow()

    private val _selectedCoordinate = MutableStateFlow<MarsCoordinate?>(null)
    val selectedCoordinate: StateFlow<MarsCoordinate?> = _selectedCoordinate.asStateFlow()

    private val _imagesForLocation = MutableStateFlow<List<MarsImage>>(emptyList())
    val imagesForLocation: StateFlow<List<MarsImage>> = _imagesForLocation.asStateFlow()

    private val _isLoadingImages = MutableStateFlow(false)
    val isLoadingImages: StateFlow<Boolean> = _isLoadingImages.asStateFlow()

    private val _imageError = MutableStateFlow<String?>(null)
    val imageError: StateFlow<String?> = _imageError.asStateFlow()

    // Route Planning State
    private val _plannerStart = MutableStateFlow<MarsLocation?>(null)
    val plannerStart: StateFlow<MarsLocation?> = _plannerStart.asStateFlow()

    private val _plannerDest = MutableStateFlow<MarsLocation?>(null)
    val plannerDest: StateFlow<MarsLocation?> = _plannerDest.asStateFlow()

    private val _plannedRoute = MutableStateFlow<MarsRoute?>(null)
    val plannedRoute: StateFlow<MarsRoute?> = _plannedRoute.asStateFlow()

    // Global Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow("All")
    val searchFilter: StateFlow<String> = _searchFilter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()

    init {
        // Default initial selected location is Jezero Crater
        _selectedLocation.value = AuthenticMarsDatasets.LOCATIONS.firstOrNull()
        _selectedCoordinate.value = _selectedLocation.value?.coordinate
        loadImagesForLocation("Jezero")
    }

    fun selectLocation(location: MarsLocation) {
        _selectedLocation.value = location
        _selectedCoordinate.value = location.coordinate
        SoundManager.playTargetLock()
        loadImagesForLocation(location.name)
    }

    fun selectCoordinate(coordinate: MarsCoordinate) {
        _selectedCoordinate.value = coordinate
        // Find if an existing location matches closely
        val match = locations.value.find { loc ->
            val dist = kotlin.math.hypot(
                loc.coordinate.latitude - coordinate.latitude,
                loc.coordinate.normalizedLongitude - coordinate.normalizedLongitude
            )
            dist < 0.5
        }
        _selectedLocation.value = match
        if (match != null) {
            loadImagesForLocation(match.name)
        }
    }

    fun loadImagesForLocation(locationName: String) {
        viewModelScope.launch {
            _isLoadingImages.value = true
            _imageError.value = null
            val result = imageRepository.getImagesForLocation(locationName)
            result.onSuccess {
                _imagesForLocation.value = it
                _isLoadingImages.value = false
            }.onFailure {
                _imageError.value = it.message
                _isLoadingImages.value = false
            }
        }
    }

    fun isLocationSaved(id: String): Flow<Boolean> = savedRepository.isLocationSaved(id)

    fun toggleSaveLocation(location: MarsLocation, isAlreadySaved: Boolean) {
        viewModelScope.launch {
            SoundManager.playClick()
            if (isAlreadySaved) {
                savedRepository.removeSavedLocation(location.id)
            } else {
                savedRepository.saveLocation(location)
            }
        }
    }

    fun deleteSavedLocation(id: String) {
        viewModelScope.launch {
            savedRepository.removeSavedLocation(id)
        }
    }

    // Route Planner methods
    fun setPlannerStart(location: MarsLocation) {
        _plannerStart.value = location
        calculateRouteIfReady()
    }

    fun setPlannerDest(location: MarsLocation) {
        _plannerDest.value = location
        calculateRouteIfReady()
    }

    private fun calculateRouteIfReady() {
        val start = _plannerStart.value
        val dest = _plannerDest.value
        if (start != null && dest != null) {
            val route = routeRepository.calculateRoute(start, dest, null)
            _plannedRoute.value = route
            SoundManager.playNavigationClick()
        } else {
            _plannedRoute.value = null
        }
    }

    fun saveCurrentRoute() {
        val route = _plannedRoute.value ?: return
        viewModelScope.launch {
            routeRepository.saveRoute(route)
        }
    }

    fun deleteSavedRoute(id: String) {
        viewModelScope.launch {
            routeRepository.deleteSavedRoute(id)
        }
    }

    // Search methods
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        executeSearch()
    }

    fun updateSearchFilter(filter: String) {
        _searchFilter.value = filter
        executeSearch()
    }

    private fun executeSearch() {
        val results = searchUseCase(query = _searchQuery.value, filterType = _searchFilter.value)
        _searchResults.value = results
    }
}
