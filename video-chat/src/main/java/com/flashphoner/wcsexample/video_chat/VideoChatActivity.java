package com.flashphoner.wcsexample.video_chat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.flashphoner.fpwcsapi.Flashphoner;
import com.flashphoner.fpwcsapi.bean.Connection;
import com.flashphoner.fpwcsapi.bean.Data;
import com.flashphoner.fpwcsapi.bean.StreamStatus;
import com.flashphoner.fpwcsapi.layout.PercentFrameLayout;
import com.flashphoner.fpwcsapi.room.Message;
import com.flashphoner.fpwcsapi.room.Participant;
import com.flashphoner.fpwcsapi.room.Room;
import com.flashphoner.fpwcsapi.room.RoomEvent;
import com.flashphoner.fpwcsapi.room.RoomManager;
import com.flashphoner.fpwcsapi.room.RoomManagerEvent;
import com.flashphoner.fpwcsapi.room.RoomManagerOptions;
import com.flashphoner.fpwcsapi.room.RoomOptions;
import com.flashphoner.fpwcsapi.session.RestAppCommunicator;
import com.flashphoner.fpwcsapi.session.Stream;
import com.flashphoner.fpwcsapi.session.StreamStatusEvent;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example for two way video chat.
 * Can be used to participate in video chat for two participants on Web Call Server.
 */
public class VideoChatActivity extends AppCompatActivity {

    public static String TAG = "TLSKYPE";

    private static final int PUBLISH_REQUEST_CODE = 100;
    private static final int PICKFILE_RESULT_CODE = 200;

    // UI references.
    private EditText mWcsUrlView;
    private EditText mLoginView;
    private TextView mConnectStatus;
    private ImageButton mConnectButton;
    private ImageButton mFileUploadButton;
    private EditText mJoinRoomView;
    private TextView mJoinStatus;
    private Button mJoinButton;

    private SeekBar mParticipantVolume;

    private TextView mParticipantName;

    private TextView mPublishStatus;
    private Button mPublishButton;
    private Switch mMuteAudio;
    private Switch mMuteVideo;
    private TextView mMessageHistory;
    private EditText mMessage;
    private Button mSendButton;

    /**
     * RoomManager object is used to manage connection to server and video chat room.
     */
    private RoomManager roomManager;

    /**
     * Room object is used for work with the video chat room, to which the user is joined.
     */
    private Room room;

    private SurfaceViewRendererCustom localRenderer;

    private Queue<ParticipantView> freeViews = new LinkedList<>();
    private Map<String, ParticipantView> busyViews = new ConcurrentHashMap<>();

    private Stream stream;

    private EditText loginName;

    private boolean   permissionGiven = false;

    public  static   Context    applicationContext;

    private static VideoChatActivity Instance;

    public boolean connected = false;

    public static void ShowToast(String message)
    {
        Toast toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG);
        toast.show();
    }

    public static VideoChatActivity getInstance(){
        if(Instance == null){
            Instance = new VideoChatActivity();
        }
        return Instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videochat);

//        DownloadFile("5EN.mp3");

//        //mute audio
//        AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
//        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
//        amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
//        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
//        amanager.setStreamMute(AudioManager.STREAM_RING, true);
//        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

        Instance = this;

        if(ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(VideoChatActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    123);
        }

        applicationContext = getApplicationContext();

//        ftpManager.execute();

        localRenderer = (SurfaceViewRendererCustom) findViewById(R.id.local_video_view);
        PercentFrameLayout localRenderLayout = (PercentFrameLayout) findViewById(R.id.local_video_layout);
        localRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localRenderer.setMirror(true);
        localRenderer.requestLayout();

        SurfaceViewRendererCustom remote1Render = (SurfaceViewRendererCustom) findViewById(R.id.remote_video_view);
        PercentFrameLayout remote1RenderLayout = (PercentFrameLayout) findViewById(R.id.remote_video_layout);
        remote1Render.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        remote1Render.setMirror(false);
        remote1Render.requestLayout();

        freeViews.add(new ParticipantView(remote1Render, mParticipantName));

        /**
         * Initialization of the API.
         */
        Flashphoner.init(this);

        mConnectButton = (ImageButton) findViewById(R.id.TLconnect_button);
        mFileUploadButton = (ImageButton) findViewById(R.id.TLfileupload_button);

        loginName = (EditText) findViewById(R.id.loginName);
        loginName.setText(Build.MODEL);

        final String wcsURL = "ws://123.176.34.172:8080";
        final String roomName = "TLSkypeRoom";


        /**
         * Connection to server will be established when Connect button is clicked.
         */

        mFileUploadButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent fileintent = new Intent(Intent.ACTION_GET_CONTENT);
                fileintent.setType("gagt/sdf");
                try {
                    startActivityForResult(fileintent, PICKFILE_RESULT_CODE);
                } catch (ActivityNotFoundException e) {
                    Log.e("tag", "No activity can handle picking a file. Showing alternatives.");
                }
            }
        });

        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                {
                    if(connected)
                        return;

                    if(loginName.getText().toString().length() == 0)
                    {
                        ShowToast("Login Name is empty");
                        return;
                    }

                    permissionGiven = false;
                    /**
                     * The connection options are set.
                     * WCS server URL and user name are passed when RoomManagerOptions object is created.
                     */

                    RoomManagerOptions roomManagerOptions = new RoomManagerOptions(wcsURL, loginName.getText().toString());

                    /**
                     * RoomManager object is created with method createRoomManager().
                     * Connection session is created when RoomManager object is created.
                     */
                    roomManager = Flashphoner.createRoomManager(roomManagerOptions);

                    /**
                     * Callback functions for connection status events are added to make appropriate changes in controls of the interface when connection is established and closed.
                     */

                    roomManager.on(new RoomManagerEvent() {
                        @Override
                        public void onConnected(final Connection connection)
                        {
                            connected = true;

                            RoomOptions roomOptions = new RoomOptions();
                            roomOptions.setName(roomName);

                            /**
                             * The participant joins a video chat room with method RoomManager.join().
                             * RoomOptions object is passed to the method.
                             * Room object is created and returned by the method.
                             */
                            room = roomManager.join(roomOptions);

                            if(ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED )
                            {
                                permissionGiven = true;
                            }

                            if(!permissionGiven) {
                                ActivityCompat.requestPermissions(VideoChatActivity.this,
                                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                                        PUBLISH_REQUEST_CODE);
                            }

                            /**
                             * Callback functions for events occurring in video chat room are added.
                             * If the event is related to actions performed by one of the other participants, Participant object with data of that participant is passed to the corresponding function.
                             */
                            room.on(new RoomEvent() {
                                @Override
                                public void onState(final Room room) {

                                    Log.d(TAG, "ON STATE");

                                    if(permissionGiven && stream == null) {
                                        stream = room.publish(localRenderer);
                                        stream.muteAudio();
                                    }
                                    /**
                                     * Callback function for stream status change is added to make appropriate changes in controls of the interface when stream is being published.
                                     */

                                    Log.i(TAG, "Permission has been granted by user");

                                    /**
                                     * After joining, Room object with data of the room is received.
                                     * Method Room.getParticipants() is used to check the number of already connected participants.
                                     * The method returns collection of Participant objects.
                                     * The collection size is determined, and, if the maximum allowed number (in this case, three) has already been reached, the user leaves the room with method Room.leave().
                                     */

                                    /**
                                     * Iterating through the collection of the other participants returned by method Room.getParticipants().
                                     * There is corresponding Participant object for each participant.
                                     */
                                    for (final Participant participant : room.getParticipants()) {
                                        /**
                                         * A player view is assigned to each of the other participants in the room.
                                         */
                                        final ParticipantView participantView = freeViews.poll();
                                        if (participantView != null) {
                                            busyViews.put(participant.getName(), participantView);

                                            /**
                                             * Playback of the stream being published by the other participant is started with method Participant.play().
                                             * SurfaceViewRenderer to be used to display the video stream is passed when the method is called.
                                             */
                                            participant.play(participantView.surfaceViewRenderer);
                                        }
                                    }
                                }

                                @Override
                                public void onJoined(final Participant participant) {

                                    Log.d(TAG, "ON JOINED " + participant.getName());
                                    /**
                                     * When a new participant joins the room, a player view is assigned to that participant.
                                     */
                                    final ParticipantView participantView = freeViews.poll();
                                    if (participantView != null) {
                                        busyViews.put(participant.getName(), participantView);
                                    }
                                }

                                @Override
                                public void onLeft(final Participant participant) {
                                    /**
                                     * When one of the other participants leaves the room, player view assigned to that participant is freed.
                                     */
                                    final ParticipantView participantView = busyViews.remove(participant.getName());
                                    if (participantView != null) {
                                        freeViews.add(participantView);
                                    }
                                }

                                @Override
                                public void onPublished(final Participant participant) {

                                    Log.d(TAG, "ON onPublished " + participant.getName());

                                    /**
                                     * When one of the other participants starts publishing, playback of the stream published by that participant is started.
                                     */
                                    final ParticipantView participantView = busyViews.get(participant.getName());
                                    if (participantView != null) {
                                        participant.play(participantView.surfaceViewRenderer);
                                    }
                                }

                                @Override
                                public void onFailed(Room room, final String info) {
                                    connected = false;
                                    room.leave(null);
                                }

                                @Override
                                public void onMessage(final Message message) {
                                    /**
                                     * When one of the participants sends a text message, the received message is added to the messages log.
                                     */

                                    if(message.getFrom() == loginName.getText().toString())
                                        return;

                                    Log.d(TAG, "ON MESSAGE " + message.getText());

                                    String messageReceived = message.getText();

                                    if(messageReceived.contains(":FU-"))
                                    {
                                        DownloadFile(messageReceived.replace(":FU-/", ""));
                                    }
                                }
                            });
                        }

                        @Override
                        public void onDisconnection(final Connection connection) {
                            connected = false;
                            Log.d(TAG, "ON DISCCONEASd");
                        }
                    });
                }
            }
        });
    }

    public  void SendMessage(String message)
    {
        if(room == null)
            return;

        for (Participant participant : room.getParticipants()) {
            participant.sendMessage(message);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
        if (data == null)
            return;
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK)
                {
                    ContentResolver contentResolver = getApplicationContext().getContentResolver();
                    try {
                        InputStream fileInputStream = contentResolver.openInputStream(data.getData());
                        String FilePath = data.getData().getPath();
                        UploadFile(FilePath, fileInputStream);
                        Log.d(TAG, FilePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    public void UploadFile(String filePath, InputStream inputStream)
    {
        FTPManager  ftpManager = new FTPManager();

        if(ftpManager.running)
        {
            ShowToast("FTP Manager Running Already");
            return;
        }

        ftpManager.fileInputStream = inputStream;
        ftpManager.uploadORdownload = 1;
        ftpManager.applicationContext = getApplicationContext();
        ftpManager.filePath = filePath;
        ftpManager.execute();
    }

    public void DownloadFile(String fileName)
    {
        FTPManager  ftpManager = new FTPManager();

        if(ftpManager.running)
        {
            ShowToast("FTP Manager Running Already");
            return;
        }

        String filePath = "/sdcard/ReceivedFiles/" + fileName;
        ftpManager.uploadORdownload = 2;
        ftpManager.applicationContext = getApplicationContext();
        ftpManager.filePath = filePath;
        ftpManager.execute();
    }

    public void OpenFile(String filePath){

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String fileExt = MimeTypeMap.getFileExtensionFromUrl(filePath);
        Uri fileProviderURI = FileProvider.getUriForFile(VideoChatActivity.this, BuildConfig.APPLICATION_ID + ".provider",
                new File(filePath));

        newIntent.setDataAndType(fileProviderURI, MimeTypes.getMimeType(fileExt));
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            this.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            ShowToast("No handler for this type of file.");
        }
    }

    private class ParticipantView {

        SurfaceViewRenderer surfaceViewRenderer;
        TextView login;

        public ParticipantView(SurfaceViewRenderer surfaceViewRenderer, TextView login) {
            this.surfaceViewRenderer = surfaceViewRenderer;
            this.login = login;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PUBLISH_REQUEST_CODE: {
                if (grantResults.length == 0 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                        grantResults[1] != PackageManager.PERMISSION_GRANTED) {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    /**
                     * Stream is created and published with method Room.publish().
                     * SurfaceViewRenderer to be used to display video from the camera is passed to the method.
                     */

                    if(stream == null && room != null) {
                        stream = room.publish(localRenderer);
                        stream.muteAudio();
                    }
                    /**
                     * Callback function for stream status change is added to make appropriate changes in controls of the interface when stream is being published.
                     */

                    Log.i(TAG, "Permission has been granted by user");
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (roomManager != null) {
            roomManager.disconnect();
        }
    }

}

