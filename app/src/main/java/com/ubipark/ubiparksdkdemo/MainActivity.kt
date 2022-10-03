package com.ubipark.ubiparksdkdemo

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.ubipark.ubiparksdk.BeaconLogLevel
import com.ubipark.ubiparksdk.UbiParkSDKConfig
import com.ubipark.ubiparksdk.api.CarParkAPI
import com.ubipark.ubiparksdk.api.UserAPI
import com.ubipark.ubiparksdk.models.*
import com.ubipark.ubiparksdk.services.BeaconJobService
import com.ubipark.ubiparksdk.services.BeaconService
import com.ubipark.ubiparksdk.services.BeaconServiceCallback
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"

    private var _beaconServiceCallback: BeaconManagerCallback? = null
    private var beaconService = BeaconService
    private var beaconServiceStarted = false

    private var fusedLocationClient: FusedLocationProviderClient? = null
    var PERMISSION_ID = 44

    val JOB_SCHEDULER_ID = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _beaconServiceCallback = BeaconManagerCallback(this@MainActivity)

        // Apply client specific SDK settings.
        UbiParkSDKConfig.setAppId("{ADD YOUR AppID HERE}")
        UbiParkSDKConfig.setServerName("https://staging.ubipark.com") // https://api.ubipark.com
        UbiParkSDKConfig.setBeaconToken("{ADD YOUR BeaconToken HERE}")
        UbiParkSDKConfig.setClientSecret("{ADD YOUR ClientSecret HERE}")

        UbiParkSDKConfig.setBeaconLogLevel(BeaconLogLevel.HIGH)

        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val textView = findViewById<View>(R.id.statusTextView) as TextView
        textView.setText("Ready.")
    }

    override fun onResume() {
        super.onResume()

        if (checkPermissions()) {
            getLastLocation()
        }

        if (beaconServiceStarted) {
            beaconService.onForeground()
        }

        val textView = findViewById<View>(R.id.statusTextView) as TextView
        var text = "Resuming. UserId = " + UbiParkSDKConfig.getUserId().toString()
        val statusResult = beaconService.getUserStatusCurrent()

        if (statusResult != null) {
            text += ", User Status:" + statusResult.toString()
        } else {
            text += ", User Status: null"

        }

        textView.setText(text)
    }

    override fun onPause() {
        super.onPause()
        if (beaconServiceStarted) {
            beaconService.onBackground()
        }
    }

    /* Location Services */
    fun requestLocation_Click(view: View) {
        getLastLocation()
    }

    fun requestBackgroundLocation_Click(view: View) {
        checkBackgroundLocationPermissionAPI30(1)
    }

    private fun Context.checkSinglePermission(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(30)
    private fun Context.checkBackgroundLocationPermissionAPI30(backgroundLocationRequestCode: Int) {
        if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location access for this app")
        builder.setMessage("Allow app to use background location")
        builder.setPositiveButton("Yes") { dialog, which ->
            // this request will take user to Application's Setting page
            requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), backgroundLocationRequestCode)
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.create()
        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                fusedLocationClient?.getLastLocation()?.addOnCompleteListener {
                    val locationTask = it
                    if (locationTask != null) {
                        val location = locationTask.getResult()
                        if (location == null) {
                            requestNewLocationData()
                        } else {
                            UbiParkSDKConfig.setCurrentLatitude(location.getLatitude())
                            UbiParkSDKConfig.setCurrentLongitude(location.getLatitude())
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG)
                        .show()
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates = 1

        val fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        Looper.myLooper()
            ?.let { fusedLocation.requestLocationUpdates(locationRequest, mLocationCallback, it) }
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            UbiParkSDKConfig.setCurrentLatitude(lastLocation.getLatitude())
            UbiParkSDKConfig.setCurrentLongitude(lastLocation.getLatitude())
        }
    }

    // method to check for permissions
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        ), PERMISSION_ID
        )
    }

    // method to check
    // if location is enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        )
    }

    /* UserAPI Samples */
    fun create_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        val userRequest = UserRequest(
                email = "test6@ubipark.com",
                password = "testPassword",
                firstname = "TestFirstName",
                lastname = "TestLastName",
                phoneNo = "+61414000000"
        )

        UserAPI.create(userRequest, callback = ({
            spinner.setVisibility(View.GONE)

            val userResult = it

            if (userResult != null) {
                Log.v(TAG, "Create result:" + userResult.toString())
                Toast.makeText(
                        view.context,
                        "Create result:" + userResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
                UbiParkSDKConfig.setUserId(userResult.userId)
                UbiParkSDKConfig.setUserToken(userResult.authenticationToken)
            }
        }))
    }

    fun createUser_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        /* Create a user without a password */
        val createUserRequest = CreateUserRequest(
                email = "test7@ubipark.com",
                firstname = "TestFirstName",
                lastname = "TestLastName",
                phoneNo = "+61414000000"
        )

        UserAPI.createUser(createUserRequest, callback = ({
            spinner.setVisibility(View.GONE)

            val createUserResult = it

            if (createUserResult != null) {
                Log.v(TAG, "CreateUser result:" + createUserResult.toString())
                Toast.makeText(
                        view.context,
                        "CreateUser result:" + createUserResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
                UbiParkSDKConfig.setUserId(createUserResult.userId)
                UbiParkSDKConfig.setUserToken(createUserResult.authenticationToken)
            }
        }))
    }

    fun login_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        val username = "test1@ubipark.com"
        val password = "testPassword"

        UserAPI.login(username, password, callback = ({
            spinner.setVisibility(View.GONE)

            val loginResult = it

            if (loginResult != null) {
                Log.v(TAG, "Login result:" + loginResult.toString())
                Toast.makeText(
                        view.context,
                        "Login result:" + loginResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
                if (loginResult.result == "Success") {
                    UbiParkSDKConfig.setUserId(loginResult.userId)
                    UbiParkSDKConfig.setUserToken(loginResult.authenticationToken)
                } else {
                    UbiParkSDKConfig.resetUser() // clear existing userId and authenticationToken
                }
            }
        }))
    }

    fun detail_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        UserAPI.detail(callback = ({
            spinner.setVisibility(View.GONE)

            val detailResult = it

            if (detailResult != null) {
                Log.v(TAG, "UserDetail result:" + detailResult.toString())
                Toast.makeText(
                        view.context,
                        "UserDetail result:" + detailResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }
        }))
    }

    fun update_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        UserAPI.detail(callback = ({
            val detailResult = it

            if (detailResult != null) {
                val updateRequest = UpdateRequest(
                        email = detailResult.email,
                        firstName = detailResult.firstName,
                        lastName = detailResult.lastName,
                        phoneNo = detailResult.phoneNo
                )

                updateRequest.lastName += "x"

                UserAPI.update(updateRequest, callback = ({
                    spinner.setVisibility(View.GONE)

                    val updateResult = it

                    if (updateResult != null) {
                        Log.v(TAG, "Update result:" + updateResult.toString())
                        Toast.makeText(
                                view.context,
                                "Update result:" + updateResult.toString(),
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }))
            }
        }))
    }

    fun status_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        UserAPI.status(callback = ({
            spinner.setVisibility(View.GONE)

            val statusResult = it

            if (statusResult != null) {
                Log.v(TAG, "Status result:" + statusResult.toString())
                Toast.makeText(
                        view.context,
                        "Status result:" + statusResult.toString(),
                        Toast.LENGTH_LONG
                ).show()

                // Set the status of the user, so that the beacon service
                // knows what state the user is in.
                // e.g. if user is not in car park then search for entry beacons,
                //      if user is in car park search for exit beacons.
                beaconService.setUserStatus(statusResult)
            }
        }))
    }

    fun authToken_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        UserAPI.authToken("test1@ubipark.com", null, callback = ({
            spinner.setVisibility(View.GONE)

            val authTokenResult = it

            if (authTokenResult != null) {
                Log.v(TAG, "AuthToken result:" + authTokenResult.toString())
                Toast.makeText(
                        view.context,
                        "AuthToken result:" + authTokenResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }
        }))
    }

    /* CarParkAPI Samples */
    fun carParkDetail_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        CarParkAPI.detail("7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:5:8", null, callback = ({
            spinner.setVisibility(View.GONE)

            val detailsResult = it

            if (detailsResult != null) {
                Log.v(TAG, "CarPark Detail result:" + detailsResult.toString())
                Toast.makeText(
                        view.context,
                        "CarPark Detail result:" + detailsResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }
        }))
    }

    fun enter_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        val laneId: Long = 289 // Test Lane: Entry 1

        CarParkAPI.enter(laneId, null, callback = ({
            spinner.setVisibility(View.GONE)

            val enterResult = it

            if (enterResult != null) {
                Log.v(TAG, "CarPark Enter result:" + enterResult.toString())
                Toast.makeText(
                        view.context,
                        "CarPark Enter result:" + enterResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }
        }))
    }

    fun exit_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        val laneId: Long = 293 // Test Lane: Exit 2

        CarParkAPI.exit(laneId, laneId, callback = ({
            spinner.setVisibility(View.GONE)

            val exitResult = it

            if (exitResult != null) {
                Log.v(TAG, "CarPark Exit result:" + exitResult.toString())
                Toast.makeText(
                        view.context,
                        "CarPark Exit result:" + exitResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }
        }))
    }

    /* Beacon Service Samples */
    fun startService_Click(view: View) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) !== PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN)) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            }else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            }
        }

        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        beaconService.initService(this, _beaconServiceCallback as BeaconServiceCallback)

        // Disable the timer on the BeaconService so that beacon detection
        // is not reset while debugging
        beaconService.setTimerEnabled(false)

        // Modify any BluecatsSDK options if needed
        //BlueCatsSDK.setOptions([
        //    BCOptionMaximumDailyBackgroundUsageInMinutes: 1440,
        //    BCOptionBackgroundSessionTimeIntervalInSeconds: 3 * 60 * 60
        //])

        beaconService.startService(this, callback = ({
            runOnUiThread {
                spinner.setVisibility(View.GONE)
            }

            val beaconServiceResult = it

            Log.v(TAG, "startService result:" + beaconServiceResult.toString())
            runOnUiThread {
                Toast.makeText(
                        view.context,
                        "startService result:" + beaconServiceResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }

            beaconServiceStarted = true

            // Tell the BeaconService tha the app is in the foreground so that it will
            // scan for beacons more aggressively - call beaconService.onBackground()
            // when app is sent to background to conserve battery and avoid battery
            // usage warnings
            beaconService.onForeground()
        }))
    }

    fun startServiceAsJob_Click(view: View) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) !== PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN)) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            }else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 1)
            }
        }

        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        // Set variables on service
        beaconService.initService(this, _beaconServiceCallback as BeaconServiceCallback)

        // Disable the timer on the BeaconService so that beacon detection
        // is not reset while debugging
        beaconService.setTimerEnabled(false)

        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler

        val jobInfo = JobInfo.Builder(JOB_SCHEDULER_ID, ComponentName(this, BeaconJobService::class.java))
                .setMinimumLatency(0)
                .build()

        jobScheduler.schedule(jobInfo)

        beaconServiceStarted = true

        runOnUiThread {
            spinner.setVisibility(View.GONE)
        }
    }

    fun stopService_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        beaconService.stopService(callback = ({
            runOnUiThread {
                spinner.setVisibility(View.GONE)
            }

            val beaconServiceResult = it

            Log.v(TAG, "stopService result:" + beaconServiceResult.toString())
            runOnUiThread {
                Toast.makeText(
                        view.context,
                        "stopService result:" + beaconServiceResult.toString(),
                        Toast.LENGTH_LONG
                ).show()
            }

            beaconServiceStarted = false
        }))
    }

    fun statusService_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        beaconService.getStatus(callback = ({
            runOnUiThread {
                spinner.setVisibility(View.GONE)
            }

            val beaconServiceResult = it

            Log.v(TAG, "statusService result:" + beaconServiceResult.toString())
            runOnUiThread {
                Toast.makeText(
                    view.context,
                    "statusService result:" + beaconServiceResult.toString(),
                    Toast.LENGTH_LONG
                ).show()

                val textView = findViewById<View>(R.id.statusTextView) as TextView
                textView.setText("statusService result:" + beaconServiceResult.toString())
            }
        }))
    }

    fun updateCarParkText(carParkText: String) {
        runOnUiThread {
            val textView = findViewById<View>(R.id.carParkTextView) as TextView
            textView.setText("Car Park: " + carParkText)
        }
    }

    fun updateClosestLane(closestLaneText: String) {
        runOnUiThread {
            val textView = findViewById<View>(R.id.closestLaneTextView) as TextView
            textView.setText("Closest Lane: " + closestLaneText)
        }
    }

    class BeaconManagerCallback(context: Context) : BeaconServiceCallback
    {
        private var TAG = "BeaconManagerCallback"
        private var _context = context

        override fun carParkNameChanged(carParkName: String) {
            Log.v(TAG, "Car Park Found: " + carParkName)
            (_context as MainActivity).updateCarParkText(carParkName)
        }

        override fun carParkIdChanged(carParkId: Long) {
            Log.v(TAG, "CarParkId: " + carParkId.toString())
        }

        override fun beaconsChanged(beacons: ArrayList<BeaconModel>?) {
            Log.v(TAG,"BeaconsChanged")
        }

        override fun lanesChanged(lanes: ArrayList<LaneModel>?) {
            Log.v(TAG,"LanesChanged: ")
            if (lanes != null) {
                for (lane in lanes) {
                    Log.v(TAG, "Lane: " + lane.name + ", " + lane.shortName)
                }
            }
        }

        override fun laneBeaconsChanged(laneBeacons: ArrayList<LaneModel>?) {
            Log.v(TAG,"LaneBeaconsChanged")
        }

        override fun closestLaneChanged(closestLane: LaneModel?) {
            if (closestLane != null) {
                Log.v(TAG,"Closest Lane is now: " + closestLane.name + ", " + closestLane.shortName)
                (_context as MainActivity).updateClosestLane(closestLane.name + ", " + closestLane.shortName)

            } else {
                Log.v(TAG,"No Closest Lane")
                (_context as MainActivity).updateClosestLane("No Closest Lane")
            }
        }
    }
}