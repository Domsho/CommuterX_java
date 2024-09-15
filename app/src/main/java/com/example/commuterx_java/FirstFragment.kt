package com.example.commuterx_java

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.view.SearchResultsView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchOptions
import com.mapbox.search.SearchSelectionCallback
import com.mapbox.search.SearchSuggestionsCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView






class FirstFragment : Fragment() {
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager
    private var isLocationInitialized = false
    private lateinit var recenterButton: FloatingActionButton
    private var locationUpdateListener: OnIndicatorPositionChangedListener? = null
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngine: SearchEngine

    interface PermissionsListener {
        fun onPermissionsGranted()
        fun onPermissionsDenied()
    }

    inner class PermissionsManager(
        private val activity: FragmentActivity,
        private val listener: PermissionsListener,
        private val permissions: Array<String>,
        private val requestCode: Int
    ) {
        fun checkPermissions() {
            if (!hasPermissions()) {
                requestPermissions()
            } else {
                listener.onPermissionsGranted()
            }
        }

        private fun hasPermissions(): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        private fun requestPermissions() {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }

        fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
            if (requestCode == this.requestCode) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.onPermissionsGranted()
                } else {
                    listener.onPermissionsDenied()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_first, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        mapView = view.findViewById(R.id.mapView)
        recenterButton = view.findViewById(R.id.recenter_button)
        searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        searchResultsView = view.findViewById(R.id.searchResultsView)

        val accessToken = getString(R.string.mapbox_access_token)

        val settings = SearchEngineSettings(null)
        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(settings)

        setupMapbox()
        setupRecenterButton()
        setupFirebase(view)
        setupPermissions()
        setupSearch()


        return view
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    if (it.length >= 3) { // Only search if there are at least 3 characters
                        performSearch(it)
                    } else {
                        // Clear suggestions if text is too short
                        searchResultsView.visibility = View.GONE
                    }
                }
                return true
            }
        })
    }

    private fun performSearch(query: String) {
        if (!this::searchEngine.isInitialized) {
            Log.e("FirstFragment", "SearchEngine not initialized")
            return
        }

        val options = SearchOptions.Builder()
            .limit(5)
            .build()

        searchEngine.search(query, options, object : SearchSuggestionsCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                if (suggestions.isNotEmpty()) {
                    // Handle suggestions
                    activity?.runOnUiThread {
                        handleSearchSuggestions(suggestions)
                    }
                }
            }

            override fun onError(e: Exception) {
                Log.e("Search", "Error: ${e.message}")
            }
        })
    }

    private fun handleSearchSuggestions(suggestions: List<SearchSuggestion>) {
        if (suggestions.isNotEmpty()) {
            val firstSuggestion = suggestions[0]

            activity?.runOnUiThread {
                updateSearchSuggestionsUI(suggestions)
            }

            searchEngine.select(firstSuggestion, object : SearchSelectionCallback {

                override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                    handleSearchResult(result)
                }

                override fun onResults(
                    suggestion: SearchSuggestion,
                    results: List<SearchResult>,
                    responseInfo: ResponseInfo
                ) {
                    if (results.isNotEmpty()) {
                        handleSearchResult(results[0])
                    }
                }

                override fun onSuggestions(
                    suggestions: List<SearchSuggestion>,
                    responseInfo: ResponseInfo
                ) {
                    if (suggestions.isNotEmpty()) {
                        // Handle the suggestions
                        activity?.runOnUiThread {
                            // Update UI with suggestions
                            updateSearchSuggestionsUI(suggestions)
                        }
                    } else {
                        // Handle case when no suggestions are available
                        Log.d("Search", "No suggestions available")
                    }
                }

                override fun onError(e: Exception) {
                    Log.e("Search", "Error selecting suggestion: ${e.message}")
                }

                // Add any other methods that might be required by your specific SearchSelectionCallback interface
            })
        }
    }

    private fun updateSearchSuggestionsUI(suggestions: List<SearchSuggestion>) {
        searchResultsView.removeAllViews()

        for (suggestion in suggestions) {
            val suggestionView = layoutInflater.inflate(R.layout.item_search_suggestion, searchResultsView, false)

            val titleTextView = suggestionView.findViewById<TextView>(R.id.suggestion_title)
            val addressTextView = suggestionView.findViewById<TextView>(R.id.suggestion_address)

            titleTextView.text = suggestion.name
            addressTextView.text = suggestion.address?.formattedAddress() ?: ""

            suggestionView.setOnClickListener {
                handleSuggestionClick(suggestion)
            }

            searchResultsView.addView(suggestionView)
        }

        searchResultsView.visibility = View.VISIBLE
    }




    private fun handleSuggestionClick(suggestion: SearchSuggestion) {
        searchEngine.select(suggestion, object : SearchSelectionCallback {
            override fun onResult(
                suggestion: SearchSuggestion,
                result: SearchResult,
                responseInfo: ResponseInfo
            ) {
                handleSearchResult(result)
            }

            override fun onResults(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                if (results.isNotEmpty()) {
                    handleSearchResult(results[0])
                }
            }

            override fun onSuggestions(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
                TODO("Not yet implemented")
            }

            override fun onError(e: Exception) {
                Log.e("Search", "Error selecting suggestion: ${e.message}")
            }
        })

        // Clear the search view and hide suggestions
        searchView.setQuery("", false)
        searchResultsView.visibility = View.GONE
    }

    private fun handleSearchResult(result: SearchResult) {
        result.coordinate?.let { coordinate ->
            val point = Point.fromLngLat(coordinate.longitude(), coordinate.latitude())
            activity?.runOnUiThread {
                addMarkerToMap(point)
                centerMapOnPoint(point)
            }
        }
    }


    private fun addMarkerToMap(point: Point) {
        val annotationApi = mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager()
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(generateBitmapFromVector(R.drawable.ic_map_marker))
        pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun generateBitmapFromVector(vectorResId: Int): Bitmap {
        val vectorDrawable = ContextCompat.getDrawable(requireContext(), vectorResId)
        vectorDrawable?.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable?.intrinsicWidth ?: 1, vectorDrawable?.intrinsicHeight ?: 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable?.draw(canvas)
        return bitmap
    }

    private fun centerMapOnPoint(point: Point) {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(14.0)
                .build()
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMapbox()
    }

    private fun setupMapbox() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            Log.d("Mapbox", "Style loaded successfully.")
            mapView.scalebar.enabled = false
            enableLocationTracking()
        }
    }

    private fun enableLocationComponent() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
        } else {
            // Request permissions
        }
    }

    private fun centerOnUserLocation() {
        locationUpdateListener = OnIndicatorPositionChangedListener { point ->
            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(20.0)
                    .pitch(65.0)
                    .build()
            )
            // Remove the listener after centering
            locationUpdateListener?.let { listener ->
                mapView.location.removeOnIndicatorPositionChangedListener(listener)
            }
            locationUpdateListener = null
        }
        locationUpdateListener?.let { listener ->
            mapView.location.addOnIndicatorPositionChangedListener(listener)
        }
    }

    private fun setupRecenterButton() {
        recenterButton.setOnClickListener {
            centerOnUserLocation()
        }
    }

    private fun setupFirebase(view: View) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        val greetingTextView = view.findViewById<TextView>(R.id.greeting_text1)

        if (uid != null) {
            val myRef = database.getReference("users").child(uid)
            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val username = dataSnapshot.child("username").getValue(String::class.java)
                    greetingTextView.text = if (username.isNullOrEmpty()) "Hi" else "Hi, $username"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching data", error.toException())
                    greetingTextView.text = "Hi" // Fallback greeting if data fetch fails
                }
            })
        } else {
            greetingTextView.text = "Hi"
        }
    }

    private fun setupPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        permissionsManager = PermissionsManager(
            requireActivity(),
            object : PermissionsListener {
                override fun onPermissionsGranted() {
                    enableLocationTracking()
                }

                override fun onPermissionsDenied() {
                    Log.e("Permissions", "Location permissions denied")
                }
            },
            permissions,
            1
        )

        permissionsManager.checkPermissions()
    }

    private fun enableLocationTracking() {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.mapbox_puck)
        if (drawable == null) {
            Log.e("EnableLocationTracking", "Drawable resource not found")
            return
        }

        // Convert drawable to bitmap
        val bitmap = drawableToBitmap(drawable)

        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
            locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.from(bitmap)
            )
        }

        mapView.gestures.pitchEnabled = true

        mapView.location.addOnIndicatorPositionChangedListener { point ->
            if (!isLocationInitialized) {
                centerOnUserLocation()
                isLocationInitialized = true
            }
            else {
                mapView.getMapboxMap().setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .build()
                )
            }
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)
        }

        mapView.location.addOnIndicatorBearingChangedListener { bearing ->
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(bearing).build())
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapView.getMapboxMap().getStyle {
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
        } else {
            // Request location permission if not granted
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationUpdateListener?.let { listener ->
            mapView.location.removeOnIndicatorPositionChangedListener(listener)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}
