package com.example.commuterx_java

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.result.SearchResult
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import com.mapbox.common.location.AccuracyAuthorization
import com.mapbox.common.location.Location as MapboxLocation
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.common.location.LocationServiceObserver
import com.mapbox.common.location.PermissionStatus
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import android.Manifest
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate


class FullScreenMapActivity : AppCompatActivity(), LocationServiceObserver, MapboxNavigationObserver {

    private lateinit var mapView: MapView
    private lateinit var searchView: CustomSearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchEngine: SearchEngine
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var detailsCard: MaterialCardView
    private lateinit var detailsPlaceNameTextView: TextView
    private lateinit var detailsAddressTextView: TextView
    private lateinit var scrim: View
    private var selectedLocationPoint: Point? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private lateinit var locationService: LocationService
    private lateinit var navigationLocationProvider: NavigationLocationProvider
    private val locationObserver = MyLocationObserver()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var mapboxMap: MapboxMap



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("LocationDebug", "onCreate started")
        Log.e("LocationDebug", "Mapbox access token: ${getString(R.string.mapbox_access_token)}")

        searchView = findViewById(R.id.fullScreenSearchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        detailsCard = findViewById(R.id.detailsCard)
        detailsPlaceNameTextView = findViewById(R.id.detailsPlaceNameTextView)
        detailsAddressTextView = findViewById(R.id.detailsAddressTextView)
        scrim = findViewById(R.id.scrim)
        navigationLocationProvider = NavigationLocationProvider()


        try {

            setContentView(R.layout.full_screen_map)
            Log.e("LocationDebug", "setContentView called")


            initializeViews()
            setupMapboxNavigation()
            initializeViews()
            initializeMapboxComponents()
            setupLocationServices()
            setupSearch()
            setupRecyclerView()
            setupLocationUpdates()



            searchView.requestFocus()
            searchView.postDelayed({
                showKeyboard(searchView)
            }, 200)

            Log.e("LocationDebug", "onCreate completed successfully")

        } catch (e: Exception) {
            Log.e("LocationDebug", "Exception in onCreate: ${e.message}", e)
        }
    }

    private fun setupMapboxNavigation() {
        if (!MapboxNavigationApp.isSetup()) {
            Log.e("LocationDebug", "Setting up MapboxNavigationApp")
            MapboxNavigationApp.setup {
                NavigationOptions.Builder(this)
                    .build()
            }
            Log.e("LocationDebug", "MapboxNavigationApp setup complete")
        } else {
            Log.e("LocationDebug", "MapboxNavigationApp was already set up")
        }

        // Retry mechanism to obtain MapboxNavigation instance
        var retryCount = 0
        val maxRetries = 3
        val handler = Handler(Looper.getMainLooper())

        fun retryObtainingNavigation() {
            mapboxNavigation = MapboxNavigationApp.current()
            if (mapboxNavigation == null && retryCount < maxRetries) {
                Log.e("LocationDebug", "Failed to obtain MapboxNavigation instance, retrying in 1 second (Attempt ${retryCount + 1}/$maxRetries)")
                retryCount++
                handler.postDelayed({ retryObtainingNavigation() }, 1000) // Retry after 1 second
            } else if (mapboxNavigation == null) {
                Log.e("LocationDebug", "Failed to obtain MapboxNavigation instance after $maxRetries attempts")
            } else {
                Log.e("LocationDebug", "MapboxNavigation obtained successfully")
                initializeMapboxComponents()
            }
        }

        retryObtainingNavigation()
    }

    private fun initializeMapboxComponents() {
        mapboxMap = mapView.getMapboxMap()
        Log.e("LocationDebug", "MapboxMap obtained")

        Log.e("LocationDebug", "MapboxNavigation instance is valid")

        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(mapboxMap, mapView.camera, viewportDataSource)
        Log.e("LocationDebug", "ViewportDataSource and NavigationCamera initialized")

        navigationLocationProvider = NavigationLocationProvider()
        mapView.location.setLocationProvider(navigationLocationProvider)

        MapboxNavigationApp.attach(this)
        Log.e("LocationDebug", "Successfully attached to MapboxNavigationApp")

        setInitialMapStyle()

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            Log.e("LocationDebug", "Map style loaded")
            Handler(Looper.getMainLooper()).postDelayed({
                enableLocationComponent()
                initializeNavigation()
                centerOnUserLocation()
            }, 1000) // 1 second delay
        }
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.fullScreenMapView)
        searchView = findViewById(R.id.fullScreenSearchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        detailsCard = findViewById(R.id.detailsCard)
        detailsPlaceNameTextView = findViewById(R.id.detailsPlaceNameTextView)
        detailsAddressTextView = findViewById(R.id.detailsAddressTextView)
        scrim = findViewById(R.id.scrim)
        Log.e("LocationDebug", "Views initialized")
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.e("LocationDebug", "FusedLocationClient initialized")

        locationService = LocationServiceFactory.getOrCreate()

        checkLocationPermission()
    }

    @SuppressLint("MissingPermission")
    private fun initializeNavigation() {
        Log.e("LocationDebug", "Initializing navigation")
        if (mapboxNavigation == null) {
            Log.e("LocationDebug", "mapboxNavigation is null in initializeNavigation()")
            return
        }

        try {
            mapboxNavigation?.registerLocationObserver(locationObserver)
            Log.e("LocationDebug", "Location observer registered")

            mapboxNavigation?.startTripSession()
            Log.e("LocationDebug", "Trip session started")
        } catch (e: Exception) {
            Log.e("LocationDebug", "Error in initializeNavigation", e)
        }
    }

    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            viewportDataSource.onRouteChanged(routeUpdateResult.navigationRoutes.first())
            viewportDataSource.evaluate()
        } else {
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationDebug", "Location permission is granted")
            enableLocationComponent()
        } else {
            Log.e("LocationDebug", "Requesting location permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun enableLocationComponent() {
        Log.e("LocationDebug", "Enabling location component")
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
        mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = true

            // Create and set the location puck
            locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.from(R.drawable.mapbox_puck),
            )
        }
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        Log.e("LocationDebug", "Location component enabled: ${mapView.location.enabled}")
    }

    @SuppressLint("MissingPermission")
    private fun centerOnUserLocation() {
        Log.e("LocationDebug", "Attempting to center on user location")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0
            fastestInterval = 0
            numUpdates = 1
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.e("LocationDebug", "Location update received")
                locationResult.lastLocation?.let { location ->
                    Log.e("LocationDebug", "Centering on location: ${location.latitude}, ${location.longitude}")
                    val point = Point.fromLngLat(location.longitude, location.latitude)
                    updateCamera(point)
                } ?: Log.e("LocationDebug", "Location is null")
                fusedLocationClient.removeLocationUpdates(this)
            }
        }, Looper.getMainLooper())
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private fun onCameraTrackingDismissed() {
        mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        Log.e("LocationDebug", "Indicator position changed: ${point.latitude()}, ${point.longitude()}")
        updateCamera(point)
        viewportDataSource.onLocationChanged(point.toLocation())
        viewportDataSource.evaluate()
    }

    private fun updateCamera(point: Point, zoom: Double = 17.0, animate: Boolean = true) {
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(zoom)
            .build()

        if (animate) {
            mapView.camera.easeTo(
                cameraOptions,
                mapAnimationOptions {
                    duration(300)
                }
            )
        } else {
            mapView.getMapboxMap().setCamera(cameraOptions)
        }
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)
    }

    private fun setInitialMapStyle() {
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            Log.e("LocationDebug", "Map style loaded")
            // Set initial camera position with high zoom
            updateCamera(mapView.getMapboxMap().cameraState.center, 19.0, false)
            enableLocationComponent()
            initializeNavigation()
            centerOnUserLocation()
        }
    }

    private fun Point.toLocation(): MapboxLocation {
        return MapboxLocation.Builder()
            .latitude(latitude())
            .longitude(longitude())
            .build()
    }

    private fun ImageHolder.Companion.from(drawable: Drawable): ImageHolder {
        return from(drawable.toBitmap())
    }


    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return bitmap

        val bitmap = if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        android.graphics.Canvas(bitmap).apply {
            setBounds(0, 0, width, height)
            draw(this)
        }

        return bitmap
    }

    private fun setupLocationUpdates() {
        Log.e("LocationDebug", "Setting up location updates")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.create().setInterval(5000),
                object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            Log.e("FusedLocation", "New location from fused provider: $location")
                        }
                    }
                },
                null
            )
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("LocationDebug", "Location permission granted")
                    centerOnUserLocation()
                } else {
                    Log.e("LocationDebug", "Location permission denied")
                }
            }
        }
    }



    private fun setupSearch() {
        try {
            // Initialize SearchEngine first
            searchEngine = SearchEngine.createSearchEngine(
                SearchEngineSettings()
            )
            Log.e("SearchDebug", "SearchEngine initialized")

            val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            searchEditText?.apply {
                setTextColor(Color.BLACK)
                setHintTextColor(Color.GRAY)
                background = null
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (::searchEngine.isInitialized) {
                        query?.let { performSearch(it) }
                    } else {
                        Log.e("SearchDebug", "SearchEngine not initialized")
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        searchResultsRecyclerView.visibility = View.GONE
                        detailsCard.visibility = View.GONE
                    } else if (::searchEngine.isInitialized) {
                        performSearch(newText)
                    }
                    return true
                }
            })
            Log.e("SearchDebug", "Search setup completed")
        } catch (e: Exception) {
            Log.e("SearchDebug", "Error setting up search: ${e.message}", e)
        }
    }



    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onBackPressed() {
        when {
            detailsCard.visibility == View.VISIBLE -> hideDetailsCard()
            searchResultsRecyclerView.visibility == View.VISIBLE -> {
                searchResultsRecyclerView.visibility = View.GONE
                searchView.setQuery("", false)
            }
            else -> super.onBackPressed()
        }
    }

    private fun showDetailsCard() {
        scrim.visibility = View.VISIBLE
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(300).start()

        detailsCard.visibility = View.VISIBLE
        val slideUp = ObjectAnimator.ofFloat(detailsCard, "translationY", detailsCard.height.toFloat(), 0f)
        slideUp.duration = 300
        slideUp.interpolator = DecelerateInterpolator()
        slideUp.start()
    }

    private fun hideDetailsCard() {
        scrim.animate().alpha(0f).setDuration(300).withEndAction {
            scrim.visibility = View.GONE
        }.start()

        val slideDown = ObjectAnimator.ofFloat(detailsCard, "translationY", 0f, detailsCard.height.toFloat())
        slideDown.duration = 300
        slideDown.interpolator = AccelerateInterpolator()
        slideDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                detailsCard.visibility = View.GONE
            }
        })
        slideDown.start()
    }

    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { searchSuggestion ->
            searchEngine.select(searchSuggestion, object : SearchSelectionCallback {
                override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                    result.coordinate?.let { coordinate ->
                        Log.e("SearchDebug", "Search result coordinates: ${coordinate.latitude()}, ${coordinate.longitude()}")
                        selectedLocationPoint = Point.fromLngLat(coordinate.longitude(), coordinate.latitude())
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(selectedLocationPoint)
                                .zoom(14.0)
                                .build()
                        )
                        Log.e("SearchDebug", "Camera moved to: ${selectedLocationPoint?.latitude()}, ${selectedLocationPoint?.longitude()}")
                    }
                    searchResultsRecyclerView.visibility = View.GONE
                    searchView.background = ContextCompat.getDrawable(this@FullScreenMapActivity, R.drawable.search_background)

                    detailsPlaceNameTextView.text = result.name
                    detailsAddressTextView.text = result.address?.formattedAddress() ?: "Address not available"

                    showDetailsCard()
                }

                override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {
                    // Not implemented
                }

                override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                    // Not implemented
                }

                override fun onError(e: Exception) {
                    // Handle error
                }
            })
        }
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsRecyclerView.adapter = searchResultsAdapter
    }

    private fun performSearch(query: String) {
        val options = SearchOptions.Builder()
            .limit(5)
            .build()

        searchEngine.search(query, options, object : SearchSuggestionsCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                runOnUiThread {
                    searchResultsAdapter.updateResults(suggestions)
                    searchResultsRecyclerView.visibility = View.VISIBLE
                    searchView.background = ContextCompat.getDrawable(this@FullScreenMapActivity, R.drawable.search_background_top)
                    detailsCard.visibility = View.GONE
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    // Handle error
                }
            }
        })
    }




    override fun onAvailabilityChanged(isAvailable: Boolean) {
        Log.e("LocationService", "Availability changed: $isAvailable")
    }

    override fun onPermissionStatusChanged(permission: PermissionStatus) {
        Log.e("LocationService", "Permission status changed: $permission")
    }

    override fun onAccuracyAuthorizationChanged(authorization: AccuracyAuthorization) {
        Log.e("LocationService", "Accuracy authorization changed: $authorization")
    }

    private fun updateCameraToLocation(location: MapboxLocation) {
        Log.e("LocationDebug", "Updating camera to location: ${location.latitude}, ${location.longitude}")
        val point = Point.fromLngLat(location.longitude, location.latitude)
        updateCamera(point)
    }

    private inner class MyLocationObserver : LocationObserver {
        override fun onNewRawLocation(rawLocation: MapboxLocation) {
            Log.e("LocationObserver", "New raw location: ${rawLocation.latitude}, ${rawLocation.longitude}")
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            Log.e("LocationObserver", "New enhanced location: ${enhancedLocation.latitude}, ${enhancedLocation.longitude}")
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            updateCameraToLocation(enhancedLocation)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationService.unregisterObserver(this)
        mapboxNavigation?.stopTripSession()
        MapboxNavigationApp.detach(this)
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
    }

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        Log.e("LocationDebug", "onAttached called")
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.startTripSession()
        Log.e("LocationDebug", "Observers registered and trip session started in onAttached")
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
    }
}