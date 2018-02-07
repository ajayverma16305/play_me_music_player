package com.androidteam.playme.MainModule.baseModule

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.androidteam.playme.HelperModule.UtilityApp
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import kotlinx.android.synthetic.main.activity_base.*
import com.miguelcatalan.materialsearchview.MaterialSearchView
import android.support.design.widget.BottomSheetBehavior
import kotlinx.android.synthetic.main.persistent_bottomsheet.*
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.Toast
import com.androidteam.playme.MainModule.adapter.MusicAdapter
import com.androidteam.playme.HelperModule.PlaybackStatus
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.HelperModule.StorageUtil
import com.androidteam.playme.HelperModule.TimeUtilities
import com.androidteam.playme.MusicProvider.MediaPlayerService
import com.androidteam.playme.MusicProvider.MusicContentProvider
import com.bumptech.glide.Glide
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class BaseActivity : AppCompatActivity(), View.OnClickListener,
        OnAudioPickedListener, MaterialSearchView.OnQueryTextListener,
        MaterialSearchView.SearchViewListener,SeekBar.OnSeekBarChangeListener, MediaPlayerService.OnAudioChangedListener,
        MusicAdapter.OnShuffleIconClickListener , MediaPlayerService.OnNotificationChangeListener{

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

    companion object {
        val BROAD_CAST_PLAY_NEW_AUDIO = "com.androidteam.playme.PlayNewAudio"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name_title)

        //Store Serializable audioList to SharedPreferences
        storage = StorageUtil(applicationContext)

        // Audio List
        audioList = MusicContentProvider.getAllMusicPathList(this@BaseActivity)
        Collections.sort(audioList) { lhs, rhs -> lhs.title.compareTo(rhs.title) }

        //Store Serializable audioList to SharedPreferences
        storage?.storeAudio(audioList)

        initUIComponents()
        setBottomSheetDrawerView()

        startMediaService()
        setInitialStateOnViews()
    }

    /**
     * Initialization of UI components
     */
    private fun initUIComponents() {
        val musicAdapter = MusicAdapter(audioList)
        music_recycler_view.layoutManager = LinearLayoutManager(this) as LinearLayoutManager
        music_recycler_view.adapter = musicAdapter
        musicAdapter.setSongClickedListener(this)
        musicAdapter.setOnShuffleIconClickListener(this)
        frontSeekBar.max = 100

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
        search_view.setSuggestionIcon(resources.getDrawable(R.drawable.ic_music_note_black_24dp, null))
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
        if(storage?.loadAudioShuffledState()!!){
            shuffle.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.colorPrimary))
        } else {
            shuffle.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.lightGray))
        }

        if(storage?.loadAudioIsRepeatOne()!!){
            repeatIcon.setImageResource(R.drawable.repeat_one)
        } else {
            repeatIcon.setImageResource(R.drawable.infinite_loop)
        }

        // last audio index
        audioIndex = storage?.loadAudioIndex()!!

        // Load data from SharedPreferences
        if (audioList.size > 0) {
            musicContentObj = audioList[audioIndex]
        }

        playOnHomeIcon.setImageResource(R.drawable.play_main)
        playingSongName.text = (musicContentObj?.title)
        artistName.text = (musicContentObj?.artist)

        if (storage?.loadAudioProgress() != 0) {
            frontSeekBar.progress = (storage!!.loadAudioProgress())
            detailSeekBar.progress = (storage!!.loadAudioProgress())
        } else {
            frontSeekBar.progress = 0
            detailSeekBar.progress = 0
        }

        // Set Media Detail Initial State
        playButton.setImageResource(R.drawable.ic_play_white)
        closeSongName.text = musicContentObj?.title
        closeArtistName.text = musicContentObj?.artist

        Glide.with(this@BaseActivity).load(musicContentObj?.cover).into(picOnFrontView)
        Glide.with(this@BaseActivity).load(musicContentObj?.cover).into(albumImageView)
        startTimer.text = storage?.loadAudioCurrentTime().toString()

        if(storage?.loadAudioTotalTime() != "00:00"){
            endTimer.text = musicContentObj!!.duration
        }
        else {
            endTimer.text = storage?.loadAudioTotalTime().toString()
        }

        playOnHomeIcon.tag = if (storage?.loadAudioIconTag() == "pause") {
            null
        } else {
            storage?.loadAudioIconTag()
        }
        playButton.tag = if (storage?.loadAudioIconTag() == "pause") {
            null
        } else {
            storage?.loadAudioIconTag()
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
        playingSongName.text = (musicContentObj?.title)
        artistName.text = (musicContentObj?.artist)

        if (!musicContentObj?.cover.isNullOrEmpty()) {
            Glide.with(this@BaseActivity)
                    .load(musicContentObj?.cover)
                    .into(picOnFrontView)
        } else {
            picOnFrontView.setImageResource(R.drawable.ic_music_note_black_24dp)
        }

        playOnHomeIcon.setImageResource(R.drawable.pause_main)
        storage?.storeAudioIconTag("playing")

        updateProgressBar()
        setCurrentDetailsToMainUI()
    }

    // Set Current Details To Main UI
    private fun setCurrentDetailsToMainUI(){
        closeSongName.text = musicContentObj?.title
        closeArtistName.text = musicContentObj?.artist

        if (!musicContentObj?.cover.isNullOrEmpty()) {
            Glide.with(this@BaseActivity)
                    .load(musicContentObj?.cover)
                    .into(albumImageView)
        } else {
            albumImageView.setImageResource(R.drawable.ic_music_note_black_24dp)
        }

        playButton.setImageResource(R.drawable.pause)
        playButton.tag = "playing"

        endTimer.text = musicContentObj?.duration
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
            val mediaPlayer = playerService?.mediaPlayer

            if (null != mediaPlayer) { // Updating progress bar
                val totalDuration = mediaPlayer.duration
                val currentDuration = mediaPlayer.currentPosition

                startTimer.text = (utils.milliSecondsToTimer(currentDuration.toLong()));

                val progress = utils.getProgressPercentage(currentDuration.toLong(), totalDuration.toLong())

                frontSeekBar.progress = progress
                detailSeekBar.progress = progress

                storage?.storeAudioProgress(progress)
                storage?.storeAudioCurrentSeekPosition(currentDuration)
                storage?.storeAudioCurrentTime(startTimer.text.toString())
            }

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
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
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
            storage?.storeAudioIndex(audioIndex)

            playerService?.activeAudio = musicContentObj
            playerService?.startPlayingMusic()
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
        val audioNameList : ArrayList<String> = arrayListOf()
        for(i in 0 until audioList.size){
            val musicContent : MusicContent = audioList[i]
            audioNameList.add(musicContent.title)
        }

        search_view.setSuggestions(audioNameList.toTypedArray());
    }

    override fun onNewIntent(intent: Intent) {
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED)
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
        if(!storage?.loadAudioIsRepeatOne()!!){
            storage?.storeAudioRepeatOne(true)
            repeatIcon.setImageResource(R.drawable.repeat_one)
        } else {
            storage?.storeAudioRepeatOne(false)
            repeatIcon.setImageResource(R.drawable.infinite_loop)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun shuffleIconAction() {
        if (storage?.loadAudioShuffledState()!!) {
            storage?.storeAudioShuffle(false)
            shuffle.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.hintColor, null))
        } else {
            storage?.storeAudioShuffle(true)
            shuffle.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.colorPrimary, null))
        }
    }

    private fun previousIconAction() {
        if (playerService?.mediaPlayer != null) {
            playerService?.skipToPrevious()
            musicContentObj = playerService?.currentAudioDetails()
            setCurrentMusicDetailsToUI()
        }
    }

    private fun nextIconAction() {
        if (playerService?.mediaPlayer != null) {
            playerService?.skipToNext()
            musicContentObj = playerService?.currentAudioDetails()
            setCurrentMusicDetailsToUI()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun playHomeIconAction(view: View) {
        val musicPlayer = playerService?.mediaPlayer

        if(null != musicPlayer){
            val tag = view.tag
            when (tag) {
                "playing" -> {
                    playOnHomeIcon.setImageResource(R.drawable.pause_main)
                    playButton.setImageResource(R.drawable.pause)
                    playButton.tag = "pause"
                    playOnHomeIcon.tag = "pause"
                    playerService?.resumeMedia()
                }
                "pause" -> {
                    playOnHomeIcon.setImageResource(R.drawable.play_main)
                    playButton.setImageResource(R.drawable.ic_play_white)
                    playButton.tag = "playing"
                    playOnHomeIcon.tag = "playing"
                    playerService?.pauseMedia()
                }
                else -> {
                    playOnHomeIcon.setImageResource(R.drawable.pause_main)
                    playButton.setImageResource(R.drawable.pause_main)
                    playButton.tag = "pause"
                    playOnHomeIcon.tag = "pause"
                    playerService?.startPlayingMusic()
                }
            }
            updateProgressBar()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun detailPlayAction(view: View) {
        val weakSelf : WeakReference<BaseActivity> = WeakReference<BaseActivity>(this)
        val self = weakSelf.get()

        val musicPlayer = playerService?.mediaPlayer

        if (null != self) {
            if(null != musicPlayer){
                val tag = view.tag
                when (tag) {
                    "playing" -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.setImageResource(R.drawable.pause)
                        self.playButton.tag = "pause"
                        self.playOnHomeIcon.tag = "pause"
                        self.playerService?.resumeMedia()
                    }
                    "pause" -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                        self.playButton.setImageResource(R.drawable.ic_play_white)
                        self.playButton.tag = "playing"
                        self.playOnHomeIcon.tag = "playing"
                        self.playerService?.pauseMedia()
                    }
                    else -> {
                        self.playButton.setImageResource(R.drawable.pause_main)
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.tag = "pause"
                        self.playOnHomeIcon.tag = "pause"
                        self.playerService?.playMedia()
                    }
                }
                updateProgressBar()
            }
        }
    }

    private fun updateOnScreenIcons(status: PlaybackStatus){
        val weakSelf : WeakReference<BaseActivity> = WeakReference<BaseActivity>(this)
        val self = weakSelf.get()

        if (null != self) {
            if(status == (PlaybackStatus.PLAYING)){
                self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                self.playButton.setImageResource(R.drawable.pause)
                self.playButton.tag = "pause"
                self.playOnHomeIcon.tag = "pause"
            }
            else {
                self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                self.playButton.setImageResource(R.drawable.ic_play_white)
                self.playButton.tag = "playing"
                self.playOnHomeIcon.tag = "playing"
            }
        }
    }

    private fun closeAction() {
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
        if(p2){
            val mediaPlayer = playerService?.mediaPlayer
            if (mediaPlayer != null) {
                val currentDuration = mediaPlayer.currentPosition
                startTimer.text = (utils.milliSecondsToTimer(currentDuration.toLong()));
            }
        }
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
        if (!UtilityApp.getAppDatabaseValue(this)) {
            UtilityApp.startTapTargetViewForPlayIcon(this, playOnHomeIcon, toolbar, R.id.action_search)
        }
        super.onResume()
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
        if (search_view.isSearchOpen) {
            search_view.closeSearch()
        } else {
            if(BottomSheetBehavior.from(bottomSheet).state == (BottomSheetBehavior.STATE_EXPANDED)){
                BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED;
            } else {
                super.onBackPressed()
            }
        }
    }
}





