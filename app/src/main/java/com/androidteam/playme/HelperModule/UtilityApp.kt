package com.androidteam.playme.HelperModule

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.View
import android.support.v7.widget.Toolbar
import com.androidteam.playme.R
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView

/**
 * Created by AJAY VERMA on 19/01/18.
 * Company : CACAO SOLUTIONS
 */
class UtilityApp {
    companion object {

        /**
         * Save Values To App Database
         */
        fun saveValuesToAppDatabase(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()
            editor.putBoolean(Constants.USER_DEFAULT_KEY_STORE,true)
            editor.apply()
        }

        /**
         * Get App Database Value
         */
        fun getAppDatabaseValue(context: Context) : Boolean{
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return sharedPreferences.getBoolean(Constants.USER_DEFAULT_KEY_STORE,false)
        }

        /**
         * Start Tap Target View For Play Icon
         */
        fun startTapTargetViewForPlayIcon(context: Context,view: View,toolbar: Toolbar,menuId : Int){
            TapTargetView.showFor(context as Activity, // `this` is an Activity
                    TapTarget.forView(view, Constants.PLAY_MUSIC_STR, Constants.PLAY_MUSIC_DESC)
                            // All options below are optional
                            .outerCircleColor(R.color.colorPrimary)      // Specify a color for the outer circle
                            .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                            .targetCircleColor(R.color.white)   // Specify a color for the target circle
                            .titleTextSize(20)                  // Specify the size (in sp) of the title text
                            .titleTextColor(R.color.white)      // Specify the color of the title text
                            .descriptionTextSize(14)            // Specify the size (in sp) of the description text
                            .descriptionTextColor(R.color.white)  // Specify the color of the description text
                            .textColor(R.color.white)            // Specify a color for both the title and description text
                            .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                            .dimColor(R.color.colorPrimary)            // If set, will dim behind the view with 30% opacity of the given color
                            .drawShadow(true)                   // Whether to draw a drop shadow or not
                            .cancelable(true)                  // Whether tapping outside the outer circle dismisses the view
                            .tintTarget(true)                   // Whether to tint the target view's color
                            .transparentTarget(false)           // Specify whether the target is transparent (displays the content underneath)
                            .icon(ContextCompat.getDrawable(context, R.drawable.play_icon))                     // Specify a custom drawable to draw as the target
                            .targetRadius(60), // Specify the target radius (in dp)

                    object : TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                        override fun onTargetClick(view: TapTargetView) {
                            super.onTargetClick(view)      // This call is optional
                            startTapTargetOnSearchIcon(context,toolbar,menuId)
                        }

                        override fun onTargetCancel(view: TapTargetView?) {
                            super.onTargetCancel(view)
                            startTapTargetOnSearchIcon(context,toolbar,menuId)
                        }
                    })
        }

        /**
         * Start Tap Target On Search Icon
         */
        fun startTapTargetOnSearchIcon(context: Context,toolbar: Toolbar, menuId : Int){
            TapTargetView.showFor(context as Activity,
                    TapTarget.forToolbarMenuItem(toolbar,menuId, Constants.SEARCH_MUSIC_STR, Constants.SEARCH_MUSIC_DESC)
                            // All options below are optional
                            .outerCircleColor(R.color.colorPrimary)      // Specify a color for the outer circle
                            .outerCircleAlpha(0.96f)            // Specify the alpha amount for the outer circle
                            .targetCircleColor(R.color.white)   // Specify a color for the target circle
                            .titleTextSize(20)                  // Specify the size (in sp) of the title text
                            .titleTextColor(R.color.white)      // Specify the color of the title text
                            .descriptionTextSize(14)            // Specify the size (in sp) of the description text
                            .descriptionTextColor(R.color.white)  // Specify the color of the description text
                            .textColor(R.color.white)            // Specify a color for both the title and description text
                            .textTypeface(Typeface.SANS_SERIF)  // Specify a typeface for the text
                            .dimColor(R.color.colorPrimary)            // If set, will dim behind the view with 30% opacity of the given color
                            .drawShadow(true)                   // Whether to draw a drop shadow or not
                            .cancelable(true)                  // Whether tapping outside the outer circle dismisses the view
                            .tintTarget(true)                   // Whether to tint the target view's color
                            .transparentTarget(false)           // Specify whether the target is transparent (displays the content underneath)
                            .icon(ContextCompat.getDrawable(context,R.drawable.search_icon))                     // Specify a custom drawable to draw as the target
                            .targetRadius(60), // Specify the target radius (in dp)

                    object : TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                        override fun onTargetClick(view: TapTargetView) {
                            super.onTargetClick(view)      // This call is optional
                            UtilityApp.saveValuesToAppDatabase(context)
                        }
                        override fun onTargetCancel(view: TapTargetView?) {
                            super.onTargetCancel(view)
                            UtilityApp.saveValuesToAppDatabase(context)
                        }
                    })
        }
    }
}