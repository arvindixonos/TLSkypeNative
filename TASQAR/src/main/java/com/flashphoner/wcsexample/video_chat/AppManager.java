package com.flashphoner.wcsexample.video_chat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.internal.NavigationMenuItemView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
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
import static android.view.View.VISIBLE;

public class AppManager extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NavigationMenuItemView.OnClickListener
{
    public static String TAG = "TLSKYPE";

    public static AppManager Instance = null;

    private static final int ALL_PERMISSIONS = 555;
    private     boolean  allPermissionsGiven = false;
    private     boolean  profilePicPresent = false;
    private     boolean  drawerOpen = false;
    private     boolean  pinMode = false;
    private String password = "0000";
    private String passwordFilling = "0000";

    private ArCoreApk.InstallStatus installStatus = ArCoreApk.InstallStatus.INSTALL_REQUESTED;
    private boolean installRequested = false;

    private LoginUIHandler loginUIHandler;

    //lobby Elements
    private CallHistoryDatabaseHelper callHistoryDatabaseHelper;
    private ConstraintLayout mST_SettingsScreen;
    private DrawerLayout drawer;
    private Switch ST_PasswordToggle;
    private LoginDatabaseHelper loginDB;

    public  String userID;
    //lobby Elements

    public static AppManager getInstance()
    {
        if(Instance == null)
        {
            return new AppManager();
        }
        return Instance;
    }

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

    Session session = null;
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Instance = this;

        final Thread arCoreCheckThread = new Thread(() ->
        {
            Log.d(VideoChatActivity.TAG, " VideoChatActivity HERE ");

            CheckArCoreAvailablity();
        });

        callHistoryDatabaseHelper = new CallHistoryDatabaseHelper(getApplicationContext());
        loginDB = new LoginDatabaseHelper(getApplicationContext());
        Cursor data = loginDB.showData();
        data.moveToFirst();

//        if (session == null)
//        {
//            Exception exception = null;
//            String message = null;
//            try {
//                // Create the session.
//                session = new Session(/* context= */ getApplicationContext());
//            } catch (UnavailableApkTooOldException e) {
//                message = "Please update ARCore";
//                exception = e;
//            } catch (UnavailableSdkTooOldException e) {
//                message = "Please update this app";
//                exception = e;
//            } catch (UnavailableArcoreNotInstalledException e) {
//                message = "Failed to create AR session";
//                exception = e;
//
//                arCoreCheckThread.start();
//            }
//            catch (Exception e) {
//
//                VideoCapturerAndroid.arCorePresent = false;
//
//                message = "Please update this app";
//                exception = e;
//            }
//        }

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

        String message = getIntent().getStringExtra("PIC");
        if(message == null)
        {
            setContentView(R.layout.activity_login);
            loginUIHandler = new LoginUIHandler(this , this);
        }
        else if(message.equals("ABSENT"))
        {
            SetupUserScreen(false);
        }
        else if(message.equals("PRESENT"))
        {
            SetupUserScreen(true);
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
    public void ClickFunction (char digit)
    {
        char[] pwd = passwordFilling.toCharArray();
        pwd[count] = digit;
        passwordFilling = new String(pwd);
        Log.d(TAG, passwordFilling);

        count += 1;
        if(count == 4)
        {
            if (allPermissionsGiven)
            {
                if(passwordFilling.equals(password))
                {
                    SetupUserScreen(profilePicPresent);
                }
                else
                {
                    Toast.makeText(this.getApplicationContext(), "Password Incorrect", Toast.LENGTH_LONG).show();
                    count = 0;
                }
            }
            else
            {
                count = 0;
                RequestAllPermissions();
            }
        }
    }


    public void ChangePIN(View v)
    {
        Log.d(TAG, "CHANGE PIN");
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(AppManager.this);
        View view = getLayoutInflater().inflate(R.layout.get_pin, null);

        EditText PINText = view.findViewById(R.id.editText);
        Button submitButton = view.findViewById(R.id.submitButton);

        mBuilder.setView(view);
        AlertDialog dialog = mBuilder.create();
        dialog.show();

        submitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                password = PINText.getText().toString();
                loginDB = new LoginDatabaseHelper(getApplicationContext());
                loginDB.AddPINData(password);
                dialog.dismiss();
            }
        });
    }


    public void SetupPasswordScreen (boolean picPresent)
    {
        loginDB = new LoginDatabaseHelper(getApplicationContext());
        Cursor data = loginDB.showData();
        data.moveToFirst();
        password = data.getString(9);
        profilePicPresent = picPresent;
        setContentView(R.layout.activity_pin);
    }

    public void SetupUserScreen (boolean profilePicPresent)
    {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this.getApplicationContext());

        if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED)
        {
            VideoCapturerAndroid.arCorePresent = true;
        }
        else
        {
            VideoCapturerAndroid.arCorePresent = false;
        }

        setContentView(R.layout.drawer_callscreen);
        Cursor data = callHistoryDatabaseHelper.showData();
        if(data.getCount() > 0)
        {
            data.moveToFirst();
            do
            {
                SpawnHistoryButton(data.getString(1), data.getString(2), data.getString(3), data.getString(4), data.getString(5));
            }while(data.moveToNext());
        }

        if(loginUIHandler == null)
        {
            mST_SettingsScreen = findViewById(R.id.SettingsScreen);
            SetPasswordToggle();
        }
        else
        {
            loginUIHandler.SetPasswordToggle();
        }

        final Button callUserButton = findViewById(R.id.buttonCall);
        callUserButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ChangeActivity("participantID");
            }
        });
    }

    public void SetPasswordToggle ()
    {
        drawer = findViewById(R.id.CommonDrawer);
        android.support.v7.app.ActionBarDrawerToggle toggle = new android.support.v7.app.ActionBarDrawerToggle
                (this, drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
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

        NavigationView navigationView = findViewById(R.id.call_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mST_SettingsScreen = findViewById(R.id.SettingsScreen);

        ST_PasswordToggle = findViewById(R.id.ST_PasswordToggle);
        Cursor data = loginDB.showData();
        data.moveToFirst();
        String pinMode = data.getString(8);
        ST_PasswordToggle.setChecked(pinMode.equals("ENABLED"));

        ST_PasswordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {

            }
        });
    }


    public void SpawnHistoryButton (String userID, String name, String role, String date, String duration)
    {
        LinearLayout parent = findViewById(R.id.ButtonInflater);
        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.history_element_button, null);
        parent.addView(view);
        HistoryButton historyButton = (HistoryButton) view;
        historyButton.Initialise(userID, name, date, duration, role);
    }

    void ChangeActivity (String participantID)
    {
        Intent intent = new Intent(this, VideoChatActivity.class);
        if(profilePicPresent)
        {
            intent.putExtra("PIC", "PRESENT");
        }
        else
        {
            intent.putExtra("PIC", "ABSENT");
        }
        intent.putExtra("ROOMNAME", userID + "_CALL_" + participantID);
        intent.putExtra("USERID", userID);
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
        if(mST_SettingsScreen != null)
        {
            if (mST_SettingsScreen.getVisibility() == VISIBLE)
            {
                loginDB.AddSpecificData("PIN_MODE", ST_PasswordToggle.isChecked() ? "ENABLED" : "DISABLED");
                Log.d(TAG, "BACKPRESSED" + ST_PasswordToggle.isChecked());
                mST_SettingsScreen.setVisibility(View.GONE);

                return;
            }
        }

        if(session != null) {
            session.pause();
        }

        if(loginUIHandler != null)
            loginUIHandler.backKey();
        else
            System.exit(0);
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
    public void onClick(View view) {

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