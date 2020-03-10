package uk.com.egecius.inappupdatesdemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // Creates instance of the manager.
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private val fakeAppUpdateManager: FakeAppUpdateManager by lazy {
        FakeAppUpdateManager(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableFakeUpdate()
        setClickListeners()
    }

    private fun enableFakeUpdate() {
        fakeAppUpdateManager.setUpdateAvailable(2)
    }

    private fun setClickListeners() {
        check_for_update.setOnClickListener {
            checkIfUpdateAvailable()
        }
        start_update_flow.setOnClickListener {
            startUpdateFlow()
        }
    }

    private fun startUpdateFlow() {
        fakeAppUpdateManager.userAcceptsUpdate()
        fakeAppUpdateManager.downloadStarts()
    }

    private fun checkIfUpdateAvailable() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val isUpdateAvailable = appUpdateInfo.updateAvailability() == UPDATE_AVAILABLE
            val isUpdateTypeImmediate = appUpdateInfo.isUpdateTypeAllowed(IMMEDIATE)
            if (isUpdateAvailable && isUpdateTypeImmediate) {
                requestUpdate(appUpdateInfo)
            }
        }
    }

    private fun requestUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
            IMMEDIATE,
            // The current activity making the update request.
            this,
            // Include a request code to later monitor this update request.
            MY_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MY_REQUEST_CODE) {
            when (requestCode) {
                RESULT_OK -> Log.v("Eg:MainActivity:58", "onActivityResult(): result successful")
                RESULT_CANCELED -> Log.v(
                    "Eg:MainActivity:59",
                    "onActivityResult(): the user has denied or cancelled the update"
                )
                RESULT_IN_APP_UPDATE_FAILED -> Log.v(
                    "Eg:MainActivity:60",
                    "onActivityResult(): Some other error prevented either the user from providing consent or the update to proceed."
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // In case the user has restarted the app while update is running int the background,
        // prevent them from proceeding and resume a potentially stalled update
        // You should execute this check at all entry points into the app.
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        IMMEDIATE,
                        this,
                        MY_REQUEST_CODE
                    )
                }
            }
    }

    companion object {
        const val MY_REQUEST_CODE = 813132
    }
}
