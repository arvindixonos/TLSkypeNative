package com.flashphoner.wcsexample.video_chat;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.util.Rational;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flashphoner.fpwcsapi.room.Room;
import com.flashphoner.fpwcsapi.room.RoomManager;
import com.flashphoner.fpwcsapi.session.Stream;

import org.webrtc.SurfaceViewRenderer;

import java.security.Policy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MainUIHandler extends CameraCaptureSession.StateCallback
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
    private Activity    currentActivity;
    private static  String TAG = "UI_TEST";
    private VideoChatActivity chatActivity;
    private Handler     timerHandler;
    private Runnable    timerRunnable;
    private long        startTime;
    public CameraTorchMode cameraTorchMode;

    public Camera      camera;
    public FileButtonHelper    fileButtonHelper;

    SurfaceViewRendererCustom remote1Render;
    SurfaceViewRendererCustom localRender;

    ConstraintLayout mFloatingButtonsLayout;
    ConstraintLayout mHistoryScreen;

    FloatingActionButton mEndCallButton;
    FloatingActionButton mPlusButton;
    FloatingActionButton mSwitchLayoutButton;
    FloatingActionButton mToggleDrawingMode;
    FloatingActionButton mSwitchCamera;
    FloatingActionButton mStartRecordingButton;
    FloatingActionButton mPointToPlaneButton;
    FloatingActionButton mHistoryButton;
    FloatingActionButton mHistoryBackButton;
    FloatingActionButton mFlashButton;

    TextView recordingText;
    TextView pointModeText;
    TextView timerText;

    LinearLayout historyScreen;

    Button mButton;
    LinearLayout mSpawnButtonLayout;

    RelativeLayout currentRenderLayout;
    RelativeLayout streamRenderLayout;
    RelativeLayout mRenderHolder;
    RelativeLayout.LayoutParams   fullScreenlayoutParams;
    RelativeLayout.LayoutParams   smallScreenlayoutParams;
    Rational aspectRatio;

    private final PictureInPictureParams.Builder mPictureInPictureParamsBuilder =
            new PictureInPictureParams.Builder();

    public enum CameraTorchMode
    {
        ON,
        OFF,
        TO_TURN_ON
    }

    public MainUIHandler (Activity activity)
    {

//        if (!isFlashAvailable) {
//
//            AlertDialog alert = new AlertDialog.Builder(currentActivity)
//                    .create();
//            alert.setTitle("Error !!");
//            alert.setMessage("Your device doesn't support flash light!");
//            alert.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
//            {
//                public void onClick(DialogInterface dialog, int which)
//                {
//                    // closing the application
//                    System.exit(0);
//                }
//            });
//            alert.show();
//            return;
//        }
        currentActivity = activity;

        chatActivity = VideoChatActivity.getInstance();

        remote1Render = currentActivity.findViewById(R.id.StreamRender);
        localRender = currentActivity.findViewById(R.id.CurrentRender);

        mFloatingButtonsLayout = currentActivity.findViewById(R.id.FloatingButtonsLayout);
        mHistoryScreen = currentActivity.findViewById(R.id.FileHistory);

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

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N)
        {
            mRenderHolder.removeView(streamRenderLayout);
            mRenderHolder.removeView(currentRenderLayout);

            mRenderHolder.addView(streamRenderLayout, 0);
            mRenderHolder.addView(currentRenderLayout, 1);

            isAboveEight = true;
            Log.d(TAG, "Current Version is Above 7");
        }
        else
        {
            Log.d(TAG, "Current Version is 7 or below");
        }
//test
//        final FileButtonHelper fileButtonHelper = new FileButtonHelper(currentActivity, historyScreen);
//
//        testButton.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                fileButtonHelper.AddButton();
//            }
//        });
//
//        Button testButton2 = currentActivity.findViewById(R.id.button4);
//        testButton2.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                fileButtonHelper.GetData();
//            }
//        });
//test
        mFlashButton.setOnClickListener(new View.OnClickListener()
        {
            CameraManager cameraManager = (CameraManager) currentActivity.getSystemService(Context.CAMERA_SERVICE);


            @Override
            public void onClick(View v)
            {
                boolean isFlashAvailable = currentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

                if(!isFlashAvailable)
                {
                    VideoChatActivity.ShowToast("Flash not Available", currentActivity);
                    return;
                }
                else
                {
                    VideoChatActivity.ShowToast("Flash Available", currentActivity);
                }
                if(camera == null)
                {
                    Log.d(TAG, "No Camera Present");
                }
                if(!flashOn)
                {
                    try
                    {
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
                            try
                            {
                                String cameraId = cameraManager.getCameraIdList()[0];
                                cameraManager.setTorchMode(cameraId, true);
                            }
                            catch(CameraAccessException e)
                            {
                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                            }
//                        }
                        mFlashButton.setImageResource(R.drawable.flash_off);
                        mFlashButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.redLight)));
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
//                        if(backCam)
//                        {
//                            Camera.Parameters parameters = camera.getParameters();
//                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                            camera.setParameters(parameters);
//                            camera.startPreview();
//                        }
//                        else
//                        {
                            try
                            {
                                String cameraId = cameraManager.getCameraIdList()[0];
                                cameraManager.setTorchMode(cameraId, false);
                            }
                            catch(CameraAccessException e)
                            {
                                VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                            }
//                        }
                        mFlashButton.setImageResource(R.drawable.flash_on);
                        mFlashButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
                        flashOn = false;
                    }
                    catch (Exception e)
                    {
                        VideoChatActivity.ShowToast(e.getMessage(), currentActivity);
                    }
                }
            }
        });

        mFlashButton.callOnClick();
        mSwitchCamera.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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
            }
        });

        mHistoryBackButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                backKey();
            }
        });

        mHistoryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                mHistoryScreen.setVisibility(View.VISIBLE);
                mFloatingButtonsLayout.setVisibility(View.GONE);
                mEndCallButton.setVisibility(View.GONE);
                mRenderHolder.setVisibility(View.GONE);

                fileButtonHelper.GetData();
            }
        });

        mPlusButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(mSpawnButtonLayout.getVisibility() == View.VISIBLE)
                {
                    mSpawnButtonLayout.setVisibility(View.GONE);
                }
                else
                {
                    mSpawnButtonLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        mPointToPlaneButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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
            }
        });

        mStartRecordingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!recording)
                {
                    chatActivity.screenRecorder.GetPermission();
                    mStartRecordingButton.setImageResource(R.drawable.button_stop);
                    recordingText.setText("Stop Recording");
                    mStartRecordingButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.redLight)));
                    recording = true;
                }
                else
                {
                    chatActivity.screenRecorder.StopRecording();
                    mStartRecordingButton.setImageResource(R.drawable.button_record);
                    recordingText.setText("Start Recording");
                    mStartRecordingButton.setBackgroundTintList(ColorStateList.valueOf(currentActivity.getResources().getColor(R.color.blueDark)));
                    recording = false;
                }
            }
        });

        mEndCallButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ToggleVideoView();
                chatActivity.SendMessage("Disconnect");
                chatActivity.Disconnect();
                ChangeActivity();
                if(flashOn)
                {
                    mFlashButton.callOnClick();
                }
            }
        });

        mButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Minimise();
            }
        });

        mToggleDrawingMode.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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
            }
        });

        mSwitchLayoutButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
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
            }
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

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session)
    {
        CameraDevice device = session.getDevice();

    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session)
    {

    }

    void backKey ()
    {
        if(historyScreen.getVisibility() == View.VISIBLE)
        {
            mHistoryScreen.setVisibility(View.GONE);
            mFloatingButtonsLayout.setVisibility(View.VISIBLE);
            mEndCallButton.setVisibility(View.VISIBLE);
            mRenderHolder.setVisibility(View.VISIBLE);
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
        if(switched)
        {
            Log.d(TAG, " layout has been switched");
            aspectRatio = new Rational(localRender.getWidth(), localRender.getHeight());
            PreMinimise();
            minimisedSwitched = true;
        }
        else
        {
            aspectRatio = new Rational(remote1Render.getWidth(), remote1Render.getHeight());
        }
//        SurfaceViewRendererCustom remoteRender = currentActivity.findViewById(R.id.StreamRender);
//
        mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
        currentActivity.enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());
    }

    List<RemoteAction> remoteActions;

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
            currentRender.setVisibility(View.GONE);
            mEndCall.setVisibility(View.GONE);
            mPlusButton.setVisibility(View.GONE);
            mSpawnButtonLayout.setVisibility(View.GONE);
        }
        else
        {
            Log.d(TAG, "Screen is big");
            currentRender.setVisibility(View.VISIBLE);
            if(!(historyScreen.getVisibility() == View.VISIBLE))
                mEndCall.setVisibility(View.VISIBLE);
            mPlusButton.setVisibility(View.VISIBLE);
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
        DateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy_HH-mm-ss");

        return dateFormat.format(date);
    }

    void TorchCallBack()
    {

    }

    public void ToggleVideoView ()
    {
        RelativeLayout mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
        if(!videoView)
        {
            mRenderHolder.setVisibility(View.VISIBLE);
            videoView = true;
        }
        else
        {
            mRenderHolder.setVisibility(View.GONE);
            videoView = false;
        }
    }
}