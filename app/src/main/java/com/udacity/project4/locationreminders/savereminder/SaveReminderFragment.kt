package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var selectedReminderItem: ReminderDataItem

    private lateinit var geofencingClient: GeofencingClient
    private val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, flag)
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "Udacity.Project4.LocationReminder.ACTION_GEOFENCE_EVENT"
        private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        private const val foregroundPermissionIndex = 0
        private const val backgroundPermissionIndex = 1
        private const val requestDeviceLocation = 29
        private const val requestForegroundPermission = 34
        private const val requestForegroundAndBackgroundPermissions = 33
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value
            selectedReminderItem =
                ReminderDataItem(title, description, location, latitude, longitude)
            if (_viewModel.validateEnteredData(selectedReminderItem)) {
                // check if permission enabled to start add geofence
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                   checkDeviceLocationSettingsAndStartSaveReminder()
                } else {
                    // if not approved request them
                    requestForegroundAndBackgroundLocationPermissions()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }


    // check if foregroundAndBackgroundLocationPermissionApproved is true or false
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ))
        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    // if location permissions not approved request them
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartSaveReminder()
            return
        }
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val requestCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                requestForegroundAndBackgroundPermissions
            }
            else -> requestForegroundPermission
        }
        requestPermissions(permissionsArray, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestDeviceLocation) {
            if (resultCode != Activity.RESULT_OK) {
                checkDeviceLocationSettingsAndStartSaveReminder(false)
            }
        }
    }

    // on callback check if user grant permissions or not
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (grantResults.isEmpty() || grantResults[foregroundPermissionIndex] == PackageManager.PERMISSION_DENIED || (requestCode == requestForegroundAndBackgroundPermissions && grantResults[backgroundPermissionIndex] == PackageManager.PERMISSION_DENIED)) {
            // when user denied them again display Snackbar to navigate to app setting to let user accepted them manually
            Snackbar.make(
                requireView(), R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
        } else {
            // handle user device location is on
            checkDeviceLocationSettingsAndStartSaveReminder()
        }
    }

    // handle user device location is on
    private fun checkDeviceLocationSettingsAndStartSaveReminder(resolve: Boolean = true) {
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
                    checkDeviceLocationSettingsAndStartSaveReminder()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            Log.e("TAG", "checkDeviceLocationSettingsAndStartSaveReminder:  $it")
            if (it.isSuccessful) {
                Log.e("TAG", "checkDeviceLocationSettingsAndStartSaveReminder:  SUCCESS")
                addGeofenceForSelectedLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceForSelectedLocation() {
        val geofence = Geofence.Builder().setRequestId(selectedReminderItem.id).setCircularRegion(
            selectedReminderItem.latitude!!, selectedReminderItem.longitude!!, 100f
        ).setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()

        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.e("TAG", geofence.requestId)
                _viewModel.validateAndSaveReminder(selectedReminderItem)

            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT
                ).show()
                if ((it.message != null)) {
                    Log.w("TAG", it.message.toString())
                }
            }
        }
    }

}
