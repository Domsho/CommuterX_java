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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder


class FirstFragment : Fragment() {
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager

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

        setupMapbox()
        setupFirebase(view)
        setupPermissions()

        return view
    }

    private fun setupMapbox() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            Log.d("Mapbox", "Style loaded successfully.")
            enableLocationTracking()
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
                    val map = dataSnapshot.value as? HashMap<String, Any>
                    val username = map?.get("username") as? String
                    greetingTextView.text = if (username.isNullOrEmpty()) "Hi" else "Hi, $username"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error fetching data", error.toException())
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


        mapView.gestures.pitchEnabled = false

        mapView.location.addOnIndicatorPositionChangedListener { point ->
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).zoom(14.0).build())
            mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(point)
        }
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
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
