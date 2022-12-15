package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by inject()
    private lateinit var binding: FragmentRemindersBinding

    companion object {
        private val runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        private val foregroundPermissionIndex = 0
        private val backgroundPermissionIndex = 1
        private val requestDeviceLocation = 29
        private val requestForegroundPermission = 34
        private val requestForegroundAndBackgroundPermissions = 33
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_reminders, container, false
        )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        requestForegroundAndBackgroundLocationPermissions()
    }

    private fun navigateToSaveReminder() {
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {}

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                proceedToSignOut()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    // start sign out and navigate to authentication activity when success
    private fun proceedToSignOut() {
        AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
            navigateToAuthLandingPage()
        }
    }

    private fun navigateToAuthLandingPage() {
        Intent(requireActivity(), AuthenticationActivity::class.java).apply {
            startActivity(this)
            requireActivity().finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
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
        Log.e(
            "TAG",
            "requestForegroundAndBackgroundLocationPermissions: " + foregroundAndBackgroundLocationPermissionApproved()
        )
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            navigateToSaveReminder()
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
                navigateToSaveReminder()
            }
        }
    }

}
