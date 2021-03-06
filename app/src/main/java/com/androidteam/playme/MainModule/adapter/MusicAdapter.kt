package com.androidteam.playme.MainModule.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import de.hdodenhof.circleimageview.CircleImageView
import timber.log.Timber

/**
 * Created by AJAY VERMA on 24/04/15.
 * Company : CACAO SOLUTIONS
 */
class MusicAdapter(val context : Context,var songsList: ArrayList<MusicContent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        SectionTitleProvider{

    private var audioPickedListener: OnAudioPickedListener? = null

    fun setSongClickedListener(listener: OnAudioPickedListener) {
        audioPickedListener = listener
    }

    override fun getSectionTitle(position: Int): String {
        try {
            val musicContent : MusicContent = songsList[position - 1]
            val title = musicContent.title
            return title.substring(0, 1)
        } catch (e: Exception) {
            Timber.d(e.message)
            return "#"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_card_small, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val songObject = songsList[position]
        val itemHolder = holder as ItemHolder


        Glide.with(context)
                .load(songObject.cover)
                .error(R.drawable.ic_music_note_white_24dp)
                .override(100,100)
                .listener(object : RequestListener<String, GlideDrawable>{
                    override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                        itemHolder.mCoverView.setImageResource(R.drawable.ic_music_note_white_24dp)
                        return true
                    }

                    override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        Timber.d("Resource Ready")
                        return false
                    }
                }).into(itemHolder.mCoverView)

        itemHolder.mTitleView.text = songObject.title
        itemHolder.mArtistView.text = songObject.artist
        itemHolder.mDurationView.text = songObject.duration

        itemHolder.cardView.setOnClickListener {
            if (null != audioPickedListener) {
                audioPickedListener!!.audioPicked(songObject,position)
            }
        }
    }

    class ItemHolder(mView : View) : RecyclerView.ViewHolder(mView) {
        var mCoverView : CircleImageView = mView.findViewById(R.id.cover)
        val mTitleView : TextView = mView.findViewById(R.id.title)
        val mArtistView : TextView = mView.findViewById(R.id.artist)
        val mDurationView : TextView = mView.findViewById(R.id.duration)
        val cardView : LinearLayout = mView.findViewById(R.id.card_view)
    }

    override fun getItemCount(): Int {
        return if(songsList.isNotEmpty()){
            (songsList.size)
        } else{
           0
        }
    }
}