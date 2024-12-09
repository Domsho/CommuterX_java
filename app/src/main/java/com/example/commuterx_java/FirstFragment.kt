package com.example.commuterx_java

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
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
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi


class FirstFragment : Fragment() {
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager
    private var isLocationInitialized = false
    private var locationUpdateListener: OnIndicatorPositionChangedListener? = null
    private lateinit var searchView: SearchView
    private lateinit var searchEngine: SearchEngine
    private lateinit var mapOverlay: View
    private lateinit var mapboxMap: com.mapbox.maps.MapboxMap
    private lateinit var connectivityManager: ConnectivityManager
    private var isLocationTrackingEnabled = false
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var walletBalanceTextView: TextView
    private var walletUpdateReceiver: BroadcastReceiver? = null


    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Handle the network becoming available
        }

        override fun onLost(network: Network) {
            // Handle the network being lost
        }
    }


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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_first, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        mapView = view.findViewById(R.id.mapView)
        searchView = view.findViewById(R.id.search_view)
        mapOverlay = view.findViewById(R.id.mapOverlay)

        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
        setupPermissions()

        val settings = SearchEngineSettings(null)
        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(settings)

        walletBalanceTextView = view.findViewById(R.id.wallet_balance)
        updateWalletDisplay()

        // Replace the existing walletUpdateReceiver with this updated version
        walletUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "WALLET_BALANCE_UPDATED") {
                    val balance = intent.getDoubleExtra("balance", 0.0)
                    activity?.runOnUiThread {
                        walletBalanceTextView.text = "₱%.2f".format(balance)
                        Log.d("WalletUpdate", "Received balance update: $balance")
                    }
                }
            }
        }

        // Update the receiver registration
        val filter = IntentFilter("WALLET_BALANCE_UPDATED")
        requireActivity().registerReceiver(
            walletUpdateReceiver,
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )

        setupMapbox()
        setupFirebase(view)
        setupSearch()

        mapOverlay.setOnClickListener {
            Log.d("FirstFragment", "Map overlay clicked")
            navigateToFullScreenMap()
        }

        searchView.setOnClickListener {
            Log.d("FirstFragment", "SearchView clicked")
            navigateToFullScreenMap()
        }

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                navigateToFullScreenMap()
            }
        }

        mapView.setOnClickListener {
            Log.d("FirstFragment", "MapView clicked")
            navigateToFullScreenMap()
        }

        bottomNavigation = view.findViewById(R.id.bottom_navigation)
        setupBottomNavigation()


        return view
    }


    private fun updateWalletDisplay() {
        val sharedPreferences = requireActivity().getSharedPreferences(
            "MySharedPref",
            Context.MODE_PRIVATE
        )
        val balance = sharedPreferences.getFloat("wallet_balance", 1000f)
        walletBalanceTextView.text = "₱%.2f".format(balance)
        Log.d("WalletUpdate", "Wallet display updated: $balance")
    }


    private fun setupBottomNavigation() {
        // Set home as selected when fragment is created/shown
        bottomNavigation.selectedItemId = R.id.navigation_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_settings -> {
                    val intent = Intent(requireContext(), SettingsActivity::class.java)
                    // First start the new activity
                    startActivity(intent)
                    // Then apply the transition animation to the entire activity
                    requireActivity().overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left)
                    true // Changed to true to indicate we handled the selection
                }
                R.id.navigation_home -> {
                    true
                }
                else -> false
            }
        }
    }


    private fun navigateToFullScreenMap() {
        val intent = Intent(requireContext(), FullScreenMapActivity::class.java)
        startActivity(intent)
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                navigateToFullScreenMap()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText?.isNotEmpty() == true) {
                    navigateToFullScreenMap()
                }
                return true
            }
        })

        searchView.setOnClickListener {
            navigateToFullScreenMap()
        }

    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkLocationServices() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Show a dialog to the user asking them to enable location services
            AlertDialog.Builder(requireContext())
                .setMessage("Location services are disabled. Would you like to enable them?")
                .setPositiveButton("Yes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("No", null)
                .show()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.mapView)
        mapboxMap = mapView.mapboxMap
        setupMapbox()
    }

    private fun setupMapbox() {
        loadStyle {
            mapView.scalebar.enabled = false
            setupMapboxView()
        }
    }

    private fun loadStyle(function: () -> Unit) {

    }

    private fun setupMapboxView() {
        // Set camera to focus on North America
        val cameraPosition = CameraOptions.Builder()
            .center(Point.fromLngLat(-100.0, 40.0))
            .zoom(2.5)
            .pitch(50.0)
            .bearing(0.0)
            .build()
        mapboxMap.setCamera(cameraPosition)

        // Enable gestures
        mapView.gestures.pitchEnabled = true
    }



    private fun centerOnUserLocation() {
        locationUpdateListener = OnIndicatorPositionChangedListener { point ->
            mapView.mapboxMap.setCamera(
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


    private fun setupFirebase(view: View) {
        val database = FirebaseDatabase.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid

        val greetingTextView = view.findViewById<TextView>(R.id.greeting_text)

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

        if (isLocationTrackingEnabled) return

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }


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
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .build()
                )
            }
            mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(point)
        }

        mapView.location.addOnIndicatorBearingChangedListener { bearing ->
            mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(bearing).build())
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mapView.mapboxMap.getStyle {
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
        } else {
            // Request location permission if not granted
            requestLocationPermission()
        }

        isLocationTrackingEnabled = true
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


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    enableLocationTracking()
                } else {
                    Toast.makeText(context, "Location permission is required for this feature", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                permissionsManager.onRequestPermissionsResult(requestCode, grantResults)
            }
        }
    }



    override fun onResume() {
        super.onResume()
        // Set home as selected when returning to the fragment
        bottomNavigation.selectedItemId = R.id.navigation_home
        checkLocationServices()
        updateWalletDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationUpdateListener?.let { listener ->
            mapView.location.removeOnIndicatorPositionChangedListener(listener)
        }
        connectivityManager.unregisterNetworkCallback(networkCallback)

        // Add this block
        walletUpdateReceiver?.let {
            requireActivity().unregisterReceiver(it)
        }
    }

    private fun updateWallet(amount: Double) {
        val editor = sharedPreferences?.edit()
        editor?.putFloat("wallet_balance", amount.toFloat())
        editor?.apply()
    }

}
