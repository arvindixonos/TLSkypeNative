package com.takeleap.tasqar;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class FileButtonHelper
{
    private DatabaseHelper dbHelper;
    private Activity        currentActivity;
    private static  String  TAG = "UI_TEST";
    private LinearLayout    parent;

    public FileButtonHelper (Activity appActivity, LinearLayout parentView)
    {
        dbHelper = new DatabaseHelper(appActivity);
        currentActivity = appActivity;
        parent = parentView;
    }

    private void RefreshView()
    {
        parent.removeAllViews();
    }

    public void AddData (String fileName, String filePath, String state, String timeStamp)
    {
        dbHelper.addData(fileName, filePath, state, timeStamp);
    }

    public void GetData ()
    {
        RefreshView();

        Cursor data = dbHelper.showData();
        data.moveToFirst();

        for(int i = 0; i < data.getCount(); i++)
        {
            String buttonName = data.getString(1);
            String buttonID = data.getString(0);
            String buttonPath = data.getString(2);
            String buttonState = data.getString(3);
            String buttonDate = data.getString(4);
            Log.d(TAG, "Button Name: " + buttonName + " Button path : " + buttonPath + " Button State " + buttonState);
            SpawnButton(buttonName, buttonID, buttonPath, buttonState, buttonDate);
            data.moveToNext();
        }
    }

    private void SpawnButton (String buttonName, String buttonID, String buttonPath, String buttonState, String buttonDate)
    {
        ViewGroup view = (ViewGroup) LayoutInflater.from(currentActivity).inflate(R.layout.button_spawn, null);
        parent.addView(view);

        FileButton button = (FileButton) view.getChildAt(0);
        button.setId(Integer.parseInt(buttonID));
        button.SetValues(buttonName, buttonPath, buttonState, buttonDate, currentActivity);
    }
}

class FileButton  extends AppCompatImageButton implements View.OnClickListener
{
    private         String  filePath;
    private         String  fileName;
    private         String  fileState;
    private static  String  TAG = "UI_TEST";
    private Activity    currentActivity;
    private VideoChatActivity chatActivity;
    private String fileDate;

    public FileButton (Activity appActivity)
    {
        super(appActivity, null);
    }

    public FileButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public FileButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void onClick(View v)
    {

    }

    void SetValues (String buttonName, String buttonFilePath, String buttonState,String buttonDate, Activity appActivity)
    {

        currentActivity = appActivity;
        chatActivity = (VideoChatActivity) currentActivity;

        fileName = buttonName;
        filePath = buttonFilePath;
        fileState = buttonState;
        fileDate = buttonDate;

        ViewGroup viewGroup = (ViewGroup) this.getParent();
        TextView text = (TextView) viewGroup.getChildAt(2);
        text.setText(fileName);
        TextView dateText = (TextView) viewGroup.getChildAt(3);
        dateText.setText(buttonDate);
        TextView fileType = (TextView) viewGroup.getChildAt(4);
        String[] file = fileName.split("\\.");
        String type = file[file.length - 1].toUpperCase();
        fileType.setText(type);
        fileType.setBackgroundTintList(currentActivity.getResources().getColorStateList(GetColor(type)));

        ImageView fileState = (ImageView) viewGroup.getChildAt(1);

        if(buttonState.equals("SENT"))
        {
            fileState.setImageResource(R.drawable.sent);
            fileState.setColorFilter(Color.GREEN);
        }
        else
        {
            fileState.setImageResource(R.drawable.recieved);
            fileState.setColorFilter(Color.BLUE);
        }
    }

    int GetColor (String text)
    {
        int id = 0;
        switch(text)
        {
            case "MP4":
                id = R.color.MP4;
                break;
            case "PDF":
                id = R.color.PDF;
                break;
            case "JPEG":
                id = R.color.JPEG;
                break;
            case "JPG":
                id = R.color.JPEG;
                break;
            case "DOCX":
            case "DOC":
            case "DOT":
            case "WBK":
            case "DOCM":
            case "DOTX":
            case "DOTM":
            case "DOCB":
                id = R.color.WORD;
                break;
            case "XLSX":
            case "XLSM":
            case "XLTX":
            case "XLTM":
            case "XLSB":
            case "XLA":
            case "XLAM":
            case "XLL":
            case "XLW":
                id = R.color.EXCEL;
                break;
            case "PPT":
            case "POT":
            case "PPS":
            case "PPTX":
            case "PPTM":
            case "POTX":
            case "POTM":
            case "PPAM":
            case "PPSX":
            case "PPSM":
            case "SLDX":
            case "SLDM":
                id = R.color.PPT;
                break;
            case "TXT":
                id = R.color.TXT;
                break;
            default:
                id = R.color.DEFAULT;
                break;
        }
        return id;
    }

    void ClickFunction ()
    {
        Log.d(TAG, "This Works thanks");
        OpenFile(filePath);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_UP:
                ClickFunction();
                break;
        }
        return super.onTouchEvent(event);
    }

    public void OpenFile(String filePath) {
        Log.d(TAG, filePath);
        chatActivity.uiHandler.Minimise();
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String fileExt = MimeTypeMap.getFileExtensionFromUrl(filePath);
        Uri fileProviderURI = FileProvider.getUriForFile(chatActivity, BuildConfig.APPLICATION_ID + ".provider",
                new File(filePath));

        newIntent.setDataAndType(fileProviderURI, MimeTypes.getMimeType(fileExt));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            chatActivity.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            VideoChatActivity.ShowToast("No handler for this type of file.", chatActivity.getApplicationContext());
        }
    }
}
