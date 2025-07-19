package com.zaigame.ecospotmapgoogle
// --- START OF CORRECTED IMPORT SECTION ---
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.zaigame.ecospotmapgoogle.services.GamificationService

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // Map and Location variables
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var currentRoutePolyline: Polyline? = null

    // Places and Directions variables
    private lateinit var placesClient: PlacesClient
    private lateinit var directionsApiService: DirectionsApiService

    // Gamification variables
    private lateinit var gamificationService: GamificationService

    // UI variables for Bottom Sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvPlaceName: TextView
    private lateinit var tvPlaceAddress: TextView
    private lateinit var btnGetDirections: Button
    private lateinit var ivPlacePhoto: ImageView

    // 3D View variables
    private var is3DModeEnabled = false
    private var lastZoomLevel = 0f
    private val HIGH_ZOOM_THRESHOLD = 18f
    private val MAX_ZOOM_LEVEL = 21f
    private val MIN_TILT_ANGLE = 30f
    private val MAX_TILT_ANGLE = 60f
    private val BEARING_ANGLE_3D = 0f
    private val ANIMATION_DURATION_MS = 2000L

    // Haptic feedback
    private lateinit var vibrator: Vibrator

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize APIs
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize haptic feedback
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Initialize gamification service
        gamificationService = GamificationService(this)

        // Initialize Retrofit for Directions API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsApiService = retrofit.create(DirectionsApiService::class.java)

        // Initialize UI Components
        setupBottomSheet()
        setupSearchView()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this) // Set this activity to listen for marker clicks

        // Enable 3D buildings
        mMap.isBuildingsEnabled = true

        // Set up camera change listener for immersive 3D view
        setupImmersive3DView()

        enableMyLocation()

        // Set up location button
        findViewById<FloatingActionButton>(R.id.my_location_button).setOnClickListener {
            lastKnownLocation?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            }
        }

        // Set up 3D toggle button
        findViewById<FloatingActionButton>(R.id.three_d_toggle_button).setOnClickListener {
            toggle3DMode()
        }

        // Set up profile button
        findViewById<FloatingActionButton>(R.id.profile_button).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Retrieve the Place ID stored in the marker's tag
        val placeId = marker.tag as? String
        if (placeId == null) {
            Toast.makeText(this, "No details available for this location.", Toast.LENGTH_SHORT).show()
            return false
        }
        getPlaceDetails(placeId)
        return false // Return false to allow default behavior (camera centers on marker)
    }

    private fun setupBottomSheet() {
        val bottomSheetLayout = findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN // Start hidden

        // Find child views from the bottomSheetLayout itself (better practice)
        tvPlaceName = bottomSheetLayout.findViewById(R.id.tv_place_name)
        tvPlaceAddress = bottomSheetLayout.findViewById(R.id.tv_place_address)
        btnGetDirections = bottomSheetLayout.findViewById(R.id.btn_get_directions)
        ivPlacePhoto = bottomSheetLayout.findViewById(R.id.iv_place_photo)
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performTextSearch(query)
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                    searchView.clearFocus()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?) = false
        })
    }

    private fun performTextSearch(query: String) {
        mMap.clear()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        Toast.makeText(this, "Searching for '$query'...", Toast.LENGTH_SHORT).show()

        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        val request = SearchByTextRequest.builder(query, placeFields).setMaxResultCount(10).build()

        placesClient.searchByText(request).addOnSuccessListener { response ->
            if (response.places.isEmpty()) {
                Toast.makeText(this, "No results found for '$query'.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            val boundsBuilder = LatLngBounds.Builder()
            for (place in response.places) {
                place.latLng?.let { latLng ->
                    boundsBuilder.include(latLng)
                    val marker = mMap.addMarker(
                        MarkerOptions().position(latLng).title(place.name)
                    )
                    marker?.tag = place.id // IMPORTANT: Store the Place ID in the marker
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }.addOnFailureListener { exception ->
            Log.e("Places API", "Text search failed: $exception")
        }
    }

    private fun getPlaceDetails(placeId: String) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHOTO_METADATAS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            tvPlaceName.text = place.name
            tvPlaceAddress.text = place.address

            // Handle the photo
            val photoMetadata = place.photoMetadatas?.firstOrNull()
            if (photoMetadata != null) {
                val photoRequest = FetchPhotoRequest.builder(photoMetadata).build()
                placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
                    ivPlacePhoto.setImageBitmap(fetchPhotoResponse.bitmap)
                    ivPlacePhoto.visibility = View.VISIBLE
                }.addOnFailureListener {
                    ivPlacePhoto.visibility = View.GONE
                }
            } else {
                ivPlacePhoto.visibility = View.GONE
            }

            // Set up directions button
            btnGetDirections.setOnClickListener {
                if (lastKnownLocation != null && place.latLng != null) {
                    drawRoute(LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude), place.latLng!!)
                } else {
                    Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show()
                }
            }
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }.addOnFailureListener { exception ->
            Log.e("Places API", "Place details failed: $exception")
            Toast.makeText(this, "Failed to get place details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        // Use Kotlin Coroutines for network call on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val originStr = "${origin.latitude},${origin.longitude}"
                val destStr = "${destination.latitude},${destination.longitude}"
                val response = directionsApiService.getDirections(originStr, destStr, getString(R.string.google_maps_key))

                if (response.isSuccessful) {
                    val overviewPolyline = response.body()?.routes?.firstOrNull()?.overview_polyline?.points
                    if (overviewPolyline != null) {
                        // Use the non-KTX version of decode
                        val decodedPath = PolyUtil.decode(overviewPolyline)

                        // Switch back to the Main thread to draw on the map
                        launch(Dispatchers.Main) {
                            currentRoutePolyline?.remove() // Remove old route
                            currentRoutePolyline = mMap.addPolyline(
                                PolylineOptions().addAll(decodedPath).color(Color.BLUE).width(12f)
                            )
                            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                } else {
                    Log.e("Directions API", "API call failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Directions API", "Exception: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lastKnownLocation = location
                if (location != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 14f))
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 10f))
                }
            }
        } else {
            requestMultiplePermissionsLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /**
     * Sets up the immersive 3D view functionality
     * Monitors camera zoom level and automatically enables 3D mode for dense city areas
     */
    private fun setupImmersive3DView() {
        mMap.setOnCameraMoveListener {
            val currentZoom = mMap.cameraPosition.zoom
            val currentPosition = mMap.cameraPosition

            // Check if we should transition to 3D mode
            if (currentZoom >= HIGH_ZOOM_THRESHOLD && !is3DModeEnabled) {
                enable3DMode(currentPosition)
            } else if (currentZoom < HIGH_ZOOM_THRESHOLD && is3DModeEnabled) {
                disable3DMode(currentPosition)
            }

            // Update tilt dynamically based on zoom level for more immersive experience
            if (is3DModeEnabled && currentZoom > HIGH_ZOOM_THRESHOLD) {
                updateDynamicTilt(currentPosition)
            }

            // Update 3D toggle button appearance based on current state
            update3DToggleButtonAppearance()

            lastZoomLevel = currentZoom
        }
    }

    /**
     * Enables immersive 3D mode with dramatic camera positioning
     */
    private fun enable3DMode(currentPosition: CameraPosition) {
        is3DModeEnabled = true

        // Calculate dynamic tilt based on zoom level
        val dynamicTilt = calculateDynamicTilt(currentPosition.zoom)

        // Create a dramatic 3D camera position
        val cameraPosition3D = CameraPosition.Builder()
            .target(currentPosition.target)
            .zoom(currentPosition.zoom)
            .tilt(dynamicTilt) // Dynamic tilt for immersive view
            .bearing(BEARING_ANGLE_3D)
            .build()

        // Provide haptic feedback for 3D mode activation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100L)
        }

        // Animate to 3D position with smooth transition
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition3D),
            ANIMATION_DURATION_MS.toInt(),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    // Show immersive mode indicator
                    Toast.makeText(
                        this@MainActivity,
                        "🌆 Immersive 3D View Enabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCancel() {
                    // Handle cancellation if needed
                }
            }
        )

        Log.d("3D View", "Immersive 3D mode enabled at zoom level: ${currentPosition.zoom} with tilt: $dynamicTilt")
    }

    /**
     * Disables 3D mode and returns to normal top-down view
     */
    private fun disable3DMode(currentPosition: CameraPosition) {
        is3DModeEnabled = false

        // Return to normal top-down view
        val cameraPositionNormal = CameraPosition.Builder()
            .target(currentPosition.target)
            .zoom(currentPosition.zoom)
            .tilt(0f) // Top-down view
            .bearing(0f)
            .build()

        // Animate back to normal position
        mMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPositionNormal),
            ANIMATION_DURATION_MS.toInt(),
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    Toast.makeText(
                        this@MainActivity,
                        "🗺️ Returned to Normal View",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCancel() {
                    // Handle cancellation if needed
                }
            }
        )

        Log.d("3D View", "3D mode disabled at zoom level: ${currentPosition.zoom}")
    }

    /**
     * Manually toggle 3D mode for user control
     */
    private fun toggle3DMode() {
        val currentPosition = mMap.cameraPosition

        if (is3DModeEnabled) {
            disable3DMode(currentPosition)
        } else {
            // Check if zoom level is appropriate for 3D mode
            if (currentPosition.zoom >= HIGH_ZOOM_THRESHOLD) {
                enable3DMode(currentPosition)
            } else {
                // Zoom to appropriate level first, then enable 3D
                val targetZoom = HIGH_ZOOM_THRESHOLD + 1f
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentPosition.target, targetZoom)
                mMap.animateCamera(cameraUpdate, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        enable3DMode(mMap.cameraPosition)
                    }
                    override fun onCancel() {
                        // Handle cancellation
                    }
                })
            }
        }
    }

    /**
     * Calculate dynamic tilt angle based on zoom level for more immersive experience
     */
    private fun calculateDynamicTilt(zoomLevel: Float): Float {
        val zoomRange = MAX_ZOOM_LEVEL - HIGH_ZOOM_THRESHOLD
        val currentZoomInRange = (zoomLevel - HIGH_ZOOM_THRESHOLD).coerceAtLeast(0f)
        val tiltProgress = (currentZoomInRange / zoomRange).coerceIn(0f, 1f)

        return MIN_TILT_ANGLE + (tiltProgress * (MAX_TILT_ANGLE - MIN_TILT_ANGLE))
    }

    /**
     * Update tilt dynamically as user zooms for continuous immersive experience
     */
    private fun updateDynamicTilt(currentPosition: CameraPosition) {
        val newTilt = calculateDynamicTilt(currentPosition.zoom)
        val currentTilt = currentPosition.tilt

        // Only update if tilt change is significant (more than 5 degrees)
        if (kotlin.math.abs(newTilt - currentTilt) > 5f) {
            val updatedPosition = CameraPosition.Builder()
                .target(currentPosition.target)
                .zoom(currentPosition.zoom)
                .tilt(newTilt)
                .bearing(currentPosition.bearing)
                .build()

            mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(updatedPosition)
            )
        }
    }

    /**
     * Update the 3D toggle button appearance to reflect current state
     */
    private fun update3DToggleButtonAppearance() {
        val toggleButton = findViewById<FloatingActionButton>(R.id.three_d_toggle_button)
        val currentZoom = mMap.cameraPosition.zoom

        if (is3DModeEnabled) {
            // 3D mode is active - show active state
            toggleButton.setColorFilter(Color.parseColor("#4CAF50")) // Green tint
            toggleButton.alpha = 1.0f
        } else if (currentZoom >= HIGH_ZOOM_THRESHOLD) {
            // Zoom level supports 3D mode but not active - show available state
            toggleButton.setColorFilter(Color.parseColor("#FF9800")) // Orange tint
            toggleButton.alpha = 0.8f
        } else {
            // Zoom level too low for 3D mode - show disabled state
            toggleButton.setColorFilter(Color.GRAY)
            toggleButton.alpha = 0.5f
        }
    }
}