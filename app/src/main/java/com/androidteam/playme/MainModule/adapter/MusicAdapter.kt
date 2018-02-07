package com.androidteam.playme.MainModule.adapter

import android.net.Uri
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Created by AJAY VERMA on 24/04/15.
 * Company : CACAO SOLUTIONS
 */
class MusicAdapter(private var songsList: List<MusicContent>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            if(!(songObject.cover).isEmpty()){
                itemHolder.mCoverView.setImageURI(Uri.parse(songObject.cover))
            } else{
                itemHolder.mCoverView.setImageResource(R.drawable.ic_music_note_black_24dp)
            }

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