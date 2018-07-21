package com.flashphoner.wcsexample.video_chat;

import android.app.Activity;
import android.app.PictureInPictureParams;
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

import com.flashphoner.fpwcsapi.room.Room;
import com.flashphoner.fpwcsapi.room.RoomManager;
import com.flashphoner.fpwcsapi.session.Stream;

import org.webrtc.SurfaceViewRenderer;

public class MainUIHandler
{
    private boolean     drawMode;
    private boolean     switched;
    private boolean     videoView = false;
    private boolean     backCam = true;
    private Activity    currentActivity;
    private static  String TAG = "UI_TEST";
    private boolean     isAboveEight;
    private VideoChatActivity chatActivity;

    private final PictureInPictureParams.Builder mPictureInPictureParamsBuilder =
            new PictureInPictureParams.Builder();

    public MainUIHandler (Activity activity)
    {
         currentActivity = activity;
         chatActivity = VideoChatActivity.getInstance();

         final SurfaceViewRendererCustom remote1Render = currentActivity.findViewById(R.id.StreamRender);
         final SurfaceViewRendererCustom localRender = currentActivity.findViewById(R.id.CurrentRender);

         final FloatingActionButton mEndCallButton = currentActivity.findViewById(R.id.EndCallButton);
         final FloatingActionButton mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
         final FloatingActionButton mSwitchLayoutButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
         final FloatingActionButton mToggleDrawingMode = currentActivity.findViewById(R.id.DrawingModeButton);
         final FloatingActionButton mSwitchCamera = currentActivity.findViewById(R.id.SwitchCamButton);

         final Button mButton = currentActivity.findViewById(R.id.button);

         final LinearLayout mSpawnButtonLayout = currentActivity.findViewById(R.id.ButtonLayout);

         final RelativeLayout currentRenderLayout = currentActivity.findViewById(R.id.currentLayout);
         final RelativeLayout streamRenderLayout = currentActivity.findViewById(R.id.streamLayout);
         final RelativeLayout mRenderHolder = currentActivity.findViewById(R.id.RenderHolder);
         final RelativeLayout.LayoutParams   fullScreenlayoutParams = (RelativeLayout.LayoutParams) streamRenderLayout.getLayoutParams();
         final RelativeLayout.LayoutParams   smallScreenlayoutParams = (RelativeLayout.LayoutParams) currentRenderLayout.getLayoutParams();

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
                chatActivity.currentActivityIntent.putExtra("MIN", "TRUE");
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
        SurfaceViewRendererCustom remoteRender = currentActivity.findViewById(R.id.StreamRender);
        Rational aspectRatio = new Rational(remoteRender.getWidth(), remoteRender.getHeight());
        chatActivity.currentActivityIntent.putExtra("MIN", "FALSE");

        mPictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
        currentActivity.enterPictureInPictureMode(mPictureInPictureParamsBuilder.build());

    }

    public void setUItoPiP (boolean isSmall)
    {
//        FloatingActionButton switchButton = currentActivity.findViewById(R.id.SwitchLayoutButton);
//
//        switchButton.callOnClick();
        SurfaceViewRendererCustom currentRender = currentActivity.findViewById(R.id.CurrentRender);
        FloatingActionButton mEndCall = currentActivity.findViewById(R.id.EndCallButton);
        FloatingActionButton mPlusButton = currentActivity.findViewById(R.id.floatingActionButton4);
        if(isSmall)
        {
            Log.d(TAG, "Screen is small");
            currentRender.setVisibility(View.GONE);
            mEndCall.setVisibility(View.GONE);
            mPlusButton.setVisibility(View.GONE);
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