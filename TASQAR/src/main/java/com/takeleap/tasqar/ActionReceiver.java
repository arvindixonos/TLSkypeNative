package com.takeleap.tasqar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ActionReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d("UI_TEST", "IntentReceived");

        String action = intent.getStringExtra("Action");

        if(action.equals("CancelDownload"))
        {
            PerformAction();
            VideoChatActivity.ShowToast("File Transfer Cancelled", context);
        }

        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(it);
    }

    void PerformAction ()
    {
        VideoChatActivity.getInstance().SendMessage(":FUC-");
        Log.d("UI_TEST", "Perform Action");
        VideoChatActivity.getInstance().StopFTP();
        VideoChatActivity.getInstance().uiHandler.StopNotification();
    }
}