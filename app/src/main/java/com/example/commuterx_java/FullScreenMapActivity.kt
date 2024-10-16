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
import android.widget.Button
import android.view.inputmethod.InputMethodManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.mapbox.common.location.AccuracyAuthorization
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.common.location.LocationServiceObserver
import com.mapbox.common.location.PermissionStatus


class FullScreenMapActivity : AppCompatActivity(), LocationServiceObserver {

    private lateinit var mapView: MapView
    private lateinit var searchView: CustomSearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchEngine: SearchEngine
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var detailsCard: MaterialCardView
    private lateinit var detailsPlaceNameTextView: TextView
    private lateinit var detailsAddressTextView: TextView
    private lateinit var trackRouteButton: Button
    private lateinit var scrim: View
    private var selectedLocationPoint: Point? = null
    private var mapboxNavigation: MapboxNavigation? = null
    private lateinit var routeLineApi: MapboxRouteLineApi
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_screen_map)

        mapView = findViewById(R.id.fullScreenMapView)
        searchView = findViewById(R.id.fullScreenSearchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        detailsCard = findViewById(R.id.detailsCard)
        detailsPlaceNameTextView = findViewById(R.id.detailsPlaceNameTextView)
        detailsAddressTextView = findViewById(R.id.detailsAddressTextView)
        scrim = findViewById(R.id.scrim)
        val isNavigating = false

        // Initialize map
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        // Initialize search engine
        searchEngine = SearchEngine.createSearchEngine(SearchEngineSettings())

        setupSearch()

        // Set up RecyclerView
        setupRecyclerView()

        initializeNavigation()

        locationService = LocationServiceFactory.getOrCreate()


        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup {
                NavigationOptions.Builder(this)
                    // Add any additional options here
                    .build()
            }
        }

        MapboxNavigationApp.attach(this)

        mapboxNavigation = MapboxNavigationApp.current()

        val navigateButton = findViewById<Button>(R.id.trackRouteButton)
        navigateButton.setOnClickListener {
            if (isNavigating) {
                stopNavigation()
            } else {
                startNavigation()
            }
        }

        setupLocationUpdates()

        // Focus on the SearchView and show the keyboard
        searchView.requestFocus()
        searchView.postDelayed({
            showKeyboard(searchView)
        }, 200)
    }

    private fun setupLocationUpdates() {
        locationService.registerObserver(this)
        // The trip session is started when the user clicks the navigate button, so we don't need to start it here
    }



    private fun initializeNavigation() {
        mapboxNavigation = if (MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.current()
        } else {
            MapboxNavigationApp.setup {
                NavigationOptions.Builder(this)
                    .build()
            }
            MapboxNavigationApp.current()
        }
    }

    private fun startNavigation() {
        try {
            mapboxNavigation?.startTripSession()
            val isNavigating = true
            updateNavigationUI(isNavigating)
        } catch (e: SecurityException) {
            Log.e("Navigation", "Failed to start navigation", e)
            Toast.makeText(this, "Failed to start navigation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopNavigation() {
        mapboxNavigation?.stopTripSession()
        updateNavigationUI(false)
        Log.d("Navigation", "Navigation stopped")
        Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show()
    }

    fun onLocationUpdated(point: Point) {
        Log.d("Location", "New location: Lat ${point.latitude()}, Lon ${point.longitude()}")
        // Update the map with the new location
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build()
        )
        // If navigation is active, you might want to update the navigation here
    }


    private fun updateNavigationUI(isNavigating: Boolean) {
        val navigateButton = findViewById<Button>(R.id.trackRouteButton)
        navigateButton.text = if (isNavigating) "Stop Navigation" else "Start Navigation"
        // Add any other UI updates here
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNavigation()
            } else {
                Toast.makeText(this, "Location permission is required for navigation", Toast.LENGTH_SHORT).show()
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

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
                        Log.d("SearchDebug", "Search result coordinates: ${coordinate.latitude()}, ${coordinate.longitude()}")
                        selectedLocationPoint = Point.fromLngLat(coordinate.longitude(), coordinate.latitude())
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(selectedLocationPoint)
                                .zoom(14.0)
                                .build()
                        )
                        Log.d("SearchDebug", "Camera moved to: ${selectedLocationPoint?.latitude()}, ${selectedLocationPoint?.longitude()}")
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


    override fun onDestroy() {
        super.onDestroy()
        locationService.unregisterObserver(this)
        mapboxNavigation?.stopTripSession()
        MapboxNavigationApp.detach(this)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onAvailabilityChanged(isAvailable: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onPermissionStatusChanged(permission: PermissionStatus) {
        TODO("Not yet implemented")
    }

    override fun onAccuracyAuthorizationChanged(authorization: AccuracyAuthorization) {
        TODO("Not yet implemented")
    }
}