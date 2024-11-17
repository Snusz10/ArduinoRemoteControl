/**
 * Description: A fragment that will show a pressure reading on a speedometer-like gauge
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 *
 * =================================================================================================
 */

package com.example.arduinoproject.Fragments;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.arduinoproject.R;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.components.Section;
import com.github.anastr.speedviewlib.components.Style;

import java.util.ArrayList;
import java.util.List;

public class PressureGaugeFragment extends Fragment {

    View rootView;
    SpeedView speedView;

    int black;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.pressure_gauge, container, false);
        speedView = rootView.findViewById(R.id.speedView);
        initColours();

        return rootView;
    }

    @Override
    public void onResume() {
        initSpeedView();
        super.onResume();
    }

    private void initColours(){
        black = getResources().getColor(R.color.black);
    }

    private void initSpeedView(){
        float widthOfFrame = ((float)rootView.getWidth());
        speedView.setCenterCircleRadius(widthOfFrame*0.03f);

        float speedometerWidth = widthOfFrame*0.1f;
        speedView.setSpeedometerWidth(speedometerWidth);
        speedView.setTickPadding(speedometerWidth);
    }


    public void updateSections(float firstSection, float secondSection){
        speedView.clearSections();
        speedView.addSections(
                new Section(0, firstSection, Color.RED, speedView.getSpeedometerWidth(), Style.BUTT),
                new Section(firstSection, secondSection, Color.YELLOW, speedView.getSpeedometerWidth(), Style.BUTT),
                new Section(secondSection, 1.0f, Color.GREEN, speedView.getSpeedometerWidth(), Style.BUTT)
        );
    }

    public void changeReading(float reading){
        speedView.speedTo(reading);
    }



}
