package com.example.commuterx_java

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class FullScreenMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var searchView: SearchView
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchEngine: SearchEngine
    private lateinit var searchResultsAdapter: SearchResultsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_screen_map)

        mapView = findViewById(R.id.fullScreenMapView)
        searchView = findViewById(R.id.fullScreenSearchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        // Initialize map
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)



        // Initialize search engine
        searchEngine = SearchEngine.createSearchEngine(SearchEngineSettings())

        // Set up search functionality
        setupSearch()

        // Set up RecyclerView
        setupRecyclerView()
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    searchResultsRecyclerView.visibility = View.GONE
                    searchView.background = ContextCompat.getDrawable(this@FullScreenMapActivity, R.drawable.search_background)
                } else {
                    performSearch(newText)
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        searchResultsAdapter = SearchResultsAdapter { searchSuggestion ->
            // Handle search suggestion selection
            searchEngine.select(searchSuggestion, object : SearchSelectionCallback {
                override fun onResult(suggestion: SearchSuggestion, result: com.mapbox.search.result.SearchResult, responseInfo: ResponseInfo) {
                    result.coordinate?.let { coordinate ->
                        mapView.getMapboxMap().setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(coordinate.longitude(), coordinate.latitude()))
                                .zoom(14.0)
                                .build()
                        )
                    }
                    searchResultsRecyclerView.visibility = View.GONE
                    searchView.background = ContextCompat.getDrawable(this@FullScreenMapActivity, R.drawable.search_background)
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
                }
            }

            override fun onError(e: Exception) {
                runOnUiThread {
                    // Handle error
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}