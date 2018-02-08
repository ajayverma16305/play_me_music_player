package com.androidteam.playme.MainModule.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.persistent_bottomsheet.*
import timber.log.Timber

/**
 * Created by AJAY VERMA on 24/04/15.
 * Company : CACAO SOLUTIONS
 */
class MusicAdapter(var context : Context,private var songsList: List<MusicContent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
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
        return if(viewType == TYPE_HEADER){
            HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.music_header_view, parent, false))
        } else {
            ItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_card_small, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_ITEM) {
            val songObject = songsList[position - 1]

            val itemHolder = holder as ItemHolder
            Glide.with(context)
                    .load(songObject.cover)
                    .listener(object  : RequestListener<Drawable> {

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            itemHolder.mCoverView.setImageResource(R.drawable.ic_music_note_black_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    })
                    .into(itemHolder.mCoverView)

            itemHolder.mTitleView.text = songObject.title
            itemHolder.mArtistView.text = songObject.artist
            itemHolder.mDurationView.text = songObject.duration

            itemHolder.cardView.setOnClickListener {
                if (null != audioPickedListener) {
                    audioPickedListener!!.audioPicked(songObject,position - 1)
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
        val cardView : CardView = mView.findViewById(R.id.card_view)
    }

    override fun getItemViewType(position: Int): Int {
        return if(position == 0){
            TYPE_HEADER
        } else {
            TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return if(songsList.size > 0){
            (songsList.size + 1)
        } else{
           0
        }
    }
}