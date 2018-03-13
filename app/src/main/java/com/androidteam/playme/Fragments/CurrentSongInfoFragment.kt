package com.androidteam.playme.Fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import timber.log.Timber
import com.androidteam.playme.HelperModule.IntentHelper

/**
 * A simple [Fragment] subclass.
 */
class CurrentSongInfoFragment : Fragment() {

    private var activeMusicContentInfo : MusicContent? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.current_song_detail_view,container,false)
    }

    fun setActiveMusicContentInfo(activeMusicContentInfo : MusicContent){
        this.activeMusicContentInfo = activeMusicContentInfo
    }

    override fun onViewCreated(view : View?, savedInstanceState: Bundle?) {
        val closeInfoAction = view!!.findViewById<ImageView>(R.id.closeInfoAction)
        val songNameInfo = view.findViewById<TextView>(R.id.songNameInfo)
        val artistNameInfo = view.findViewById<TextView>(R.id.artistNameInfo)
        val albumNameInfo = view.findViewById<TextView>(R.id.albumNameInfo)
        val sizeInfo = view.findViewById<TextView>(R.id.sizeInfo)
        val dateInfo = view.findViewById<TextView>(R.id.dateInfo)
        val shareAction = view.findViewById<TextView>(R.id.shareAction)
        val songInfoCover = view.findViewById<ImageView>(R.id.songInfoCover)

        Glide.with(activity.applicationContext)
                .load(activeMusicContentInfo?.cover)
                .error(R.drawable.placeholder)
                .override(600, 600)
                .centerCrop()
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                        songInfoCover.setImageResource(R.drawable.placeholder)
                        return true
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        Timber.d("Resource Ready")
                        return false
                    }
                }).into(songInfoCover)

        songNameInfo.text = activeMusicContentInfo!!.title
        artistNameInfo.text = activeMusicContentInfo!!.artist
        albumNameInfo.text = activeMusicContentInfo!!.thisAlbumName
        sizeInfo.text = activeMusicContentInfo!!.sizeInMb
        dateInfo.text = activeMusicContentInfo!!.thisDateAdded

        closeInfoAction.setOnClickListener({
            activity.onBackPressed()
        })

        shareAction.setOnClickListener({
            IntentHelper.shareAudioWithOtherApps(activity.applicationContext,activeMusicContentInfo!!.data)
        })

        super.onViewCreated(view, savedInstanceState)
    }
}
