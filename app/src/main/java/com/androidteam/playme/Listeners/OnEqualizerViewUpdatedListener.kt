package com.androidteam.playme.Listeners

import android.support.v7.widget.RecyclerView

/**
 * Created by Ajay Verma on 2/12/2018.
 */
interface OnEqualizerViewUpdatedListener {
    fun updatePositionForEqualizer(position : Int, itemHolder: RecyclerView.ViewHolder)
}
