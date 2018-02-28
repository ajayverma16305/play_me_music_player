package com.androidteam.playme.Listeners

import android.support.v7.widget.RecyclerView

/**
 * Created by AJAY VERMA on 28/02/18.
 * Company : CACAO SOLUTIONS
 */

interface AdapterPositionChangeListener {
    fun currentPosition(position: Int,holder: RecyclerView.ViewHolder?)
}
