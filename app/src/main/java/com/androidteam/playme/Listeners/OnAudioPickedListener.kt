package com.androidteam.playme.Listeners

import com.androidteam.playme.MusicProvider.MusicContent
import java.util.*

/**
 * Created by AJAY VERMA on 29/01/18.
 * Company : CACAO SOLUTIONS
 */
 interface OnAudioPickedListener {
    fun audioPicked(activeMusic : MusicContent, position : Int)
}