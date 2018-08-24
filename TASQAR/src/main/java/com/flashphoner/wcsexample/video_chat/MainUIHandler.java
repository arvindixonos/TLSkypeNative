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
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.NavigationMenuItemView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;

import org.webrtc.VideoCapturerAndroid;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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

    public Camera      camera;
    public FileButtonHelper    fileButtonHelper;

    //Notification Variables
    private NotificationManager notificationManager;
    private NotificationChannel mChannel;
    private NotificationCompat.Builder mBuilder;
    private TaskStackBuilder stackBuilder;
    private PendingIntent resultPendingIntent;
    private int notificationId = 1;
    private String channelId = "channel-01";
    private String channelName = "Channel Name";
    private int importance = NotificationManager.IMPORTANCE_HIGH;
    private String userName = "";
    //Notification Variables

    DrawerLayout    drawerLayout;

    ImageView progessBar;

    SurfaceViewRendererCustom remote1Render;
    SurfaceViewRendererCustom localRender;

    CircularImageView   profilePhoto;

    ConstraintLayout mFloatingButtonsLayout;
    ConstraintLayout mHistoryScreen;
    ConstraintLayout mExtraButtons;

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

    LinearLayout historyScreen;

    Button mButton;
    TempButton mTempButton;
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
    public void onClick(View v) {
        VideoChatActivity.ShowToast("Selected", currentActivity);
    }

    public MainUIHandler (Activity activity)
    {//        WCSAudioManager wcsAudioManager = WCSAudioManager.create(getApplicationContext(), deviceStateChangedListener);
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

        remote1Render = currentActivity.findViewById(R.id.StreamRender);
        localRender = currentActivity.findViewById(R.id.CurrentRender);

        mFloatingButtonsLayout = currentActivity.findViewById(R.id.FloatingButtonsLayout);
        mHistoryScreen = currentActivity.findViewById(R.id.FileHistory);
        mExtraButtons = currentActivity.findViewById(R.id.AddedButtons);

        mEndCallButton = currentActivity.findViewById(R.id.EndCallButton);
        mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
        mSwitchLayoutButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
        mToggleDrawingMode = currentActivity.findViewById(R.id.DrawingModeButton);
        mSwitchCamera = currentActivity.findViewById(R.id.SwitchCamButton);
        mStartRecordingButton = currentActivity.findViewById(R.id.StartRecordingButton);
        mPointToPlaneButton = currentActivity.findViewById(R.id.PointToPlaneButton);
        mHistoryButton = currentActivity.findViewById(R.id.HistoryButton);
        mHistoryBackButton = currentActivity.findViewById(R.id.historyBackButton);
        mFlashButton = currentActivity.findViewById(R.id.FlashButton);

        drawer = currentActivity.findViewById(R.id.drawer_layout);

        recordingText = currentActivity.findViewById(R.id.startRecord);
        pointModeText = currentActivity.findViewById(R.id.Point2Plane);
        timerText = currentActivity.findViewById(R.id.timerText);

        mButton = currentActivity.findViewById(R.id.button);

        historyScreen = currentActivity.findViewById(R.id.FileHistoryLayout);

        mSpawnButtonLayout = currentActivity.findViewById(R.id.ButtonLayout);

        currentRenderLayout = currentActivity.findViewById(R.id.currentLayout);
        streamRenderLayout = currentActivity.findViewById(R.id.streamLayout);
        mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
        fullScreenlayoutParams = (RelativeLayout.LayoutParams) streamRenderLayout.getLayoutParams();
        smallScreenlayoutParams = (RelativeLayout.LayoutParams) currentRenderLayout.getLayoutParams();

        fileButtonHelper = new FileButtonHelper(currentActivity, historyScreen);

        currentActivity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        mTempButton = currentActivity.findViewById(R.id.tempButton);
        mTempButton.layoutHolder = currentActivity.findViewById(R.id.ConLayout);
        mTempButton.mArrowModeButton = currentActivity.findViewById(R.id.ArrowModeButton);
        mTempButton.mDrawModeButton = currentActivity.findViewById(R.id.DrawModeButton);
        mTempButton.mPointOrPlaneButton = currentActivity.findViewById(R.id.PointOrPlaneButton);
        mTempButton.mArrowModeFloatingButton = currentActivity.findViewById(R.id.ArrowModeFloatingButton);
        mTempButton.mDrawModeFloatingButton = currentActivity.findViewById(R.id.DrawModeFloatingButton);
        mTempButton.mPointOrPlaneModeFloatingButton = currentActivity.findViewById(R.id.PointOrPlaneFloatingButton);
        mTempButton.chatActivity = chatActivity;
        mTempButton.InitialiseButtons();

        //Set Photo and Name
        NavigationView headerLayout = currentActivity.findViewById(R.id.nav_view);
        View header = headerLayout.getHeaderView(0);
        profilePhoto = header.findViewById(R.id.ProfilePics);
        profilePhoto.setImageBitmap(GetUsetProfilePhoto());
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
                toggleBackcamItem = currentActivity.findViewById(R.id.back_cam_switch);
                toggleBackcamButton = (Switch) toggleBackcamItem.getChildAt(0);
                toggleBackcamButton.setOnClickListener(v ->
                {
                    TurnOffOnDelay(toggleBackcamItem, toggleBackcamButton, 3000);
                    mSwitchLayoutButton.callOnClick();
                    mSwitchCamera.callOnClick();
                });

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
        mFlashButton.setOnClickListener(new View.OnClickListener()
        {
            CameraManager cameraManager = (CameraManager) currentActivity.getSystemService(Context.CAMERA_SERVICE);

            @Override
            public void onClick(View v)
            {
                if(!flashOn)
                {
                    try
                    {
                        if(backCam)
                        {
                            Camera.Parameters parameters = camera.getParameters();
                            List<String> strs = parameters.getSupportedFlashModes();
                            for (String str:strs)
                            {
                                Log.d(TAG, "Message string is " + str);
                            }
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            camera.setParameters(parameters);
                            camera.startPreview();

                        }
                        else
                        {
                            try
                            {
                                String cameraId = cameraManager.getCameraIdList()[0];
                                cameraManager.setTorchMode(cameraId, true);
                            }
                            catch(CameraAccessException e)
                            {
                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                            }
                        }
                        mFlashButton.setImageResource(R.drawable.flash_off);
                        flashOn = true;
                    }
                    catch (Exception e)
                    {
                        VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                    }
                }
                else
                {
                    try
                    {
                        if(backCam)
                        {
                            Camera.Parameters parameters = camera.getParameters();
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            camera.setParameters(parameters);
                            camera.startPreview();
                        }
                        else
                        {
                            try
                            {
                                String cameraId = cameraManager.getCameraIdList()[0];
                                cameraManager.setTorchMode(cameraId, false);
                            }
                            catch(CameraAccessException e)
                            {
                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                            }
                        }
                        mFlashButton.setImageResource(R.drawable.flash_on);
                        flashOn = false;
                    }
                    catch (Exception e)
                    {
                        VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                    }
                }
            }
        });

        mSwitchCamera.setOnClickListener(v ->
        {
            VideoChatActivity.getInstance().ToggleCamera();

            if(backCam)
            {
                mSwitchCamera.setImageResource(R.drawable.flip_cam_front);
                backCam = false;
                if(flashOn)
                {
                    cameraTorchMode = CameraTorchMode.TO_TURN_ON;
                    flashOn = false;
                }
            }
            else
            {
                mSwitchCamera.setImageResource(R.drawable.flip_cam_rear);
                backCam = true;
                if(flashOn)
                {
                    cameraTorchMode = CameraTorchMode.TO_TURN_ON;
                    flashOn = false;
                }
            }
        });

        mHistoryBackButton.setOnClickListener(v -> backKey());

        mHistoryButton.setOnClickListener(v ->
        {

            mHistoryScreen.setVisibility(VISIBLE);
            mFlashButton.setVisibility(GONE);
            mStartRecordingButton.setVisibility(GONE);
            mFloatingButtonsLayout.setVisibility(GONE);
            mEndCallButton.setVisibility(GONE);
            mRenderHolder.setVisibility(GONE);
            mExtraButtons.setVisibility(GONE);

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
//                chatActivity.CleanUp();
                chatActivity.SendMessage("Disconnect");
                chatActivity.Disconnect();
                ChangeActivity();
                if(flashOn)
                {
                    mFlashButton.callOnClick();
                }
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
    int count = 0;
    int width;

    public Bitmap GetUsetProfilePhoto ()
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
            mFlashButton.setVisibility(VISIBLE);
            mStartRecordingButton.setVisibility(VISIBLE);
            mEndCallButton.setVisibility(VISIBLE);
            mRenderHolder.setVisibility(VISIBLE);
            mExtraButtons.setVisibility(VISIBLE);
        }
    }

    private void ChangeActivity ()
    {
        Intent intent = new Intent(currentActivity, AppManager.class);
        intent.putExtra("KEY", "loggedin");
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

    public void PreMinimise ()
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
            mFlashButton.setVisibility(GONE);
            mStartRecordingButton.setVisibility(GONE);
        }
        else
        {
            Log.d(TAG, "Screen is big");
            currentRender.setVisibility(VISIBLE);
            if((mHistoryScreen.getVisibility() != VISIBLE))
            {
                mEndCall.setVisibility(VISIBLE);

                mFlashButton.setVisibility(VISIBLE);
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

    public int GetScreenWidth()
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