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
import android.text.Layout;
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
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;

import com.flashphoner.fpwcsapi.Flashphoner;
import com.flashphoner.fpwcsapi.bean.Connection;
import com.flashphoner.fpwcsapi.bean.Data;
import com.flashphoner.fpwcsapi.room.Message;
import com.flashphoner.fpwcsapi.room.Participant;
import com.flashphoner.fpwcsapi.room.Room;
import com.flashphoner.fpwcsapi.room.RoomEvent;
import com.flashphoner.fpwcsapi.room.RoomManager;
import com.flashphoner.fpwcsapi.room.RoomManagerEvent;
import com.flashphoner.fpwcsapi.room.RoomManagerOptions;
import com.flashphoner.fpwcsapi.room.RoomOptions;
import com.flashphoner.fpwcsapi.session.RestAppCommunicator;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.webrtc.VideoCapturerAndroid;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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

    //lobby UI Elements
    private CallHistoryDatabaseHelper   callHistoryDatabaseHelper;
    private ConstraintLayout            mST_SettingsScreen;
    private DrawerLayout                drawer;
    private Switch                      ST_PasswordToggle;
    private LoginDatabaseHelper         loginDB;

    public  String userID;
    //lobby UI Elements

    //lobby Functional Elements
    private RoomManager roomManager;
    private Room        room;
    private Thread  getUserThread = null;

    private String wcsURL = "ws://123.176.34.172:8080";
    //lobby Functional Elements

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

    public void SetupLobby ()
    {
        if(loginDB == null)
            loginDB = new LoginDatabaseHelper(getApplicationContext());

        Cursor data = loginDB.showData();
        data.moveToFirst();
        userID = "User_" + data.getString(3);

        RoomManagerOptions roomManagerOptions = new RoomManagerOptions(wcsURL, userID);
        roomManager = Flashphoner.createRoomManager(roomManagerOptions);

        roomManager.on(new RoomManagerEvent()
        {
            @Override
            public void onConnected(Connection connection)
            {
                Log.d(TAG, "Connected " + userID);
                RoomOptions roomOptions = new RoomOptions();
                roomOptions.setName("TLLobby");
                room = roomManager.join(roomOptions);

                room.on(new RoomEvent()
                {
                    @Override
                    public void onState(Room room)
                    {
                        Log.d(TAG, "RoomName : " + room.getName() + " onState");
                        if(room.getParticipants().size() == 0)
                        {
                            Log.d(TAG, "A ForeScout");
                        }
                        else
                        {
                            ListOnlineUsers();
                        }
                    }

                    @Override
                    public void onJoined(Participant participant)
                    {
                        Log.d(TAG, "Joined : " + participant.getName());
                        ListOnlineUsers();
                    }

                    @Override
                    public void onLeft(Participant participant)
                    {
                        Log.d(TAG, "Left : " + participant.getName());
                        ListOnlineUsers();
                    }

                    @Override
                    public void onPublished(Participant participant)
                    {

                    }

                    @Override
                    public void onFailed(Room room, String s)
                    {

                    }

                    @Override
                    public void onMessage(Message message)
                    {
                        Log.d(TAG, "RemoteMessage : " + message.getText());
                        String msg = message.getText();
                        msg = msg.replace("CALL:-", "");
                        ChangeActivity(msg.split("_CALL_")[0], false);
                    }
                });
            }

            @Override
            public void onDisconnection(Connection connection)
            {

            }
        });
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
        SetupButtons();
//        ListCallHistory();

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
                ChangeActivity("participant", true);
            }
        });

        SetupLobby();
    }

    Button mHistoryButton;
    Button mOnlineButton;
    ScrollView mHistorylayout;
    ScrollView mOnlineLayout;
    private void SetupButtons()
    {
        mHistoryButton = findViewById(R.id.CallHistoryButton);
        mOnlineButton = findViewById(R.id.OnlineButton);
        mHistorylayout = findViewById(R.id.HistoryButtonInflater);
        mOnlineLayout = findViewById(R.id.OnlineButtonInflater);

        mHistoryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mHistorylayout.setVisibility(VISIBLE);
                mOnlineLayout.setVisibility(View.GONE);
                ListCallHistory();
            }
        });

        mOnlineButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mHistorylayout.setVisibility(View.GONE);
                mOnlineLayout.setVisibility(VISIBLE);
                ListOnlineUsers();
            }
        });
    }

    private void ListCallHistory ()
    {
        LinearLayout parent = findViewById(R.id.HistoryInflater);
        parent.removeAllViewsInLayout();

        Cursor data = callHistoryDatabaseHelper.showData();
        if(data.getCount() > 0)
        {
            data.moveToLast();
            do
            {
                SpawnUserButton(data.getString(1), data.getString(3), data.getString(4), data.getString(5), data.getString(6), data.getString(2), true);
            }
            while(data.moveToPrevious());
        }
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

    public void SpawnUserButton (String userID, String name, String role, String date, String duration, String email, boolean isHistory)
    {
        LinearLayout parent;

        if(isHistory)
            parent = findViewById(R.id.HistoryInflater);
        else
            parent = findViewById(R.id.OnlineInflater);

        ViewGroup view = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.history_element_button, null);
        parent.addView(view);
        HistoryButton historyButton = (HistoryButton) view;
        historyButton.Initialise(userID, name, date, duration, role, email);
    }

    void ChangeActivity (String participantID, boolean isSender)
    {
        if(isSender)
            participantID = "User_" + participantID;
        String roomName;
        if(isSender)
        {
            roomName = userID + "_CALL_" + participantID;
        }
        else
        {
            roomName = participantID + "_CALL_" + userID;
        }

        if(isSender)
            SendMessageToPerson(participantID, roomName);

        Intent intent = new Intent(this, VideoChatActivity.class);
        if(profilePicPresent)
        {
            intent.putExtra("PIC", "PRESENT");
        }
        else
        {
            intent.putExtra("PIC", "ABSENT");
        }
        intent.putExtra("ROOMNAME", roomName);
        this.finish();
        startActivity(intent);
    }

    private void SendMessageToPerson (String particpantName, String Message)
    {
        for (Participant participant:room.getParticipants())
        {
            Log.d(TAG, particpantName);
            if(participant.getName().equals(particpantName))
            {
                Log.d(TAG, "Found participant");
                participant.sendMessage("CALL:-" + Message);
            }
        }
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

        return false;
    }

    @Override
    public void onClick(View view)
    {

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
                    final String[] userData = value.split(";");
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AssembleUser(userData, emailID);
                        }
                    });
                }
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
        });
        getUserThread.start();
    }

    public void onDestroy()
    {
        super.onDestroy();
        room.leave(null);
        roomManager.disconnect();
    }

    private void ListOnlineUsers ()
    {
        LinearLayout parent = findViewById(R.id.OnlineInflater);
        parent.removeAllViewsInLayout();
        Thread listUserThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Collection<Participant> participants = room.getParticipants();
                for (Participant participant : participants)
                {
                    GetUserDetails(participant.getName().replace("User_", ""));

                    while(getUserThread.isAlive())
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
                }
            }
        });

        listUserThread.start();

    }

    private void AssembleUser (String[] serverData, String email)
    {
        SpawnUserButton (serverData[0], serverData[1], serverData[5], "", "",email, false);
    }

    public String GetDate()
    {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY_hh-mm-ss");

        return dateFormat.format(date);
    }
}