/**
 * Description: A way to run some code as the activity closes. This is used to stop all arduino
 * instructions no matter how the user closes the application
 *
 * Changelog:
 * Version || Author || Date        || Comment
 * =================================================================================================
 * 1.0.0   || Mathew || 2024-11-16  || Initial Creation
 * =================================================================================================
 */

package com.example.arduinoproject.Activites;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ExitActivity extends Activity
{
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        finishAndRemoveTask();
    }

    public static void exitApplication(Context context)
    {
        Intent intent = new Intent(context, ExitActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }
}