package com.androidteam.playme;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

/**
 * Created by AJAY VERMA on 05/02/18.
 * Company : CACAO SOLUTIONS
 */

public class abc {


    private void xys(View view){
        TranslateAnimation translateAnimation =
                new TranslateAnimation(0f,0f,70f,0f);
        translateAnimation.setInterpolator(new DecelerateInterpolator());
        translateAnimation.setDuration(350);
        view.startAnimation(translateAnimation);
    }
}
