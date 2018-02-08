package com.androidteam.playme.MusicProvider

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.AudioManager;
import android.util.Log
import java.io.IOException
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.telephony.TelephonyManager
import android.telephony.PhoneStateListener
import com.androidteam.playme.HelperModule.StorageUtil
import com.androidteam.playme.MainModule.baseModule.BaseActivity
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.media.session.MediaSessionManager
import android.support.v4.media.MediaMetadataCompat
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Patterns
import android.webkit.URLUtil
import android.widget.Toast
import com.androidteam.playme.HelperModule.PlaybackStatus
import com.androidteam.playme.R
import timber.log.Timber
import java.util.*

/**
 * Created by AJAY VERMA on 29/01/18.
 * Company : CACAO SOLUTIONS
 */
class MediaPlayerService : Service(), MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    val ACTION_PLAY = "com.androidteam.playme.ACTION_PLAY"
    val ACTION_PAUSE = "com.androidteam.playme.ACTION_PAUSE"
    val ACTION_PREVIOUS = "com.androidteam.playme.ACTION_PREVIOUS"
    val ACTION_NEXT = "com.androidteam.playme.ACTION_NEXT"
    private val ACTION_STOP = "com.androidteam.playme.ACTION_STOP"

    //MediaSession
    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    //AudioPlayer notification ID
    private val NOTIFICATION_ID = 101

    // Binder given to clients
    private val iBinder = LocalBinder()
    var mediaPlayer: MediaPlayer? = null

    //Used to pause/resume MediaPlayer
    var resumePosition: Int = 0
    private var audioManager: AudioManager? = null

    //Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    private var audioList = ArrayList<MusicContent>()
    private var audioIndex = 0
    var activeAudio : MusicContent? = null;
    private var storageUtil : StorageUtil? = null

    interface OnAudioChangedListener {
        fun updateUI(activeAudio : MusicContent)
    }
    private var updateUIListener : OnAudioChangedListener?= null

    interface OnNotificationChangeListener{
        fun onAudioStateChange(status: PlaybackStatus)
    }
    private var notificationChangedListener : OnNotificationChangeListener ?= null

    fun setOnSongChangedListener(updateUIListener : OnAudioChangedListener){
        this.updateUIListener = updateUIListener
    }

    fun setOnNotificationChangeListener(notificationChangedListener : OnNotificationChangeListener){
        this.notificationChangedListener = notificationChangedListener
    }

    // The system calls this method when an activity, requests the service be started
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            //Load data from SharedPreferences
            storageUtil = StorageUtil(this)
            if (null != storageUtil!!.loadAudio()) {
                audioList = storageUtil!!.loadAudio()
                audioIndex = storageUtil!!.loadAudioIndex()
            }

            if (audioIndex < audioList.size) {
                //index is in a valid range
                activeAudio = audioList[audioIndex]
            } else {
                stopSelf()
            }
        } catch (e: NullPointerException) {
            stopSelf()
        }

        //Request audio focus
        if (!requestAudioFocus()) {
            //Could not gain focus
            stopSelf()
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession()
                initMediaPlayer()
                //startPlayingMusic()
            } catch (e: RemoteException) {
                Timber.d(e.message)
                stopSelf()
            }
            buildNotification(PlaybackStatus.PAUSED)
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    fun initMediaPlayer() {
        if (null == mediaPlayer) {
            mediaPlayer = MediaPlayer()
            //Set up MediaPlayer event listeners
            mediaPlayer?.setOnCompletionListener(this)
            mediaPlayer?.setOnErrorListener(this)
            mediaPlayer?.setOnPreparedListener(this)
            mediaPlayer?.setOnBufferingUpdateListener(this)
            mediaPlayer?.setOnSeekCompleteListener(this)
            mediaPlayer?.setOnInfoListener(this)
            //Reset so that the MediaPlayer is not pointing to another data source
            mediaPlayer?.reset()
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
    }

    fun startPlayingMusic(){
        try {
            // Set the data source to the mediaFile location
            if (null != activeAudio) {
                mediaPlayer?.reset()

                mediaPlayer?.setDataSource(activeAudio!!.data)
                mediaPlayer?.prepare()

            } else {
                Toast.makeText(applicationContext,"No media",Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Timber.d(e.message)
            mediaPlayer?.reset()
            stopSelf()
        }
    }

    fun playMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.start()
        }
    }

    fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.stop()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            resumePosition = mediaPlayer!!.currentPosition
        }
    }

    fun resumeMedia() {
        if (!mediaPlayer!!.isPlaying) {
            mediaPlayer!!.seekTo(resumePosition)
            mediaPlayer!!.start()
        }
    }

    fun resumeMediaPlayerWhereUserLeft(){
        if (!mediaPlayer!!.isPlaying) {
            startPlayingMusic()

            mediaPlayer!!.seekTo(resumePosition)
            mediaPlayer!!.start()
        }
    }

    fun isPlaying() : Boolean {
        return mediaPlayer!!.isPlaying
    }

    override fun onBind(intent: Intent): IBinder? {
        return iBinder
    }

    override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    override fun onCompletion(mp: MediaPlayer) {
        //Invoked when playback of a media source has completed.

        stopMedia()
        //stop the service
        stopSelf()

        if(storageUtil!!.loadAudioIsRepeatOne()){
            activeAudio = audioList[audioIndex]
            stopMedia()

            //reset mediaPlayer
            mediaPlayer!!.reset()
            initMediaPlayer()
            startPlayingMusic()
        }
        else {
            if (!storageUtil!!.loadAudioShuffledState()) {

                if(storageUtil!!.loadAvailable()){
                    skipToNext()
                }
                else {
                    activeAudio = audioList[audioIndex]
                    mediaPlayer?.reset()
                    startPlayingMusic()
                }
            }
            else {
                activeAudio = audioList[getRandomAudioFileIndex()]
                stopMedia()

                //reset mediaPlayer
                mediaPlayer!!.reset()
                initMediaPlayer()
                startPlayingMusic()
            }
        }
    }

    fun getRandomAudioFileIndex() : Int {
        try {
            val rand = Random()
            audioIndex = rand.nextInt((audioList.size - 1) - 0 + 1) + 0

            //Update stored index
            storageUtil?.storeAudioIndex(audioIndex)
        }
        catch (e: Exception) {
            Timber.d(e.message)
            audioIndex = 0
        }
        return audioIndex
    }

    //Handle errors
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked when there has been an error during an asynchronous operation.
        //Invoked when there has been an error during an asynchronous operation
        when (what) {
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra)
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra)
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra)
                Toast.makeText(applicationContext,"MEDIA ERROR UNKNOWN ",Toast.LENGTH_SHORT).show()
            }

        }
        return false
    }

    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        //Invoked to communicate some info.
        return false
    }

    override fun onPrepared(mp: MediaPlayer) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    override fun onSeekComplete(mp: MediaPlayer) {
        //Invoked indicating the completion of a seek operation.
    }

    override fun onCreate() {
        super.onCreate()
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener()
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        // registerBecomingNoisyReceiver()
        //Listen for new Audio to play -- BroadcastReceiver
        registerPlayNewAudio()
    }

    private val playNewAudio = object : BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {

            //Get the new media index form SharedPreferences
            audioIndex = storageUtil!!.loadAudioIndex()

            if (audioIndex < audioList.size) {
                //index is in a valid range
                activeAudio = audioList[audioIndex]
            } else {
                stopSelf()
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia()
            mediaPlayer!!.reset()
            initMediaPlayer()
            startPlayingMusic()
            updateMetaData()
            buildNotification(PlaybackStatus.PLAYING)
        }
    }

    private fun registerPlayNewAudio() {
        //Register playNewMedia receiver
        //Get the new media index form SharedPreferences
        val filter = IntentFilter(BaseActivity.BROAD_CAST_PLAY_NEW_AUDIO)
        registerReceiver(playNewAudio, filter)
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Throws(RemoteException::class)
    private fun initMediaSession() {
        if (mediaSessionManager != null) return  //mediaSessionManager exists

        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        // Create a new MediaSession
        mediaSession = MediaSessionCompat(applicationContext, "AudioPlayer")
        //Get MediaSessions transport controls
        transportControls = mediaSession!!.controller.transportControls
        //set MediaSession -> ready to receive media commands
        mediaSession!!.isActive = true
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        //Set mediaSession's MetaData
        updateMetaData()

        // Attach Callback to receive MediaSession updates
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            // Implement callbacks
            @RequiresApi(Build.VERSION_CODES.O)
            @TargetApi(Build.VERSION_CODES.O)
            override fun onPlay() {
                super.onPlay()
                resumeMedia()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onPause() {
                super.onPause()
                pauseMedia()
                buildNotification(PlaybackStatus.PAUSED)
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                skipToNext()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                skipToPrevious()
                updateMetaData()
                buildNotification(PlaybackStatus.PLAYING)
            }

            override fun onStop() {
                super.onStop()
                removeNotification()
                //Stop the service
                stopSelf()
            }

            override fun onSeekTo(position: Long) {
                super.onSeekTo(position)
            }
        })
    }

    private fun updateMetaData() {
        val albumArt = BitmapFactory.decodeResource(resources, R.drawable.playme_app_logo) //replace with medias albumArt
        // Update the current metadata
        mediaSession!!.setMetadata(MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio?.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio?.cover)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio?.title)
                .build())
    }

    fun skipToNext() {
        if (!storageUtil!!.loadAudioShuffledState()) {
            if (audioIndex == audioList.size - 1) {
                //if last in playlist
                audioIndex = 0
                activeAudio = audioList[audioIndex]
            } else {
                //get next in playlist
                activeAudio = audioList[++audioIndex]
            }
        } else {
            audioIndex = getRandomAudioFileIndex()
            activeAudio = audioList[audioIndex]
        }

        //Update stored index
        storageUtil!!.storeAudioIndex(audioIndex)

        stopMedia()
        //reset mediaPlayer
        mediaPlayer!!.reset()
        initMediaPlayer()
        startPlayingMusic()

        updateMainUIOnFromNotificationStatus()
    }

    fun currentAudioDetails() : MusicContent? {
        return activeAudio
    }

    fun skipToPrevious() {
        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size - 1
            activeAudio = audioList[audioIndex]
        } else {
            //get previous in playlist
            activeAudio = audioList[--audioIndex]
        }

        //Update stored index
        storageUtil!!.storeAudioIndex(audioIndex)

        stopMedia()
        //reset mediaPlayer
        mediaPlayer!!.reset()
        initMediaPlayer()
        startPlayingMusic()

        updateMainUIOnFromNotificationStatus()
    }

    private fun updateMainUIOnFromNotificationStatus(){
        if(null != updateUIListener){
            updateUIListener!!.updateUI(activeAudio!!)
        }
    }

    private fun updateIconOnMainUI(status: PlaybackStatus){
        if(null != notificationChangedListener){
            notificationChangedListener!!.onAudioStateChange(status)
        }
    }

    //Handle incoming phone calls
    private fun callStateListener() {
        // Get the telephony manager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        //Starting listening for PhoneState changes
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                //if at least one call exists or the phone is ringing
                //pause the MediaPlayer
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> if (mediaPlayer != null) {
                        pauseMedia()
                        ongoingCall = true
                    }
                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                resumeMedia()
                            }
                        }
                }
            }
        }
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager!!.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        //Invoked when the audio focus of the system is updated.
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                if (mediaPlayer == null) {
                    initMediaPlayer()
                    startPlayingMusic()
                }
                else if (!mediaPlayer!!.isPlaying) mediaPlayer!!.start()
                mediaPlayer!!.setVolume(1.0f, 1.0f)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.stop()
                mediaPlayer!!.release()
                mediaPlayer = null
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        //Could not gain focus
    }

    private fun removeAudioFocus(): Boolean {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager!!.abandonAudioFocus(this)
    }

    inner class LocalBinder : Binder() {
        val service: MediaPlayerService
            get() = this@MediaPlayerService
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            stopMedia()
            mediaPlayer!!.release()
        }
        removeAudioFocus()
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }

        removeNotification()

        //unregister BroadcastReceivers
       // unregisterReceiver(becomingNoisyReceiver)
        unregisterReceiver(playNewAudio)
        stopForeground(true)

        //clear cached playlist
        storageUtil!!.clearCachedAudioPlaylist()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildNotification(playbackStatus: PlaybackStatus) {
        try {
            var notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
            var play_pauseAction : PendingIntent? = null

            //Build a new notification according to the current state of the MediaPlayer
            if (playbackStatus == PlaybackStatus.PLAYING) {
                notificationAction = android.R.drawable.ic_media_pause;
                //create the pause action
                play_pauseAction = playbackAction(1);
            } else if (playbackStatus == PlaybackStatus.PAUSED) {
                notificationAction = android.R.drawable.ic_media_play;
                //create the play action
                play_pauseAction = playbackAction(0);
            }

            val largeIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.playme_app_logo); //replace with your own image
            val notificationManager : NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Sets an ID for the notification, so it can be updated.
            val CHANNEL_ID = "PlayMe_Notification"// The id of the channel.
            val name = "PlayMe"// The user-visible name of the channel.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
                val notificationChannel = NotificationChannel(CHANNEL_ID,name,importance)
                notificationChannel.enableLights(true);
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(mChannel);
            }

            val notification = NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentTitle(activeAudio!!.title)
                    .setContentText(activeAudio!!.artist)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(android.R.drawable.stat_sys_headset)
                    .setStyle(android.support.v7.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSession!!.sessionToken)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(playbackAction(4)))

                    .setColor(resources.getColor(R.color.colorPrimary))
                    .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                    .addAction(notificationAction, "pause", play_pauseAction)
                    .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                    .setDeleteIntent(playbackAction(4))
                    .setChannelId(CHANNEL_ID)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setWhen(0)

            val contentIntent = PendingIntent.getActivity(this, 0,
                    Intent(this, BaseActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setContentIntent(contentIntent);
            notificationManager.notify(NOTIFICATION_ID, notification.build())

        } catch (e: Exception) {
            Timber.d(e.message)
        }
    }

    fun removeNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as (NotificationManager);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    fun playbackAction(actionNumber: Int): PendingIntent? {
        val playbackAction = Intent(this, MediaPlayerService::class.java)
        when (actionNumber) {
            0 -> {
                // Play
                playbackAction.action = (ACTION_PLAY)
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            1 -> {
                // Pause
                playbackAction.action = (ACTION_PAUSE)
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            2 -> {
                // Next track
                playbackAction.action = (ACTION_NEXT)
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            3 -> {
                // Previous track
                playbackAction.action = (ACTION_PREVIOUS)
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            else -> {
                playbackAction.action =  ACTION_STOP
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
        }
    }

    fun handleIncomingActions(playbackAction : Intent ) {
        if (playbackAction == null && playbackAction.action == null) return;
        val actionString = playbackAction.action;

        when (actionString) {
            (ACTION_PLAY) ->  {
                transportControls!!.play()
                Handler().postDelayed({
                    updateIconOnMainUI(PlaybackStatus.PLAYING)
                },300)
            }
            (ACTION_PAUSE) ->  {
                transportControls!!.pause()
                Handler().postDelayed({
                    updateIconOnMainUI(PlaybackStatus.PAUSED)
                },300)
            }
            (ACTION_NEXT) ->   {
                transportControls!!.skipToNext()
                Handler().postDelayed({
                    updateMainUIOnFromNotificationStatus()
                },200)
            }
            (ACTION_PREVIOUS) -> {
                transportControls!!.skipToPrevious()
                Handler().postDelayed({
                    updateMainUIOnFromNotificationStatus()
                },200)

            }
            (ACTION_STOP) ->  {
                stopForeground(true)
                transportControls!!.stop() }
        }
    }
}

