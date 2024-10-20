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
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.maps.MapboxMap


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

            if (!MapboxNavigationApp.isSetup()) {
                Log.e("LocationDebug", "Setting up MapboxNavigationApp")
                MapboxNavigationApp.setup {
                    NavigationOptions.Builder(this)
                        .build()
                }
                Log.e("LocationDebug", "MapboxNavigationApp setup complete")
            }
            mapboxNavigation = MapboxNavigationApp.current()
            Log.e("LocationDebug", "MapboxNavigation obtained")



            setContentView(R.layout.full_screen_map)
            Log.e("LocationDebug", "setContentView called")


            initializeViews()


            initializeMapboxComponents()


            setupLocationServices()


            mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
            {
                Log.e("LocationDebug", "Map style loaded")
                enableLocationComponent()
                initializeNavigation()
                centerOnUserLocation()
            }


            setupSearch()
            setupRecyclerView()
            setupLocationUpdates()


            // Focus on the SearchView and show the keyboard
            searchView.requestFocus()
            searchView.postDelayed({
                showKeyboard(searchView)
            }, 200)

            Log.e("LocationDebug", "onCreate completed successfully")

        } catch (e: Exception) {
            Log.e("LocationDebug", "Exception in onCreate: ${e.message}", e)
        }
    }

    private fun initializeMapboxComponents() {
        mapboxMap = mapView.getMapboxMap()
        Log.e("LocationDebug", "MapboxMap obtained")

        mapboxNavigation = MapboxNavigationApp.current()
        if (mapboxNavigation == null) {
            Log.e("LocationDebug", "Failed to obtain MapboxNavigation instance")
            return
        }
        Log.e("LocationDebug", "MapboxNavigation obtained")

        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(mapboxMap, mapView.camera, viewportDataSource)
        Log.e("LocationDebug", "ViewportDataSource and NavigationCamera initialized")

        navigationLocationProvider = NavigationLocationProvider()
        mapView.location.setLocationProvider(navigationLocationProvider)

        MapboxNavigationApp.attach(this)
        Log.e("LocationDebug", "Successfully attached to MapboxNavigationApp")

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            Log.e("LocationDebug", "Map style loaded")
            enableLocationComponent()
            initializeNavigation()
            centerOnUserLocation()
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
        val navigation = mapboxNavigation
        if (navigation == null) {
            Log.e("LocationDebug", "mapboxNavigation is null in initializeNavigation()")
            return
        }

        try {
            navigation.registerLocationObserver(locationObserver)
            Log.e("LocationDebug", "Location observer registered")

            navigation.startTripSession()
            Log.e("LocationDebug", "Trip session started")

            // Instead of using getLocation(), we'll rely on the LocationObserver to receive updates
            Handler(Looper.getMainLooper()).postDelayed({
                Log.e("LocationDebug", "Delayed location check")
                // The location should be available through the LocationObserver by now
            }, 1000) // 1 second delay

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

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        mapboxNavigation?.startTripSession()
        navigationCamera.requestNavigationCameraToFollowing(
            stateTransitionOptions = NavigationCameraTransitionOptions.Builder().build(),
            frameTransitionOptions = NavigationCameraTransitionOptions.Builder().build()
        )
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableLocationComponent()
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
            locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.from(R.drawable.mapbox_puck)
            )
        }
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
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
                    mapView.getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(15.0)
                            .build()
                    )
                    mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)
                } ?: Log.e("LocationDebug", "Location is null")
                fusedLocationClient.removeLocationUpdates(this)
            }
        }, Looper.getMainLooper())
    }

    private fun centerOnLocation(location: android.location.Location) {
        Log.e("LocationDebug", "Centering on Android location: ${location.latitude}, ${location.longitude}")
        val point = Point.fromLngLat(location.longitude, location.latitude)
        setCameraPosition(point)
    }

    @SuppressLint("MissingPermission")
    private fun setInitialCameraPosition() {
        Log.e("LocationDebug", "Setting initial camera position")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    centerOnLocation(location)
                } else {
                    // If location is null, request a single update
                    val locationRequest = LocationRequest.create().apply {
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        numUpdates = 1
                    }
                    fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let { centerOnLocation(it) }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }, Looper.getMainLooper())
                }
            }
        } else {
            // If no permission, default to a specific location
            val defaultLocation = Point.fromLngLat(-122.4194, 37.7749) // San Francisco
            setCameraPosition(defaultLocation)
        }
    }

    private fun setCameraPosition(point: Point) {
        Log.e("CameraDebug", "Setting camera position to: ${point.latitude()}, ${point.longitude()}")
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0) // Adjust this value to change the zoom level
                .build()
        )
        // Optionally, if you want to animate to this position:
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build(),
            mapAnimationOptions {
                duration(1000) // Animation duration in milliseconds
            }
        )
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
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)

        // Update the viewport data source with the new location
        viewportDataSource.onLocationChanged(point.toLocation())
        viewportDataSource.evaluate()
    }

    private fun Point.toLocation(): MapboxLocation {
        return MapboxLocation.Builder()
            .latitude(latitude())
            .longitude(longitude())
            .build()
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun createLocationPuck2D(
        bearingImage: Any?,
        shadowImage: Any? = null,
        topImage: Any? = bearingImage,
        scaleExpression: Any? = null
    ): LocationPuck2D {
        return LocationPuck2D(
            topImage = drawableToUnit(topImage),
            bearingImage = drawableToUnit(bearingImage),
            shadowImage = drawableToUnit(shadowImage),
            scaleExpression = scaleExpression.toString()
        )
    }

    private fun drawableToUnit(drawable: Any?): ImageHolder? {
        return when (drawable) {
            is Drawable -> ImageHolder.from(drawable)
            is Int -> ContextCompat.getDrawable(this, drawable)?.let { ImageHolder.from(it) }
            else -> null
        }
    }

    private fun ImageHolder.Companion.from(drawable: Drawable): ImageHolder {
        return from(drawable.toBitmap())
    }

    // You might also need this utility function
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
        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.apply {
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            background = null
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    searchResultsRecyclerView.visibility = View.GONE
                    detailsCard.visibility = View.GONE
                } else {
                    performSearch(newText)
                }
                return true
            }
        })
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
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build()
        )
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build(),
            mapAnimationOptions {
                duration(1000)
            }
        )
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


