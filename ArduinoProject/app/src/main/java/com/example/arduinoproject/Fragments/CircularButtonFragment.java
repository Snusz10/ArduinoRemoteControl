/**
 * Description: A fragment that requires to be held for a certain amount of time to be considered "pressed"
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 *
 * =================================================================================================
 */

package com.example.arduinoproject.Fragments;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;

import com.example.arduinoproject.Activites.MainActivity;
import com.example.arduinoproject.Animations.ProgressBarAnimation;
import com.example.arduinoproject.R;

public class CircularButtonFragment extends Fragment {

    private MainActivity mainActivity;
    private View rootView;
    private ProgressBar circularProgressBar;
    private Button circularButton;
    private boolean pressed = false;

    int lightIndigo;
    int brightYellow;
    int indigo;
    int deepGray;
    int midGray;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();

        rootView = inflater.inflate(R.layout.circular_button, container, false);
        circularProgressBar = rootView.findViewById(R.id.circular_progress_bar);
        circularButton = rootView.findViewById(R.id.circular_button);

        initColours();

        circularButton.setOnTouchListener(this::circularButtonHoldCheck);

        return rootView;
    }

    private void initColours(){
        lightIndigo = getResources().getColor(R.color.lightIndigo);
        indigo = getResources().getColor(R.color.indigo);
        deepGray = getResources().getColor(R.color.deepGray);
        midGray = getResources().getColor(R.color.midGray);
    }

    final Runnable checkLongPress = new Runnable() {
        public void run() {
            long holdTime = System.currentTimeMillis() - timeOfPress;
            if (!cancelled // if the button is still being held
                    && (holdTime > millisecondsToHoldFor-millisecondsToHoldFor*0.1) /* if it was held from start to finish */) {
                circularButtonPressed();
            }
        }
    };

    private void changeButtonColor(int color){
        Drawable background = circularButton.getBackground();
        if (background instanceof GradientDrawable){
            GradientDrawable gradientBackground = (GradientDrawable) background;
            gradientBackground.setColor(color);
        }
    }

    private int millisecondsToHoldFor = 2000;
    private boolean cancelled = false;
    private long timeOfPress;
    private boolean circularButtonHoldCheck(View view, MotionEvent event) {
        // if the button has already been pressed, do nothing
        if (pressed){
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                buttonColorLockout = true;
                // start the animation of the progress bar filling
                changeButtonColor(indigo);
                fillProgressBar();
                timeOfPress = System.currentTimeMillis();

                //start timer, if the progress bar fills and the user is still holding the button
                // then call circular button pressed
                cancelled = false;
                view.postDelayed(checkLongPress, millisecondsToHoldFor);
                return true;
            case MotionEvent.ACTION_UP:
                buttonColorLockout = false;

                if (!pressed){
                    changeButtonColor(lightIndigo);
                    emptyProgressBar();
                    cancelled = true;
                }
                return true;
        }
        return false;
    }

    @SuppressLint("NewApi")
    private void circularButtonPressed(){
        pressed = true;
        changeButtonColor(deepGray);
        circularButton.setEnabled(false);
        mainActivity.circularButtonPressed(this);
    }

    private void fillProgressBar(){
        ProgressBarAnimation anim = new ProgressBarAnimation(circularProgressBar, 0, 100);
        anim.setDuration(millisecondsToHoldFor);
        circularProgressBar.startAnimation(anim);
    }

    private void emptyProgressBar(){
        circularButton.setBackgroundResource(R.drawable.round_button);
        // create an animation to overwrite the old one (it fills no space)
        ProgressBarAnimation anim = new ProgressBarAnimation(circularProgressBar, 0, 0);
        anim.setDuration(0);
        circularProgressBar.startAnimation(anim);
        circularProgressBar.setProgress(0);
    }

    public void setButtonText(String text){
        circularButton.setText(text);
    }

    private boolean buttonColorLockout = false;
    private boolean notAllowedToEnable = false;
    public void setEnabled(boolean enabled){
        if (notAllowedToEnable){
            circularButton.setEnabled(false);
            if (!buttonColorLockout){
                changeButtonColor(deepGray);
            }
            return;
        }
        circularButton.setEnabled(enabled);
        // only change the colour if the button is not being pressed
        if (!buttonColorLockout){
            if (enabled){
                changeButtonColor(lightIndigo);
            }else{
                changeButtonColor(midGray);
            }
        }
    }

    public void resetButton(){
        changeButtonColor(lightIndigo);
        emptyProgressBar();
        cancelled = true;
        circularButton.setEnabled(true);
        pressed = false;
    }

    public void disallowEnabling(){
        notAllowedToEnable = true;
    }
    public void allowEnabling(){
        notAllowedToEnable = false;
    }
}
