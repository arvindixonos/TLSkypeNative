package com.flashphoner.wcsexample.video_chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.NavigationMenuItemView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;
import com.obsez.android.lib.filechooser.ChooserDialog;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.webrtc.VideoCapturerAndroid;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainUIHandler implements NavigationView.OnNavigationItemSelectedListener, NavigationMenuItemView.OnClickListener
{
    private boolean     drawMode;
    private boolean     switched;
    private boolean     minimisedSwitched;
    private boolean     videoView = false;
    private boolean     backCam = false;
    private boolean     recording;
    private boolean     pointMode = false;
    private boolean     isAboveEight;
    private boolean     timerRunning = false;
    private boolean     flashOn;
    private boolean     drawerOpen = false;
    private Activity    currentActivity;
    private static  String TAG = "UI_TEST";
    private VideoChatActivity chatActivity;
    private Handler     timerHandler;
    private Handler     buttonActivateHandler;
    private Runnable    timerRunnable;
    private long        startTime;
    public CameraTorchMode cameraTorchMode;
    public  boolean      startTransfer = false;
    private ActionBarDrawerToggle mDrawerToggle;
    public boolean      profilePicPresent = false;

    public Camera      camera;
    public FileButtonHelper    fileButtonHelper;

    //Notification Variables
    private NotificationManager notificationManager;
    private NotificationChannel mChannel;
    private NotificationCompat.Builder mBuilder;
    private TaskStackBuilder stackBuilder;
    private PendingIntent resultPendingIntent;
    private int notificationId = 1;
    private int width;
    private String channelId = "channel-01";
    private String channelName = "Channel Name";
    private int importance = NotificationManager.IMPORTANCE_HIGH;
    private String userName = "";
    //Notification Variables

    DrawerLayout    drawerLayout;

    ImageView progessBar;

    ImageButton mSettingsButton;
    ImageButton mUndoButton;

    SurfaceViewRendererCustom remote1Render;
    SurfaceViewRendererCustom localRender;

    CircularImageView   profilePhoto;

    ConstraintLayout mFloatingButtonsLayout;
    ConstraintLayout mHistoryScreen;
    ConstraintLayout mSettingLayout;

    FloatingActionButton mEndCallButton;
    FloatingActionButton mPlusButton;
    FloatingActionButton mSwitchLayoutButton;
    FloatingActionButton mToggleDrawingMode;
    FloatingActionButton mSwitchCamera;
    FloatingActionButton mStartRecordingButton;
    FloatingActionButton mPointToPlaneButton;
    FloatingActionButton mHistoryButton;
    FloatingActionButton mFlashButton;

    RelativeLayout switchLayoutItem;
    RelativeLayout togglePointItem;
    RelativeLayout toggleBackcamItem;
    RelativeLayout toggleArrowMode;

    Switch  switchLayoutButton;
    Switch  togglePointButton;
    Switch  toggleBackcamButton;
    Switch  toggleArrowButton;

    DrawerLayout drawer;

    TextView recordingText;
    TextView pointModeText;
    TextView timerText;
    TextView userNameText;
    TextView arrowText;
    TextView drawText;

    LinearLayout historyScreen;

    Button mButton;
    Button mArrowButton;
    Button mDrawButton;
    Button mHistoryBackButton;
    LinearLayout mSpawnButtonLayout;

    RelativeLayout currentRenderLayout;
    RelativeLayout streamRenderLayout;
    RelativeLayout mRenderHolder;
    RelativeLayout.LayoutParams   fullScreenlayoutParams;
    RelativeLayout.LayoutParams   smallScreenlayoutParams;
    Rational aspectRatio;

    public enum CameraTorchMode
    {
        ON,
        OFF,
        TO_TURN_ON
    }

    @Override
    public void onClick(View v)
    {
        VideoChatActivity.ShowToast("Selected", currentActivity);
    }

    public MainUIHandler (Activity activity, boolean picPresent)
    {
        profilePicPresent = picPresent;
//        WCSAudioManager wcsAudioManager = WCSAudioManager.create(getApplicationContext(), deviceStateChangedListener);
//        deviceStateChangedListener = new Runnable() {
//            @Override
//            public void run()
//            {
//                Log.d(TAG, "device state changed");
//            }
//        };
//        boolean earPiece = false;
//        wcsAudioManager.init();
//        Log.d(TAG, "onCreate");
//        Set<WCSAudioManager.AudioDevice> devices = wcsAudioManager.getAudioDevices();
//        for (WCSAudioManager.AudioDevice aud: devices)
//        {
//            if(aud.name().contains("EARPIECE"))
//            {
//                earPiece = true;
//                Log.d(TAG, "EARpiece detected");
//                wcsAudioManager.setAudioDevice(aud);
//            }
//            Log.d(TAG, aud.name());
//        }
        currentActivity = activity;

        width = GetScreenWidth();

        chatActivity = VideoChatActivity.getInstance();

        progessBar = currentActivity.findViewById(R.id.ProgressBar);

        mSettingsButton = currentActivity.findViewById(R.id.SettingButton);

        remote1Render = currentActivity.findViewById(R.id.StreamRender);
        localRender = currentActivity.findViewById(R.id.CurrentRender);

        mFloatingButtonsLayout = currentActivity.findViewById(R.id.FloatingButtonsLayout);
        mHistoryScreen = currentActivity.findViewById(R.id.FileHistory);
        mSettingLayout = currentActivity.findViewById(R.id.SettingLayout);

        mEndCallButton = currentActivity.findViewById(R.id.EndCallButton);
        mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
        mSwitchLayoutButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
        mToggleDrawingMode = currentActivity.findViewById(R.id.DrawingModeButton);
        mSwitchCamera = currentActivity.findViewById(R.id.SwitchCamButton);
        mStartRecordingButton = currentActivity.findViewById(R.id.StartRecordingButton);
        mPointToPlaneButton = currentActivity.findViewById(R.id.PointToPlaneButton);
        mHistoryButton = currentActivity.findViewById(R.id.HistoryButton);
        mHistoryBackButton = currentActivity.findViewById(R.id.historyBackButton);
//        mFlashButton = currentActivity.findViewById(R.id.FlashButton);

        drawer = currentActivity.findViewById(R.id.drawer_layout);

        recordingText = currentActivity.findViewById(R.id.startRecord);
        pointModeText = currentActivity.findViewById(R.id.Point2Plane);
        timerText = currentActivity.findViewById(R.id.timerText);

        mButton = currentActivity.findViewById(R.id.button);
        mArrowButton = currentActivity.findViewById(R.id.ArrowButton);
        mDrawButton = currentActivity.findViewById(R.id.DrawButton);
        mUndoButton = currentActivity.findViewById(R.id.UndoButton);
        arrowText = currentActivity.findViewById(R.id.ArrowText);
        drawText = currentActivity.findViewById(R.id.DrawText);

        historyScreen = currentActivity.findViewById(R.id.FileHistoryLayout);

        mSpawnButtonLayout = currentActivity.findViewById(R.id.ButtonLayout);

        currentRenderLayout = currentActivity.findViewById(R.id.currentLayout);
        streamRenderLayout = currentActivity.findViewById(R.id.streamLayout);
        mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
        fullScreenlayoutParams = (RelativeLayout.LayoutParams) streamRenderLayout.getLayoutParams();
        smallScreenlayoutParams = (RelativeLayout.LayoutParams) currentRenderLayout.getLayoutParams();

        fileButtonHelper = new FileButtonHelper(currentActivity, historyScreen);

        currentActivity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mUndoButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chatActivity.UndoClicked();
            }
        });

        mSettingsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(mSettingLayout.getVisibility() == View.VISIBLE)
                {
                    mSettingLayout.setVisibility(GONE);
                    mSettingsButton.setImageTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.white)));
                }
                else
                {
                    mSettingLayout.setVisibility(VISIBLE);
                    mSettingsButton.setImageTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.black)));
                }
            }
        });

        mDrawButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chatActivity.arrowMode = !chatActivity.arrowMode;
                AdaptToggleButtons(chatActivity.arrowMode);
            }
        });

        mArrowButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chatActivity.arrowMode = !chatActivity.arrowMode;
                AdaptToggleButtons(chatActivity.arrowMode);
            }
        });

        //Set Photo and Name

        NavigationView headerLayout = currentActivity.findViewById(R.id.nav_view);
        View header = headerLayout.getHeaderView(0);
        if(profilePicPresent)
        {
            profilePhoto = header.findViewById(R.id.ProfilePics);
            profilePhoto.setImageBitmap(GetUsetProfilePhoto());
        }
        Log.d(TAG, "User Name " + userName);
        userNameText = header.findViewById(R.id.ProfileName);
        userNameText.setText(userName);
        //Set Photo and Name

        android.support.v7.app.ActionBarDrawerToggle toggle = new android.support.v7.app.ActionBarDrawerToggle
                (currentActivity, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        {
            public void onDrawerClosed(View view)
            {
                super.onDrawerClosed(view);
                drawerOpen = false;
            }

            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                drawerOpen = true;
//                switchLayoutItem = currentActivity.findViewById(R.id.app_bar_switch);
//                switchLayoutButton = (Switch) switchLayoutItem.getChildAt(0);
//                switchLayoutButton.setOnClickListener(v ->
//                {
//                    TurnOffOnDelay(switchLayoutItem, switchLayoutButton);
//                    mSwitchLayoutButton.callOnClick();
//                });
//                togglePointItem = currentActivity.findViewById(R.id.point2plane);
//                togglePointButton = (Switch) togglePointItem.getChildAt(0);
//                togglePointButton.setOnClickListener(v ->
//                {
//                    TurnOffOnDelay(togglePointItem, togglePointButton, 2000);
//                    mPointToPlaneButton.callOnClick();
//                });
//                toggleArrowMode = currentActivity.findViewById(R.id.arrow_mode);
//                toggleArrowButton = (Switch) toggleArrowMode.getChildAt(0);
//                toggleArrowButton.setOnClickListener(v ->
//                {
//                    TurnOffOnDelay(toggleArrowMode, toggleArrowButton, 2000);
//                    chatActivity.arrowMode = !chatActivity.arrowMode;
//                });
//                toggleBackcamItem = currentActivity.findViewById(R.id.back_cam_switch);
//                toggleBackcamButton = (Switch) toggleBackcamItem.getChildAt(0);
//                toggleBackcamButton.setOnClickListener(v ->
//                {
//                    TurnOffOnDelay(toggleBackcamItem, toggleBackcamButton, 3000);
//                    mSwitchLayoutButton.callOnClick();
//                    mSwitchCamera.callOnClick();
//                });

            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = currentActivity.findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
        {
            mRenderHolder.removeView(streamRenderLayout);
            mRenderHolder.removeView(currentRenderLayout);

            mRenderHolder.addView(streamRenderLayout, 0);
            mRenderHolder.addView(currentRenderLayout, 1);

            isAboveEight = true;
        }
//        mFlashButton.setOnClickListener(new View.OnClickListener()
//        {
//            CameraManager cameraManager = (CameraManager) currentActivity.getSystemService(Context.CAMERA_SERVICE);
//
//            @Override
//            public void onClick(View v)
//            {
//                if(!flashOn)
//                {
//                    try
//                    {
//                        if(backCam)
//                        {
//                            Camera.Parameters parameters = camera.getParameters();
//                            List<String> strs = parameters.getSupportedFlashModes();
//                            for (String str:strs)
//                            {
//                                Log.d(TAG, "Message string is " + str);
//                            }
//                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                            camera.setParameters(parameters);
//                            camera.startPreview();
//
//                        }
//                        else
//                        {
//                            try
//                            {
//                                String cameraId = cameraManager.getCameraIdList()[0];
//                                cameraManager.setTorchMode(cameraId, true);
//                            }
//                            catch(CameraAccessException e)
//                            {
//                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
//                            }
//                        }
//                        mFlashButton.setImageResource(R.drawable.flash_off);
//                        flashOn = true;
//                    }
//                    catch (Exception e)
//                    {
//                        VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
//                    }
//                }
//                else
//                {
//                    try
//                    {
//                        if(backCam)
//                        {
//                            Camera.Parameters parameters = camera.getParameters();
//                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                            camera.setParameters(parameters);
//                            camera.startPreview();
//                        }
//                        else
//                        {
//                            try
//                            {
//                                String cameraId = cameraManager.getCameraIdList()[0];
//                                cameraManager.setTorchMode(cameraId, false);
//                            }
//                            catch(CameraAccessException e)
//                            {
//                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
//                            }
//                        }
//                        mFlashButton.setImageResource(R.drawable.flash_on);
//                        flashOn = false;
//                    }
//                    catch (Exception e)
//                    {
//                        VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
//                    }
//                }
//            }
//        });

        mSwitchCamera.setOnClickListener(v ->
        {
            VideoChatActivity.getInstance().ToggleCamera();

            if(backCam)
            {
                mSwitchCamera.setImageResource(R.drawable.flip_cam_front);
                Log.d(TAG, "Front");
                backCam = false;
                mSwitchLayoutButton.callOnClick();
                TurnOffOnDelay(mSwitchCamera, 3000);
//                if(flashOn)
//                {
//                    cameraTorchMode = CameraTorchMode.TO_TURN_ON;
//                    flashOn = false;
//                }
            }
            else
            {
                mSwitchCamera.setImageResource(R.drawable.flip_cam_rear);
                Log.d(TAG, "Back");
                backCam = true;
                mSwitchLayoutButton.callOnClick();
                TurnOffOnDelay(mSwitchCamera, 3000);
//                if(flashOn)
//                {
//                    cameraTorchMode = CameraTorchMode.TO_TURN_ON;
//                    flashOn = false;
//                }
            }
        });

        mHistoryBackButton.setOnClickListener(v -> backKey());

        mHistoryButton.setOnClickListener(v ->
        {

            mHistoryScreen.setVisibility(VISIBLE);
            mSwitchCamera.setVisibility(GONE);
            mStartRecordingButton.setVisibility(GONE);
            mFloatingButtonsLayout.setVisibility(GONE);
            mEndCallButton.setVisibility(GONE);
            mRenderHolder.setVisibility(GONE);
            mSettingLayout.setVisibility(GONE);
            mSettingsButton.setVisibility(GONE);

            fileButtonHelper.GetData();
        });

        mPlusButton.setOnClickListener(v ->
        {
            if(mSpawnButtonLayout.getVisibility() == VISIBLE)
            {
                mSpawnButtonLayout.setVisibility(GONE);
            }
            else
            {
                mSpawnButtonLayout.setVisibility(VISIBLE);
            }
        });

        mPointToPlaneButton.setOnClickListener(v ->
        {
            if(pointMode)
            {
                chatActivity.TogglePointPlaneSpawn();
                mPointToPlaneButton.setImageResource(R.drawable.botton_plane);
                pointModeText.setText("Switch to Point");
                pointMode = false;
            }
            else
            {
                chatActivity.TogglePointPlaneSpawn();
                mPointToPlaneButton.setImageResource(R.drawable.button_blur);
                pointModeText.setText("Switch to Plane");
                pointMode = true;
            }
        });

        mStartRecordingButton.setOnClickListener(v ->
        {
            if (!recording)
            {
                chatActivity.screenRecorder.GetPermission();
                mStartRecordingButton.setImageResource(R.drawable.button_stop);
                recordingText.setText("Stop Recording");
//                    mStartRecordingButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.redLight)));
                recording = true;
            }
            else
            {
                chatActivity.screenRecorder.StopRecording();
                mStartRecordingButton.setImageResource(R.drawable.button_record);
                recordingText.setText("Start Recording");
//                    mStartRecordingButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
                recording = false;
            }
        });

        mEndCallButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ToggleVideoView();
                chatActivity.CleanUp();
                chatActivity.SendMessage("CTRL:-DC");
                chatActivity.Disconnect();
                ChangeActivity();
            }
        });

        mButton.setOnClickListener(v ->
        {
            char[] charray = new char[] {'a', 'd', 'i', 's', 'h'};
            Log.d(TAG, charray.toString());
            chatActivity.SendMessage("Something");
//                count += 10;
//                SetProgress(count);
//                showNotification(currentActivity, "Download", "filepath", new Intent());
//                DisplayNotification();
//                if(count == 0)
//                    showNotification(currentActivity, "Title", "This is download", new Intent());
//                else
//                    UpdateNotification(count);
//
//                count += 10;
        });

        mToggleDrawingMode.setOnClickListener(v ->
        {
            if(!drawMode)
            {
                Log.d(TAG, "clicking");
                drawMode = true;
//                    remote1Render.touchEnabled = true;
                remote1Render.drawEnabled = true;
                mToggleDrawingMode.setImageResource(R.drawable.baseline_gesture_black_18dp);
                mToggleDrawingMode.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.redLight)));
            }
            else
            {
                Log.d(TAG, "clickingAlso");
                drawMode = false;
//                    remote1Render.touchEnabled = false;
                remote1Render.drawEnabled = false;
                mToggleDrawingMode.setImageResource(R.drawable.baseline_gesture_white_18dp);
                mToggleDrawingMode.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
            }
        });

        mSwitchLayoutButton.setOnClickListener(v ->
        {
            VideoChatActivity.ShowToast("Switching", chatActivity.getApplicationContext());
            currentActivity.runOnUiThread (new Thread(new Runnable() {
                public void run()
                {
                    mRenderHolder.removeView(streamRenderLayout);
                    mRenderHolder.removeView(currentRenderLayout);
                    if(!switched)
                    {
                        streamRenderLayout.setLayoutParams(smallScreenlayoutParams);
                        currentRenderLayout.setLayoutParams(fullScreenlayoutParams);

                        if(!isAboveEight)
                        {
                            mRenderHolder.addView(streamRenderLayout, 0);
                            mRenderHolder.addView(currentRenderLayout, 1);
                        }
                        else
                        {
                            mRenderHolder.addView(currentRenderLayout, 0);
                            mRenderHolder.addView(streamRenderLayout, 1);
                        }
                        switched = true;
                    }
                    else
                    {
                        streamRenderLayout.setLayoutParams(fullScreenlayoutParams);
                        currentRenderLayout.setLayoutParams(smallScreenlayoutParams);

                        if(!isAboveEight)
                        {
                            mRenderHolder.addView(currentRenderLayout, 0);
                            mRenderHolder.addView(streamRenderLayout, 1);
                        }
                        else
                        {
                            mRenderHolder.addView(streamRenderLayout, 0);
                            mRenderHolder.addView(currentRenderLayout, 1);
                        }
                        switched = false;
                    }
                    mRenderHolder.invalidate();
                }
            }));
        });
        timerHandler = new Handler();
        timerRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                timerText.setText(String.format("%d:%02d", minutes, seconds));

                timerHandler.postDelayed(this, 500);
            }
        };
    }

    private void AdaptToggleButtons (boolean arrowMode)
    {
        if(arrowMode)
        {
            arrowText.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
            drawText.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.disabled_text_light)));
        }
        else
        {
            arrowText.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.disabled_text_light)));
            drawText.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        int num = item.getItemId();
        switch(num)
        {
            case R.id.nav_history:
                mHistoryButton.callOnClick();
                drawer.closeDrawers();
                break;
            case R.id.nav_upload:
                chatActivity.mFileUploadButton.callOnClick();
                drawer.closeDrawers();
                break;
        }

        return true;
    }

    private Bitmap GetUsetProfilePhoto ()
    {
        LoginDatabaseHelper loginHelper = new LoginDatabaseHelper(currentActivity);
        Cursor data = loginHelper.showData();
        data.moveToFirst();
        String imageName = data.getString(1) + "_PIC.jpg";
        userName = data.getString(2);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(LoginUIHandler.filePath + "/" + imageName, options);
    }

    public void showNotification(Context context, String title, String body, Intent intent)
    {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mChannel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent actionIntent = new Intent(currentActivity, ActionReceiver.class);
        actionIntent.putExtra("Action", "CancelDownload");

        PendingIntent pIntentCancel = PendingIntent.getBroadcast(currentActivity, 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setOnlyAlertOnce(true)
                .setProgress(100, 0, false)
                .addAction(R.drawable.arrow_down, "Cancel", pIntentCancel)
                .setOngoing(true);

        stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        resultPendingIntent = stackBuilder.getPendingIntent
        (
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    public void UpdateNotification (int progress)
    {
        Log.d(TAG, "Progress : " + progress);
        mBuilder.setProgress(100, progress, false);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    public void StopNotification ()
    {
        if(notificationManager != null)
            notificationManager.cancel(notificationId);
    }

    public void SetProgress (float scale, String filePath)
    {
        currentActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                ViewGroup.LayoutParams layoutParams = progessBar.getLayoutParams();

                layoutParams.width = (int) ((scale * width));

                progessBar.setLayoutParams(layoutParams);
                progessBar.invalidate();
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private void TurnOffOnDelay (final RelativeLayout parentView, final Switch selectedToggle, final int delayTime)
    {
        parentView.setEnabled(false);
        selectedToggle.setEnabled(false);

        buttonActivateHandler = new Handler();
        buttonActivateHandler.postDelayed(() ->
        {
            parentView.setEnabled(true);
            selectedToggle.setEnabled(true);
        }, delayTime);
    }

    @SuppressLint("HandlerLeak")
    private void TurnOffOnDelay (final FloatingActionButton selectedToggle, final int delayTime)
    {
        selectedToggle.setEnabled(false);

        buttonActivateHandler = new Handler();
        buttonActivateHandler.postDelayed(() ->
        {
            selectedToggle.setEnabled(true);
        }, delayTime);
    }

    void backKey ()
    {
        if(drawerOpen)
        {
            drawer.closeDrawers();
        }
        if(mHistoryScreen.getVisibility() == VISIBLE)
        {
            mHistoryScreen.setVisibility(GONE);
            mFloatingButtonsLayout.setVisibility(VISIBLE);
            mSwitchCamera.setVisibility(VISIBLE);
            mStartRecordingButton.setVisibility(VISIBLE);
            mEndCallButton.setVisibility(VISIBLE);
            mRenderHolder.setVisibility(VISIBLE);
            mSettingLayout.setVisibility(VISIBLE);
            mSettingsButton.setVisibility(VISIBLE);
        }
    }

    private void ChangeActivity ()
    {
        Intent intent = new Intent(currentActivity, AppManager.class);
        if(profilePicPresent)
        {
            intent.putExtra("PIC", "PRESENT");
        }
        else
        {
            intent.putExtra("PIC", "ABSENT");
        }
        currentActivity.finish();
        currentActivity.startActivity(intent);
    }

    public void CameraFlashHandler()
    {
        if(cameraTorchMode == CameraTorchMode.TO_TURN_ON)
        {
            cameraTorchMode = CameraTorchMode.ON;
            mFlashButton.callOnClick();
        }
    }

    public void StartTimer ()
    {
        if(timerRunning)
            return;

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        timerRunning = true;
    }

    public void StopTimer()
    {
        if(!timerRunning)
            return;

        timerHandler.removeCallbacks(timerRunnable);
        timerRunning = false;
    }

    public void Minimise ()
    {
        if (switched) {
            Log.d(TAG, " layout has been switched");
            aspectRatio = new Rational(localRender.getWidth(), localRender.getHeight());
            PreMinimise();
            minimisedSwitched = true;
        } else {
            aspectRatio = new Rational(remote1Render.getWidth(), remote1Render.getHeight());
        }

        if (Build.VERSION.SDK_INT >= 26) {

            PictureInPictureParams.Builder mPictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
            mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
            currentActivity.enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
        }
    }

    private void PreMinimise ()
    {
        currentActivity.runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                mRenderHolder.removeView(streamRenderLayout);
                mRenderHolder.removeView(currentRenderLayout);
                if(!switched)
                {
                    streamRenderLayout.setLayoutParams(smallScreenlayoutParams);
                    currentRenderLayout.setLayoutParams(fullScreenlayoutParams);
                    ViewGroup.LayoutParams layoutParams = currentRenderLayout.getLayoutParams();
                    layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;


                    if(!isAboveEight)
                    {
                        mRenderHolder.addView(streamRenderLayout, 0);
                        mRenderHolder.addView(currentRenderLayout, 1);
                    }
                    else
                    {
                        mRenderHolder.addView(currentRenderLayout, 0);
                        mRenderHolder.addView(streamRenderLayout, 1);
                    }
                    currentRenderLayout.setLayoutParams(layoutParams);
                    switched = true;
                }
                else
                {
                    streamRenderLayout.setLayoutParams(fullScreenlayoutParams);
                    currentRenderLayout.setLayoutParams(smallScreenlayoutParams);

                    if(!isAboveEight)
                    {
                        mRenderHolder.addView(currentRenderLayout, 0);
                        mRenderHolder.addView(streamRenderLayout, 1);
                    }
                    else
                    {
                        mRenderHolder.addView(streamRenderLayout, 0);
                        mRenderHolder.addView(currentRenderLayout, 1);
                    }
                    switched = false;
                }
                mRenderHolder.invalidate();
            }
        }));

    }

    public void setUItoPiP (boolean isSmall)
    {
//        FloatingActionButton switchButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
//
//        switchButton.callOnClick();
        if(minimisedSwitched && !isSmall)
        {
            PreMinimise();
            minimisedSwitched = false;
        }

        SurfaceViewRendererCustom currentRender = currentActivity.findViewById(R.id.CurrentRender);
        FloatingActionButton mEndCall = currentActivity.findViewById(R.id.EndCallButton);
        FloatingActionButton mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
        if(isSmall)
        {
            Log.d(TAG, "Screen is small");
            currentRender.setVisibility(GONE);
            mEndCall.setVisibility(GONE);
            mPlusButton.setVisibility(GONE);
            mSpawnButtonLayout.setVisibility(GONE);
            mSwitchCamera.setVisibility(GONE);
            mStartRecordingButton.setVisibility(GONE);
        }
        else
        {
            Log.d(TAG, "Screen is big");
            currentRender.setVisibility(VISIBLE);
            if((mHistoryScreen.getVisibility() != VISIBLE))
            {
                mEndCall.setVisibility(VISIBLE);

                mSwitchCamera.setVisibility(VISIBLE);
                mStartRecordingButton.setVisibility(VISIBLE);
            }
        }
    }

    public String AddTimeStampToName(String fileName, String timeStamp)
    {
        String[] fileNames = fileName.split(Pattern.quote("."));

        return fileNames[0] + timeStamp + "." + fileNames[1];
    }

    public String GetDate()
    {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY_hh-mm-ss");

        return dateFormat.format(date);
    }

    void TorchCallBack()
    {

        WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;
        Camera camera = videoCapturerAndroid.camera;

        if(camera != null)
        {
            Log.d(TAG, "Camera Present");
        }

        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        camera.setParameters(parameters);
        camera.startPreview();
    }

    private int GetScreenWidth()
    {
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point ();
        display.getSize(size);

        return size.x;
    }

    public void ToggleVideoView ()
    {
        RelativeLayout mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
        if(!videoView)
        {
            mRenderHolder.setVisibility(VISIBLE);
            videoView = true;
        }
        else
        {
            mRenderHolder.setVisibility(GONE);
            videoView = false;
        }
    }
}

class LoginUIHandler implements NavigationView.OnNavigationItemSelectedListener, NavigationMenuItemView.OnClickListener
{
    private Activity            currentActivity;
    private static String       TAG = "TLSKYPE";
    private AppManager          manager;
    private int                 currentScreen;
    private LoginDatabaseHelper loginDB;
    private String[]            userData;
    public  boolean             profilePicPresent = false;
    private boolean             drawerOpen = false;
    private boolean             pinMode = false;
    private String              ID;

    private DrawerLayout drawer;

    public  final static  String filePath = Environment.getExternalStorageDirectory().getPath() + "/TASQAR/ReceivedFiles/UserData/";

    private FloatingActionButton    SA_BackButton;

    //SI UI Elements
    private EditText    SI_PasswordField;
    private EditText    SI_UserIDField;

    private Switch      ST_PasswordToggle;

    private Button      SI_SubmitButton;
    private Button      SI_SignUpButton;
    //SI UI Elements

    //SU UI Elements
    private EditText    SU_NameField;
    private EditText    SU_EmailField;
    private EditText    SU_PhoneField;
    private EditText    SU_RoleField;
    private EditText    SU_HeirarchyField;
    private EditText    SU_OTPField;
    private EditText    SU_PasswordText;
    private EditText    SU_ReEnterText;

    private Button      SU_NextButton;
    private Button      SU_SubmitButton;
    private Button      SU_SubmitPasswordButton;

    private ImageButton SU_UserImageButton;

//    private ImageView SU_NameFieldImage;
//    private ImageView   SU_EmailFieldImage;
//    private ImageView   SU_PhoneFieldImage;
//    private ImageView   SU_ReEnterErrorImage;
//    private ImageView   SU_PasswordErrorImage;
//    private ImageView   SU_OTPErrorImage;

    //SU UI Elements

    //SU Variables
    private boolean isNamePresent;
    private boolean isEmailPresent;
    private boolean isPhonePresent;
    private boolean isPasswordPresent;
    private boolean isReEnterPresent;
    private boolean isOTPPresent;
    //SU Variables

    //All Screens
    private ConstraintLayout    mLoginScreen;
    private ConstraintLayout    mSignUpScreen;
    private ConstraintLayout    mDetailsScreen;
    private ConstraintLayout    mPasswordScreen;
    private ConstraintLayout    mST_SettingsScreen;
    //All Screens

    private Thread  submitNewUserThread = null;
    private Thread  emailIDCheckThread = null;
    private Thread  submitNextThread = null;
    private Thread  getUserThread = null;
    private Thread  loginThread = null;

    private int lastFetchedUserID = -1;
    private String photoftpLink = "";
    private Handler mHandler = new Handler();

    public LoginUIHandler (Activity appContext, AppManager managerClass)
    {
        currentActivity = appContext;
        manager = managerClass;
        getNextUserId();

        loginDB = new LoginDatabaseHelper(currentActivity);
        SA_BackButton = currentActivity.findViewById(R.id.SA_BackButton);

        //Screens
        mLoginScreen = currentActivity.findViewById(R.id.SignInScreen);
        mSignUpScreen = currentActivity.findViewById(R.id.SignUpScreen);
        mDetailsScreen = currentActivity.findViewById(R.id.SU_DetailsScreen);
        mPasswordScreen = currentActivity.findViewById(R.id.SU_PasswordScreen);
        //Screens

        //SI Screen
        SI_PasswordField = currentActivity.findViewById(R.id.SI_PasswordField);
        SI_UserIDField = currentActivity.findViewById(R.id.SI_UserIDField);

        SI_SubmitButton = currentActivity.findViewById(R.id.SI_SignInButton);
        SI_SignUpButton = currentActivity.findViewById(R.id.SI_SignUpButton);

        //SU Screen
        SU_NameField = currentActivity.findViewById(R.id.SU_NameField);
        SU_EmailField = currentActivity.findViewById(R.id.SU_EmailField);
        SU_PhoneField = currentActivity.findViewById(R.id.SU_PhoneNoField);
        SU_RoleField = currentActivity.findViewById(R.id.SU_RoleField);
        SU_HeirarchyField = currentActivity.findViewById(R.id.SU_HeirarchyField);
        SU_OTPField = currentActivity.findViewById(R.id.SU_OTPField);
        SU_PasswordText = currentActivity.findViewById(R.id.SU_PasswordField);
        SU_ReEnterText = currentActivity.findViewById(R.id.SU_ReEnterField);

        SU_UserImageButton = currentActivity.findViewById(R.id.SU_ProfilePic);

        SU_NextButton = currentActivity.findViewById(R.id.SU_NextButton);

//        SU_NameFieldImage = currentActivity.findViewById(R.id.SU_NameErrorImage);
//        SU_EmailFieldImage = currentActivity.findViewById(R.id.SU_EmailErrorImage);
//        SU_PhoneFieldImage = currentActivity.findViewById(R.id.SU_PhoneErrorImage);
        //SU Screen

        TryLogin();

        if(SA_BackButton == null)
        {
            return;
        }
        SA_BackButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                backKey();
            }
        });

        SI_SignUpButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mLoginScreen.setVisibility(View.GONE);
                mSignUpScreen.setVisibility(View.VISIBLE);
                mDetailsScreen.setVisibility(View.VISIBLE);
                mPasswordScreen.setVisibility(View.GONE);
                SA_BackButton.setVisibility(View.VISIBLE);
                currentScreen = 1;
            }
        });

        SI_SubmitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {

                TryLogin();
//            Login("arvindsemail@gmail.com", "asaadfghjk");
            }
        });

        SU_NextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                OnSubmitClicked();
            }
        });

        SU_NameField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

                currentActivity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
//                        SU_NameFieldImage.setVisibility(View.INVISIBLE);
                    }
                });

                isNamePresent = !(SU_NameField.getText().toString().length() == 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_EmailField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
//                SU_EmailFieldImage.setVisibility(View.INVISIBLE);
                String email = SU_EmailField.getText().toString();

                if (isValidEmail(email))
                {
                    isEmailPresent(email);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        SU_PhoneField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
//                        SU_PhoneFieldImage.setVisibility(View.INVISIBLE);
                    }
                });

                isPhonePresent = !(SU_PhoneField.getText().toString().length() == 0);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        SU_UserImageButton.setOnClickListener(v -> new ChooserDialog(currentActivity)
                .withStartFile(null)
//                        .withResources(R.string.title_choose_any_file, R.string.title_choose, R.string.dialog_cancel)
//                        .withFileIconsRes(false, R.mipmap.ic_my_file, R.mipmap.ic_my_folder)
                .withAdapterSetter(adapter ->
                {
                    //
                })
                .withChosenListener((path, pathFile) ->
                {

                    if(!path.contains(".jpg"))
                        return;

                    Log.d(TAG, path);

                    try
                    {
                        FileInputStream fileReadStream = new FileInputStream(pathFile);

                        Bitmap bitmap = BitmapFactory.decodeFile(path);

                        SU_UserImageButton.setImageBitmap(bitmap);

                        UploadFile(path, fileReadStream);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (RuntimeException e)
                    {
                        Log.d(TAG, e.getMessage());
                    }
                })
                .build()
                .show());
    }

    public void SetPasswordToggle ()
    {
        drawer = currentActivity.findViewById(R.id.CommonDrawer);
        android.support.v7.app.ActionBarDrawerToggle toggle = new android.support.v7.app.ActionBarDrawerToggle
                (currentActivity, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        {
            public void onDrawerClosed(View view)
            {
                super.onDrawerClosed(view);
                drawerOpen = false;
            }

            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                drawerOpen = true;
            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = currentActivity.findViewById(R.id.call_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mST_SettingsScreen = currentActivity.findViewById(R.id.SettingsScreen);

        ST_PasswordToggle = currentActivity.findViewById(R.id.ST_PasswordToggle);

        ST_PasswordToggle.setChecked(pinMode);

        ST_PasswordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

            }
        });
    }

    void SignOut ()
    {
        loginDB.deleteData("0");
    }

    private void TryLogin ()
    {
        Cursor data = loginDB.showData();
        data.moveToFirst();
        if(data.getCount() == 0)
        {
            String userID = SI_UserIDField.getText().toString();
            String password = SI_PasswordField.getText().toString();

            if(!userID.equals("") && !password.equals(""))
            {
                Login(userID, password);
            }

            return;
        }
        ID = data.getString(1);
        String pin_Mode = data.getString(8);
        pinMode = pin_Mode.equals("ENABLED");
        Log.d(TAG, pinMode ? "ENABLED" : "DISABLED");
        String fileName = ID + "_PIC.jpg";
        File profilePic = new File(filePath + "/" + fileName);
        if(!profilePic.exists())
        {
            DownloadFile(fileName);
        }
        else
        {
            if(pinMode)
            {
                manager.SetupPasswordScreen(true);
                return;
            }

            manager.SetupUserScreen(true);
            SetPasswordToggle();
        }
    }

    private void UploadFile(final String filePath, final InputStream inputStream)
    {

        final FTPManager ftpManager = new FTPManager();
        ftpManager.fileInputStream = inputStream;
        ftpManager.uploadORdownload = 1;
        ftpManager.applicationContext = currentActivity.getApplicationContext();
        ftpManager.filePath = filePath;
        ftpManager.fileName = lastFetchedUserID + "_PIC" + ".jpg";
        if (ftpManager.running)
        {
            Log.d(TAG, "FTP Manager Running Already");
            return;
        }
        new Thread(new Runnable()
        {
            @Override
            public void run() {

                Looper looper = mHandler.getLooper();
                looper.prepare();
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ftpManager.execute();
                        while (!ftpManager.transferSuccess)
                        {
                            try
                            {
                                Thread.sleep(10);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        photoftpLink = lastFetchedUserID + "_PIC.jpg";

                        Log.d(TAG, "Uploaded File");
                    }
                });
            }
        }).start();
    }

    private void DownloadFile (final String fileName)
    {
        final FTPManager ftpManager = new FTPManager();
        ftpManager.uploadORdownload = 0;
        ftpManager.applicationContext = currentActivity.getApplicationContext();
        ftpManager.filePath = Environment.getExternalStorageDirectory().getPath() + "/TASQAR/ReceivedFiles/UserData/";
        ftpManager.fileName = fileName;

        new Thread(new Runnable()
        {
            @Override
            public void run() {

                Looper looper = mHandler.getLooper();
                looper.prepare();
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ftpManager.execute();
                        while (!ftpManager.transferSuccess && !ftpManager.serverReply.contains("550"))
                        {
                            try
                            {
                                Thread.sleep(10);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        if(ftpManager.serverReply.contains("550"))
                        {
                            Log.d(TAG, "No Image");
                            profilePicPresent = false;
                            String fileName = ID + "_PIC.jpg";
                            File profilePic = new File(filePath + "/" + fileName);
                            if(profilePic.exists())
                            {
                                profilePic.delete();
                            }
                            if(pinMode)
                            {
                                manager.SetupPasswordScreen(profilePicPresent);
                                return;
                            }
                            manager.SetupUserScreen(profilePicPresent);
                            return;
                        }
                        else
                        {
                            Log.d(TAG, "Image");
                            profilePicPresent = true;
                        }
                        currentActivity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                TryLogin();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void isEmailPresent(final String emailId)
    {
        if(emailIDCheckThread != null && emailIDCheckThread.isAlive())
        {
            emailIDCheckThread.interrupt();
            emailIDCheckThread = null;
        }

        emailIDCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/IsEmailIDPresent?emailID="+emailId);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        isEmailPresent = !(Integer.parseInt(value) == 1);

//                        currentActivity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if(!isEmailPresent)
//                                    SU_EmailFieldImage.setVisibility(View.VISIBLE);
//                                else
//                                    SU_EmailFieldImage.setVisibility(View.INVISIBLE);
//                            }
//                        });

                        break;
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        emailIDCheckThread.start();
    }

    private void SubmitNewUser(final int userID, final  String photoftpLink, final String name, final String emailId, final String phoneNumber, final String role, final String hierarchy)
    {
        if(submitNewUserThread != null && submitNewUserThread.isAlive())
        {
            submitNewUserThread.interrupt();
            submitNewUserThread = null;
        }
        Log.d(TAG, "Submit New User");

        submitNewUserThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/SubmitUserInfo?userID=" + userID + "&" + "name=" + name
                            + "&" + "emailID=" + emailId + "&" + "phone=" + phoneNumber + "&" + "role=" + role + "&" + "photoURL=" + photoftpLink + "&" + "hierarchy=" + hierarchy);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        boolean submitted = Integer.parseInt(value) == 1 ? true : false;

                        if(submitted)
                        {
                            currentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    OpenOTPPasswordWindow();
                                }
                            });
                        }

                        break;
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        submitNewUserThread.start();
    }

    private void SubmitNext(final int userID, final String otp, final String password)
    {

        if(submitNextThread != null && submitNextThread.isAlive())
        {
            submitNextThread.interrupt();
            submitNextThread = null;
        }

        Log.d(TAG, "Submit Next");

        submitNextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/UpdatePassword?userID=" + userID + "&" + "otp=" + otp
                            + "&" + "password=" + password);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        boolean submitted = Integer.parseInt(value) == 1 ? true : false;
                        Log.d(TAG, submitted ? "Submitted" : "Not Submitted");
                        if(submitted)
                        {
                            currentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPasswordScreen.setVisibility(View.GONE);
                                    mDetailsScreen.setVisibility(View.VISIBLE);
                                    mSignUpScreen.setVisibility(View.GONE);
                                    mLoginScreen.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                        break;
                    }
                }
                catch (IOException | ParserConfigurationException | SAXException e)
                {
                    e.printStackTrace();
                }
            }
        });

        submitNextThread.start();
    }

    private void getNextUserId()
    {
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/GetNextUserID");

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        lastFetchedUserID = Integer.parseInt(value);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        networkThread.start();
    }

    private void OnSubmitClicked()
    {
        Log.d(TAG, "Working");
        String name = SU_NameField.getText().toString();
        String phone = SU_PhoneField.getText().toString();
        String email = SU_EmailField.getText().toString();
        String role = SU_RoleField.getText().toString();
        String hierarchy = SU_HeirarchyField.getText().toString();

        if (isNamePresent && isPhonePresent && isEmailPresent)
        {
            if (lastFetchedUserID != -1)
            {
                Log.d(TAG, "OnSubmitClicked");
                SubmitNewUser(lastFetchedUserID, photoftpLink, name, email, phone , role, hierarchy);

//                if (result) {
//                    Log.d(TAG, "ALL FINE BRO 1");
//
//                    OpenOTPPasswordWindow();
//                } else {
//                    Log.d(TAG, "NOT ALL FIND 1");
//                }
            }
        }
    }

    private void OnSubmitPasswordClicked()
    {
        String otp = SU_OTPField.getText().toString();
        String password = SU_PasswordText.getText().toString();
        String passwordText = SU_PasswordText.getText().toString();
        String reEnterText = SU_ReEnterText.getText().toString();


        Log.d(TAG, "OnSubmitPasswordClicked");

        if(isOTPPresent && isPasswordPresent && isReEnterPresent)
        {
            if(!passwordText.equals(reEnterText))
            {
                Toast.makeText(currentActivity, "Password Mismatch", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Password Text : " + SU_PasswordText.getText() + " ReEnter Text : " + SU_ReEnterText.getText());
            }
            else
            {
                SubmitNext(lastFetchedUserID, otp, password);
            }
        }
    }

    private void OpenOTPPasswordWindow()
    {
        mDetailsScreen.setVisibility(View.GONE);
        mPasswordScreen.setVisibility(View.VISIBLE);
        currentScreen = 2;

        Log.d(TAG, "OpenOTPPasswordWindow");

//        SU_PasswordErrorImage = currentActivity.findViewById(R.id.SU_PasswordError);
        SU_PasswordText = currentActivity.findViewById(R.id.SU_PasswordField);
        SU_PasswordText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                isPasswordPresent = isValidPassword(SU_PasswordText.getText().toString());

//                currentActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        SU_PasswordErrorImage.setVisibility(isPasswordPresent ? View.INVISIBLE : View.VISIBLE);
//                    }
//                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

//        SU_ReEnterErrorImage = currentActivity.findViewById(R.id.SU_ReEnterError);
        SU_ReEnterText = currentActivity.findViewById(R.id.SU_ReEnterField);
        SU_ReEnterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

//                currentActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        SU_ReEnterErrorImage.setVisibility(View.INVISIBLE);
//
//                    }
//                });

                String password = SU_PasswordText.getText().toString();

                isPasswordPresent = isValidPassword(password);

                if (isPasswordPresent) {
                    String reenter = SU_ReEnterText.getText().toString();

                    isReEnterPresent = reenter.equals(password);

//                    currentActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            SU_ReEnterErrorImage.setVisibility(isReEnterPresent ? View.INVISIBLE : View.VISIBLE);
//                        }
//                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

//        SU_OTPErrorImage = currentActivity.findViewById(R.id.SU_OTPError);
        SU_OTPField = currentActivity.findViewById(R.id.SU_OTPField);
        SU_OTPField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        SU_OTPErrorImage.setVisibility(View.INVISIBLE);
                    }
                });

                isOTPPresent = (s.toString().length() > 0);

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        SU_OTPErrorImage.setVisibility(isOTPPresent ? View.INVISIBLE : View.VISIBLE);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_SubmitPasswordButton = currentActivity.findViewById(R.id.SU_SubmitPasswordButton);
        SU_SubmitPasswordButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                OnSubmitPasswordClicked();
            }
        });

//        currentActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                SU_PasswordErrorImage.setVisibility(View.INVISIBLE);
//                SU_OTPErrorImage.setVisibility(View.INVISIBLE);
//                SU_ReEnterErrorImage.setVisibility(View.INVISIBLE);
//            }
//        });
    }

    private boolean isValidEmail(String email)
    {

        Log.d(TAG, "IS CVALUD");

        if (email.length() > 5)
        {
            if (email.contains("@") && email.contains("."))
            {
                return true;
            }
        }

        return false;
    }

    private boolean isValidPassword(String password)
    {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.{8,})(?=.*[!@#$%^&*])";//"^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%\^&\*])(?=.{8,})"

        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(password);

        if (m.find())
        {
            return true;
        }

        return false;
    }

    private void OnLoginSuccess ()
    {
        Cursor data = loginDB.showData();
        data.moveToFirst();
        if(data.getCount() == 0)
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    GetUserDetails(SI_UserIDField.getText().toString());
                    while(userData == null)
                    {
                        try
                        {
                            Thread.sleep(10);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    loginDB.addData
                        (
                            userData[0],                        //User ID
                            userData[1],                        //Name
                            SI_UserIDField.getText().toString(),//Email
                            userData[2],                        //Number
                            userData[3],                        //Password
                            userData[4],                        //Hierarchy
                            userData[5],                        //Role
                            "0000"
                        );
                    currentActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            TryLogin();
                        }
                    });
                }
            }).start();
        }
    }

    private void Login(final String userID, final String Password)
    {
        if (loginThread != null && loginThread.isAlive())
        {
            loginThread.interrupt();
            loginThread = null;
        }

        loginThread = new Thread(() ->
        {
            URL url = null;
            try {
                url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/Login?emailID=" + userID + "&" + "Password=" + Password);
                URLConnection con = url.openConnection();
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(con.getInputStream());

                Node userIDNode = doc.getElementsByTagName("int").item(0);
                NodeList list = userIDNode.getChildNodes();

                Node node = list.item(0);
                String value = node.getNodeValue();

                boolean submitted = Integer.parseInt(value) == 1;



                if (submitted)
                    currentActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            OnLoginSuccess();
                        }
                    });
                else
                    Log.e(TAG, "NotDone");


            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
        });

        loginThread.start();
    }

    private void GetUserDetails(final String emailID)
    {
        if (getUserThread != null && getUserThread.isAlive())
        {
            getUserThread.interrupt();
            getUserThread = null;
        }

        getUserThread = new Thread(() ->
        {
            URL url = null;
            try
            {
                url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/UserData?emailID=" + emailID);
                URLConnection con = url.openConnection();
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(con.getInputStream());

                Log.d(TAG, "Document : " + doc.getElementsByTagName("string").item(0));

                Node userIDNode = doc.getElementsByTagName("string").item(0);
                NodeList list = userIDNode.getChildNodes();
                Node node = list.item(0);
                String value = node.getNodeValue();

                if(!value.equals(""))
                {
                    userData = value.split(";");
                }
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
        });

        getUserThread.start();
    }

    public void backKey ()
    {
        if(mST_SettingsScreen.getVisibility() == VISIBLE)
        {
            mST_SettingsScreen.setVisibility(GONE);
            loginDB.AddSpecificData("PIN_MODE", ST_PasswordToggle.isChecked() ? "ENABLED" : "DISABLED");
            return;
        }

        switch(currentScreen)
        {
            case 0:
                System.exit(0);
                break;
            case 1:
                mLoginScreen.setVisibility(View.VISIBLE);
                mSignUpScreen.setVisibility(View.GONE);
                mDetailsScreen.setVisibility(View.VISIBLE);
                mPasswordScreen.setVisibility(View.GONE);
                SA_BackButton.setVisibility(View.GONE);
                currentScreen = 0;
                break;
            case 2:
                mLoginScreen.setVisibility(View.GONE);
                mSignUpScreen.setVisibility(View.VISIBLE);
                mDetailsScreen.setVisibility(View.VISIBLE);
                mPasswordScreen.setVisibility(View.GONE);
                currentScreen = 1;
                break;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        int num = item.getItemId();
        switch(num)
        {
            case R.id.nav_setting:
                mST_SettingsScreen.setVisibility(VISIBLE);
                drawer.closeDrawers();
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v)
    {

    }
}