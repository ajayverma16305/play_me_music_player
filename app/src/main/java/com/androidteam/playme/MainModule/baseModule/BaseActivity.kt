package com.androidteam.playme.MainModule.baseModule

import android.annotation.TargetApi
import android.content.*
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import com.androidteam.playme.Listeners.OnAudioPickedListener
import com.androidteam.playme.R
import kotlinx.android.synthetic.main.activity_base.*
import com.miguelcatalan.materialsearchview.MaterialSearchView
import android.support.design.widget.BottomSheetBehavior
import kotlinx.android.synthetic.main.persistent_bottomsheet.*
import android.content.res.ColorStateList
import android.net.Uri
import android.os.*
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.androidteam.playme.Fragments.CurrentSongInfoFragment
import com.androidteam.playme.HelperModule.*
import com.androidteam.playme.Listeners.OnAudioResourcesReadyListener
import com.androidteam.playme.MainModule.adapter.MusicAdapter
import com.androidteam.playme.MusicProvider.MusicContent
import com.androidteam.playme.MusicProvider.MediaPlayerService
import com.androidteam.playme.MusicProvider.MusicContentProvider
import com.androidteam.playme.SplashModule.LaunchScreenActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class BaseActivity : AppCompatActivity(), View.OnClickListener,
        MaterialSearchView.OnQueryTextListener, MaterialSearchView.SearchViewListener,
        SeekBar.OnSeekBarChangeListener, MediaPlayerService.OnAudioChangedListener,
        MediaPlayerService.OnNotificationChangeListener,OnAudioPickedListener ,
        MediaPlayerService.OnMediaPlayerErrorListener{

    private lateinit var sheetBehavior: BottomSheetBehavior<*>
    private var playerService: MediaPlayerService? = null
    private var serviceBound = false

    //List of available Audio files
    private var audioList = ArrayList<MusicContent>()
    private var audioIndex = 0
    private var musicContentObj: MusicContent? = null
    private var mHandler: Handler = Handler()
    private var storage: StorageUtil? = null
    private val utils: TimeUtilities = TimeUtilities()
    private var weakSelf: WeakReference<BaseActivity> = WeakReference(this)
    private var musicAdapter : MusicAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name_title)

        toolbar.setNavigationOnClickListener({
            toolbarNavigationAction()
        })

        val self = weakSelf.get()
        if (null != self) {
            self.music_recycler_view.visibility = View.GONE
            self.card_view.visibility = View.GONE
        }
        initializeRecyclerView()

        audioList = LaunchScreenActivity.audioList
        if (audioList.size > 0) {
            startFetchingAudioFilesFromStorage()
        } else {
            setErrorViewForNoAudio()
        }
    }

    // Toolbar Navigation Action
    private fun toolbarNavigationAction() {
        val audioBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.app_info_view, null)
        audioBuilder.setView(dialogView)
        val alertDialog = audioBuilder.create()
        alertDialog.show()

        val versionNumberTextView = dialogView.findViewById<TextView>(R.id.versionNumber)
        versionNumberTextView.text = "App Version : " + getAppVersion()

        val rateButton = dialogView.findViewById<Button>(R.id.rateButton)
        val rateLaterButton = dialogView.findViewById<Button>(R.id.rateLaterButton)

        rateLaterButton.setOnClickListener {
            alertDialog.dismiss()
            Toast.makeText(applicationContext,"Thanks for your feedback",Toast.LENGTH_SHORT).show()
        }

        rateButton.setOnClickListener {
            alertDialog.dismiss()
            val appPackageName = packageName // getPackageName() from Context or Activity object
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
            } catch (anfe: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
            }
        }
    }

    // Version number
    private fun getAppVersion() : String{
        val pInfo = packageManager.getPackageInfo(packageName, 0);
        if(null != pInfo){
            val versionNumber = pInfo.versionCode
            val versionName = pInfo.versionName
            return versionName
        }
        return "1.0.1"
    }

    // Start Fetching Audio Files From Storage
    private fun startFetchingAudioFilesFromStorage() {
        music_recycler_view.visibility = View.VISIBLE
        card_view.visibility = View.VISIBLE

        if (!UtilityApp.getAppDatabaseValue(this@BaseActivity)) {
            SystemClock.sleep(2000);
            UtilityApp.startTapTargetViewForPlayIcon(this@BaseActivity, playOnHomeIcon, toolbar, R.id.action_search)
        }

        //Store Serializable audioList to SharedPreferences
        storage = StorageUtil(applicationContext)

        Collections.sort(audioList) { lhs, rhs -> lhs.title.compareTo(rhs.title) }

        //Store Serializable audioList to SharedPreferences
        storage?.storeAudio(audioList)
        storage?.storeAudioListSize(audioList.size)

        checkForNewlyAddedFilesFromStorage()

        initUIComponents()
        setBottomSheetDrawerView()

        startMediaService()
        setInitialStateOnViews()
    }

    /**
     * Check For Newly Added Files From Storage
     */
    private fun checkForNewlyAddedFilesFromStorage(){
        val musicAsyncObj = MusicContentProvider(WeakReference(applicationContext),object : OnAudioResourcesReadyListener{
            override fun resourcesList(musicContentList: ArrayList<MusicContent>?) {

                if(null != musicContentList){
                    if(musicContentList.size != storage!!.loadAudioListSize()){
                        storage!!.clearCachedAudioPlaylist()
                        audioList = musicContentList

                        Collections.sort(audioList) { lhs, rhs -> lhs.title.compareTo(rhs.title) }

                        //Store Serializable audioList to SharedPreferences
                        storage?.storeAudio(audioList)
                        storage?.storeAudioListSize(audioList.size)

                        if (null != playerService!!.mediaPlayer) {
                            playerService!!.mediaPlayer!!.reset()
                        }
                        if (null != musicAdapter) {
                            musicAdapter!!.songsList = audioList
                            musicAdapter!!.notifyDataSetChanged()
                        }
                    }
                }
            }
        })
        musicAsyncObj.execute()
    }

    // Set Error View For No Audio
    private fun setErrorViewForNoAudio() {
        val self = weakSelf.get()
        if (null != self) {
            self.music_recycler_view.visibility = View.GONE
            self.card_view.visibility = View.GONE
            self.errorView.visibility = View.VISIBLE
        }
    }

    private fun initializeRecyclerView() {
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
        val self = weakSelf.get()

        if (null != self) {
            self.artistName.isSelected = true
            self.playingSongName.isSelected = true
            self.closeArtistName.isSelected = true
            self.closeSongName.isSelected = true
            self.header_playingSongName.isSelected = true
            self.header_artistName.isSelected = true
        }

        musicAdapter = MusicAdapter(this, audioList)
        music_recycler_view.adapter = musicAdapter
        musicAdapter!!.setSongClickedListener(this)
        fastscroll.setRecyclerView(music_recycler_view)

        playLayout.setOnClickListener(this)
        closeAction.setOnClickListener(this)
        playOnHomeIcon.setOnClickListener(this)
        nextIcon.setOnClickListener(this)
        previousIcon.setOnClickListener(this)
        repeatIcon.setOnClickListener(this)
        playButton.setOnClickListener(this)
        shuffleDetail.setOnClickListener(this)
        infoAction.setOnClickListener(this)
        shuffleFab.setOnClickListener(this)
        detailSeekBar.setOnSeekBarChangeListener(this)

        search_view.setOnQueryTextListener(this)
        search_view.setOnSearchViewListener(this)
        search_view.setHint(getString(R.string.action_search))
        search_view.setSuggestionIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_music_note_black_24dp))
        search_view.setSubmitOnClick(true)
    }

    // Set Bottom Sheet Drawer View
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

                    val fragment = supportFragmentManager.findFragmentByTag("info")
                    if (fragment != null ) {
                        supportFragmentManager.beginTransaction().remove(fragment).commit()
                    }
                }
            }
        })
    }

    // Start Media Service
    private fun startMediaService() {
        val playerIntent = Intent(this, MediaPlayerService::class.java)
        startService(playerIntent)
        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // Set initial state on views
    private fun setInitialStateOnViews() {
        val self = weakSelf.get()
        if (null != self) {
            if (storage?.loadAudioShuffledState()!!) {
                self.shuffleDetail.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.white))
            } else {
                self.shuffleDetail.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.black))
            }

            if (storage?.loadAudioIsRepeatOne()!!) {
                self.repeatIcon.setImageResource(R.drawable.repeat_one)
            } else {
                self.repeatIcon.setImageResource(R.drawable.repeat_all)
            }

            // last audio index
            audioIndex = storage?.loadAudioIndex()!!

            // Load data from SharedPreferences
            if (audioList.size > 0) {
                musicContentObj = audioList[audioIndex]
            }

            if(audioIndex <= audioList.size ){
                music_recycler_view.scrollToPosition(audioIndex)
            }

            self.setCurrentFileIndex()

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.playme_app_logo)
                    .override(100, 100)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.currentPlayingCoverImage.setImageResource(R.drawable.placeholder)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.currentPlayingCoverImage)

            self.playOnHomeIcon.setImageResource(R.drawable.play_main)
            self.playButton.setImageResource(R.drawable.play_big)

            self.playingSongName.text = (musicContentObj?.title)
            self.artistName.text = (musicContentObj?.artist)

            self.header_playingSongName.text = (musicContentObj?.title)
            self.header_artistName.text = (musicContentObj?.artist)

            self.frontSeekBar.progress = (storage!!.loadAudioProgress())
            self.detailSeekBar.progress = (storage!!.loadAudioProgress())

            // Set Media Detail Initial State
            self.closeSongName.text = musicContentObj?.title
            self.closeArtistName.text = musicContentObj?.artist

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.ic_music_note_white_24dp)
                    .override(100, 100)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.picOnFrontView.setImageResource(R.drawable.ic_music_note_white_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.picOnFrontView)

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.placeholder)
                    .override(300, 300)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.albumImageView.setImageResource(R.drawable.placeholder)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.albumImageView)

            val startTimerText = if(storage?.loadAudioCurrentTime().toString() != PlayMeConstants.TIMER_ZERO){
                storage?.loadAudioCurrentTime().toString()
            } else {
                PlayMeConstants.TIMER_ZERO
            }

            self.startTimer.text = startTimerText
            self.endTimer.text = musicContentObj?.duration

            self.playOnHomeIcon.tag = null
            self.playButton.tag = null
        }
    }

    private fun setCurrentPositionOnAdapterView() {
        try {
            val position = storage!!.loadAudioIndex()
            val holder : RecyclerView.ViewHolder = music_recycler_view.findViewHolderForAdapterPosition(position)

            if (null != holder) {
                musicAdapter!!.currentPosition(audioIndex, holder)
            }
        } catch (e: Exception) {
            Log.d("TAG:: ","findViewHolder :: " +e)
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
            playerService?.setOnMediaPlayerErrorListener(this@BaseActivity)
            Timber.d("Service Bound")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceBound = false
            Timber.d("Service Disconnected")
        }
    }

    override fun updateUI(activeAudio: MusicContent) {
        musicContentObj = activeAudio
        setCurrentMusicDetailsToUI()
    }

    override fun onAudioStateChange(status: PlaybackStatus) {
        updateOnScreenIcons(status)
        mediaStateChangeAction(status)
    }

    override fun audioPicked(activeMusic: MusicContent, position: Int) {
        musicContentObj = activeMusic

        setCurrentMusicDetailsToUI()
        playAudio(position)
    }

    private fun setCurrentFileIndex(){
        Handler().postDelayed(Runnable {
            totalTrack.text = ("" + (storage!!.loadAudioIndex() + 1).toInt() + "/" + audioList.size)
        },200)
    }

    // Set Current Music Details To UI
    private fun setCurrentMusicDetailsToUI() {
        val self = weakSelf.get()

        if (null != self) {
            self.setCurrentPositionOnAdapterView()
            self.mediaStateChangeAction(PlaybackStatus.PLAYING)
            self.setCurrentFileIndex()
            self.playingSongName.text = (musicContentObj?.title)
            self.artistName.text = (musicContentObj?.artist)

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.ic_music_note_white_24dp)
                    .override(100, 100)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.picOnFrontView.setImageResource(R.drawable.ic_music_note_white_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.picOnFrontView)

            self.playOnHomeIcon.setImageResource(R.drawable.pause_main)

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.playme_app_logo)
                    .override(100, 100)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.currentPlayingCoverImage.setImageResource(R.drawable.placeholder)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.currentPlayingCoverImage)

            self.header_playingSongName.text = (musicContentObj?.title)
            self.header_artistName.text = (musicContentObj?.artist)
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

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.placeholder)
                    .override(300, 300)
                    .fitCenter()
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.albumImageView.setImageResource(R.drawable.placeholder)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.albumImageView)

            self.playButton.setImageResource(R.drawable.pause)
            self.endTimer.text = musicContentObj?.duration
        }
    }

    override fun onErrorHandled(activeAudio: MusicContent) {
        musicContentObj = activeAudio
        setLastSavedStateAfterMediaPlayerErrorOccurred()
    }

    // Set Last Saved State After Media Player Error Occurred
    private fun setLastSavedStateAfterMediaPlayerErrorOccurred(){
        val self = weakSelf.get()

        if(null != self){
            storage?.storeAudioIndex(playerService!!.audioIndex)
            self.setCurrentFileIndex()
            self.frontSeekBar.progress = 0
            self.detailSeekBar.progress = 0

            self.startTimer.text = PlayMeConstants.TIMER_ZERO
            self.playingSongName.text = (musicContentObj?.title)
            self.artistName.text = (musicContentObj?.artist)

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.ic_music_note_white_24dp)
                    .override(100, 100)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.currentPlayingCoverImage.setImageResource(R.drawable.ic_music_note_white_24dp)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.currentPlayingCoverImage)

            self.header_playingSongName.text = (musicContentObj?.title)
            self.header_artistName.text = (musicContentObj?.artist)

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.playme_app_logo)
                    .override(60, 60)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.picOnFrontView.setImageResource(R.drawable.playme_app_logo)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.picOnFrontView)

            self.playOnHomeIcon.setImageResource(R.drawable.play_main)

            self.closeSongName.text = musicContentObj?.title
            self.closeArtistName.text = musicContentObj?.artist

            Glide.with(applicationContext)
                    .load(musicContentObj?.cover)
                    .error(R.drawable.placeholder)
                    .override(300, 300)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: java.lang.Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            self.albumImageView.setImageResource(R.drawable.placeholder)
                            return true
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Timber.d("Resource Ready")
                            return false
                        }
                    }).into(self.albumImageView)

            self.playButton.setImageResource(R.drawable.play_big)
            self.endTimer.text = musicContentObj?.duration
        }
    }

    private fun playAudio(position: Int) {
        audioIndex = position

        //Store Serializable audioList to SharedPreferences
        storage?.storeAudioIndex(audioIndex)
        mediaStateChangeAction(PlaybackStatus.PLAYING)

        //Check is service is active
        if (!serviceBound) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            val broadcastIntent = Intent(PlayMeConstants.BROAD_CAST_PLAY_NEW_AUDIO)
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
                val mediaPlayer = playerService?.mediaPlayer

                if (null != mediaPlayer) { // Updating progress bar
                    val totalDuration = mediaPlayer.duration
                    val currentDuration = mediaPlayer.currentPosition

                    self.startTimer.text = (utils.milliSecondsToTimer(currentDuration.toLong()))

                    val progress = utils.getProgressPercentage(currentDuration.toLong(), totalDuration.toLong())
                    self.frontSeekBar.progress = progress
                    self.detailSeekBar.progress = progress

                    storage?.storeAudioProgress(progress)
                    storage?.storeAudioCurrentSeekPosition(currentDuration)
                    storage?.storeAudioCurrentTime(startTimer.text.toString())
                }
            }

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.playButton -> {
                detailPlayAction(view)
            }
            R.id.closeAction -> {
                closeAction()
            }
            R.id.playLayout -> {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            R.id.playOnHomeIcon -> {
                detailPlayAction(view)
            }
            R.id.nextIcon -> {
                nextIconAction()
            }
            R.id.previousIcon -> {
                previousIconAction()
            }
            R.id.repeatIcon -> {
                repeatAllIconAction()
            }
            R.id.shuffleDetail -> {
                shuffleIconAction()
            }
            R.id.infoAction -> {
                showCurrentInfo()
            }
            R.id.shuffleFab -> {
                view.bringToFront()
                shuffleAction(view)
            }
        }
    }

    // Show Current Info
    private fun showCurrentInfo() {
        val currentSongInfoFragment = CurrentSongInfoFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        transaction.add(R.id.infoFrameLayout, currentSongInfoFragment,"info")
        currentSongInfoFragment.setActiveMusicContentInfo(audioList[playerService!!.audioIndex])
        transaction.addToBackStack("CurrentSongInfoFragment")
        transaction.commit()
    }

    private fun shuffleAction(view: View) {
        view.isEnabled = false

        audioIndex = playerService?.getRandomAudioFileIndex()!!
        musicContentObj = audioList[audioIndex]
        playerService?.activeAudio = musicContentObj
        playAudio(audioIndex)
        setCurrentMusicDetailsToUI()

        Handler().postDelayed({
            view.isEnabled = true
        }, 200)
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun repeatAllIconAction() {
        val self = weakSelf.get()

        if (null != self) {
            if (!storage?.loadAudioIsRepeatOne()!!) {
                storage?.storeAudioRepeatOne(true)
                self.repeatIcon.setImageResource(R.drawable.repeat_one)
            } else {
                storage?.storeAudioRepeatOne(false)
                self.repeatIcon.setImageResource(R.drawable.repeat_all)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun shuffleIconAction() {
        val self = weakSelf.get()

        if (null != self) {
            if (storage?.loadAudioShuffledState()!!) {
                storage?.storeAudioShuffle(false)
                self.shuffleDetail.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.black))
            } else {
                storage?.storeAudioShuffle(true)
                self.shuffleDetail.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(applicationContext, R.color.white))
            }
        }
    }

    // Previous Icon Action
    private fun previousIconAction() {
        if (playerService != null) {
            musicContentObj = playerService!!.currentAudioDetails()
            playerService!!.handleIncomingActions(Intent(playerService!!.ACTION_PREVIOUS))
            setCurrentMusicDetailsToUI()
        }
    }

    // Next Icon Action
    private fun nextIconAction() {
        if (playerService != null) {
            musicContentObj = playerService!!.currentAudioDetails()
            playerService!!.handleIncomingActions(Intent(playerService!!.ACTION_NEXT))
            setCurrentMusicDetailsToUI()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun detailPlayAction(view: View) {
        val self = weakSelf.get()
        if (null != self) {
            val musicPlayer = playerService?.mediaPlayer
            if (null != musicPlayer) {
                val tag = view.tag
                when (tag) {
                    PlayMeConstants.PLAYING -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                        self.playButton.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        playerService?.handleIncomingActions(Intent(playerService?.ACTION_PLAY))
                        self.mediaStateChangeAction(PlaybackStatus.PLAYING)

                    }
                    PlayMeConstants.PAUSE -> {
                        self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                        self.playButton.setImageResource(R.drawable.play_big)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        playerService?.handleIncomingActions(Intent(playerService?.ACTION_PAUSE))
                        self.mediaStateChangeAction(PlaybackStatus.PAUSED)

                    }
                    else -> {
                        self.playButton.setImageResource(R.drawable.pause_main)
                        self.playOnHomeIcon.setImageResource(R.drawable.pause)
                        self.playButton.tag = PlayMeConstants.PAUSE
                        self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
                        storage?.storeAvailable(false)
                        playerService?.handleIncomingActions(Intent(playerService?.ACTION_PLAY))

                        if (storage?.loadAudioSeekPosition() != 0) {
                            playerService?.resumePosition = storage?.loadAudioSeekPosition()!!.toInt()
                            playerService?.resumeMediaPlayerWhereUserLeft()
                        } else {
                            playerService?.startPlayingMusic()
                        }
                        self.mediaStateChangeAction(PlaybackStatus.PLAYING)
                    }
                }
                self.updateProgressBar()
            }
        }
    }
    
    /**
     * Update On Screen Icons
     */
    private fun updateOnScreenIcons(status: PlaybackStatus) {
        val self = weakSelf.get()

        if (null != self) {
            if (status == (PlaybackStatus.PLAYING)) {
                self.playOnHomeIcon.setImageResource(R.drawable.pause_main)
                self.playButton.setImageResource(R.drawable.pause)
                self.playButton.tag = PlayMeConstants.PAUSE
                self.playOnHomeIcon.tag = PlayMeConstants.PAUSE
            } else {
                self.playOnHomeIcon.setImageResource(R.drawable.play_main)
                self.playButton.setImageResource(R.drawable.play_big)
                self.playButton.tag = PlayMeConstants.PLAYING
                self.playOnHomeIcon.tag = PlayMeConstants.PLAYING
            }
            self.setCurrentPositionOnAdapterView()
        }
    }

    private fun mediaStateChangeAction(status: PlaybackStatus){
        if(status == PlaybackStatus.PLAYING){
            equalizerView.animateBars()
        } else {
            equalizerView.stopBars()
        }
    }

    // Close Action
    private fun closeAction() {
        val self = weakSelf.get()

        if (null != self) {
            BottomSheetBehavior.from(self.bottomSheet).state = (BottomSheetBehavior.STATE_COLLAPSED);
        }
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

    override fun onPause() {
        val self = weakSelf.get()
        if (null != self) {
            val mediaPlayer = playerService?.mediaPlayer
            if (null != mediaPlayer) {
                storage?.storeAudioIndex(playerService!!.audioIndex)
                storage?.storeAudioCurrentSeekPosition(mediaPlayer.currentPosition)
                storage?.storeAudioProgress(self.frontSeekBar.progress)
                storage?.storeAudioCurrentTime(self.startTimer.text.toString())
            }
        }
        super.onPause()
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
            val infoFragment = supportFragmentManager.findFragmentByTag("info")

            if (infoFragment != null) {
                supportFragmentManager.popBackStackImmediate()
                supportFragmentManager.beginTransaction().remove(infoFragment).commit()
            }
            else {
                if (self.search_view.isSearchOpen) {
                    self.search_view.closeSearch()
                } else {
                    if (BottomSheetBehavior.from(self.bottomSheet).state == (BottomSheetBehavior.STATE_EXPANDED)) {
                        BottomSheetBehavior.from(self.bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED;
                    } else {
                        actionOnBackPressedToGoAndroidHome()
                    }
                }
            }
        }
    }

    // Action OnBackPressed To Go Android Home
    private fun actionOnBackPressedToGoAndroidHome(){
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        hideKeyboard()

        var isFound = false
        if (null != query) {
            for (i in 0 until audioList.size) {
                val musicContent: MusicContent = audioList[i]
                if (musicContent.title.contentEquals(query)) {
                    isFound = true
                    audioIndex = i;
                    break
                }
            }
        }

        if (!isFound) {
            Toast.makeText(applicationContext, "No Such Song Available", Toast.LENGTH_SHORT).show()
        }
        else {
            musicContentObj = audioList[audioIndex]
            playerService?.activeAudio = musicContentObj
            setCurrentMusicDetailsToUI()
            playAudio(audioIndex)

            BottomSheetBehavior.from(bottomSheet).state = (BottomSheetBehavior.STATE_EXPANDED)
        }
        return false
    }

    private fun  hideKeyboard() {
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

        val audioNameList: ArrayList<String> = arrayListOf()
        for (i in 0 until audioList.size) {
            val musicContent: MusicContent = audioList[i]
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
}





