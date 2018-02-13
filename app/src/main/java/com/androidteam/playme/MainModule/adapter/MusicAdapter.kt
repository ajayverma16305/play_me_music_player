package com.androidteam.playme.MainModule.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.androidteam.playme.HelperModule.MiniEqualizer
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.Listeners.OnEqualizerViewUpdatedListener
import com.androidteam.playme.Listeners.OnMediaStateChangeListener
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
class MusicAdapter(val context : Context,var songsList: List<MusicContent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        SectionTitleProvider ,OnEqualizerViewUpdatedListener{

    private var audioPickedListener: OnAudioPickedListener? = null
    private var musicStateChangeListener : OnMediaStateChangeListener ? = null
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1
    private var currentPosition = -1

    interface OnShuffleIconClickListener{
        fun shuffleAction(view: View)
    }
    var shuffleClickListener : OnShuffleIconClickListener? = null

    override fun updatePositionForEqualizer(position: Int, itemHolder : RecyclerView.ViewHolder) {
        if(itemHolder is ItemHolder){
            val holder : ItemHolder = itemHolder
            updateCurrentPositionForEqualizer(position,holder)
        }
    }

    fun setOnShuffleIconClickListener(shuffleClickListener : OnShuffleIconClickListener){
        this.shuffleClickListener = shuffleClickListener
    }

    fun setSongClickedListener(listener: OnAudioPickedListener) {
        audioPickedListener = listener
    }

    fun setOnMediaStateChangeListener(musicStateChangeListener : OnMediaStateChangeListener) {
        this.musicStateChangeListener = musicStateChangeListener
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
         if(viewType == TYPE_HEADER){
            return  HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.music_header_view, parent, false))
        } else {
             return ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_card_small, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_ITEM) {
            val lPosition = position - 1
            val songObject = songsList[lPosition]
            val itemHolder = holder as ItemHolder

            updateCurrentEqualizerView(lPosition,itemHolder)

            Glide.with(context)
                    .load(songObject.cover)
                    .error(R.drawable.playme_app_logo)
                    .override(100,100)
                    .listener(object : RequestListener<String, GlideDrawable>{
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            itemHolder.mCoverView.setImageResource(R.drawable.playme_app_logo)
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
                updateCurrentPositionForEqualizer(lPosition,itemHolder)

                if(null != musicStateChangeListener){
                    musicStateChangeListener!!.mediaStateChanged(itemHolder.equalizer_view)
                }

                if (null != audioPickedListener) {
                    audioPickedListener!!.audioPicked(songObject,lPosition)
                }
            }
        } else {
            val headerView = holder as HeaderViewHolder
            headerView.mHeaderShuffleTextView.setOnClickListener{
                if(null != shuffleClickListener){
                    shuffleClickListener!!.shuffleAction(headerView.mHeaderShuffleTextView)
                }
            }
        }
    }

    private fun updateCurrentPositionForEqualizer(lPosition: Int, itemHolder : ItemHolder) {
        if (currentPosition == lPosition) {
            currentPosition = -1
        } else {
            notifyItemChanged(currentPosition)
            currentPosition = lPosition
        }
        notifyItemChanged(lPosition)

        if(currentPosition != -1){
            itemHolder.equalizer_view.visibility = View.VISIBLE
            itemHolder.equalizer_view.animateBars()
        }
    }

    private fun updateCurrentEqualizerView(lPosition : Int,itemHolder : ItemHolder){
        if (currentPosition == lPosition) {
            if(!itemHolder.equalizer_view.isAnimating){
                itemHolder.equalizer_view.visibility = View.VISIBLE
                itemHolder.equalizer_view.animateBars()
                itemHolder.mCoverView.alpha = .1f
            }
        }
        else {
            itemHolder.mCoverView.alpha = 1f
            itemHolder.equalizer_view.visibility = View.GONE
            if(itemHolder.equalizer_view.isAnimating){
                itemHolder.equalizer_view.stopBars()
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder?) {
        if(holder is ItemHolder){
            val itemHolder : ItemHolder = holder
            val lPosition = itemHolder.adapterPosition

            updateCurrentEqualizerView(lPosition,itemHolder)
        }

        super.onViewRecycled(holder)
    }

    private class HeaderViewHolder(mView : View) : RecyclerView.ViewHolder(mView){
        val mHeaderShuffleTextView : TextView = mView.findViewById(R.id.shuffle)
    }

    private class ItemHolder(mView : View) : RecyclerView.ViewHolder(mView) {
        var mCoverView : CircleImageView = mView.findViewById(R.id.cover)
        val mTitleView : TextView = mView.findViewById(R.id.title)
        val mArtistView : TextView = mView.findViewById(R.id.artist)
        val mDurationView : TextView = mView.findViewById(R.id.duration)
        val cardView : LinearLayout = mView.findViewById(R.id.card_view)
        var equalizer_view = mView.findViewById<MiniEqualizer>(R.id.equalizer_view)
    }

    override fun getItemViewType(position: Int): Int {
         if(position == 0){
            return TYPE_HEADER
        } else {
             return TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return if(songsList.isNotEmpty()){
            (songsList.size + 1)
        } else{
           0
        }
    }
}