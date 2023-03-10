package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var marker: Marker? = null

    companion object {
        private const val requestPermissionCode = 80
        private const val requestDeviceLocation = 81
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        binding.saveLocationBtn.setOnClickListener(View.OnClickListener {
            onLocationSelected()
        })

        return binding.root
    }

    private fun onLocationSelected() {
        Log.e("TAG", "onLocationSelected: " + marker?.title)
        _viewModel.reminderSelectedLocationStr.value = marker?.title
        _viewModel.latitude.value = marker?.position?.latitude
        _viewModel.longitude.value = marker?.position?.longitude
        _viewModel.reminderSelectedLocationStr.value = marker?.title
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onMapReady(p0: GoogleMap) {
        if (p0 != null) {

            map = p0
            enableMyLocationButton()

//            if (!ifisPermissionGranted()) {
//                checkDeviceLocationSettingsAndLocationListener()
//            }
            setMapLongClick(map)
            setPoiClick(map)
            setMapStyle(map)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.map_style
                )
            )
            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            poiMarker?.showInfoWindow()
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(poi.latLng, 16f)
            )
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(), "Lat: %1$.5f, Long: %2$.5f", latLng.latitude, latLng.longitude
            )
            marker = map.addMarker(
                MarkerOptions().position(latLng).title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            )
        }
    }


    // check if permission granted
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    // handle request permissions response
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // if location permission granted enable my location btn
        if (requestPermissionCode == requestCode) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocationButton()
            } else {
                _viewModel.showSnackBar.value = getString(R.string.permission_denied_explanation)
            }
        }
    }

    private fun enableMyLocationButton() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            this.requestPermissions(
                arrayOf<String>(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), requestPermissionCode
            )
        } else {
            Log.e("TAG", "enableMyLocationButton: Start Enable Location")
            map.isMyLocationEnabled = true
            checkDeviceLocationSettingsAndLocationListener()
        }
    }

    // get current user location and move camera to it and add marker to this location
    private fun startListenForLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            Log.e("TAG", "startListenForLocation: " + location)
            location?.let {
                // when get location permission just assign lat and long to map camera and add new marker
                val userLocation = LatLng(location.latitude, location.longitude)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        userLocation, 15f
                    )
                )
                marker = map.addMarker(
                    MarkerOptions().position(userLocation).title("Current Location")
                )
                marker?.showInfoWindow()
            }
            if (location == null) {
                startListenForLocation()
            }
        }
    }

    // handle user device location is on
    private fun checkDeviceLocationSettingsAndLocationListener(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        requestDeviceLocation,
                        null,
                        0,
                        0,
                        0,
                        null
                    )

                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(
                        requireContext(),
                        "Error Occurred when getting location setting",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            } else {
                Snackbar.make(
                    requireView(), R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndLocationListener()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            Log.e("TAG", "checkDeviceLocationSettingsAndStartSaveReminder:  SUCCESS")
            startListenForLocation()
        }
    }
}