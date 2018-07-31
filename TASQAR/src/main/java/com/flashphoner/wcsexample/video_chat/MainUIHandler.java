package com.flashphoner.wcsexample.video_chat;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
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

import java.util.List;

public class MainUIHandler
{
    private boolean     drawMode;
    private boolean     switched;
    private boolean     minimisedSwitched;
    private boolean     videoView = false;
    private boolean     backCam = true;
    private boolean     recording;
    private boolean     pointMode = true;
    private Activity    currentActivity;
    private static  String TAG = "UI_TEST";
    private boolean     isAboveEight;
    private VideoChatActivity chatActivity;

    SurfaceViewRendererCustom remote1Render;
    SurfaceViewRendererCustom localRender;

    FloatingActionButton mEndCallButton;
    FloatingActionButton mPlusButton;
    FloatingActionButton mSwitchLayoutButton;
    FloatingActionButton mToggleDrawingMode;
    FloatingActionButton mSwitchCamera;
    FloatingActionButton mStartRecordingButton;
    FloatingActionButton mPointToPlaneButton;

    TextView recordingText;
    TextView pointModeText;

    Button mButton;
    Button mExpandButton;

    LinearLayout mSpawnButtonLayout;

    RelativeLayout currentRenderLayout;
    RelativeLayout streamRenderLayout;
    RelativeLayout mRenderHolder;
    RelativeLayout.LayoutParams   fullScreenlayoutParams;
    RelativeLayout.LayoutParams   smallScreenlayoutParams;
    Rational aspectRatio;

    private final PictureInPictureParams.Builder mPictureInPictureParamsBuilder =
            new PictureInPictureParams.Builder();

    public MainUIHandler (Activity activity)
    {
        currentActivity = activity;
        chatActivity = VideoChatActivity.getInstance();

        remote1Render = currentActivity.findViewById(R.id.StreamRender);
        localRender = currentActivity.findViewById(R.id.CurrentRender);

        mEndCallButton = currentActivity.findViewById(R.id.EndCallButton);
        mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
        mSwitchLayoutButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
        mToggleDrawingMode = currentActivity.findViewById(R.id.DrawingModeButton);
        mSwitchCamera = currentActivity.findViewById(R.id.SwitchCamButton);
        mStartRecordingButton = currentActivity.findViewById(R.id.StartRecordingButton);
        mPointToPlaneButton = currentActivity.findViewById(R.id.PointToPlaneButton);
        mExpandButton = currentActivity.findViewById(R.id.expandButton);

        recordingText = currentActivity.findViewById(R.id.startRecord);
        pointModeText = currentActivity.findViewById(R.id.Point2Plane);

        mButton = currentActivity.findViewById(R.id.button);

        mSpawnButtonLayout = currentActivity.findViewById(R.id.ButtonLayout);

        currentRenderLayout = currentActivity.findViewById(R.id.currentLayout);
        streamRenderLayout = currentActivity.findViewById(R.id.streamLayout);
        mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
        fullScreenlayoutParams = (RelativeLayout.LayoutParams) streamRenderLayout.getLayoutParams();
        smallScreenlayoutParams = (RelativeLayout.LayoutParams) currentRenderLayout.getLayoutParams();

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

        mExpandButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ViewGroup.LayoutParams layoutParams = currentRenderLayout.getLayoutParams();
                Log.d(TAG, Integer.toString(layoutParams.height));
                if(layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT)
                {
                    layoutParams.height = 600;
                }
                else
                {
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                currentRenderLayout.setLayoutParams(layoutParams);
                currentRenderLayout.invalidate();
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
                }
                else
                {
                    mSwitchCamera.setImageResource(R.drawable.flip_cam_rear);
                    backCam = true;
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
    }

    void ChangeActivity ()
    {
        Intent intent = new Intent(currentActivity, AppManager.class);
        intent.putExtra("KEY", "loggedin");
        currentActivity.finish();
        currentActivity.startActivity(intent);
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
            mEndCall.setVisibility(View.VISIBLE);
            mPlusButton.setVisibility(View.VISIBLE);
        }
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