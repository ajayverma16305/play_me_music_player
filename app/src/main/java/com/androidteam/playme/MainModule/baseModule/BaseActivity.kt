package com.androidteam.playme.MainModule.baseModule

import android.annotation.TargetApi
import android.content.Intent
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import kotlinx.android.synthetic.main.activity_base.*
import com.miguelcatalan.materialsearchview.MaterialSearchView
import android.support.design.widget.BottomSheetBehavior
import kotlinx.android.synthetic.main.persistent_bottomsheet.*
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.androidteam.playme.HelperModule.*
import com.androidteam.playme.MainModule.adapter.MusicAdapter
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.MusicProvider.MediaPlayerService
import com.androidteam.playme.MusicProvider.MusicContentProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class BaseActivity : AppCompatActivity(), View.OnClickListener, OnAudioPickedListener,
        MaterialSearchView.OnQueryTextListener, MaterialSearchView.SearchViewListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayerService.OnAudioChangedListener,
        MusicAdapter.OnShuffleIconClickListener, MediaPlayerService.OnNotificationChangeListener {

    private lateinit var sheetBehavior : BottomSheetBehavior<*>
    private var playerService: MediaPlayerService? = null
    private var serviceBound = false

    //List of available Audio files
    private var audioList = ArrayList<MusicContent>()
    private var audioIndex = 0
    private var musicContentObj: MusicContent? = null
    private var mHandler : Handler = Handler()
    private var storage : StorageUtil? = null
    private val utils : TimeUtilities = TimeUtilities()
    private var weakSelf : WeakReference<BaseActivity> = WeakReference(this)
    private interface OnAudioResourcesListReadyListener {
        fun resourcesList(musicContentList: ArrayList<MusicContent>)
    }

    companion object {
        val BROAD_CAST_PLAY_NEW_AUDIO = "com.androidteam.playme.PlayNewAudio"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name_title)

        val self = weakSelf.get()
        if (null != self) {
            self.loader.visibility = View.VISIBLE
        }
        initializeRecyclerView()

        val musicAsyncObj = AudioRetrieverAsync(WeakReference(applicationContext),object : OnAudioResourcesListReadyListener {
            override fun resourcesList(musicContentList: ArrayList<MusicContent>) {
                if (null != self) {
                    self.loader.visibility = View.GONE
                }

                audioList = musicContentList
                if(audioList.size > 0){
                    startFetchingAudioFilesFromStorage()
                } else {
                    setErrorViewForNoAudio()
                }
            }
        })
        musicAsyncObj.execute()
    }

    /**
     * Async class to get all music list from Storage
     */
    private class AudioRetrieverAsync(val selfWeak : WeakReference<Context>, private var audioResourceReadyListener
                            : OnAudioResourcesListReadyListener) : AsyncTask<Void, Void, ArrayList<MusicContent>>() {

        override fun doInBackground(vararg p0: Void?): ArrayList<MusicContent> {
            return MusicContentProvider.getAllMusicPathList(selfWeak.get()!!)
        }

        override fun onPostExecute(result: ArrayList<MusicContent>?) {
            if(null != result){
                if(result.size > 0) {
                    audioResourceReadyListener.resourcesList(result)
                }
            }
        }
    }

    // Start Fetching Audio Files From Storage
    private fun startFetchingAudioFilesFromStorage() {
        music_recycler_view.visibility = View.VISIBLE
        card_view.visibility = View.VISIBLE

        //Store Serializable audioList to SharedPreferences
        storage = StorageUtil(applicationContext)

        Collections.sort(audioList) { lhs, rhs -> lhs.title.compareTo(rhs.title) }

        //Store Serializable audioList to SharedPreferences
        storage?.storeAudio(audioList)

        initUIComponents()
        setBottomSheetDrawerView()

        startMediaService()
        setInitialStateOnViews()
    }

    // Set Error View For No Audio
    private fun setErrorViewForNoAudio() {
        music_recycler_view.visibility = View.GONE
        card_view.visibility = View.GONE

        val relative = RelativeLayout(this)

        val errorTextView = TextView(this)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        errorTextView.layoutParams = layoutParams
        errorTextView.text = PlayMeConstants.NO_SONG_AVAILABLE
        errorTextView.textSize = 24f
        errorTextView.gravity = Gravity.CENTER

        relative.addView(errorTextView)
        main_content.addView(relative)
    }

    private fun initializeRecyclerView(){
        music_recycler_view.layoutManager = LinearLayoutManager(this) as LinearLayoutManager

        /**
         * Must set recycler view to avoid
         * Attempt to invoke virtual method 'int android.support.v7.widget.RecyclerView.computeVerticalScrollOffset()'
         * on a null object reference
         */
        fastscroll.setRecyclerView(music_recycler_view)
    }

    /**
     * Initialization of UI components
     */
    private fun initUIComponents() {
        val musicAdapter = MusicAdapter(this,audioList)
        music_recycler_view.adapter = musicAdapter
        musicAdapter.setSongClickedListener(this)
        musicAdapter.setOnShuffleIconClickListener(this)
        fastscroll.setRecyclerView(music_recycler_view)

        playLayout.setOnClickListener(this)
        closeAction.setOnClickListener(this)
        playOnHomeIcon.setOnClickListener(this)
        nextIcon.setOnClickListener(this)
        previousIcon.setOnClickListener(this)
        repeatIcon.setOnClickListener(this)
        playButton.setOnClickListener(this)
        shuffle.setOnClickListener(this)
        detailSeekBar.setOnSeekBarChangeListener(this)

        search_view.setOnQueryTextListener(this)
        search_view.setOnSearchViewListener(this)
        search_view.setHint(getString(R.string.action_search))
        search_view.setSuggestionIcon(ContextCompat.getDrawable(applicationContext,R.drawable.ic_music_note_black_24dp))
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun shuffleAction(view : View) {
        view.isEnabled = false

        audioIndex = playerService?.getRandomAudioFileIndex()!!
        musicContentObj = audioList[audioIndex]

        playerService?.startPlayingMusic()
        setCurrentMusicDetailsToUI()

        Handler().postDelayed({
            view.isEnabled = true
        },250)
    }

    private fun setBottomSheetDrawerView() {
        sheetBehavior = BottomSheetBehavior.from(bottomSheet) as BottomSheetBehavior;
        sheetBehavior.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0.2) {
                    playLayout.visibility = View.GONE
                    closeLayout.visibility = View.VISIBLE

                } else if (slideOffset < 0.1) {
                    playLayout.visibility = View.VISIBLE
                    closeLayout.visibility = View.GONE
                }
            }
        })
    }

    private fun startMediaService() {
        val playerIntent = Intent(this, MediaPlayerService::class.java)
        startService(playerIntent)
        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // Set initial state on views
    private fun setInitialStateOnViews(){
        val self = weakSelf.get()
        if (null != self) {
            if(storage?.loadAudioShuffledState()!!){
                self.shuffle.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext,R.color.colorPrimary))
            } else {
                self.shuffle.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext,R.color.lightGray))
            }

            if(storage?.loadAudioIsRepeatOne()!!){
                self.repeatIcon.setImageResource(R.drawable.repeat_one)
            } else {
                self.repeatIcon.setImageResource(R.drawable.infinite_loop)
            }

            // last audio index
            audioIndex = storage?.loadAudioIndex()!!

            // Load data from SharedPreferences
            if (audioList.size > 0) {
                musicContentObj = audioList[audioIndex]
            }

            self.playOnHomeIcon.setImageResource(R.drawable.play_main)
            self.playButton.setImageResource(R.drawable.ic_play_white)

            self.playingSongName.text = (musicContentObj?.title)
            self.artistName.text = (musicContentObj?.artist)

            self.frontSeekBar.progress = (storage!!.loadAudioProgress())
            self.detailSeekBar.progress = (storage!!.loadAudioProgress())

            // Set Media Detail Initial State
            self.closeSongName.text = musicContentObj?.title
            self.closeArtistName.text = musicContentObj?.artist

            Glide.with(this)
                    .load(musicContentObj?.cover)
                    .listener(object  : RequestListener<Drawable> {

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            self.picOnFrontView.setImageResource(R.drawable.ic_music_note_black_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    })
                    .into(self.picOnFrontView)

            Glide.with(this)
                    .load(musicContentObj?.cover)
                    .listener(object  : RequestListener<Drawable> {

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            self.albumImageView.setImageResource(R.drawable.ic_music_note_black_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    })
                    .into(self.albumImageView)

            self.startTimer.text = storage?.loadAudioCurrentTime().toString()
            self.endTimer.text = musicContentObj?.duration

            self.playOnHomeIcon.tag = if (storage?.loadAudioIconTag() == PlayMeConstants.PAUSE) {
                null
            } else {
                storage?.loadAudioIconTag()
            }
            self.playButton.tag = if (storage?.loadAudioIconTag() == PlayMeConstants.PAUSE) {
                null
            } else {
                storage?.loadAudioIconTag()
            }
        }
    }

    // Binding this Client to the AudioPlayer Service
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance

            val binder = service as MediaPlayerService.LocalBinder
            playerService = binder.service
            serviceBound = true

            playerService?.initMediaPlayer()
            playerService?.setOnSongChangedListener(this@BaseActivity)
            playerService?.setOnNotificationChangeListener(this@BaseActivity)
            Timber.d("Service Bound")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
            Timber.d("Service Disconnected")
        }
    }

    override fun updateUI(activeAudio : MusicContent) {
        musicContentObj = activeAudio
        setCurrentMusicDetailsToUI()
    }

    override fun onAudioStateChange(status: PlaybackStatus) {
        updateOnScreenIcons(status)
    }

    override fun audioPicked(activeMusic : MusicContent, position: Int) {
        musicContentObj = activeMusic

        setCurrentMusicDetailsToUI()
        playAudio(position)
    }

    // Set Current Music Details To UI
    private fun setCurrentMusicDetailsToUI() {
        val self = weakSelf.get()

        if (null != self) {
            self.playingSongName.text = (musicContentObj?.title)
            self.artistName.text = (musicContentObj?.artist)

            Glide.with(this@BaseActivity)
                    .load(musicContentObj?.cover)
                    .listener(object  : RequestListener<Drawable>{

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            self.picOnFrontView.setImageResource(R.drawable.ic_music_note_black_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    })
                    .into(self.picOnFrontView)

            self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
        }

        updateProgressBar()
        setCurrentDetailsToMainUI()
    }

    // Set Current Details To Main UI
    private fun setCurrentDetailsToMainUI() {
        val self = weakSelf.get()

        if (null != self) {
            self.closeSongName.text = musicContentObj?.title
            self.closeArtistName.text = musicContentObj?.artist

            Glide.with(this@BaseActivity)
                    .load(musicContentObj?.cover)
                    .listener(object  : RequestListener<Drawable>{

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            self.albumImageView.setImageResource(R.drawable.muic_note_big)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    })
                    .into(self.albumImageView)

            self.playButton.setImageResource(R.drawable.pause)
            self.endTimer.text = musicContentObj?.duration
        }
    }

    private fun playAudio(position : Int) {
        audioIndex = position

        //Store Serializable audioList to SharedPreferences
        storage?.storeAudioIndex(audioIndex)
        storage?.storeAudioTotalTime(endTimer.text.toString())

        //Check is service is active
        if (!serviceBound) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        else {
            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            val broadcastIntent = Intent(BROAD_CAST_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }
    }

    /**
     * Update timer on seekbar
     */
    private fun updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100)
    }

    private val mUpdateTimeTask = object : Runnable {
        override fun run() {
            val self = weakSelf.get()

            if (null != self) {
                val mediaPlayer = self.playerService?.mediaPlayer

                if (null != mediaPlayer) { // Updating progress bar
                    val totalDuration = mediaPlayer.duration
                    val currentDuration = mediaPlayer.currentPosition

                    self.startTimer.text = (utils.milliSecondsToTimer(currentDuration.toLong()));
                    val progress = utils.getProgressPercentage(currentDuration.toLong(), totalDuration.toLong())
                    self.frontSeekBar.progress = progress
                    self.detailSeekBar.progress = progress

                    self.storage?.storeAudioProgress(progress)
                    self.storage?.storeAudioCurrentSeekPosition(currentDuration)
                    self.storage?.storeAudioCurrentTime(startTimer.text.toString())
                }
            }

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        val self = weakSelf.get()

        var isFound = false
        if (null != query) {
            for(i in 0 until audioList.size){
                val musicContent : MusicContent = audioList[i]
                if(musicContent.title.contentEquals(query)){
                    isFound = true
                    audioIndex = i;
                    break
                }
            }
        }

        if(!isFound){
            Toast.makeText(applicationContext,"No result",Toast.LENGTH_SHORT).show()
        } else {
            musicContentObj = audioList[audioIndex]

            setCurrentMusicDetailsToUI()
            playAudio(audioIndex)

            if(null != self) {
                BottomSheetBehavior.from(self.bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
                self.storage?.storeAudioIndex(audioIndex)
                self.playerService?.activeAudio = musicContentObj
                self.playerService?.startPlayingMusic()
            }
        }

        hideKeyboard()
        return false
    }

    private fun hideKeyboard(){
        val imm = applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.window.decorView.windowToken, 0)
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun onSearchViewClosed() {
    }

    override fun onSearchViewShown() {
        val self = weakSelf.get()

        val audioNameList : ArrayList<String> = arrayListOf()
        for(i in 0 until audioList.size){
            val musicContent : MusicContent = audioList[i]
            audioNameList.add(musicContent.title)
        }

        if (null != self) {
            self.search_view.setSuggestions(audioNameList.toTypedArray());
        }
    }

    override fun onNewIntent(intent: Intent) {
        val self = weakSelf.get()

        if (null != self) {
            BottomSheetBehavior.from(self.bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(view : View?) {
        when (view?.id) {
            R.id.playButton -> {
                detailPlayAction(view)
            }
            R.id.closeAction -> {
                closeAction()
            }
            R.id.playLayout ->{
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            R.id.playOnHomeIcon ->{
                playHomeIconAction(view)
            }
            R.id.nextIcon ->{
                nextIconAction()
            }
            R.id.previousIcon ->{
                previousIconAction()
            }
            R.id.repeatIcon ->{
                repeatAllIconAction()
            }
            R.id.shuffle ->{
                shuffleIconAction()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun repeatAllIconAction(){
        val self = weakSelf.get()

        if (null != self) {
            if(!self.storage?.loadAudioIsRepeatOne()!!){
                self.storage?.storeAudioRepeatOne(true)
                self.repeatIcon.setImageResource(R.drawable.repeat_one)
            } else {
                self.storage?.storeAudioRepeatOne(false)
                self.repeatIcon.setImageResource(R.drawable.infinite_loop)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun shuffleIconAction() {
        val self = weakSelf.get()

        if (null != self) {
            if (self.storage?.loadAudioShuffledState()!!) {
                self.storage?.storeAudioShuffle(false)
                self.shuffle.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext,R.color.hintColor))
            } else {
                self.storage?.storeAudioShuffle(true)
                self.shuffle.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext,R.color.colorPrimary))
            }
        }
    }

    private fun previousIconAction() {
        val self = weakSelf.get()

        if (null != self) {
            if (self.playerService?.mediaPlayer != null) {
                self.playerService?.skipToPrevious()
                musicContentObj = self.playerService?.currentAudioDetails()
                setCurrentMusicDetailsToUI()
            }
        }
    }

    private fun nextIconAction() {
        val self = weakSelf.get()

        if (null != self) {
            if (self.playerService?.mediaPlayer != null) {
                self.playerService?.skipToNext()
                musicContentObj = self.playerService?.currentAudioDetails()
                setCurrentMusicDetailsToUI()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun playHomeIconAction(view: View) {

        val self = weakSelf.get()
        if (null != self) {
            val musicPlayer = self.playerService?.mediaPlayer
            if(null != musicPlayer){
                val tag = view.tag
                when (tag) {
                    PlayMeConstants.PLAYING -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        self.playerService?.resumeMedia()
                        self.playerService?.handleIncomingActions(Intent(self.playerService?.ACTION_PLAY))
                    }
                    PlayMeConstants.PAUSE -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                        self.playButton.setImageResource(R.drawable.ic_play_white)
                        self.playButton.tag = PlayMeConstants.PLAYING
                        self.playOnHomeIcon.tag = PlayMeConstants.PLAYING
                        self.playerService?.pauseMedia()
                        self.playerService?.handleIncomingActions(Intent(self.playerService?.ACTION_PAUSE))
                    }
                    else -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        self.storage?.storeAvailable(false)
                        self.playerService?.handleIncomingActions(Intent(playerService?.ACTION_PLAY))

                        if(storage?.loadAudioSeekPosition() != 0){
                            self.playerService?.resumePosition = storage?.loadAudioSeekPosition()!!.toInt()
                            self.playerService?.resumeMediaPlayerWhereUserLeft()
                        } else {
                            self.playerService?.startPlayingMusic()
                        }
                    }
                }
                updateProgressBar()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun detailPlayAction(view: View) {
        val self = weakSelf.get()

        if (null != self) {
            val musicPlayer =  self.playerService?.mediaPlayer
            if(null != musicPlayer){
                val tag = view.tag
                when (tag) {
                    PlayMeConstants.PLAYING -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        self.playerService?.resumeMedia()
                        self.playerService?.handleIncomingActions(Intent(playerService?.ACTION_PLAY))
                    }
                    PlayMeConstants.PAUSE -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                        self.playButton.setImageResource(R.drawable.ic_play_white)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        self.playerService?.pauseMedia()
                        self.playerService?.handleIncomingActions(Intent(playerService?.ACTION_PAUSE))
                    }
                    else -> {
                        self.playButton.setImageResource(R.drawable.pause_main)
                        self.playOnHomeIcon.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        self.storage?.storeAvailable(false)
                        self.playerService?.handleIncomingActions(Intent(playerService?.ACTION_PLAY))

                        if(storage?.loadAudioSeekPosition() != 0){
                            self.playerService?.resumePosition = storage?.loadAudioSeekPosition()!!.toInt()
                            self.playerService?.resumeMediaPlayerWhereUserLeft()
                        } else {
                            self.playerService?.startPlayingMusic()
                        }
                    }
                }
                updateProgressBar()
            }
        }
    }

    private fun updateOnScreenIcons(status: PlaybackStatus){
        val self = weakSelf.get()

        if (null != self) {
            if(status == (PlaybackStatus.PLAYING)){
                self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                self.playButton.setImageResource(R.drawable.pause)
                self.playButton.tag = PlayMeConstants.PAUSE
                self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
            }
            else {
                self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                self.playButton.setImageResource(R.drawable.ic_play_white)
                self.playButton.tag = PlayMeConstants.PLAYING
                self.playOnHomeIcon.tag = PlayMeConstants.PLAYING
            }
        }
    }

    private fun closeAction() {
        val self = weakSelf.get()

        if (null != self) {
            BottomSheetBehavior.from(self.bottomSheet).setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
        // remove message Handler from updating progress bar
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        mHandler.removeCallbacks(mUpdateTimeTask)

        val mediaPlayer = playerService?.mediaPlayer
        if (mediaPlayer != null) {

            val totalDuration = mediaPlayer.duration
            val currentDuration = mediaPlayer.currentPosition

            val currentPosition = utils.progressToTimer(detailSeekBar.progress, totalDuration)
            startTimer.text = (utils.milliSecondsToTimer(currentDuration.toLong()));

            // forward or backward to certain seconds
            playerService?.resumePosition = currentPosition

            mediaPlayer.seekTo(currentPosition)
            storage?.storeAudioCurrentSeekPosition(currentDuration)

            // update timer progress again
            updateProgressBar()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState!!.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        serviceBound = savedInstanceState.getBoolean("ServiceState")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            // service is active
            playerService!!.stopSelf()
        }
         mHandler.removeCallbacks(mUpdateTimeTask)
    }

    override fun onResume() {
        if (audioList.size > 0) {
            if (!UtilityApp.getAppDatabaseValue(this)) {
                UtilityApp.startTapTargetViewForPlayIcon(this, playOnHomeIcon, toolbar, R.id.action_search)
            }
        }
        super.onResume()
    }

    override fun onPause() {
        val self = weakSelf.get()

        if (null != self) {
            val mediaPlayer = self.playerService?.mediaPlayer
            if (null != mediaPlayer) {
                self.storage?.storeAudioIndex(self.audioIndex)
                self.storage?.storeAudioCurrentSeekPosition(mediaPlayer.currentPosition)
                self.storage?.storeAudioProgress(self.frontSeekBar.progress)
                self.storage?.storeAudioCurrentTime(self.startTimer.text.toString())
            }
        }
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_base, menu)

        val item = menu.findItem(R.id.action_search)
        search_view.setMenuItem(item)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_search -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        val self = weakSelf.get()

        if (null != self) {
            if (self.search_view.isSearchOpen) {
                self.search_view.closeSearch()
            } else {
                if(BottomSheetBehavior.from(self.bottomSheet).state == (BottomSheetBehavior.STATE_EXPANDED)){
                    BottomSheetBehavior.from(self.bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED;
                } else {
                    super.onBackPressed()
                }
            }
        }
    }
}





