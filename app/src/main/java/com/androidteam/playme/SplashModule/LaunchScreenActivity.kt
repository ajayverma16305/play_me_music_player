package com.androidteam.playme.SplashModule

import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import com.androidteam.playme.HelperModule.PlayMeConstants
import com.androidteam.playme.HelperModule.PermissionManager
import com.androidteam.playme.HelperModule.StorageUtil
import com.androidteam.playme.MainModule.baseModule.BaseActivity
import com.androidteam.playme.R
import java.util.HashMap

class LaunchScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_screen)

        if(PermissionManager.checkForStoragePermission(this)) {
            Handler().postDelayed(Runnable {
                StorageUtil(this).storeAvailable(true)
                showMainScreen()
            },200)
        }
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
            PlayMeConstants.MEDIA_ACCESS_REQUEST_CODE -> {
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
