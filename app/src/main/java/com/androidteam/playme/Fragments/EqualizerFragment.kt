package com.androidteam.playme.Fragments

import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.androidteam.playme.R
import timber.log.Timber

/**
 * A simple [Fragment] subclass.
 */
class EqualizerFragment : Fragment() ,AdapterView.OnItemSelectedListener {

    private var mediaPlayer : MediaPlayer? = null
    private var mEqualizer : Equalizer? = null
    private var presetSelectedItemPosition = -1
    private lateinit var seekBarDynamicLayout : LinearLayout

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.equalizer_layout_view,container,false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentInstance = activity.applicationContext
        val color = ContextCompat.getColor(currentInstance, R.color.white)

        val presetSpinner = view!!.findViewById<Spinner>(R.id.presetSpinner)
        val closeEqualizerAction = view.findViewById<ImageView>(R.id.closeEqualizerAction)
        seekBarDynamicLayout = view.findViewById<LinearLayout>(R.id.runTimeBands)

        closeEqualizerAction.setOnClickListener({
            activity.onBackPressed()
        })

        mEqualizer = Equalizer(0,mediaPlayer!!.audioSessionId)
        mEqualizer!!.enabled = true

        val size =  mEqualizer!!.numberOfPresets - 1
        val presetEqualizerNames = ArrayList<String>()

        for(i in 0 until size){
            presetEqualizerNames.add(mEqualizer!!.getPresetName(i.toShort()))
        }

        val adapter = object : ArrayAdapter<String>(currentInstance, android.R.layout.simple_spinner_item, android.R.id.text1, presetEqualizerNames) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                if (position == 0) {
                    (view as TextView).setTextColor(ContextCompat.getColor(currentInstance, R.color.hintColor))
                } else if (position == presetSelectedItemPosition) {
                    (view as TextView).setTextColor(ContextCompat.getColor(currentInstance, R.color.TextBlueColor))
                }
                return super.getDropDownView(position, null, parent)
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        presetSpinner.adapter = adapter
        presetSpinner.setSelection(0, true)
        val v = presetSpinner.selectedView
        (v as TextView).setTextColor(color)
        presetSpinner.onItemSelectedListener = this


        val numberOfBands = mEqualizer!!.numberOfBands
        val lowerEqualizerBandLevel = mEqualizer!!.bandLevelRange[0]
        val upperEqualizerBandLevel = mEqualizer!!.bandLevelRange[1]

        val bandSize = numberOfBands - 1
        for(i in 0 until bandSize){
            val equalizerBandIndex = i.toShort()

            // Frequency Header
            val frequencyHeaderTextView = TextView(currentInstance)
            frequencyHeaderTextView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            frequencyHeaderTextView.gravity = Gravity.CENTER_HORIZONTAL
            frequencyHeaderTextView.setTextColor(color)
            frequencyHeaderTextView.setPadding(8,16,8,16)
            frequencyHeaderTextView.text = "" + (mEqualizer!!.getCenterFreq(equalizerBandIndex) / 1000) + "Hz"

            seekBarDynamicLayout.addView(frequencyHeaderTextView)

            // SeekBar Root Layout
            val seekBarLayout = LinearLayout(currentInstance)
            seekBarLayout.orientation = LinearLayout.HORIZONTAL
            seekBarLayout.setPadding(8,24,8,24)

            // Lower db
            val lowerEqualizerBandLevelTextView = TextView(currentInstance)
            lowerEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            lowerEqualizerBandLevelTextView.text = "" + (lowerEqualizerBandLevel/100) + "dB"
            lowerEqualizerBandLevelTextView.setTextColor(color)

            // Upper db
            val upperEqualizerBandLevelTextView = TextView(currentInstance)
            upperEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            upperEqualizerBandLevelTextView.text = "" + (upperEqualizerBandLevel/100) + "dB"
            upperEqualizerBandLevelTextView.setTextColor(color)

            // Seek Bar params
            val seekBarSliderLinearLayoutParams =  LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            seekBarSliderLinearLayoutParams.weight = 1f

            // Seek Bar
            val seekBarSlider = SeekBar(currentInstance)
            seekBarSlider.id = i
            seekBarSlider.layoutParams = seekBarSliderLinearLayoutParams
            seekBarSlider.max = (upperEqualizerBandLevel - lowerEqualizerBandLevel)
            seekBarSlider.progress = mEqualizer!!.getBandLevel(equalizerBandIndex).toInt()
            seekBarSlider.progressTintList = ColorStateList.valueOf(color)
            seekBarSlider.thumbTintList = ColorStateList.valueOf(color)
            seekBarSlider.backgroundTintList = ColorStateList.valueOf(color)
            seekBarSlider.progressBackgroundTintList = ColorStateList.valueOf(R.color.lightGray)

            seekBarLayout.addView(lowerEqualizerBandLevelTextView)
            seekBarLayout.addView(seekBarSlider)
            seekBarLayout.addView(upperEqualizerBandLevelTextView)

            seekBarDynamicLayout.addView(seekBarLayout)

            seekBarSlider.setOnSeekBarChangeListener(object  : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                    mEqualizer!!.setBandLevel(equalizerBandIndex,(progress + lowerEqualizerBandLevel).toShort())
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                    Timber.d("StarTracking")
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                   Timber.d("StopTracking")
                }
            })

        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
       Timber.d("Nothing Selected")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        (view as TextView).setTextColor(ContextCompat.getColor(activity.applicationContext,R.color.white))

        presetSelectedItemPosition = position
        mEqualizer!!.usePreset(position.toShort())

        val numberFrequencyBand = mEqualizer!!.numberOfBands - 1
        val lowerEqualizerBandLevel = mEqualizer!!.bandLevelRange[0]

        for(i in 0 until numberFrequencyBand){
             val equalizerBandIndex = i.toShort()

            val seekBar = seekBarDynamicLayout.findViewById<SeekBar>(equalizerBandIndex.toInt())
            seekBar.progress = (mEqualizer!!.getBandLevel((equalizerBandIndex - lowerEqualizerBandLevel).toShort()).toInt())
        }
    }

    fun setMediaPlayer(mediaPlayer : MediaPlayer){
        this.mediaPlayer = mediaPlayer
    }
}
