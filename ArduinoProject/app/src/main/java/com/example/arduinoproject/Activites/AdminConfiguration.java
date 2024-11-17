/**
 * Description: This dialog is held within the app as an easter egg. To access it, simply long press
 * the pressure gauge on the left (for about 10 seconds). This dialog will open. It contains information
 * like the version of the program, and it contains settings like what the scale factor of the
 * pressure transducers should be set to. More easter eggs can be added as needed
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 * =================================================================================================
 */

package com.example.arduinoproject.Activites;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.arduinoproject.R;


public class AdminConfiguration extends AppCompatActivity {

    Button closeButton;

    private boolean frameOpen = true;

    WebView webView;

    static public Float scaleFactor = 1.0f;


    static public Float batteryVoltage100 = null;
    static public Float batteryVoltage80 = null;
    static public Float batteryVoltage60 = null;
    static public Float batteryVoltage40 = null;
    static public Float batteryVoltage20 = null;

    static public boolean bleedButtonOverride = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pressure_configuration);

        webView = findViewById(R.id.pressure_configuration_webview_hidden);

        TextView versionTextView = findViewById(R.id.pressure_configuration_version);
        versionTextView.setText("Version: " + MainActivity.version);

        EditText editText = findViewById(R.id.pressure_configuration_scale_factor);
        editText.setText(scaleFactor.toString());

        EditText voltage100EditText = findViewById(R.id.pressure_configuration_voltage_100);
        voltage100EditText.setText(batteryVoltage100 == null ? null : batteryVoltage100.toString());
        EditText voltage80EditText = findViewById(R.id.pressure_configuration_voltage_80);
        voltage80EditText.setText(batteryVoltage80 == null ? null : batteryVoltage80.toString());
        EditText voltage60EditText = findViewById(R.id.pressure_configuration_voltage_60);
        voltage60EditText.setText(batteryVoltage60 == null ? null : batteryVoltage60.toString());
        EditText voltage40EditText = findViewById(R.id.pressure_configuration_voltage_40);
        voltage40EditText.setText(batteryVoltage40 == null ? null : batteryVoltage40.toString());
        EditText voltage20EditText = findViewById(R.id.pressure_configuration_voltage_20);
        voltage20EditText.setText(batteryVoltage20 == null ? null : batteryVoltage20.toString());

        CheckBox bleedButtonCheckbox = findViewById(R.id.bleedButtonOverrideCheckbox);
        bleedButtonCheckbox.setChecked(bleedButtonOverride);

        TextView batteryVoltageTextView = findViewById(R.id.pressure_configuration_current_voltage);
        batteryVoltageTextView.setText("Current Voltage: " + MainActivity.batteryVoltage.toString());

        textWatcher(editText, "scaleFactor");
        textWatcher(voltage100EditText, "battery100");
        textWatcher(voltage80EditText, "battery80");
        textWatcher(voltage60EditText, "battery60");
        textWatcher(voltage40EditText, "battery40");
        textWatcher(voltage20EditText, "battery20");


        bleedButtonCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleedButtonOverride = bleedButtonCheckbox.isChecked();
                SharedPreferences pref = getApplicationContext().getSharedPreferences("bleedButton", MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("bleedButton", bleedButtonOverride);
                editor.commit();
            }
        });

        closeButton = (Button) findViewById(R.id.pressure_configuration_dialog_close_button);
        closeButton.setOnClickListener(this::closeButtonPressed);
    }

    private void textWatcher(EditText localEditText, String name){
        localEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try{
                    Float value = Float.parseFloat(String.valueOf(localEditText.getText()));
                    switch(name){
                        case "scaleFactor":
                            scaleFactor = value;
                            break;
                        case "battery100":
                            batteryVoltage100 = value;
                            break;
                        case "battery80":
                            batteryVoltage80 = value;
                            break;
                        case "battery60":
                            batteryVoltage60 = value;
                            break;
                        case "battery40":
                            batteryVoltage40 = value;
                            break;
                        case "battery20":
                            batteryVoltage20 = value;
                            break;
                    }
                    SharedPreferences pref = getApplicationContext().getSharedPreferences(name, MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putFloat(name, value);
                    editor.commit();
                }catch(Exception ex){
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void closeButtonPressed(View view) {
        frameOpen = false;
        finish();
    }
}
