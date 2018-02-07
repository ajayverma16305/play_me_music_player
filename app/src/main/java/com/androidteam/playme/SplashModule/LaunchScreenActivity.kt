package com.androidteam.playme.SplashModule

import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.androidteam.playme.HelperModule.Constants
import com.androidteam.playme.HelperModule.PermissionManager
import com.androidteam.playme.HelperModule.StorageUtil
import com.androidteam.playme.HelperModule.UtilityApp
import com.androidteam.playme.MainModule.baseModule.BaseActivity
import com.androidteam.playme.R
import java.util.ArrayList
import java.util.HashMap

class LaunchScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_screen)
    }

    override fun onStart() {
        if(UtilityApp.getAppDatabaseValue(this)) {
            if(PermissionManager.checkForStoragePermission(this)){
                Handler().postDelayed(Runnable {
                    showMainScreen()
                },200)
            }
        }
        super.onStart()
    }

    private fun showMainScreen() {
        startActivity(Intent(this, BaseActivity::class.java))
        finish()
    }

    /**
     * On Request Permissions Result
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.MEDIA_ACCESS_REQUEST_CODE -> {
                val perms = HashMap<String, Int>()
                perms.put(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED)

                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices)
                        perms.put(permissions[i], grantResults[i])

                    if (perms[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
                        showMainScreen()
                    } else {
                        ActivityCompat.requestPermissions(this,
                                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    }
                }
            }
        }
    }
}
