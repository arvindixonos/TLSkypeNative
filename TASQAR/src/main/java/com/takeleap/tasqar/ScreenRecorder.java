package com.takeleap.tasqar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenRecorder
{
    public static ScreenRecorder Instance;
    public boolean recording = false;
    public boolean inited = false;
    public static final int PERMISSION_CODE = 500;

    private static final String TAG = "TLSkype";
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private Activity currentActivity;


    public boolean validateMicAvailability(){
        Boolean available = true;
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try{
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
                available = false;

            }

            recorder.startRecording();
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
                recorder.stop();
                available = false;

            }
            recorder.stop();
        } finally{
            recorder.release();
            recorder = null;
        }

        return available;
    }

    public ScreenRecorder(Activity appActivity)
    {
        Instance = this;

        currentActivity = appActivity;
        mProjectionManager = (MediaProjectionManager) currentActivity.getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);

    }

    public void SetupScreen ()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        currentActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;


        if (ActivityCompat.checkSelfPermission(currentActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(currentActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(currentActivity, new String[]
                            {
                                    Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                    0);
        }
        else
        {
            initRecorder();
            prepareRecorder();
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
    }

    public void ActivityResult(int resultCode, Intent data)
    {
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        StartRecording();
//        onToggleScreenShare();
    }

    public String getCurSysDate()
    {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }

    public String getFilePath()
    {
        final String directory = Environment.getExternalStorageDirectory() + File.separator + "Recordings";
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
        {
            Toast.makeText(currentActivity, "Failed to get External Storage", Toast.LENGTH_SHORT).show();
            return null;
        }
        final File folder = new File(directory);
        boolean success = true;
        if (!folder.exists())
        {
            folder.mkdir();
        }
        String filePath;
        if (success)
        {
            String videoName = ("capture_" + getCurSysDate() + ".mp4");
            filePath = directory + File.separator + videoName;
        }
        else
        {
            Toast.makeText(currentActivity, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show();
            return null;
        }
        return filePath;
    }

    private void initRecorder()
    {
        if (mMediaRecorder == null)
        {
            mMediaRecorder = new MediaRecorder();
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setOutputFile(getFilePath());
        }
    }

    private void prepareRecorder()
    {
        try
        {
            mMediaRecorder.prepare();
        }
        catch (IllegalStateException | IOException e)
        {
            e.printStackTrace();
            currentActivity.finish();
        }
    }


    public void GetPermission()
    {
        SetupScreen();
        if (mMediaProjection == null)
        {
            currentActivity.startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }
    }

    private VirtualDisplay createVirtualDisplay()
    {
        return mMediaProjection.createVirtualDisplay("VideoChatActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    public void onToggleScreenShare()
    {
        if (!recording)
        {
            StartRecording();
            recording = true;
            VideoChatActivity.ShowToast("Recording Started", currentActivity.getApplicationContext());
        }
        else
        {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");
            VideoChatActivity.ShowToast("Recording Stopped", currentActivity.getApplicationContext());
            stopScreenSharing();
            recording = false;
        }
    }

    public void StartRecording ()
    {
        if(recording)
            return;

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        inited = true;
        recording = true;
        VideoChatActivity.ShowToast("Recording Started", currentActivity.getApplicationContext());
    }

    public void PauseRecording ()
    {
        if(!recording && !inited)
            return;

        mMediaRecorder.pause();
        recording = false;
    }

    public void ResumeRecording ()
    {
        if(!recording && inited)
        {
            mMediaRecorder.resume();
            recording = true;
        }
    }

    public void StopRecording ()
    {
        if(!recording)
            return;

        if (recording)
        {
            recording = false;
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");
        }
        mMediaProjection = null;
        mMediaRecorder.release();
        mMediaRecorder = null;
        if(validateMicAvailability())
            Log.d(TAG, "MIC Available");
        else
            Log.d(TAG, "MIC not available ");
        stopScreenSharing();
        recording = false;
        inited = false;

        VideoChatActivity.ShowToast("Recording Stopped", currentActivity.getApplicationContext());
    }

    private class MediaProjectionCallback extends MediaProjection.Callback
    {
        @Override
        public void onStop()
        {
//            if (recording)
//            {
//                recording = false;
//                mMediaRecorder.stop();
//                mMediaRecorder.reset();
//                Log.v(TAG, "Recording Stopped");
//            }
//            mMediaProjection = null;
//            mMediaRecorder.release();
//            mMediaRecorder = null;
//            if(validateMicAvailability())
//                Log.d(TAG, "MIC Available");
//            else
//                Log.d(TAG, "MIC not available ");
//            stopScreenSharing();
//            Log.i(TAG, "MediaProjection Stopped");
        }
    }

    private void stopScreenSharing()
    {
        if (mVirtualDisplay == null)
        {
            return;
        }
        mVirtualDisplay.release();
        //mMediaRecorder.release();
    }
}

