package com.example.commuterx_java

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.common.location.AccuracyAuthorization
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.common.location.LocationServiceObserver
import com.mapbox.common.location.PermissionStatus
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.mapbox.common.location.Location as MapboxLocation



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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var navigationCamera: NavigationCamera
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource
    private lateinit var mapboxMap: MapboxMap
    private lateinit var trackRouteButton: MaterialButton
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private var totalDistance = 0.0
    private var pricePerKilometer = 30.0
    private lateinit var busFareTextView: TextView
    private lateinit var jeepFareTextView: TextView
    private lateinit var uvFareTextView: TextView
    private lateinit var trainFareTextView: TextView
    private var selectedTransportType: String? = null
    private var currentRoutes: List<NavigationRoute> = emptyList()


    companion object {
        // Get base rates from FirstFragment's layout values
        private const val BUS_RATE = 10.0  // Bus: ₱10.00 / km
        private const val JEEP_RATE = 6.0  // Jeep: ₱6.00 / km
        private const val UV_RATE = 12.0   // UV: ₱12.00 / km
        private const val TRAIN_RATE = 5.0 // Train: ₱5.00 / km
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: MapboxLocation) {
            Log.d("Navigation", "Raw location update received")
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val location = locationMatcherResult.enhancedLocation
            val point = Point.fromLngLat(location.longitude, location.latitude)
            Log.d("Navigation", "New matched location: ${point.latitude()}, ${point.longitude()}")

            // Update navigation location provider
            navigationLocationProvider.changePosition(
                location = locationMatcherResult.enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // Update the route line
            val result = routeLineApi.updateTraveledRouteLine(point)
            if (result != null) {
                Log.d("Navigation", "Route line update available")
                mapView.getMapboxMap().getStyle()?.let { style ->
                    routeLineView.renderRouteLineUpdate(style, result)
                    Log.d("Navigation", "Route line rendered")
                }
            } else {
                Log.e("Navigation", "Failed to update route line - null result")
            }
        }
    }

    private val searchCallback = object : SearchSelectionCallback {
        override fun onResult(
            suggestion: SearchSuggestion,
            searchResult: SearchResult,
            responseInfo: ResponseInfo
        ) {
            runOnUiThread {
                onSearchResultSelected(searchResult)
            }
        }

        override fun onError(e: Exception) {
            Log.e("SearchDebug", "Search selection error: ${e.message}")
        }

        override fun onResults(
            suggestion: SearchSuggestion,
            results: List<SearchResult>,
            responseInfo: ResponseInfo
        ) {
            TODO("Not yet implemented")
        }

        override fun onSuggestions(
            suggestions: List<SearchSuggestion>,
            responseInfo: ResponseInfo
        ) {
            TODO("Not yet implemented")
        }
    }


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
            trackRouteButton = findViewById(R.id.trackRouteButton)

            initializeViews()
            setupMapboxNavigation()
            initializeMapboxComponents()
            setupLocationServices()
            setupSearch()
            setupRecyclerView()
            setupLocationUpdates()
            setTestWalletBalance(1000.0)



            val backButton = findViewById<MaterialButton>(R.id.backButton)
            backButton.setOnClickListener {
                finish() // This will close the activity and return to the previous fragment
            }

            initializeRouteLine()

            Handler(Looper.getMainLooper()).postDelayed({
                checkLocationComponentStatus()
            }, 2000)

            mapView.mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                setupObservers()
            }

            searchView.requestFocus()
            searchView.postDelayed({
                showKeyboard(searchView)
            }, 200)

            Log.e("LocationDebug", "onCreate completed successfully")

        } catch (e: Exception) {
            Log.e("LocationDebug", "Exception in onCreate: ${e.message}", e)
        }
    }


    private fun initializeRouteLine() {
        // Create API options
        val apiOptions = MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)  // This enables the vanishing route line feature
            .build()

        // Create view options
        val viewOptions = MapboxRouteLineViewOptions.Builder(this)
            .routeLineColorResources(RouteLineColorResources.Builder().build())
            .slotName("road-label")
            .build()

        routeLineApi = MapboxRouteLineApi(apiOptions)
        routeLineView = MapboxRouteLineView(viewOptions)
    }

    @SuppressLint("MissingPermission")
    private fun startNavigation(routes: List<NavigationRoute>) {
        Log.d("Navigation", "Starting navigation with ${routes.size} routes")

        mapboxNavigation?.let { navigation ->
            // First stop any existing trip session
            navigation.stopTripSession()

            // Set the routes
            navigation.setNavigationRoutes(routes)

            // Start the trip session
            navigation.startTripSession()
            Log.d("Navigation", "Trip session started")

            // Register observers if not already registered
            navigation.registerLocationObserver(locationObserver)
            navigation.registerRouteProgressObserver(routeProgressObserver)

            // Hide search UI but ensure details card stays visible
            if (detailsCard.visibility == View.VISIBLE) {
                // Only hide search-related UI
                searchResultsRecyclerView.visibility = View.GONE
                searchView.setQuery("", false)
                searchView.clearFocus()

                // Hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                currentFocus?.let {
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
            } else {
                hideSearchUI()
            }
        } ?: Log.e("Navigation", "MapboxNavigation is null")
    }

    private fun setupObservers() {
        // Register RoutesObserver
        mapboxNavigation?.registerRoutesObserver(routesObserver)

        // Register RouteProgressObserver
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
    }

    private fun fetchRoute(destination: Point) {
        // Get current location from navigationLocationProvider
        val currentLocation = navigationLocationProvider.lastLocation
        val origin = currentLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return // Exit if no current location

        // Log the coordinates for debugging
        Log.d("RouteDebug", "Origin: ${origin.longitude()}, ${origin.latitude()}")
        Log.d("RouteDebug", "Destination: ${destination.longitude()}, ${destination.latitude()}")

        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(origin, destination))
            .build()

        mapboxNavigation?.requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    Log.d("RouteDebug", "Route request cancelled")
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.e("RouteDebug", "Route request failed: $reasons")
                    runOnUiThread {
                        Toast.makeText(
                            this@FullScreenMapActivity,
                            "Failed to get route",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    Log.d("Navigation", "Routes ready: ${routes.size} routes received")
                    currentRoutes = routes // Store the routes

                    runOnUiThread {
                        // Get the distance in kilometers from the route
                        val distanceInKm = routes.firstOrNull()?.directionsRoute?.distance()?.let {
                            it / 1000.0  // Convert meters to kilometers
                        } ?: 0.0

                        // Calculate and display fares based on the actual route distance
                        calculateFares(distanceInKm)

                        // Set the routes and update route line
                        mapboxNavigation?.setNavigationRoutes(routes)
                        routeLineApi.setNavigationRoutes(routes) { value ->
                            mapView.getMapboxMap().getStyle()?.let { style ->
                                routeLineView.renderRouteDrawData(style, value)
                            }
                        }
                    }
                }
            }
        )
    }


    private fun handleTrackRoute() {
        if (selectedTransportType == null) {
            Toast.makeText(this, "Please select a transport type", Toast.LENGTH_SHORT).show()
            return
        }

        // Extract the fare amount from the button text
        val fareText = trackRouteButton.text.toString()
        val fareAmount = extractFareAmount(fareText)

        if (fareAmount == null) {
            Toast.makeText(this, "Invalid fare amount", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("TrackRoute", "Track route button clicked with fare: $fareAmount")
        val currentBalance = getWalletBalance()

        if (currentBalance >= fareAmount) {
            val newBalance = currentBalance - fareAmount
            updateWallet(newBalance)
            Log.d("TrackRoute", "Wallet updated. New balance: $newBalance")

            // Add these lines to ensure route is displayed
            selectedLocationPoint?.let { destination ->
                mapboxNavigation?.setNavigationRoutes(currentRoutes)

                // Update route line visibility
                routeLineApi.setNavigationRoutes(currentRoutes) { value ->
                    mapView.getMapboxMap().getStyle()?.let { style ->
                        routeLineView.renderRouteDrawData(style, value)
                    }
                }
            }

            // Hide the details card and scrim with animation
            hideDetailsCard()

            // Hide the scrim with animation
            scrim.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    scrim.visibility = View.GONE
                }
                .start()

            startTrackingRoute()
        } else {
            Log.d("TrackRoute", "Insufficient funds in wallet.")
            Toast.makeText(this, "Insufficient funds in wallet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractFareAmount(fareText: String): Double? {
        // Expected format: "Confirm Route - ₱XX.XX"
        return try {
            val regex = "₱(\\d+\\.?\\d*)".toRegex()
            val matchResult = regex.find(fareText)
            matchResult?.groupValues?.get(1)?.toDouble()
        } catch (e: Exception) {
            Log.e("FareExtraction", "Error extracting fare: ${e.message}")
            null
        }
    }

    private fun setTestWalletBalance(amount: Double) {
        val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("wallet_balance", amount.toFloat())
        editor.apply()
        Log.d("WalletBalance", "Test wallet balance set to: $amount")
    }

    private fun updateWallet(amount: Double) {
        val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("wallet_balance", amount.toFloat())
        editor.apply()

        // Broadcast the wallet update
        val intent = Intent("WALLET_BALANCE_UPDATED")
        intent.putExtra("balance", amount)
        sendBroadcast(intent)
    }

    private fun simulatePayment(): Boolean {
        // Simulate a payment process (replace with actual payment logic)
        Toast.makeText(this, "Payment Process Started", Toast.LENGTH_SHORT).show()
        return true // Simulate successful payment
    }

    private fun startTrackingRoute() {
        // Logic to start tracking the route
        Log.d("FullScreenMapActivity", "Tracking route...")
        // You can add your route tracking logic here
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
            } else if (mapboxNavigation != null) {
                Log.d("LocationDebug", "MapboxNavigation instance obtained successfully")
                initializeNavigation() // Call initializeNavigation here
                // Register observers here if needed
            }
        }

        retryObtainingNavigation()
    }

    private fun initializeMapboxComponents() {
        mapboxMap = mapView.mapboxMap
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

        loadStyle()
    }

    private fun loadStyle() {
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            Log.e("LocationDebug", "Map style loaded")
            // Set initial camera position with high zoom
            updateCamera(mapView.mapboxMap.cameraState.center, 19.0, false)
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

        busFareTextView = findViewById(R.id.busFareTextView)
        jeepFareTextView = findViewById(R.id.jeepFareTextView)
        uvFareTextView = findViewById(R.id.uvFareTextView)
        trainFareTextView = findViewById(R.id.trainFareTextView)

        findViewById<LinearLayout>(R.id.busFareLayout).setOnClickListener {
            val fareText = busFareTextView.text.toString()
            trackRouteButton.text = "Confirm Route - $fareText"
            updateSelectedTransport("bus")
        }

        findViewById<LinearLayout>(R.id.jeepneyFareLayout).setOnClickListener {
            val fareText = jeepFareTextView.text.toString()
            trackRouteButton.text = "Confirm Route - $fareText"
            updateSelectedTransport("jeepney")
        }

        findViewById<LinearLayout>(R.id.uvFareLayout).setOnClickListener {
            val fareText = uvFareTextView.text.toString()
            trackRouteButton.text = "Confirm Route - $fareText"
            updateSelectedTransport("uv")
        }

        findViewById<LinearLayout>(R.id.trainFareLayout).setOnClickListener {
            val fareText = trainFareTextView.text.toString()
            trackRouteButton.text = "Confirm Route - $fareText"
            updateSelectedTransport("train")
        }

        trackRouteButton = findViewById(R.id.trackRouteButton)
        trackRouteButton.isEnabled = false
        trackRouteButton.setOnClickListener {
            handleTrackRoute()  // Connect the button to handleTrackRoute
        }
        Log.e("LocationDebug", "Views initialized")
    }

    private fun updateSelectedTransport(type: String) {
        selectedTransportType = type

        // Reset all backgrounds
        findViewById<LinearLayout>(R.id.busFareLayout).setBackgroundResource(android.R.color.transparent)
        findViewById<LinearLayout>(R.id.jeepneyFareLayout).setBackgroundResource(android.R.color.transparent)
        findViewById<LinearLayout>(R.id.uvFareLayout).setBackgroundResource(android.R.color.transparent)
        findViewById<LinearLayout>(R.id.trainFareLayout).setBackgroundResource(android.R.color.transparent)

        // Highlight selected option
        val selectedLayout = when(type) {
            "bus" -> findViewById(R.id.busFareLayout)
            "jeepney" -> findViewById(R.id.jeepneyFareLayout)
            "uv" -> findViewById(R.id.uvFareLayout)
            "train" -> findViewById<LinearLayout>(R.id.trainFareLayout)
            else -> null
        }

        selectedLayout?.setBackgroundResource(R.drawable.selected_transport_background)

        val fareText = when(type) {
            "bus" -> busFareTextView.text
            "jeepney" -> jeepFareTextView.text
            "uv" -> uvFareTextView.text
            "train" -> trainFareTextView.text
            else -> ""
        }
        trackRouteButton.text = "Confirm Route - $fareText"
        trackRouteButton.isEnabled = true
    }

    private fun calculateFares(distanceInKm: Double) {
        // Calculate fares using per-kilometer rates
        val busFare = BUS_RATE * distanceInKm
        val jeepFare = JEEP_RATE * distanceInKm
        val uvFare = UV_RATE * distanceInKm
        val trainFare = TRAIN_RATE * distanceInKm

        // Update the TextViews with calculated fares
        busFareTextView.text = String.format("Bus: ₱%.2f", busFare)
        jeepFareTextView.text = String.format("Jeepney: ₱%.2f", jeepFare)
        uvFareTextView.text = String.format("UV Express: ₱%.2f", uvFare)
        trainFareTextView.text = String.format("Train: ₱%.2f", trainFare)

        // Make fare options visible
        findViewById<LinearLayout>(R.id.busFareLayout).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.jeepneyFareLayout).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.uvFareLayout).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.trainFareLayout).visibility = View.VISIBLE

        Log.d("FareCalculation", """
        Distance: $distanceInKm km
        Bus Fare: ₱$busFare
        Jeep Fare: ₱$jeepFare
        UV Fare: ₱$uvFare
        Train Fare: ₱$trainFare
    """.trimIndent())
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.e("LocationDebug", "FusedLocationClient initialized")

        locationService = LocationServiceFactory.getOrCreate()

        checkLocationPermission() // Ensure permission is checked
    }

    @SuppressLint("MissingPermission")
    private fun initializeNavigation() {
        Log.e("LocationDebug", "Initializing navigation")
        if (mapboxNavigation == null) {
            Log.e("LocationDebug", "mapboxNavigation is null in initializeNavigation()")
            return
        }

        try {
            // Register the location observer
            mapboxNavigation?.registerLocationObserver(locationObserver)
            Log.e("LocationDebug", "Location observer registered")

            // Start the trip session
            mapboxNavigation?.startTripSession()
            Log.e("LocationDebug", "Trip session started")
        } catch (e: Exception) {
            Log.e("LocationDebug", "Error in initializeNavigation", e)
        }
    }

    private fun onSearchResultSelected(searchResult: SearchResult) {
        searchResult.coordinate?.let { coordinate ->
            selectedLocationPoint = Point.fromLngLat(coordinate.longitude(), coordinate.latitude())

            // Update UI elements
            detailsPlaceNameTextView.text = searchResult.name
            detailsAddressTextView.text = searchResult.address?.formattedAddress() ?: "Address not available"

            // Ensure the card and scrim are visible
            detailsCard.visibility = View.VISIBLE
            scrim.visibility = View.VISIBLE

            // Show the details card with animation
            showDetailsCard()

            // Fetch route after ensuring card visibility
            selectedLocationPoint?.let { destination ->
                fetchRoute(destination)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val routesObserver = RoutesObserver { result ->
        Log.d("Navigation", "Routes observer triggered with ${result.navigationRoutes?.size} routes")

        result.navigationRoutes?.let { routes ->
            // First set the routes for the route line
            routeLineApi.setNavigationRoutes(routes) { value ->
                mapView.getMapboxMap().getStyle()?.let { style ->
                    routeLineView.renderRouteDrawData(style, value)
                }
            }

            // Start navigation with the routes
            startNavigation(routes)
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
        Log.d("Navigation", "Enabling location component")

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Wait for the style to be loaded first
                mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                    // Initialize location component
                    mapView.location.apply {
                        // Enable the location component first
                        enabled = true

                        // Set the location provider
                        setLocationProvider(navigationLocationProvider)

                        // Configure the puck appearance
                        locationPuck = LocationPuck2D(
                            bearingImage = ImageHolder.from(R.drawable.mapbox_puck), // Try a different icon
                            shadowImage = null,
                            scaleExpression = null,
                            opacity = 1.0f
                        )

                        // Enable puck features
                        puckBearingEnabled = true
                        pulsingEnabled = true


                        updateSettings {
                            this.enabled = true
                            this.pulsingEnabled = true
                            this.locationPuck = LocationPuck2D(
                                bearingImage = ImageHolder.from(R.drawable.mapbox_puck)
                            )
                        }

                        // Add position listener
                        removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                        addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                    }

                    // Update location settings
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }

                    // Request location updates
                    if (ActivityCompat.checkSelfPermission(
                            this@FullScreenMapActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val point = Point.fromLngLat(it.longitude, it.latitude)
                                updateCamera(point)

                                // Update the navigation location provider
                                navigationLocationProvider.changePosition(
                                    location = MapboxLocation.Builder()
                                        .latitude(it.latitude)
                                        .longitude(it.longitude)
                                        .build()
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Navigation", "Error enabling location component", e)
            }
        }
    }

    private fun checkLocationComponentStatus() {
        Log.d("LocationComponent", """
        Location enabled: ${mapView.location.enabled}
        Puck bearing enabled: ${mapView.location.puckBearingEnabled}
        Pulsing enabled: ${mapView.location.pulsingEnabled}
        Last location: ${navigationLocationProvider.lastLocation}
        Location provider: ${mapView.location.getLocationProvider()}
    """.trimIndent())
    }

    @SuppressLint("MissingPermission")
    private fun centerOnUserLocation() {
        mapView.location.addOnIndicatorPositionChangedListener { point ->
            // Calculate distance from previous location to current location
            val currentLocation = android.location.Location("current").apply {
                latitude = point.latitude()
                longitude = point.longitude()
            }

            // Assuming you have a way to get the previous location
            val previousLocation = getPreviousLocation()


            val distance = currentLocation.distanceTo(previousLocation) / 1000 // Convert to kilometers
            totalDistance += distance // Update total distance
            updateWallet(totalDistance * pricePerKilometer) // Update wallet based on distance


            // Set camera to the new location
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(20.0)
                    .pitch(65.0)
                    .build()
            )
        }
    }

    private fun getPreviousLocation() {
        // Retrieve the last known location from shared preferences or another source
        // Return a Location object or null if not available
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
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener { point ->
        Log.d("Navigation", "Position changed: ${point.latitude()}, ${point.longitude()}")
        updateCamera(point)

        // Update the route line to follow the user's position
        routeLineApi.updateTraveledRouteLine(point)?.let { result ->
            Log.d("Navigation", "Updating route line")
            mapView.getMapboxMap().getStyle()?.let { style ->
                routeLineView.renderRouteLineUpdate(style, result)
                Log.d("Navigation", "Route line updated")
            }
        } ?: Log.d("Navigation", "Failed to update route line")
    }

    private fun updateNavigationPuck(location: MapboxLocation) {
        navigationLocationProvider.changePosition(
            location = location,
            keyPoints = emptyList(),
            bearingTransitionOptions = {
                duration = 1000
            }
        )
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
            mapView.mapboxMap.setCamera(cameraOptions)
        }
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(point)
    }

    private fun setInitialMapStyle() {
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            Log.e("LocationDebug", "Map style loaded")
            // Set initial camera position with high zoom
            updateCamera(mapView.mapboxMap.cameraState.center, 19.0, false)
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
        // Ensure views are visible before starting animation
        detailsCard.visibility = View.VISIBLE
        scrim.visibility = View.VISIBLE

        // Reset any ongoing animations
        detailsCard.animate().cancel()
        scrim.animate().cancel()

        // Set initial states
        detailsCard.alpha = 0f  // Add this line
        detailsCard.translationY = detailsCard.height.toFloat()
        scrim.alpha = 0f

        // Animate in
        scrim.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        detailsCard.animate()
            .alpha(1f)  // Add this line
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
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
            // Hide keyboard when a search result is clicked
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let {
                imm.hideSoftInputFromWindow(it.windowToken, 0)
            }

            // Hide search results recycler view
            searchResultsRecyclerView.visibility = View.GONE

            // Reset search view background
            searchView.background = ContextCompat.getDrawable(
                this@FullScreenMapActivity,
                R.drawable.search_background_rounded_green
            )

            // Show the details card before processing the search
            showDetailsCard()

            // Process the search selection
            searchEngine.select(searchSuggestion, searchCallback)
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
                    searchView.background = ContextCompat.getDrawable(
                        this@FullScreenMapActivity,
                        R.drawable.search_background_top
                    )
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    // Handle error
                    Log.e("SearchDebug", "Search error: ${e.message}")
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


    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        routeLineApi.updateWithRouteProgress(routeProgress) { result ->
            mapView.getMapboxMap().getStyle()?.let { style ->
                routeLineView.renderRouteLineUpdate(style, result)
            }
        }

        // Calculate the distance traveled
        val distanceTraveled = routeProgress.distanceTraveled // in meters
        val distanceInKilometers = distanceTraveled / 1000 // convert to kilometers

        // Calculate the price
        val pricePerKilometer = 30.0 // in Philippine pesos
        val totalPrice = distanceInKilometers * pricePerKilometer

        // Update the wallet balance
        val currentBalance = getWalletBalance()
        if (currentBalance >= totalPrice) {
            updateWallet(currentBalance - totalPrice)
            Log.d("PriceUpdate", "Price deducted: $totalPrice. New balance: ${currentBalance - totalPrice}")
        } else {
            Log.d("PriceUpdate", "Insufficient funds. Current balance: $currentBalance, required: $totalPrice")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Remove location listeners first
            mapView.location.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

            // Clean up route line resources
            routeLineApi.cancel()
            routeLineView.cancel()

            // Clean up location service
            locationService.unregisterObserver(this)

            // Clean up navigation resources safely
            mapboxNavigation?.let { navigation ->
                // Unregister observers first
                navigation.unregisterRoutesObserver(routesObserver)
                navigation.unregisterLocationObserver(locationObserver)
                navigation.unregisterRouteProgressObserver(routeProgressObserver)

                // Stop trip session only if it's active
                try {
                    if (navigation.getTripSessionState() == TripSessionState.STARTED) {
                        navigation.stopTripSession()
                    } else {
                        Log.d("Navigation", "Trip session was not active")
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Error stopping trip session", e)
                }
            }

            // Detach from MapboxNavigationApp last
            MapboxNavigationApp.detach(this)

            // Clear the navigation instance
            mapboxNavigation = null

        } catch (e: Exception) {
            Log.e("Navigation", "Error in onDestroy", e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        Log.e("LocationDebug", "onAttached called")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Unregister observers in background
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterRoutesObserver(routesObserver)

                // Register new observers in background
                mapboxNavigation.registerLocationObserver(locationObserver)
                mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.registerRoutesObserver(routesObserver)
            } catch (e: Exception) {
                Log.e("Navigation", "Error in onAttached", e)
            }
        }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                mapboxNavigation.unregisterLocationObserver(locationObserver)
                mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
                mapboxNavigation.unregisterRoutesObserver(routesObserver)
            } catch (e: Exception) {
                Log.e("Navigation", "Error in onDetached", e)
            }
        }
    }

    private fun getWalletBalance(): Double {
        // Since SharedPreferences operations are relatively quick, this can stay on main thread
        // But if you want to be extra safe:
        return runBlocking(Dispatchers.IO) {
            val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
            sharedPreferences.getFloat("wallet_balance", 1000f).toDouble()
        }
    }

    private fun hideSearchUI() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                searchResultsRecyclerView.visibility = View.GONE
                searchView.setQuery("", false)
                searchView.clearFocus()

                // Hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                currentFocus?.let {
                    imm.hideSoftInputFromWindow(it.windowToken, 0)
                }
            } catch (e: Exception) {
                Log.e("UI", "Error hiding search UI", e)
            }
        }
    }
}

private fun android.location.Location.distanceTo(previousLocation: Unit): Float {
    return this.distanceTo(previousLocation)
}