/**
 * Description: A dialog that will pop up when the user clicks on the "Disconnected"
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 * =================================================================================================
 */

package com.example.arduinoproject.Activites;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arduinoproject.R;


public class CorrectWifiDialog extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correct_wifi_dialog);
        Button closeButton = (Button)findViewById(R.id.correct_wifi_dialog_close_button);
        closeButton.setOnClickListener(this::closeButtonPressed);
    }

    private void closeButtonPressed(View view) {
        finish();
    }
}
