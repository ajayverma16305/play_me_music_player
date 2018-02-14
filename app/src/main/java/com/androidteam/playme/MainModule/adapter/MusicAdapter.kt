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
        SectionTitleProvider{

    private var audioPickedListener: OnAudioPickedListener? = null
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    interface OnShuffleIconClickListener{
        fun shuffleAction(view: View)
    }
    var shuffleClickListener : OnShuffleIconClickListener? = null

    fun setOnShuffleIconClickListener(shuffleClickListener : OnShuffleIconClickListener){
        this.shuffleClickListener = shuffleClickListener
    }

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

    private class HeaderViewHolder(mView : View) : RecyclerView.ViewHolder(mView){
        val mHeaderShuffleTextView : TextView = mView.findViewById(R.id.shuffle)
    }

    private class ItemHolder(mView : View) : RecyclerView.ViewHolder(mView) {
        var mCoverView : CircleImageView = mView.findViewById(R.id.cover)
        val mTitleView : TextView = mView.findViewById(R.id.title)
        val mArtistView : TextView = mView.findViewById(R.id.artist)
        val mDurationView : TextView = mView.findViewById(R.id.duration)
        val cardView : LinearLayout = mView.findViewById(R.id.card_view)
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