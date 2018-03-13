package com.androidteam.playme.HelperModule;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import com.androidteam.playme.R;

/**
 * Created by Ajay Verma on 2/12/2018.
 */
public class MiniEqualizer extends LinearLayout {

    View musicBar1;
    View musicBar2;
    View musicBar3;
    View musicBar4;
    View musicBar5;

    AnimatorSet playingSet;
    AnimatorSet stopSet;
    Boolean animating = false;

    int foregroundColor;
    int duration;

    public MiniEqualizer(Context context) {
        super(context);
        initViews();
    }

    public MiniEqualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public MiniEqualizer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    private void initViews() {
        foregroundColor = ContextCompat.getColor(getContext(),R.color.lightGray);
        duration = 3500;

        LayoutInflater.from(getContext()).inflate(R.layout.equalizer_view, this, true);
        musicBar1 = findViewById(R.id.music_bar1);
        musicBar2 = findViewById(R.id.music_bar2);
        musicBar3 = findViewById(R.id.music_bar3);
        musicBar4 = findViewById(R.id.music_bar4);
        musicBar5 = findViewById(R.id.music_bar5);
        musicBar1.setBackgroundColor(foregroundColor);
        musicBar2.setBackgroundColor(foregroundColor);
        musicBar3.setBackgroundColor(foregroundColor);
        musicBar4.setBackgroundColor(foregroundColor);
        musicBar5.setBackgroundColor(foregroundColor);
        setPivots();
    }

    private void setPivots() {
        musicBar1.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (musicBar1.getHeight() > 0) {
                    musicBar1.setPivotY(musicBar1.getHeight());
                    if (Build.VERSION.SDK_INT >= 21) {
                        musicBar1.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        musicBar2.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (musicBar2.getHeight() > 0) {
                    musicBar2.setPivotY(musicBar2.getHeight());
                    if (Build.VERSION.SDK_INT >= 21) {
                        musicBar2.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        musicBar3.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (musicBar3.getHeight() > 0) {
                    musicBar3.setPivotY(musicBar3.getHeight());
                    if (Build.VERSION.SDK_INT >= 21) {
                        musicBar3.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        musicBar4.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (musicBar4.getHeight() > 0) {
                    musicBar4.setPivotY(musicBar4.getHeight());
                    if (Build.VERSION.SDK_INT >= 21) {
                        musicBar4.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
        musicBar5.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (musicBar5.getHeight() > 0) {
                    musicBar5.setPivotY(musicBar5.getHeight());
                    if (Build.VERSION.SDK_INT >= 21) {
                        musicBar5.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            }
        });
    }

    public void animateBars() {
        animating = true;
        if (playingSet == null) {
            ObjectAnimator scaleYbar1 = ObjectAnimator.ofFloat(musicBar1, "scaleY", 0.2f, 0.8f, 0.1f, 0.1f, 0.3f, 0.1f, 0.2f, 0.8f, 0.7f, 0.2f, 0.4f, 0.9f, 0.7f, 0.6f, 0.1f, 0.3f, 0.1f, 0.4f, 0.1f, 0.8f, 0.7f, 0.9f, 0.5f, 0.6f, 0.3f, 0.1f);
            scaleYbar1.setRepeatCount(ValueAnimator.INFINITE);
            ObjectAnimator scaleYbar2 = ObjectAnimator.ofFloat(musicBar2, "scaleY", 0.2f, 0.5f, 1.0f, 0.5f, 0.3f, 0.1f, 0.2f, 0.3f, 0.5f, 0.1f, 0.6f, 0.5f, 0.3f, 0.7f, 0.8f, 0.9f, 0.3f, 0.1f, 0.5f, 0.3f, 0.6f, 1.0f, 0.6f, 0.7f, 0.4f, 0.1f);
            scaleYbar2.setRepeatCount(ValueAnimator.INFINITE);
            ObjectAnimator scaleYbar3 = ObjectAnimator.ofFloat(musicBar3, "scaleY", 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.5f, 0.6f, 0.7f, 0.2f, 0.3f, 0.1f, 0.5f, 0.4f, 0.6f, 0.7f, 0.1f, 0.4f, 0.3f, 0.1f, 0.4f, 0.3f, 0.7f);
            scaleYbar3.setRepeatCount(ValueAnimator.INFINITE);
            ObjectAnimator scaleYbar4 = ObjectAnimator.ofFloat(musicBar4, "scaleY", 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.5f, 0.6f, 0.7f, 0.2f, 0.3f, 0.1f, 0.5f, 0.4f, 0.6f, 0.7f, 0.1f, 0.4f, 0.3f, 0.1f, 0.4f, 0.3f, 0.7f);
            scaleYbar4.setRepeatCount(ValueAnimator.INFINITE);
            ObjectAnimator scaleYbar5 = ObjectAnimator.ofFloat(musicBar5, "scaleY", 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.6f, 0.5f, 1.0f, 0.5f, 0.6f, 0.7f, 0.2f, 0.3f, 0.1f, 0.5f, 0.4f, 0.6f, 0.7f, 0.1f, 0.4f, 0.3f, 0.1f, 0.4f, 0.3f, 0.7f);
            scaleYbar5.setRepeatCount(ValueAnimator.INFINITE);

            playingSet = new AnimatorSet();
            playingSet.playTogether(scaleYbar2,scaleYbar4, scaleYbar3, scaleYbar5, scaleYbar1);
            playingSet.setDuration(duration);
            playingSet.setInterpolator(new LinearInterpolator());
            playingSet.start();

        } else if (Build.VERSION.SDK_INT < 22) {
            if (!playingSet.isStarted()) {
                playingSet.start();
            }
        } else {
            if (playingSet.isPaused()) {
                playingSet.resume();
            }
        }

    }

    public void stopBars() {
        animating = false;
        if (playingSet != null && playingSet.isRunning() && playingSet.isStarted()) {
            if (Build.VERSION.SDK_INT < 21) {
                playingSet.end();
            } else {
                playingSet.pause();
            }
        }

        if (stopSet == null) {
            // Animate stopping bars
            ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(musicBar1, "scaleY", 0.1f);
            ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(musicBar2, "scaleY", 0.1f);
            ObjectAnimator scaleY3 = ObjectAnimator.ofFloat(musicBar3, "scaleY", 0.1f);
            ObjectAnimator scaleY4 = ObjectAnimator.ofFloat(musicBar4, "scaleY", 0.1f);
            ObjectAnimator scaleY5 = ObjectAnimator.ofFloat(musicBar5, "scaleY", 0.1f);
            stopSet = new AnimatorSet();
            stopSet.playTogether(scaleY3,scaleY4, scaleY2, scaleY1,scaleY5);
            stopSet.setDuration(200);
            stopSet.start();
        } else if (!stopSet.isStarted()) {
            stopSet.start();
        }
    }

    public Boolean isAnimating() {
        return animating;
    }
}