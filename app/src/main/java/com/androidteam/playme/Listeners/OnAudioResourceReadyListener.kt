package com.androidteam.playme.Listeners

import com.androidteam.playme.MusicProvider.MusicContent

/**
 * Created by AJAY VERMA on 09/02/18.
 * Company : CACAO SOLUTIONS
 */
interface OnAudioResourcesReadyListener {
    fun resourcesList(musicContentList : ArrayList<MusicContent>?)
}