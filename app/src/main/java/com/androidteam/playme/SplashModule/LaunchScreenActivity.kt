package com.androidteam.playme.SplashModule

import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.View
import com.androidteam.playme.HelperModule.PlayMeConstants
import com.androidteam.playme.HelperModule.PermissionManager
import com.androidteam.playme.HelperModule.StorageUtil
import com.androidteam.playme.Listeners.OnAudioResourcesReadyListener
import com.androidteam.playme.MainModule.baseModule.BaseActivity
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.MusicProvider.MusicContentProvider
import com.androidteam.playme.R
import kotlinx.android.synthetic.main.activity_launch_screen.*
import java.lang.ref.WeakReference
import java.util.HashMap

class LaunchScreenActivity : AppCompatActivity() {

    private var weakSelf : WeakReference<LaunchScreenActivity> = WeakReference(this)

    companion object {
        var audioList = ArrayList<MusicContent>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_screen)

        if(PermissionManager.checkForStoragePermission(this)) {
            startQueringMusicListFromStorage()
        }
    }

    // Start Quering Music List From Storage
    private fun startQueringMusicListFromStorage() {
        val storageUtil = StorageUtil(applicationContext)
        if(storageUtil.loadAudioListSize() > 0) {
            showMainScreen(storageUtil.loadAudio())
        } else {
            executeMusicFilesFromStorage()
        }
    }

    /**
     * Execute Music Files From Storage
     */
    private fun executeMusicFilesFromStorage() {
        val self = weakSelf.get()

        if (null != self) {
            self.loader.visibility = View.VISIBLE
        }

        val musicAsyncObj = MusicContentProvider(WeakReference(applicationContext),object : OnAudioResourcesReadyListener{
            override fun resourcesList(musicContentList: ArrayList<MusicContent>?) {
                if (null != self) {
                    self.loader.visibility = View.GONE
                }
                showMainScreen(musicContentList)
            }
        })
        musicAsyncObj.execute()
    }

    /**
     * Async class to get all music list from Storage
    private class AudioRetrieverAsync(val selfWeak : WeakReference<Context>, private val audioResourceReadyListener
                    : OnAudioResourcesListReadyListener) : AsyncTask<Void, Void, ArrayList<MusicContent>?>() {

        override fun doInBackground(vararg p0: Void?): ArrayList<MusicContent> {
            return MusicContentProvider.getAllMusicPathList(selfWeak.get()!!)
        }

        override fun onPostExecute(result: ArrayList<MusicContent>?) {
            audioResourceReadyListener.resourcesList(result)
        }
    }*/

    /**
     * Show Main Screen
     */
    private fun showMainScreen(musicContentList: ArrayList<MusicContent>?) {
        if (null != musicContentList) {
            audioList = musicContentList
        }
        StorageUtil(applicationContext).storeAvailable(true)
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
                        startQueringMusicListFromStorage()
                    } else {
                        ActivityCompat.requestPermissions(this,
                                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                    }
                }
            }
        }
    }
}
