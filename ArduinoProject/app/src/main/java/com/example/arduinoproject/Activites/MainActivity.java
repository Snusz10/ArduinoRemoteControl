/**
 * Description: The main screen of the application. This is where the application boots up to, and
 * where the user will take any actions
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 * =================================================================================================
 */

package com.example.arduinoproject.Activites;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.arduinoproject.Fragments.BatteryView;
import com.example.arduinoproject.Fragments.CircularButtonFragment;
import com.example.arduinoproject.Fragments.PressureGaugeFragment;
import com.example.arduinoproject.Fragments.StatusTextFragment;
import com.example.arduinoproject.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    public static String version = "1.0.0";

    // get all of the status text fragments and name them
    FragmentManager fm;
    StatusTextFragment pinStatusTextView;
    StatusTextFragment connectedStatusTextView;
    StatusTextFragment ballStatusTextView;
    StatusTextFragment wiperStatusTextView;
    FragmentContainerView wiperStatusWholeFragment;

    // get the circular button fragments and name them
    CircularButtonFragment pinCircularButton;
    CircularButtonFragment ballCircularButton;
    CircularButtonFragment shutDownCircularButton;

    // get the pressure gauge fragments and name them
    PressureGaugeFragment bottlePressureGauge;
    PressureGaugeFragment backSidePressureGauge;

    // this object allows me to open urls without showing them on screen
    WebView webView;

    // get the buttons later since they cause issues if trying to get them now
    Button chargeButton;
    Button bleedButton;

    // the battery view and layout
    BatteryView batteryView;
    LinearLayout batteryLayout;

    // update the wifi connection status whenever the phone switches or looses a wifi connection
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION .equals(action)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (SupplicantState.isValidState(state)
                        && state == SupplicantState.COMPLETED) {
                }
            }
        }
    };

    private Boolean connectedToArduino = false; // if the program is connected to the arduino or not
    public static final int backgroundImportInterval = 250; // the period of time the program takes in between sending signals
    private final int timeToDelayOverride = 10000; // the amount in milliseconds before the "override" text appears on a pin or ball status
    private Float backSidePressure = null; // the most up to date pressure reading recieved from the arduino
    public static Float bottlePressure = null; // the most up to date pressure reading recieved from the arduino
    public static Float batteryVoltage = null; // the most up to date voltage reading recieved from the arduino's battery
    private final Float backSideTolerance = 80.0f; // the PSI that the backside pressure needs to be under before the release pin button lights up

    private boolean allowedToPing = true; // if the application is allowed to send signals to the arduino

    // all of the battery boolean flags that will be used to set the battery life indicator
    private boolean batteryStatus100 = false;
    private boolean batteryStatus80 = false;
    private boolean batteryStatus60 = false;
    private boolean batteryStatus40 = false;
    private boolean batteryStatus20 = false;

    // initialize any colours necessary
    int deepRed;
    int lightRed;
    int deepGreen;
    int lightGreen;
    int deepIndigo;
    int lightIndigo;
    int midIndigo;
    int darkIndigo;
    int deepGray;
    int darkGray;
    int white;
    int black;
    int midGray;
    int cherryRed;
    int deepPurple;
    int paleRed;
    int gray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSavedData();

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        batteryView = new BatteryView(getApplicationContext());

        batteryLayout = findViewById(R.id.batteryLayout);
        batteryLayout.addView(batteryView);
        batteryLayout.setVisibility(View.GONE);

        // request all of the permissions that are in the android manifest file here
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT},
                PackageManager.PERMISSION_GRANTED);

    }

    private Float getSavedFloat(String name){
        SharedPreferences pref = getApplicationContext().getSharedPreferences(name, MODE_PRIVATE);
        if (pref == null){
            return null;
        }
        Float value = pref.getFloat(name, -1.0f);
        if (value == -1.0f){
            return null;
        }
        return value;
    }

    private void getSavedData(){
        AdminConfiguration.scaleFactor = getSavedFloat("scaleFactor");
        AdminConfiguration.batteryVoltage100 = getSavedFloat("battery100");
        AdminConfiguration.batteryVoltage80 = getSavedFloat("battery80");
        AdminConfiguration.batteryVoltage60 = getSavedFloat("battery60");
        AdminConfiguration.batteryVoltage40 = getSavedFloat("battery40");
        AdminConfiguration.batteryVoltage20 = getSavedFloat("battery20");

        SharedPreferences prefBleedButtonOverride = getApplicationContext().getSharedPreferences("bleedButton", MODE_PRIVATE);
        AdminConfiguration.bleedButtonOverride = prefBleedButtonOverride.getBoolean("bleedButton", AdminConfiguration.bleedButtonOverride);
    }

    @Override
    protected void onPause(){
        // disallow any signals from being sent
        allowedToPing = false;
        // reset the bleed signal to be false, and the charge signal to be false
        sendSignal("charge", "false");
        sendSignal("bleed", "false");

        super.onPause();
    }

    private boolean createdBackgroundCheck = false;
    private boolean createdBackgroundCheck2 = false;
    // initialize anything that is necessary here
    @Override
    protected void onResume(){
        chargeButton = findViewById(R.id.chargeButton);
        bleedButton = findViewById(R.id.bleedButton);
        wiperStatusWholeFragment = findViewById(R.id.wiperStatusTextView);
        webView = findViewById(R.id.web_view_hidden);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

                if(url.toLowerCase().contains("/favicon.ico")) {
                    try {
                        return new WebResourceResponse("image/png", null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }
            @Override
            @SuppressLint("NewApi")
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

                if(!request.isForMainFrame() && request.getUrl().getPath().endsWith("/favicon.ico")) {
                    try {
                        return new WebResourceResponse("image/png", null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }
        });

        initColours();
        initComponentColours();
        initFragments();
        initStatusTexts();
        initCircularButtons();
        initPressureGauges();
        registerActionListeners();

        if (!createdBackgroundCheck){
            createBackgroundImportCheck();
            createdBackgroundCheck = true;

            // the code in this loop only executes on startup, so we can send default signals here
            // whenever the application is booted up, so lets do that
            sendDefaultSignals();
        }

        // allow signals to be sent again (they may have been disabled from onPause)
        allowedToPing = true;

        super.onResume();
    }

    private void sendDefaultSignals(){
        sendSignal("charge", "false");
        sendSignal("bleed", "false");
        sendSignal("sendPin", "false");
        sendSignal("sendBall", "false");
    }

    private void initPressureGauges(){
        // bottle pressure should be:   0-200 red;  200-300 yellow; 300-1000 green
        bottlePressureGauge.updateSections(0.2f, 0.3f);
        // backside:    0-100 red;  100-250 yellow; 250 - 1000 green
        backSidePressureGauge.updateSections(0.1f, 0.25f);
    }

    private void initComponentColours(){
        bleedButton.setBackgroundColor(deepGray);
        chargeButton.setBackgroundColor(deepGray);
    }

    private void initColours(){
        deepRed = getResources().getColor(R.color.deepRed);
        lightRed = getResources().getColor(R.color.lightRed);
        paleRed = getResources().getColor(R.color.paleRed);
        deepGreen = getResources().getColor(R.color.deepGreen);
        lightGreen = getResources().getColor(R.color.lightGreen);
        deepIndigo = getResources().getColor(R.color.deepIndigo);
        lightIndigo = getResources().getColor(R.color.lightIndigo);
        midIndigo = getResources().getColor(R.color.midIndigo);
        darkIndigo = getResources().getColor(R.color.darkIndigo);
        deepGray = getResources().getColor(R.color.deepGray);
        darkGray = getResources().getColor(R.color.darkGray);
        white = getResources().getColor(R.color.white);
        black = getResources().getColor(R.color.black);
        midGray = getResources().getColor(R.color.midGray);
        cherryRed = getResources().getColor(R.color.cherryRed);
        deepPurple = getResources().getColor(R.color.deepPurple);
        gray = getResources().getColor(R.color.gray);
    }

    private void initFragments(){
        // get all of the status text fragments and name them
        fm = getSupportFragmentManager();
        pinStatusTextView = (StatusTextFragment) fm.findFragmentById(R.id.pinStatusTextView);
        connectedStatusTextView = (StatusTextFragment) fm.findFragmentById(R.id.connectedStatusTextView);
        ballStatusTextView = (StatusTextFragment) fm.findFragmentById(R.id.ballStatusTextView);
        wiperStatusTextView = (StatusTextFragment) fm.findFragmentById(R.id.wiperStatusTextView);

        // get the circular button fragments and name them
        pinCircularButton = (CircularButtonFragment) fm.findFragmentById(R.id.pinCircularButton);
        ballCircularButton = (CircularButtonFragment) fm.findFragmentById(R.id.ballCircularButton);
        shutDownCircularButton = (CircularButtonFragment) fm.findFragmentById(R.id.shutDownCircularButton);

        // get the pressure gauge fragments and name them
        bottlePressureGauge = (PressureGaugeFragment) fm.findFragmentById(R.id.bottlePressureGaugeView);
        backSidePressureGauge = (PressureGaugeFragment) fm.findFragmentById(R.id.backSidePressureGaugeView);
    }

    private int millisecondsToHoldFor = 5000;
    private boolean cancelled = false;
    private long timeOfPress;
    View thisView;
    private boolean buttonHoldCheck(View view, MotionEvent event) {
        thisView = view;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                timeOfPress = System.currentTimeMillis();

                //start timer, if the progress bar fills and the user is still holding the button
                // then call circular button pressed
                cancelled = false;
                view.postDelayed(checkLongPress, millisecondsToHoldFor);
                return true;
        }
        return false;
    }

    final Runnable checkLongPress = new Runnable() {
        public void run() {
            long holdTime = System.currentTimeMillis() - timeOfPress;
            if (!cancelled // if the button is still being held
                    && (holdTime > millisecondsToHoldFor-millisecondsToHoldFor*0.1) /* if it was held from start to finish */) {
                bottlePressureGaugePressed(thisView);
            }
        }
    };

    private void registerActionListeners(){
        chargeButton.setOnTouchListener(this::chargeButtonPressed);
        bleedButton.setOnTouchListener(this::bleedButtonPressed);

        connectedStatusTextView.getView().setOnClickListener(this::connectedStatusTextViewPressed);
        bottlePressureGauge.getView().setOnTouchListener(this::buttonHoldCheck);

        // connectedStatusTextViewActions set through xml code
    }

    private boolean bleedButtonLockout = false;
    private boolean bleedButtonPressedOnce = false;
    private boolean bleedButtonPressed(View view, MotionEvent event) {
        if (chargeButton.isPressed() || bleedButtonLockout){
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                bleedButton.setBackgroundColor(darkGray);
                sendSignal("bleed", "true");
                bleedButtonPressedOnce = true;
                return true;
            case MotionEvent.ACTION_UP:
                bleedButton.setBackgroundColor(deepGray);
                return true;
        }
        return false;
    }

    private boolean chargeButtonLockout = false;
    private boolean chargeButtonPressed(View view, MotionEvent event) {
        if (bleedButton.isPressed() || chargeButtonLockout){
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                chargeButton.setBackgroundColor(darkGray);
                sendSignal("bleed", "false");
                sendSignal("charge", "true");
                return true;
            case MotionEvent.ACTION_UP:
                chargeButton.setBackgroundColor(deepGray);
                sendSignal("charge", "false");
                return true;
        }
        return false;
    }


    private boolean checkCorrectWifi(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ssid  = info.getSSID();
        return ssid.contains("CMT Core");
    }

    private void bottlePressureGaugePressed (View view){
        Intent intent = new Intent(MainActivity.this, AdminConfiguration.class);
        startActivity(intent);
    }

    private void connectedStatusTextViewPressed (View view){
        // check to see if the phone is on the wifi that the esp is outputting, if it isn't
        // tell the user they must connect to it
        if (!checkCorrectWifi()){
            Intent intent = new Intent(MainActivity.this, CorrectWifiDialog.class);
            startActivity(intent);

            return;
        }else{
            if (!createdBackgroundCheck2){
                webView.postDelayed(updateBackgroundVisuals, backgroundImportInterval);

                createdBackgroundCheck2 = true;
            }
        }
        // in any case, make sure that the field is updated to its most recent value, just in case
        // the background check is not updating it properly
        updateConnectedStatusText();
    }

    final Runnable updateBackgroundVisuals = new Runnable(){
        public void run(){
            updateConnectedStatusTextBackgroundThread();
            // recurse so that this function is called indefinetly
            webView.postDelayed(updateBackgroundVisuals, backgroundImportInterval);
        }
    };
    private void updateConnectedStatusTextBackgroundThread(){
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                updateConnectedStatusText();
            }
        }));
    }

    // this conversion factor will be used to change the voltage into a pressure reading, it is a rough ESTIMATE
    private Float conversionFactor = 1223f; // scales up the voltage reading to a pressure reading
    private final Float correctionFactor = 0.27f; // the pressure transducers give off a voltage of 0.27 when they read 0 psi
    private Float parsePressure(String info){
        Float myNum = Float.parseFloat(info);
        myNum = myNum * 1.4f; // corrects the voltage given by the arduino to its actual value

        myNum = myNum - correctionFactor;

        if (myNum < 0){
            myNum = 0.0f;
        }

        myNum = myNum * AdminConfiguration.scaleFactor;

        return (myNum) * conversionFactor;
    }

    private int incomingMessageNumber = -1;
    private String prevWiperState = "0";
    private String prevPinInState = "0";
    private String prevPinOutState = "1";
    private String prevBallHomeState = "0";
    private String prevBallLaunchedState = "1";
    private void handleMessage(String message){
        String[] individualMessages = message.split("~");
        for (int i = 0; i < individualMessages.length; i++){
            String[] individualMessageParts = individualMessages[i].split("`");
            String description = individualMessageParts[0];
            String info = individualMessageParts[1];

            boolean previousBatteryStatus;

            // handle the messages based on their descriptions
            switch(description){
                // if the message does not differ from the last one, try getting it again
                case "MessageNumber":
                    int parsedInfo = Integer.parseInt(info);

                    if (parsedInfo == incomingMessageNumber){ // same as the previous message, so dont process it
                        return;
                    }else{
                        incomingMessageNumber = parsedInfo;
                    }
                    break;
                case "BackSide":
                    backSidePressure = parsePressure(info);
                    backSidePressureGauge.changeReading(backSidePressure);
                    updateCircularButtonState();
                    break;
                case "BottlePressure":
                    bottlePressure = parsePressure(info);
                    bottlePressureGauge.changeReading(bottlePressure);
                    break;
                case "BatteryLevel":
                    batteryVoltage = Float.parseFloat(info);
                    updateBatteryGraphic();
                    break;
                case "Connected":
                    connectedToArduino = info.equals("T");
                    break;
                case "WiperStatus":
                    if (info.equals(prevWiperState)){
                        // the status is the same as it was last time, dont do anything
                        break;
                    }
                    prevWiperState = info;
                    if (info.equals("0")){
                        wiperStatusAwayState();
                    }else{
                        wiperStatusHomeState();
                    }
                    break;
                case "PinInStatus":
                    if (info.equals(prevPinInState)){
                        // the status is the same as it was last time, dont do anything
                        break;
                    }
                    prevPinInState = info;
                    if (info.equals("0")){
                        System.out.println("pin in");
                        pinStatusInState();
                    }else{
                        System.out.println("pin no longer in");
                        updatePinStatusAfterDelay();
                    }
                    break;
                case "PinOutStatus":
                    if (info.equals(prevPinOutState)){
                        // the status is the same as it was last time, dont do anything
                        break;
                    }
                    prevPinOutState = info;
                    if (info.equals("0")){
                        System.out.println("pin out");
                        pinStatusOutState();
                    }else{
                        updatePinStatusAfterDelay();
                        System.out.println("pin no longer out");
                    }
                    break;
                case "BallHomeStatus":
                    if (info.equals(prevBallHomeState)){
                        // the status is the same as it was last time, dont do anything
                        break;
                    }
                    prevBallHomeState = info;
                    if (info.equals("0")){
                        ballStatusHomeState();
                    }else{
                        updateBallStatusAfterDelay();
                    }
                    break;
                case "BallLaunchedStatus":
                    if (info.equals(prevBallLaunchedState)){
                        // the status is the same as it was last time, dont do anything
                        break;
                    }
                    prevBallLaunchedState = info;
                    if (info.equals("0")){
                        ballStatusLaunchedState();
                    }else{
                        updateBallStatusAfterDelay();
                    }
                    break;
            }
        }
    }

    private void updateBatteryGraphic(){
        batteryLayout.setVisibility(View.VISIBLE);
        if (AdminConfiguration.batteryVoltage100 != null && batteryVoltage > AdminConfiguration.batteryVoltage100){
            batteryView.setPercent(100);
            return;
        }else if (AdminConfiguration.batteryVoltage80 != null && batteryVoltage > AdminConfiguration.batteryVoltage80){
            batteryView.setPercent(80);
            return;
        }else if (AdminConfiguration.batteryVoltage60 != null && batteryVoltage > AdminConfiguration.batteryVoltage60){
            batteryView.setPercent(60);
            return;
        }else if (AdminConfiguration.batteryVoltage40 != null && batteryVoltage > AdminConfiguration.batteryVoltage40){
            batteryView.setPercent(40);
            return;
        }else if (AdminConfiguration.batteryVoltage20 != null && batteryVoltage > AdminConfiguration.batteryVoltage20){
            batteryView.setPercent(20);
            return;
        }
        batteryLayout.setVisibility(View.GONE);
    }

    protected final Runnable getMessageResponse = new Runnable(){
            @SuppressLint("JavascriptInterface")
            public void run(){
                pingSignal();
                // allow the webpage to be read from
                webView.getSettings().setJavaScriptEnabled(true);
                // try reading from it
                webView.evaluateJavascript(
                        "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) {
                                String[] split = html.split("Info Tag");
                                // if there was a response given by the arduino
                                if (split.length > 1){
                                    // removing all of the junk before and after my information by grabbing split[1]
                                    handleMessage(split[1]);
                                }
                            }
                        });
                // recurse so that this function is called indefinetly
                webView.postDelayed(getMessageResponse, backgroundImportInterval);
            }
    };

    // try to get the message after waiting X milliseconds
    private void createBackgroundImportCheck(){
        // this function has a line of code that will recurse itself every X milliseconds
        webView.postDelayed(getMessageResponse, backgroundImportInterval);
    }

    private void updatePinStatusAfterDelay(){
        Handler handler = new Handler();
        Runnable updateToOverride = new Runnable() {
            @Override
            public void run() {
                // no data is being read in for either pin
                if (prevPinInState.equals("1") && prevPinOutState.equals("1")){
                    pinStatusOverrideState();
                }
            }
        };
        handler.postDelayed(updateToOverride, timeToDelayOverride);
    }

    private void updateBallStatusAfterDelay(){
        Handler handler = new Handler();
        Runnable updateToOverride = new Runnable() {
            @Override
            public void run() {
                // no data is being read in for either ball state
                if (prevBallHomeState.equals("1") && prevBallLaunchedState.equals("1")){
                    ballStatusOverrideState();
                }
            }
        };
        handler.postDelayed(updateToOverride, timeToDelayOverride);
    }

    // the string that is being returned is the response from the arduino
    private LinkedHashMap<String, String> signalsToSend = new LinkedHashMap<>();
    private ArrayList<String> bannedSignals = new ArrayList<>();
    public void sendSignal(String signal, String state){
        if (bannedSignals.contains(signal)){
            return;
        }
        signalsToSend.put(signal, state);
        signalsToSend.replace(signal, state);
    }

    private int signalNumber = 0;
    public void pingSignal(){
        if (!allowedToPing){
            return;
        }
        // send the outgoing message number to the arduino's incoming message number key
        String signalString = "";
        for (Map.Entry<String, String> signal : signalsToSend.entrySet()){
            signalString += "'" + signal.getKey() + "-" + signal.getValue() + "'";
        }

        String stringToSend = "http://192.168.4.1/" + signalNumber++ + signalString;
        //System.out.println("signal = " + signalString);
        webView.loadUrl(stringToSend);
    }

    public void circularButtonPressed(CircularButtonFragment fragment){
        if (fragment.equals(pinCircularButton)) { // pin button pressed
            pinCircularButton.setButtonText("\nPin released!\n\n");
            pinCircularButton.disallowEnabling();
            pinCircularButton.setEnabled(false);

            sendSignal("sendPin", "true");

            // make both of the charge/bleed buttons invisible
            chargeButtonLockout = true;
            chargeButton.setVisibility(View.INVISIBLE);
            bleedButtonLockout = true;
            bleedButton.setVisibility(View.INVISIBLE);

            ballCircularButtonCanBeEnabled = true;
            ballCircularButton.setEnabled(true);

        } else if (fragment.equals(ballCircularButton)) { // ball button pressed

            ballCircularButton.setButtonText("\nBall released!\n\n");
            ballCircularButton.disallowEnabling();
            ballCircularButton.setEnabled(false);

            sendSignal("sendBall", "true");


            getSupportFragmentManager().beginTransaction().show(shutDownCircularButton).commit();
        } else if (fragment.equals(shutDownCircularButton)) { // shut down button pressed
            // open a dialog to inform the user that they are about to shut down the application
            // to save battery. Are they sure they want to continue?
            // if yes, then send the default signals then shut down
            shutDownDialog();
        }
    }

    private static final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    Runnable exitAppAfterDelay = new Runnable(){
        @Override
        public void run(){
            ExitActivity.exitApplication(getApplicationContext());
        }
    };

    @Override
    protected void onDestroy(){
        sendDefaultSignals();
        Runnable destroy = new Runnable() {
            @Override
            public void run() {
                MainActivity.super.onDestroy();
            }
        };
        mainThreadHandler.postDelayed(destroy, 500);
    }

    private void shutDownDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Warning");
        builder.setMessage("You are about to shut down the application completely to save battery, are you sure you want to continue?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                sendDefaultSignals();
                // close this application and kill any processes it may be running
                mainThreadHandler.postDelayed(exitAppAfterDelay, 1000);
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                shutDownCircularButton.resetButton();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void pinStatusInState(){
        pinStatusTextView.changeBackgroundColor(midIndigo, lightIndigo);
        pinStatusTextView.setTextColor(black);
        pinStatusTextView.setText("In");
    }

    private void pinStatusOverrideState(){
        pinStatusTextView.changeBackgroundColor(deepRed, lightRed);
        pinStatusTextView.setTextColor(black);
        pinStatusTextView.setText("Sensor Failed");
    }

    private boolean ballCircularButtonCanBeEnabled = false;
    private void pinStatusOutState(){
        pinStatusTextView.changeBackgroundColor(darkIndigo, deepIndigo);
        pinStatusTextView.setTextColor(midGray);
        pinStatusTextView.setText("Out");
    }

    private void ballStatusHomeState(){
        ballStatusTextView.changeBackgroundColor(midIndigo, lightIndigo);
        ballStatusTextView.setTextColor(black);
        ballStatusTextView.setText("Home");
    }

    private void ballStatusOverrideState(){
        ballStatusTextView.changeBackgroundColor(deepRed, lightRed);
        ballStatusTextView.setTextColor(black);
        ballStatusTextView.setText("Override");
    }

    private void ballStatusLaunchedState(){
        ballStatusTextView.changeBackgroundColor(darkIndigo, deepIndigo);
        ballStatusTextView.setTextColor(midGray);
        ballStatusTextView.setText("Launched");
    }

    private void stopSignal(String signal){
        bannedSignals.add(signal);
        if (signalsToSend.containsKey(signal)){
            signalsToSend.replace(signal, "false");
        }else{
            signalsToSend.put(signal, "false");
        }

    }

    private void allowSignal(String signal){
        bannedSignals.remove(signal);
    }

    private void updateConnectedStatusText() {
        // if the phone is not connected to the correct wifi signal, display it
        if(!checkCorrectWifi()){
            connectedStatusTextView.changeBackgroundColor(deepRed, lightRed);
            connectedStatusTextView.setText("Disconnected");
        }else if (connectedToArduino){ // if the phone is able to send a message to the wifi reciever
            connectedStatusTextView.changeBackgroundColor(deepGreen, lightGreen);
            connectedStatusTextView.setText("Connected");
        }else{ // if the phone might be able to send a message to the wifi reciever
            connectedStatusTextView.changeBackgroundColor(deepIndigo, lightIndigo);
            connectedStatusTextView.setText("Press To Connect");
        }
        updateCircularButtonState();
    }

    
    private void updateCircularButtonState(){
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                // if the backSidePressure is below tolerance (the value is low enough)
                // or if the override is set to true
                // it should enable the buttons
                if (!(backSidePressure == null || backSidePressure >= backSideTolerance)
                    || AdminConfiguration.bleedButtonOverride){
                    pinCircularButton.allowEnabling();
                    ballCircularButton.allowEnabling();
                    pinCircularButton.setEnabled(connectedToArduino && bleedButtonPressedOnce);
                    ballCircularButton.setEnabled(connectedToArduino && ballCircularButtonCanBeEnabled);
                }else{
                    pinCircularButton.disallowEnabling();
                    ballCircularButton.disallowEnabling();
                    pinCircularButton.setEnabled(false);
                    ballCircularButton.setEnabled(false);
                }
            }
        }));
    }

    private void wiperStatusHomeState(){
        wiperStatusWholeFragment.setVisibility(View.GONE);
        allowSignal("sendPin");
        allowSignal("sendBall");
    }

    private void wiperStatusAwayState(){
        wiperStatusWholeFragment.setVisibility(View.VISIBLE);
        wiperStatusTextView.changeBackgroundColor(deepPurple, lightIndigo);
        wiperStatusTextView.setText("Wiper Away");
        stopSignal("sendPin");
        stopSignal("sendBall");
        sendSignal("bleed", "false");
    }

    private void initStatusTexts(){
        pinStatusInState();
        ballStatusHomeState();
        updateConnectedStatusText();
        wiperStatusHomeState();
    }

    private void initCircularButtons(){
        pinCircularButton.setButtonText("Release Pin");
        ballCircularButton.setButtonText("Release Ball");
        getSupportFragmentManager().beginTransaction().hide(shutDownCircularButton).commit();
        shutDownCircularButton.setButtonText("Shut Down");
        pinCircularButton.setEnabled(false);
        ballCircularButton.setEnabled(false);
    }
}