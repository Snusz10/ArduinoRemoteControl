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

public class StatusTextFragment extends Fragment {

    View rootView;
    TextView textView;
    ConstraintLayout background;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.status_text, container, false);
        textView = rootView.findViewById(R.id.status_text_textfield);
        background = rootView.findViewById(R.id.status_text_background);

        return rootView;
    }

    public void setText(String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    public void setTextColor(int color){
        textView.setTextColor(color);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void changeBackgroundColor(int borderColor, int centerColor){
        background.setBackgroundColor(borderColor);
        textView.setBackgroundColor(centerColor);
    }
}
