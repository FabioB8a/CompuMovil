package com.example.taller02_computacinmvil

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller02_computacinmvil.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.logging.Logger

/**
 *  Coded by: Fabio Luis Buitrago Ochoa
 */

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    /**
     *  5. (10%) Programe el último botón para lanzar una actividad que muestre un mapa con un marcador en la
     *  localización actual del usuario como se muestra en la figura 3.
     */

    // Companion object for constants and class-level variables
    companion object {
        val TAG: String = CameraActivity::class.java.name
        const val lowerLeftLatitude = 4.5733      // Latitude of the lower-left boundary of Bogotá
        const val lowerLeftLongitude = -74.1502   // Longitude of the lower-left boundary of Bogotá
        const val upperRightLatitude = 4.8121     // Latitude of the upper-right boundary of Bogotá
        const val upperRightLongitude = -74.0096  // Longitude of the upper-right boundary of Bogotá
    }
    private val logger = Logger.getLogger(TAG)

    // Map-related variables
    private lateinit var mMap: GoogleMap
    private var isMapInitialized = false
    private lateinit var binding: ActivityMapsBinding

    // List to store previous user locations
    private val previousLocations: MutableList<LatLng> = ArrayList()

    // Location-related variables
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    var mCurrentLocation: Location? = null

    // Constants for location permissions and settings request
    private val LOCATION_PERMISSION_ID = 103
    private val REQUEST_CHECK_SETTINGS = 201
    var locationPerm = Manifest.permission.ACCESS_FINE_LOCATION

    // Sensor variables
    lateinit var sensorManager: SensorManager
    lateinit var lightSensor: Sensor
    lateinit var lightSensorListener: SensorEventListener

    // Geocoder instance for address conversion
    private lateinit var mGeocoder: Geocoder

    // EditText for user input of an address
    lateinit var mAddress: EditText

    // Bono (Bonus Section)
    private var polylineBond: Polyline? = null  // Polyline for bonus route
    private var start: String = ""               // Starting location for bonus route
    private var end: String = ""                 // Ending location for bonus route
    var poly: Polyline? = null                   // Polyline for drawing the route on the map



    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState The saved instance state Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflates the layout and sets it as the content view
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Creates a location request and initializes the fused location client
        mLocationRequest = createLocationRequest()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Requests location permission and initializes the activity
        requestPermission(
            this,
            locationPerm,
            "Permiso para utilizar la localización y colocarla en el mapa",
            LOCATION_PERMISSION_ID
        )

        // Initialize the activity
        init()
    }


    /**
     * Called when the activity is resumed. Registers the light sensor listener and starts location updates.
     */
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        startLocationUpdates()
    }

    /**
     * Called when the activity is paused. Unregisters the light sensor listener and stops location updates.
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightSensorListener)
        stopLocationUpdates()
    }


    /**
     * Initializes various components and settings in the MapsActivity.
     */
    private fun init(){

        // Obtain a reference to the Google Map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /**
         * 6. (10%) El mapa mostrado debe reaccionar a cambios en el sensor de luminosidad y debe presentarse con dos estilos
         * diferentes: oscuro para condiciones de baja luminosidad, y claro para condiciones de alta luminosidad.
         */

        // Initialize the sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Initialize the sensor listener for light changes
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Check for changes in light sensor values and update the map style accordingly
                if (isMapInitialized) {
                    if (event.values[0] < 5000) {
                        Log.i("MAPS", "DARK MAP " + event.values[0])
                        mMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                this@MapsActivity,
                                R.raw.style_night
                            )
                        )
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0])
                        mMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                this@MapsActivity,
                                R.raw.style_day_silver
                            )
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, i: Int) {}
        }

        /**
         * 7. (10%) Cuando el usuario se desplace en el mapa (on location change) se debe pintar un polyline que muestre
         * la ruta de desplazamiento del usuario como muestra la figura 3.
         */

        // Set up the location callback to handle user location changes
        mLocationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    logger.info("+"+location.latitude)
                    logger.info("-" +location.longitude)
                    mCurrentLocation = location
                    // Add the location to the list of previous locations
                    previousLocations.add(LatLng(location.latitude, location.longitude))

                    // Draw a Polyline on the map to display the user's route.
                    drawPolylineOnMap(previousLocations)
                }
            }
        }

        // Turn on location updates and initialize location settings
        turnOnLocationAndStartUpdates()

        /**
         * 8. (10%) La actividad de mapas debe mostrar un cuadro de texto que permite al usuario ingresar una
         * dirección en texto claro, por ejemplo “Universidad Javeriana” y cuando termine de editar el texto
         * se debe crear un pin con la dirección encontrada usando Geocoder como muestra la figura 4.
         * (Buscar posición con base en un texto). No olvide mover la cámara del mapa al punto encontrado.
         */

        // Set up the Geocoder and EditText for address search
        mGeocoder = Geocoder(baseContext)
        mAddress = binding.address
        mAddress.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                findAddress()
            }
            false
        }


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapInitialized = true
        if (ContextCompat.checkSelfPermission(
                this,
                locationPerm
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Completar
        }
        else {
                mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {

                    val here = LatLng(location.latitude, location.longitude)
                    mCurrentLocation = Location("")
                    mCurrentLocation!!.longitude = location.longitude
                    mCurrentLocation!!.latitude = location.latitude
                    mMap.addMarker(MarkerOptions().position(here).title("Marker Here"))
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(20f))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(here))

                }
            }

            /**
             * 9. (10 %) Adicionalmente, programe el mapa para que reaccione a un evento de tipo “LongClick” y cree un marcador
             * en la posición del evento. El título del marcador debe ser la dirección obtenida utilizando Geocoder
             * (Buscar texto con base en una posición).
             */
            mMap.setOnMapLongClickListener { latLng ->
                try {
                    val addresses = mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (addresses != null) {
                        if (addresses.isNotEmpty()) {
                            val address = addresses?.get(0)
                            val addressText =
                                address?.getAddressLine(0) // Obten la primera línea de la dirección

                            // Crea un marcador en la posición del "LongClick" con el título de la dirección
                            mMap.addMarker(MarkerOptions()
                                .position(latLng)
                                .title(addressText)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
                            start = "${mCurrentLocation!!.longitude},${mCurrentLocation!!.latitude}"
                            end = "${latLng.longitude},${latLng.latitude}"
                            createRoute()
                            // drawPolylineBond(LatLng(latLng.latitude, latLng.longitude))

                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }


    }


    /**
     *  -- L O C A T I O N - A W A R E --
     */

    /**
     * Initializes the location settings, including checking for necessary permissions,
     * setting location request parameters, and handling location updates.
     */
    private fun turnOnLocationAndStartUpdates() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            mLocationRequest!!
        )
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(
            this
        ) { locationSettingsResponse: LocationSettingsResponse? ->
            startLocationUpdates() // All conditions are met for receiving locations
        }
        task.addOnFailureListener(this) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                CommonStatusCodes.RESOLUTION_REQUIRED ->
                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                        val resolvable = e as ResolvableApiException
                        resolvable.startResolutionForResult(
                            this@MapsActivity,
                            REQUEST_CHECK_SETTINGS
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        // Ignore the error
                    }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
            }
        }
    }

    /**
     * Creates a location request with specified parameters.
     */
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
    }

    /**
     * Starts location updates if permission is granted and updates are requested.
     */
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient!!.requestLocationUpdates(mLocationRequest!!, mLocationCallback!!, Looper.getMainLooper())
        }
    }

    /**
     * Stops location updates if they were previously started.
     */
    private fun stopLocationUpdates() {
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
    }


    /**
     * Draws a polyline on the map using a list of locations.
     */
    private fun drawPolylineOnMap(locations: List<LatLng>) {
        val polylineOptions = PolylineOptions()
        for (location in locations) {
            polylineOptions.add(location)
        }

        // Configure the style of the line, such as color and width.
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(20f)

        // Add the Polyline to the map.
        mMap.addPolyline(polylineOptions)
    }

    /**
     *  -- G E O C O D E R --
     */

    /**
     * Searches for a location based on the user-entered address and adds a marker to the map.
     * If a valid address is found, the map's camera is moved to the new location.
     */
    private fun findAddress() {
        val addressString = mAddress.text.toString()
        if (addressString.isNotEmpty()) {
            try {
                val addresses = mGeocoder.getFromLocationName(
                    addressString, 2,
                    lowerLeftLatitude, lowerLeftLongitude,
                    upperRightLatitude, upperRightLongitude
                )
                if (addresses != null && !addresses.isEmpty()) {
                    val addressResult = addresses[0]
                    val position = LatLng(addressResult.latitude, addressResult.longitude)
                    val lastLocation = previousLocations[previousLocations.size - 1]
                    mMap.addMarker(
                        MarkerOptions().position(position)
                            .title(addressResult.featureName)
                            .snippet(addressResult.getAddressLine(0))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
                } else {
                    Toast.makeText(
                        this@MapsActivity,
                        "Dirección no encontrada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(this@MapsActivity, "La dirección está vacía", Toast.LENGTH_SHORT)
                .show()
        }
    }


    /**
     *  -- B O N O --
     *  ¡BONO! (0.5 sobre la nota del taller) Construya y despliegue en el mapa una ruta entre la localización
     *  actual del usuario y un punto creado a partir de texto como en el punto 8, o tocando un punto en el mapa
     *  como en el punto 9.
     */

    /**
     * Initiates the process of creating a route between two geographical points.
     * Uses the OpenRouteService API to calculate the route and draws it on the map.
     */
    private fun createRoute() {
        CoroutineScope(Dispatchers.IO).launch {
            val call = getRetrofit().create(ApiService::class.java)
                .getRoute("5b3ce3597851110001cf6248d4e9855b7cc848178fd19fe09eea5ebe", start, end)
            if (call.isSuccessful) {
                drawRoute(call.body())
            } else {
                Log.i("aris", "KO")
            }
        }
    }

    /**
     * Draws a route on the map based on the provided route response.
     *
     * @param routeResponse The response containing the route information.
     */
    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            poly = mMap.addPolyline(polyLineOptions)
        }
    }

    /**
     * Creates and configures a Retrofit instance for API requests to OpenRouteService.
     *
     * @return A Retrofit instance with the necessary configurations.
     */
    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    /**
     *  -- P E R M I S S I O N S --
     */

    /**
     * Requests a specific permission with optional justification, and displays a Toast message
     * if permission needs to be explained to the user.
     *
     * @param context The activity context.
     * @param permission The permission to request.
     * @param justification An optional justification message.
     * @param id The unique identifier for the permission request.
     */
    private fun requestPermission(
        context: Activity,
        permission: String,
        justification: String,
        id: Int
    ) {
        // Check if the permission has not been granted
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
            // Should we show an explanation for the permission?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show()
            }
            // Request the permission
            ActivityCompat.requestPermissions(context, arrayOf(permission), id)
        }
    }

    /**
     * Handles the result of a permission request and initiates specific actions.
     *
     * @param requestCode The code associated with the permission request.
     * @param permissions An array of permissions requested.
     * @param grantResults An array of results indicating whether permissions were granted.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if the requestCode corresponds to the LOCATION_PERMISSION_ID
        if (requestCode == LOCATION_PERMISSION_ID) {
            // Initialize the activity after permission result is received
            init()
        }
    }


}