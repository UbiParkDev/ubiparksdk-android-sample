package com.ubipark.ubiparksdkdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.bluecats.sdk.BlueCatsSDK

import com.ubipark.ubiparksdk.BeaconLogLevel
import com.ubipark.ubiparksdk.UbiParkSDKConfig
import com.ubipark.ubiparksdk.api.CarParkAPI
import com.ubipark.ubiparksdk.api.UserAPI
import com.ubipark.ubiparksdk.models.*
import com.ubipark.ubiparksdk.services.BeaconService
import com.ubipark.ubiparksdk.services.BeaconServiceCallback
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    private val TAG = "MyActivity"

    private var _beaconServiceCallback: BeaconManagerCallback? = null
    private var beaconService = BeaconService()
    private var beaconServiceStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _beaconServiceCallback = BeaconManagerCallback()

        // Apply client specific SDK settings.
        UbiParkSDKConfig.setAppId("com.equiem.app")
        UbiParkSDKConfig.setServerName("https://staging.ubipark.com") // https://api.ubipark.com
        UbiParkSDKConfig.setBeaconToken("a51c21df-72f6-447c-9c30-5652030d2417")
        UbiParkSDKConfig.setClientSecret("5d7dd81c-1229-4733-81b8-14952fb00533")

        UbiParkSDKConfig.setBeaconLogLevel(BeaconLogLevel.HIGH)
    }

    override fun onResume() {
        super.onResume()
        if (beaconServiceStarted) {
            beaconService.onForeground()
        }
    }

    override fun onPause() {
        super.onPause()
        if (beaconServiceStarted) {
            beaconService.onBackground()
        }
    }

    /* UserAPI Samples */
    fun create_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        val userRequest = UserRequest(
                email = "equiemtest6@ubipark.com",
                password = "equiemtest6",
                firstname = "Equiem",
                lastname = "Test6",
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
                email = "equiemtest7@ubipark.com",
                firstname = "Equiem",
                lastname = "Test7",
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

        val username = "equiemtest1@ubipark.com"
        val password = "equiemtest1"

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

                if (updateRequest.lastName.contains("x")) {
                    updateRequest.lastName = "Equiem"
                } else {
                    updateRequest.lastName = "Equiem_x"
                }

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

        UserAPI.authToken("equiemtest1@ubipark.com", null, callback = ({
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

        /* BeaconUIDs */
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:5:8
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:1:19
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:5:7
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:1:2
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:1:1
        //7134D0B8-49DF-487B-B2A5-0ED7CAAB2818:5:10

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

        val laneId: Long = 289 // Car Park: Equiem Test Lane: Entry 1

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

        val laneId: Long = 293 // Car Park: Equiem Test Lane: Exit 2

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

    /* Beacon Serivce Samples */
    fun startService_Click(view: View) {
        val spinner = findViewById<ProgressBar>(R.id.progressBar1)
        spinner.setVisibility(View.VISIBLE)

        beaconService.initService(_beaconServiceCallback as BeaconServiceCallback)

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
            // when app is sent to background to conserver battery and avoid battery
            // usuage warnings
            beaconService.onForeground()
        }))
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
        }))
    }

    class BeaconManagerCallback() : BeaconServiceCallback
    {
        private var TAG = "BeaconManagerCallback"

        override fun carParkNameChanged(carParkName: String) {
            Log.v(TAG, "Car Park Found: " + carParkName)
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

        override fun laneBeaconsChanged(laneBeaconss: ArrayList<LaneModel>?) {
            Log.v(TAG,"LaneBeaconsChanged")
        }

        override fun closestLaneChanged(closestLane: LaneModel?) {
            if (closestLane != null) {
                Log.v(TAG,"Closest Lane is now: " + closestLane.name + ", " + closestLane.shortName)
            } else {
                Log.v(TAG,"No Closest Lane")
            }
        }
    }
}