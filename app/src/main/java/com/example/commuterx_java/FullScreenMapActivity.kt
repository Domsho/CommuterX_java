package com.example.commuterx_java

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import android.content.Context
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import androidx.core.widget.TextViewCompat


class FullScreenMapActivity : AppCompatActivity() {

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.full_screen_map)

        mapView = findViewById(R.id.fullScreenMapView)
        searchView = findViewById(R.id.fullScreenSearchView)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        detailsCard = findViewById(R.id.detailsCard)
        detailsPlaceNameTextView = findViewById(R.id.detailsPlaceNameTextView)
        detailsAddressTextView = findViewById(R.id.detailsAddressTextView)
        trackRouteButton = findViewById(R.id.trackRouteButton)
        scrim = findViewById(R.id.scrim)

        // Initialize map
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        // Initialize search engine
        searchEngine = SearchEngine.createSearchEngine(SearchEngineSettings())

        // Set up search functionality
        setupSearch()

        // Set up RecyclerView
        setupRecyclerView()

        // Set up track route button
        setupTrackRouteButton()


        // Focus on the SearchView and show the keyboard
        searchView.requestFocus()
        searchView.postDelayed({
            showKeyboard(searchView)
        }, 200)

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
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        // Show the scrim
        scrim.visibility = View.VISIBLE
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(300).start()

        // Show and animate the card
        detailsCard.visibility = View.VISIBLE
        val slideUp = ObjectAnimator.ofFloat(detailsCard, "translationY", detailsCard.height.toFloat(), 0f)
        slideUp.duration = 300
        slideUp.interpolator = DecelerateInterpolator()
        slideUp.start()
    }

    private fun hideDetailsCard() {
        // Hide the scrim
        scrim.animate().alpha(0f).setDuration(300).withEndAction {
            scrim.visibility = View.GONE
        }.start()

        // Animate and hide the card
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
            // Handle search suggestion selection
            searchEngine.select(searchSuggestion, object : SearchSelectionCallback {
                override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
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

                    // Update and show details card
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

    private fun setupTrackRouteButton() {
        trackRouteButton.setOnClickListener {
            // Implement route tracking functionality here
            // This could involve starting a new activity or fragment for navigation
            // For now, we'll just hide the details card
            hideDetailsCard()
        }
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