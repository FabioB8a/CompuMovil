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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        val TAG: String = CameraActivity::class.java.name
        private const val INITIAL_ZOOM_LEVEL = 15
        const val lowerLeftLatitude = 4.5733  // Latitud del límite inferior izquierdo de Bogotá
        const val lowerLeftLongitude = -74.1502  // Longitud del límite inferior izquierdo de Bogotá
        const val upperRightLatitude = 4.8121  // Latitud del límite superior derecho de Bogotá
        const val upperRightLongitude = -74.0096  // Longitud del límite superior derecho de Bogotá
    }
    private val logger = Logger.getLogger(TAG)

    private lateinit var mMap: GoogleMap
    private var isMapInitialized = false
    private lateinit var binding: ActivityMapsBinding

    // Agrega esta variable para almacenar las ubicaciones anteriores del usuario.
    private val previousLocations: MutableList<LatLng> = ArrayList()

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    var mCurrentLocation: Location? = null

    private val LOCATION_PERMISSION_ID = 103
    private val REQUEST_CHECK_SETTINGS = 201
    var locationPerm = Manifest.permission.ACCESS_FINE_LOCATION

    lateinit var sensorManager: SensorManager
    lateinit var lightSensor: Sensor
    lateinit var lightSensorListener: SensorEventListener

    private lateinit var mGeocoder: Geocoder
    lateinit var mAddress: EditText

    // Bono
    private var polylineBond: Polyline? = null
    private var start: String = ""
    private var end: String = ""
    var poly: Polyline? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mLocationRequest = createLocationRequest()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermission(
            this,
            locationPerm,
            "Permiso para utilizar la localización y colocarla en el mapa",
            LOCATION_PERMISSION_ID
        )

        init()

    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            lightSensorListener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightSensorListener)
        stopLocationUpdates()
    }



    private fun init(){

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Initialize the listener
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
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

        mLocationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    logger.info("+"+location.latitude)
                    logger.info("-" +location.longitude)
                    mCurrentLocation = location
                    // Agrega la ubicación a la lista de ubicaciones anteriores.
                    previousLocations.add(LatLng(location.latitude, location.longitude))

                    // Dibuja el Polyline en el mapa para mostrar la ruta de desplazamiento.
                    drawPolylineOnMap(previousLocations)
                }
            }
        }

        turnOnLocationAndStartUpdates()

        mGeocoder = Geocoder(baseContext)
        mAddress = binding.address
        mAddress.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                findAddress()
            }
            false
        }


    }


    private fun drawPolylineOnMap(locations: List<LatLng>) {
        val polylineOptions = PolylineOptions()
        for (location in locations) {
            polylineOptions.add(location)
        }

        // Configura el estilo de la línea, como color y ancho.
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(20f)

        // Agrega el Polyline al mapa.
        mMap.addPolyline(polylineOptions)
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

    private fun requestPermission(
        context: Activity,
        permission: String,
        justification: String,
        id: Int
    ) {
        // Verificar si no hay permisos
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                Toast.makeText(context, justification, Toast.LENGTH_SHORT).show()
            }
            // Request the permission
            ActivityCompat.requestPermissions(context, arrayOf(permission), id)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_ID) {
            init()
        }
    }

    private fun turnOnLocationAndStartUpdates() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            mLocationRequest!!
        )
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(
            this
        ) { locationSettingsResponse: LocationSettingsResponse? ->
            startLocationUpdates() // Todas las condiciones para recibiir localizaciones
        }
        task.addOnFailureListener(this) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                CommonStatusCodes.RESOLUTION_REQUIRED ->                         // Location setttings are not satisfied, but this can be fixed by showing the user a dialog.
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

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1).apply {
            setMinUpdateDistanceMeters(5F)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient!!.requestLocationUpdates(mLocationRequest!!, mLocationCallback!!, Looper.getMainLooper())
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
    }

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

    // Bono
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

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polyLineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            polyLineOptions.add(LatLng(it[1], it[0]))
        }
        runOnUiThread {
            poly = mMap.addPolyline(polyLineOptions)
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


}