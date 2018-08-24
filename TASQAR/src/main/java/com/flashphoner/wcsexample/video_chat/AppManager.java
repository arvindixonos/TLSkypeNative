package com.flashphoner.wcsexample.video_chat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.webrtc.VideoCapturerAndroid;

import java.util.Random;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class AppManager extends AppCompatActivity
{
    public static String TAG = "TLSKYPE";

    private static final int ALL_PERMISSIONS = 555;
    private     boolean  allPermissionsGiven = false;

    private ArCoreApk.InstallStatus installStatus = ArCoreApk.InstallStatus.INSTALL_REQUESTED;
    private boolean installRequested = false;

    private LoginUIHandler loginUIHandler;

    public void ShowToast(final String message, final Context applicationContext)
    {
        this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    void CheckArCoreAvailablity()
    {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this.getApplicationContext());

        if(availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE || availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD)
        {
            VideoCapturerAndroid.arCorePresent = false;

            ShowToast("AR CORE NOT SUPPORTED IN THIS DEVICE", this.getApplicationContext());
        }
        else if(availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED)
        {
            try
            {
                if(!installRequested)
                {
                    ShowToast("INSTALL AR CORE", getApplicationContext());

                    installStatus = ArCoreApk.getInstance().requestInstall(AppManager.this, true);
                    installRequested = true;
                }

                try {
                    Thread.sleep(100);

                    CheckArCoreAvailablity();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } catch (UnavailableDeviceNotCompatibleException e) {
                e.printStackTrace();
            } catch (UnavailableUserDeclinedInstallationException e) {
                e.printStackTrace();
            }
        }
        else if(availability == ArCoreApk.Availability.SUPPORTED_INSTALLED)
        {
//            ShowToast("AR CORE INSTALLED", getApplicationContext());

            VideoCapturerAndroid.arCorePresent = true;
        }
        else
        {
            try {
                Thread.sleep(100);

                CheckArCoreAvailablity();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        final Thread arCoreCheckThread = new Thread(() ->
        {
            Log.d(VideoChatActivity.TAG, " VideoChatActivity HERE ");

            CheckArCoreAvailablity();
        });

        Session session = null;

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                // Create the session.
                session = new Session(/* context= */ getApplicationContext());
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableArcoreNotInstalledException e) {
                message = "Failed to create AR session";
                exception = e;

                arCoreCheckThread.start();
            }
            catch (Exception e) {

                VideoCapturerAndroid.arCorePresent = false;

                message = "Please update this app";
                exception = e;
            }

        }

        if( ActivityCompat.checkSelfPermission(AppManager.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(AppManager.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(AppManager.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {

            RequestAllPermissions();
        }
        else
        {
            allPermissionsGiven = true;
        }

        String message = getIntent().getStringExtra("KEY");
        Log.d(TAG, "Message is " + message);
        if(message != null)
        {
            Log.d(TAG, "Already Logged in");
            SetupUserScreen();
        }
        else
        {
            Log.d(TAG, "not Logged in");
            setContentView(R.layout.activity_login);
            loginUIHandler = new LoginUIHandler(this , this);
            Log.d(TAG, "not Logged in a second time");
        }
    }

    public void RequestAllPermissions()
    {
        ActivityCompat.requestPermissions(AppManager.this,
                new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                ALL_PERMISSIONS);
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case ALL_PERMISSIONS:
                int permissionsCount = grantResults.length;
                allPermissionsGiven = true;
                for(int i = 0; i < permissionsCount; i++)
                {
                    allPermissionsGiven = allPermissionsGiven & (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
                break;
        }
    }

    private int count = 0;
    public void ClickFunction (View v)
    {
        count += 1;
        if(count == 4) {
            if (allPermissionsGiven) {
                SetupUserScreen();
            }
            else
            {
                count = 0;
                RequestAllPermissions();
            }
        }
    }

    public void SetupUserScreen () {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this.getApplicationContext());

        if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED)
        {
            VideoCapturerAndroid.arCorePresent = true;
        }
        else
        {
            VideoCapturerAndroid.arCorePresent = false;
        }

        setContentView(R.layout.activity_user);
        final Button callUserButton = findViewById(R.id.buttonCall);
        callUserButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ChangeActivity();
            }
        });
    }

    void ChangeActivity ()
    {
        Intent intent = new Intent(this, VideoChatActivity.class);
        intent.putExtra("MIN", "FALSE");
        this.finish();
        startActivity(intent);
    }

    public void ShowToast(String message)
    {
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
        toast.show();
    }


    @Override
    public void onBackPressed()
    {
        loginUIHandler.backKey();
    }

//    MySurfaceView mySurfaceView;
//
//    /** Called when the activity is first created. */
//    @Override
//    public void onCreate(Bundle savedInstanceState)
//    {
//        super.onCreate(savedInstanceState);
//        mySurfaceView = new MySurfaceView(this);
//        setContentView(mySurfaceView);
//    }
//
//    @Override
//    protected void onResume()
//    {
//        // TODO Auto-generated method stub
//        super.onResume();
//        mySurfaceView.onResumeMySurfaceView();
//    }
//
//    @Override
//    protected void onPause()
//    {
//        // TODO Auto-generated method stub
//        super.onPause();
//        mySurfaceView.onPauseMySurfaceView();
//    }
//
//    class MySurfaceView extends SurfaceView implements Runnable{
//
//        Thread thread = null;
//        SurfaceHolder surfaceHolder;
//        volatile boolean running = false;
//
//        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        Random random;
//
//        public MySurfaceView(Context context)
//        {
//            super(context);
//            // TODO Auto-generated constructor stub
//            surfaceHolder = getHolder();
//            random = new Random();
//        }
//
//        public void onResumeMySurfaceView()
//        {
//            running = true;
//            thread = new Thread(this);
//            thread.start();
//        }
//
//        public void onPauseMySurfaceView()
//        {
//            boolean retry = true;
//            running = false;
//            while(retry){
//                try {
//                    thread.join();
//                    retry = false;
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        @Override
//        public void run()
//        {
//            // TODO Auto-generated method stub
//            while(running)
//            {
//                if(surfaceHolder.getSurface().isValid())
//                {
//                    Canvas canvas = surfaceHolder.lockCanvas();
//                    //... actual drawing on canvas
//
//                    paint.setStyle(Paint.Style.STROKE);
//                    paint.setStrokeWidth(10);
//
//                    int w = canvas.getWidth();
//                    int h = canvas.getHeight();
//                    int x = random.nextInt(w-1);
//                    int y = random.nextInt(h-1);
//                    int r = random.nextInt(255);
//                    int g = random.nextInt(255);
//                    int b = random.nextInt(255);
//                    paint.setColor(0xff000000 + (r << 16) + (g << 8) + b);
//                    canvas.drawPoint(x, y, paint);
//
//                    surfaceHolder.unlockCanvasAndPost(canvas);
//                }
//            }
//        }
//    }
}
