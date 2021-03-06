package com.takeleap.tasqar;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
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
import com.flashphoner.fpwcsapi.session.Stream;
import com.flashphoner.fpwcsapi.webrtc.MediaDevice;
import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;

import com.takeleap.tasqar.BuildConfig;
import com.takeleap.tasqar.R;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.SessionPausedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.obsez.android.lib.filechooser.tool.DirAdapter;

import org.android.opensource.libraryyuv.Libyuv;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturerAndroid;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * Example for two way video chat.
 * Can be used to participate in video chat for two participants on Web Call Server.
 */
public class VideoChatActivity extends AppCompatActivity implements GLSurfaceView.Renderer, android.hardware.Camera.PreviewCallback
{
    private static final int PUBLISH_REQUEST_CODE = 100;
    private static final int PICKFILE_RESULT_CODE = 200;
    private static final int FILEWRITE_REQUEST_CODE = 300;

    public static String TAG = "TLSKYPE";
    public static Context applicationContext;
    private static VideoChatActivity Instance;
    public boolean connected = false;
    public boolean isLandscape = false;
    public boolean isAdmin = false;
    public boolean isODG;
    public FloatingActionButton mFileUploadButton;
    String wcsURL = "ws://192.168.1.72:8080";
//    String wcsURL = "ws://123.176.34.172:8080";
//    String roomName = "room-cd696c";
    String roomName = "NEWFTP";
//    UI references.

    private Thread ftpThread;
    private ImageButton mConnectButton;
    private Button mPlaneOrPointButton;
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
    private Handler mHandler = new Handler();
    private Handler nHandler = new Handler();
    public  Intent  currentActivityIntent;
    private boolean cancelled;
    public MainUIHandler uiHandler;
    public ScreenRecorder screenRecorder;
    private LoginDatabaseHelper loginDB;


    private ParticipantView participantView;
    private Participant     publishedParticipant;

    public String android_id = "";

    /**
     * RoomManager object is used to manage connection to server and video chat room.
     */
    private RoomManager roomManager;
    /**
     * Room object is used for work with the video chat room, to which the user is joined.
     */
    private Room room;
    private SurfaceViewRendererCustom localRenderer;
    private SurfaceViewRendererCustom remoteRenderer;
    public Stream stream;
    public Stream remoteStream;
    private EditText loginName;
    private boolean permissionGiven = false;
    public boolean participantPublishing = false;
    public static Point screenSize;

    // ARCORE
    private Session session;
    private TapHelper tapHelper;
    int screenWidth = 1080;
    int screenHeight = 1920;
    int totalSurfaceLength;
    int totalViewLength;
    byte[] pixelData;
    byte[] pixelData2;
    byte[] argbBuffer;
    byte[] tempData;
    ByteBuffer buf;
    byte[]  ybuffer;
    byte[]  uvbuffer;
    byte[] frameData;
    public  GLSurfaceView   glsurfaceView;
    private DisplayRotationHelper displayRotationHelper;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PointRenderer pointRenderer = new PointRenderer();


    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    public      boolean arrowMode;

    public      boolean fileDownloading = false;
    public      boolean fileDownloadingFinishing = false;
    private     int     lastByteSize = 0;
    private     int     currentDataSize = uploadBlockSize;
    private     String  fileName;
    private int         lengthInBytes = 0;
    private int         targetDownloadCount = 0;
    private int         downloadCount = 0;
    private Thread      fileUploadThread;
    private Thread      downloadThread;

    public      boolean fileUploading = false;

    private FileInputStream  fileReadStream = null;
    private FileOutputStream fileWriteStream = null;

    private  Thread          localMessageHandler = null;
    private  ArrayList<String>    localMessages = new ArrayList<String>();

    private  Thread          remoteMessageHandler = null;
    private  ArrayList<String>    remoteMessages = new ArrayList<String>();

    private int mWidth;
    private int mHeight;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Log.d(TAG, "SURFACE ASD PARAMS " + glsurfaceView.getWidth() + " " + glsurfaceView.getHeight());

        try
        {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            pointRenderer.createOnGlThread(this, "models/grass.jpg");
            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {

//        width = 1280;
//        height = 720;
        Log.d("ARTEST", "onSurfaceChanged" + "Width : " + width + " Height : " + height);

        screenWidth = width;
        screenHeight = height;

        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);

        if(isLandscape)
        {
            mWidth = 1280; //width;
            mHeight = 720; //height;
        }
        else
        {
            mWidth = 720; //width;
            mHeight = 1280; //height;
        }
        totalSurfaceLength = screenWidth * screenHeight * 4;
        totalViewLength = mWidth * mHeight * 4;
        pixelData = new byte[totalSurfaceLength];
        pixelData2 = new byte[totalViewLength];
        argbBuffer = new byte[totalViewLength];
        tempData = new byte[totalViewLength];
        buf = ByteBuffer.wrap(pixelData);
        ybuffer = new byte[mWidth * mHeight];
        uvbuffer = new byte[mWidth * mHeight / 2];
        frameData = new byte[mWidth * mHeight * 3 / 2];
    }

    public VideoCapturerAndroid.CameraSwitchHandler cameraSwitchHandler;

    public static void ShowToast(final String message, final Context applicationContext)
    {
        getInstance().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Toast toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static VideoChatActivity getInstance()
    {
        if (Instance == null)
        {
            Instance = new VideoChatActivity();
        }
        return Instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Instance = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        screenSize = GetScreeenSize();

        WebRTCMediaProvider.cameraID = 1;

        SetupCallScreen();

        android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

        if (ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(VideoChatActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FILEWRITE_REQUEST_CODE);
        }

        applicationContext = getApplicationContext();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        Flashphoner.init(this);
        localMessageHandler = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    synchronized (localSyncObject)
                    {
                        if(localMessages.size() > 0)
                        {
                            String localMessage = localMessages.get(0);
                            localMessages.remove(0);

                            if (localMessage.contains("TAP: "))
                            {
                                DecodeTapMessage(localMessage);
                            }
                            else if (localMessage.contains("BREAK: "))
                            {
                                String userName = localMessage.replace("BREAK: " , "");

                                DecodeLocalBreakMessage(userName);
                            }
                            else if (localMessage.contains("UNDO: "))
                            {
                                String userName = localMessage.replace("UNDO: " , "");

                                DecodeUndoMessage(userName);
                            }

                            if (localMessage.contains("COLORCHANGE: "))
                            {
                                localMessage = localMessage.replace("COLORCHANGE: " , "");

                                String[] values = localMessage.split(" ");

                                String userName = values[0];
                                String colorValue = values[1] + " " + values[2] + " " + values[3] + " " + values[4];

                                DecodeLocalColorChangeMessage(userName, colorValue);
                            }

                        }
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        localMessageHandler.start();

        remoteMessageHandler = new Thread(new Runnable()
        {
            @Override
            public void run() {
                while (true)
                {
                    synchronized (remoteSyncObject)
                    {
                        if(remoteMessages.size() > 0)
                        {
                            String remoteMessage = remoteMessages.get(0);
                            remoteMessages.remove(0);

                            if (remoteMessage.contains("TAP: ") && WebRTCMediaProvider.cameraID == 0)
                            {
                                Log.d(TAG, "Remote Message : " + remoteMessage);
                                DecodeTapMessage(remoteMessage);
                            }
                            else if (remoteMessage.contains("BREAK: ") && WebRTCMediaProvider.cameraID == 0)
                            {
                                String userName = remoteMessage.replace("BREAK: " , "");

                                DecodeRemoteBreakMessage(userName);
                            }
                            else if (remoteMessage.contains("UNDO: "))
                            {
                                String userName = remoteMessage.replace("UNDO: " , "");

                                DecodeUndoMessage(userName);
                            }

                            if (remoteMessage.contains("COLORCHANGE: "))
                            {
                                remoteMessage = remoteMessage.replace("COLORCHANGE: " , "");
                                String[] values = remoteMessage.split(" ");

                                String userName = values[0];
                                String colorValue = values[1] + " " + values[2] + " " + values[3] + " " + values[4];

                                DecodeRemoteColorChangeMessage(userName, colorValue);
                            }

                        }
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        remoteMessageHandler.start();
    }

    private void DecodeUndoMessage(String userName)
    {
        if(isODG)
        {
            remoteRenderer.ClearCanvas();
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    remoteRenderer.invalidate();
                }
            });
            return;
        }
        pointRenderer.Undo(userName);
    }

    public Point GetScreeenSize()
    {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point ();
        display.getSize(size);

        return size;
    }

    @Override
    public void onBackPressed()
    {
        uiHandler.backKey();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "App unpaused");
        isPaused = false;
        if (session != null) {
            try
            {
                session.resume();
            }
            catch (CameraNotAvailableException e)
            {
                // In some cases (such as another camera app launching) the camera may be given to
                // a different app instead. Handle this properly by showing a message and recreate the
                // session at the next iteration.
                session = null;
                return;
            }
            room.publish(uiHandler.localRender);
            glsurfaceView.onResume();
            displayRotationHelper.onResume();
        }

//        if(screenRecorder != null)
//            screenRecorder.ResumeRecording();
    }

    void SetupCallScreen ()
    {
        setContentView(R.layout.activity_drawer);
        localRenderer = findViewById(R.id.CurrentRender);
        remoteRenderer = findViewById(R.id.StreamRender);
        participantView = new ParticipantView(remoteRenderer, mParticipantName);

        screenRecorder = new ScreenRecorder(this);

        glsurfaceView = (GLSurfaceView) findViewById(R.id.glsurfaceview);
        ViewGroup.LayoutParams layoutParams = glsurfaceView.getLayoutParams();
        layoutParams.width = 720;
        layoutParams.height = 1280;
        glsurfaceView.setLayoutParams(layoutParams);
        glsurfaceView.invalidate();
        glsurfaceView.setPreserveEGLContextOnPause(true);
        glsurfaceView.setEGLContextClientVersion(2);
        glsurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        glsurfaceView.setRenderer(this);
        glsurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glsurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glsurfaceView.setZOrderOnTop(true);
        glsurfaceView.setX(25000);

        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        currentActivityIntent = getIntent();
        String Message = currentActivityIntent.getStringExtra("PIC");
        roomName = currentActivityIntent.getStringExtra("ROOMNAME");
        String arCore = currentActivityIntent.getStringExtra("ARCORE");
        VideoCapturerAndroid.arCorePresent = arCore.equals("PRESENT");
        Log.d(TAG, "RoomName" + roomName);
        if(Message.equals("PRESENT"))
        {
            uiHandler = new MainUIHandler(Instance, true);
        }
        else
        {
            uiHandler = new MainUIHandler(Instance, false);
        }


        mFileUploadButton = findViewById(R.id.UploadFileButton);
        loginName = findViewById(R.id.ProfileName);

        String loginNameText = Build.MODEL;
        loginNameText = loginNameText.replace(" ", "");
        loginName.setText(loginNameText);

        localRenderer.setMirror(true);
        remoteRenderer.setMirror(true);

        List<MediaDevice> videoDeviceList = Flashphoner.getMediaDevices().getVideoList();

        for(int i = 0; i < videoDeviceList.size(); i++)
        {
            MediaDevice videoDevice = videoDeviceList.get(i);

            Log.d(TAG, videoDevice.getLabel() + " " + videoDevice.getType());
        }

        cameraSwitchHandler = new VideoCapturerAndroid.CameraSwitchHandler()
        {
            @Override
            public void onCameraSwitchDone(boolean frontCamera)
            {

                if(!frontCamera && VideoCapturerAndroid.arCorePresent)
                {
                    if (!openingARCORE && !arcoreRunning)
                    {
                        StartARCORE();
                    }
                }
            }

            @Override
            public void onCameraSwitchError(String s)
            {

            }
        };

        /**
         * Connection to server will be established when Connect button is clicked.
         */

        mFileUploadButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Context ctx = VideoChatActivity.this;
                new ChooserDialog(ctx)
                        .withStartFile(null)
//                        .withResources(R.string.title_choose_any_file, R.string.title_choose, R.string.dialog_cancel)
//                        .withFileIconsRes(false, R.mipmap.ic_my_file, R.mipmap.ic_my_folder)
                        .withAdapterSetter(new ChooserDialog.AdapterSetter()
                        {
                            @Override
                            public void apply(DirAdapter adapter)
                            {
                                //
                            }
                        })
                        .withChosenListener(new ChooserDialog.Result()
                        {
                            @Override
                            public void onChoosePath(String path, File pathFile)
                            {
                                Toast.makeText(ctx, "FILE: " + path, Toast.LENGTH_SHORT).show();
                                try
                                {
                                    fileReadStream = new FileInputStream(pathFile);
                                    UploadFile(path);

                                }
                                catch (FileNotFoundException e)
                                {
                                    e.printStackTrace();
                                }
                                catch (RuntimeException e)
                                {
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        })
                        .build()
                        .show();
            }
        });

        Connect();
        SetLocalRendererMirror();
    }

    Frame frame = null;
    Camera camera = null;
    boolean resetLocalCam = false;
    @Override
    public void onDrawFrame(GL10 gl)
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null || isPaused) {
            return;
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        try
        {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            frame = session.update();
            camera = frame.getCamera();

            handleLocalTap(frame, camera);
            handleRemoteTap(frame, camera);

            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
//                byte[] ardata = GetScreenPixels();                //TODO:UnComment
//                onPreviewFrame(ardata, null);
                return;
            }

//            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);


//            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

//            if(pointsOrPlaneSpawn)
            {
                // Visualize tracked points.
                PointCloud pointCloud = frame.acquirePointCloud();
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);

                // Application is responsible for releasing the point cloud resources after
                // using it.
                pointCloud.release();
            }
//            else
            {
                // Visualize planes.
                planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
            }

            pointRenderer.draw(viewmtx, projmtx);

            byte[] ardata = GetScreenPixels();
            onPreviewFrame(ardata, null);
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
            if(t instanceof SessionPausedException)
            {
                try
                {
                    session.resume();
                }
                catch (CameraNotAvailableException e)
                {
                    Log.d(TAG, "Camera not available exception : " + e.getMessage());
                }
            }
        }

    }

    public void CleanUp()
    {
        pointRenderer.DestroyAll();

        localMessages.clear();
        remoteMessages.clear();

        if(localMessageHandler != null)
        {
            localMessageHandler.interrupt();
        }

        if(remoteMessageHandler != null)
        {
            remoteMessageHandler.interrupt();
        }
    }

    public void SavePicture() throws IOException {

        int pixelData[] = new int[screenWidth * screenHeight];

        // Read the pixels from the current GL frame.
        IntBuffer buf = IntBuffer.wrap(pixelData);
        buf.position(0);
        GLES20.glReadPixels(0, 0, screenWidth, screenHeight,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

        // Create a file in the Pictures/HelloAR album.
        final File out = new File("/sdcard/arout.png");

        // Make sure the directory exists
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }

        // Convert the pixel data from RGBA to what Android wants, ARGB.
        int bitmapData[] = new int[pixelData.length];
        for (int i = 0; i < screenHeight; i++) {
            for (int j = 0; j < screenWidth; j++) {
                int p = pixelData[i * screenWidth + j];
                int b = (p & 0x00ff0000) >> 16;
                int r = (p & 0x000000ff) << 16;
                int ga = p & 0xff00ff00;
                bitmapData[(screenHeight - i - 1) * screenWidth + j] = ga | r | b;
            }
        }
        // Create a bitmap.
        Bitmap bmp = Bitmap.createBitmap(bitmapData,
                screenWidth, screenHeight, Bitmap.Config.ARGB_8888);

        // Write it to disk.
        FileOutputStream fos = new FileOutputStream(out);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    public byte[] GetScreenPixels() throws IOException
    {
        buf.position(0);
        GLES20.glReadPixels(0, 0, screenWidth, screenHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);//Original
        Libyuv.ARCORETONV21(pixelData, screenWidth * 4, tempData, argbBuffer, mWidth * 4, mWidth, -mHeight, ybuffer, uvbuffer, frameData);
        return frameData;
    }

    void  SaveBitmap(byte[] rgbbuffer, int width, int height) throws IOException
    {
        if(rgbbuffer.length == 0)
            return;

        YuvImage yuvImage = new YuvImage(rgbbuffer, ImageFormat.NV21, width, height, null);

        FileOutputStream out = new FileOutputStream("/sdcard/scr.png");
//        Bitmap stitchBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        stitchBmp.copyPixelsFromBuffer(ByteBuffer.wrap(rgbbuffer));
//        stitchBmp.compress(Bitmap.CompressFormat.PNG, 100, out);

        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        out.close();
    }

    @Override
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera)
    {
        if(isPaused)
            return;
        WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;
        if(isODG)
        {
            handleLocalTap();
            handleRemoteTap();
        }
        if (videoCapturerAndroid == null)
            return;

        if(VideoCapturerAndroid.arCorePresent && WebRTCMediaProvider.cameraID == 0)
        {
            long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            if (videoCapturerAndroid.eventsHandler != null && !videoCapturerAndroid.firstFrameReported)
            {
                videoCapturerAndroid.eventsHandler.onFirstFrameAvailable();
                videoCapturerAndroid.firstFrameReported = true;
            }

            int frameOrientation = 180; //videoCapturerAndroid.getFrameOrientation();

//            Log.d("ARTEST", "Frame : height" + videoCapturerAndroid.captureFormat.height + " Width : " + videoCapturerAndroid.captureFormat.width);
            //height : 1280 width : 720

            if (videoCapturerAndroid.frameObserver != null) {
                if(!isLandscape)
                videoCapturerAndroid.frameObserver.onByteBufferFrameCaptured(data, videoCapturerAndroid.captureFormat.width,
                        videoCapturerAndroid.captureFormat.height, frameOrientation, captureTimeNs);
                else
                    videoCapturerAndroid.frameObserver.onByteBufferFrameCaptured(data, videoCapturerAndroid.captureFormat.height,
                            videoCapturerAndroid.captureFormat.width, frameOrientation, captureTimeNs);
            }
        }
        else
        {
            long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            if (videoCapturerAndroid.eventsHandler != null && !videoCapturerAndroid.firstFrameReported)
            {
                videoCapturerAndroid.eventsHandler.onFirstFrameAvailable();
                videoCapturerAndroid.firstFrameReported = true;
            }

//           Log.d(TAG, "NO AR CORE :" + videoCapturerAndroid.getFrameOrientation() + " " + videoCapturerAndroid.captureFormat.width);

            int frameOri = videoCapturerAndroid.getFrameOrientation();

            videoCapturerAndroid.cameraStatistics.addFrame();
            videoCapturerAndroid.frameObserver.onByteBufferFrameCaptured(data, videoCapturerAndroid.captureFormat.width, videoCapturerAndroid.captureFormat.height, videoCapturerAndroid.getFrameOrientation(), captureTimeNs);
            if (videoCapturerAndroid.camera != null)
            {
                videoCapturerAndroid.camera.addCallbackBuffer(data);
            }
        }
    }

    Object localSyncObject = new Object();
    Object remoteSyncObject = new Object();

    public void DecodeLocalBreakMessage(String userName)
    {
        synchronized (localSyncObject)
        {
            if(isODG)
            {
                motionEventsLocal.clear();
                remoteRenderer.PaintUp();
                return;
            }
            motionEventsLocal.clear();
            pointRenderer.AddBreak(userName);

        }
    }

    public void DecodeLocalColorChangeMessage(String userName, String newColor)
    {
        synchronized (localSyncObject)
        {
            pointRenderer.UpdateCurrentColor(userName, newColor);
        }
    }

    public void DecodeRemoteBreakMessage(String userName)
    {
        synchronized (remoteSyncObject)
        {
            motionEventsRemote.clear();
            pointRenderer.AddBreak(userName);
        }
    }

    public void DecodeRemoteColorChangeMessage(String userName, String newColor)
    {
        synchronized (remoteSyncObject)
        {
            pointRenderer.UpdateCurrentColor(userName, newColor);
        }
    }

    public void UndoClicked()
    {
        SendMessage("UNDO: " + roomManager.getUsername());
        if(isODG)
        {
            remoteRenderer.ClearCanvas();
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    remoteRenderer.invalidate();
                }
            });
            return;
        }
        pointRenderer.Undo(roomManager.getUsername());
    }

    class TASQAR_MotionEvent
    {
        public String userName = "";
        public String mode = "";
        public MotionEvent motionEvent;

        public TASQAR_MotionEvent(String userName, String mode, float xVal, float yVal)
        {
            this.mode = mode;
            this.motionEvent = MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, xVal, yVal, 0);
            this.userName = userName;
        }
    }

    public void DecodeTapMessage(String tapMessage)
    {
        tapMessage = tapMessage.replace("TAP: ", "");
        String[] values = tapMessage.split(" ");

        float xVal = Float.valueOf(values[0]);
        float yVal = Float.valueOf(values[1]);
        float width = Float.valueOf(values[2]);
        float height = Float.valueOf(values[3]);

        String mode = values[4];
        String userName = "";

        if(values.length > 5)
           userName = values[5];

        TASQAR_MotionEvent motionEvent = new TASQAR_MotionEvent(userName, mode, xVal, yVal);
        TapHandle(motionEvent, mode, width, height);
    }

    public void BreakSend()
    {
        SendMessage("BREAK: " + roomManager.getUsername());

        synchronized (localSyncObject)
        {
            localMessages.add("BREAK: " + roomManager.getUsername());
        }
    }

    public float[]      currentColor = new float[]{1.0f, 0.0f, 0.0f, 1.0f};

    public void     SelectedRed()
    {
        SetCurrentColor(new float[]{1.0f, 0.0f, 0.0f, 1.0f});
    }

    public  void SetCurrentColor(float[] color)
    {
        currentColor = color;

        SendMessage("COLORCHANGE: " + roomManager.getUsername() + " "  + Arrays.toString(currentColor));

        synchronized (localSyncObject) {
            localMessages.add("COLORCHANGE: " + roomManager.getUsername() + " "  + Arrays.toString(currentColor));
        }
    }

    public void     SelectedBlue()
    {
        SetCurrentColor(new float[]{0.0f, 0.0f, 1.0f, 1.0f});
    }

    public void     SelectedGreen()
    {
        SetCurrentColor(new float[]{0.0f, 1.0f, 0.0f, 1.0f});
    }

    public void     SelectedOrange()
    {
        SetCurrentColor(new float[]{1.0f, 0.7f, 0.0f, 1.0f});
    }

    public void     SelectedYellow()
    {
        SetCurrentColor( new float[]{1.0f, 1.0f, 0.0f, 1.0f});
    }


    public void TapSend(int x, int y, int width, int height)
    {
        String drawType = "";
        switch (uiHandler.selectedElement)
        {
            case ARROW:
                drawType = "AR";
                break;
            case BLINKER:
                drawType = "BL";
                break;
            case LINE:
                drawType = "DR";
                break;
            case SURFACE_LINE:
                drawType = "LN";
                break;
            case SURFACE_ARROW:
                drawType = "SAR";
                break;
            case SURFACE_BLINKER:
                drawType = "SBL";
                break;
        }

        SendMessage("TAP: " + x + " " + y + " " + width + " " + height + " " + drawType + " " + roomManager.getUsername() + " " + Arrays.toString(currentColor));
        synchronized (localSyncObject)
        {
            localMessages.add("TAP: " + x + " " + y + " " + width + " " + height + " " + drawType + " " + roomManager.getUsername() + " " + Arrays.toString(currentColor));
        }
    }

    ArrayList<TASQAR_MotionEvent> motionEventsLocal = new ArrayList<TASQAR_MotionEvent>();
    ArrayList<TASQAR_MotionEvent> motionEventsRemote = new ArrayList<TASQAR_MotionEvent>();

    public void TapHandle(TASQAR_MotionEvent event, String mode, float width, float height) {
        float xMul = event.motionEvent.getX() / width;
        float yMul = event.motionEvent.getY() / height;


        float xVal;
        float yVal;

        if(isODG)
        {
            xVal = screenSize.x * xMul;
            yVal = screenSize.y * yMul;
        }
        else
        {
            xVal = screenWidth * xMul;
            yVal = screenHeight * yMul;

        }


        boolean local = event.userName.equals(roomManager.getUsername());

        if (local)
        {
            if(isODG)
            {
                xVal = screenSize.x * xMul;
                yVal = screenSize.y * yMul;
            }
            else
            {
                xVal = screenWidth * xMul;
                yVal = screenHeight * yMul;
            }
            event.motionEvent.setLocation(xVal, yVal);
            motionEventsLocal.add(event);
        }
        else
        {
            xVal = screenWidth * xMul;
            yVal = screenHeight * yMul;
            event.motionEvent.setLocation(xVal, yVal);
            motionEventsRemote.add(event);
        }
    }

    public  boolean pointsOrPlaneSpawn = false;

    private void handleLocalTap(Frame frame, Camera camera)
    {
        synchronized (localSyncObject)
        {
            while (motionEventsLocal.size() > 0)
            {
                TASQAR_MotionEvent tasqar_motionEvent = motionEventsLocal.get(0);

                String mode = tasqar_motionEvent.mode;

                MotionEvent tap = tasqar_motionEvent.motionEvent;
                motionEventsLocal.remove(0);
                if (tap != null && camera.getTrackingState() == TrackingState.TRACKING)
                {
                    for (HitResult hit : frame.hitTest(tap))
                    {
                        switch (mode)
                        {
                            case "DR":
                                SpawnPoint(hit, tasqar_motionEvent);
                                break;
                            case "AR":
                                SpawnArrow(hit, tasqar_motionEvent);
                                break;
                            case "BL":
                                SpawnBlinkingLight(hit, tasqar_motionEvent);
                                break;
                        }
                        break;
                    }
                }
            }
        }
    }

    private void handleRemoteTap(Frame frame, Camera camera)
    {
        synchronized (remoteSyncObject)
        {
            while (motionEventsRemote.size() > 0)
            {
                TASQAR_MotionEvent tasqar_motionEvent = motionEventsRemote.get(0);

                String mode = tasqar_motionEvent.mode;

                MotionEvent tap = tasqar_motionEvent.motionEvent;

                localRenderer.drawEnabled = true;
                localRenderer.drawTouch_move(tap.getX(), tap.getY());

                motionEventsRemote.remove(0);
                if (tap != null && camera.getTrackingState() == TrackingState.TRACKING)
                {
                    for (HitResult hit : frame.hitTest(tap))
                    {
                        if(mode.equals("DR"))
                        {
                            SpawnPoint(hit, tasqar_motionEvent);
                        }
                        else if(mode.equals("AR"))
                        {
                            SpawnArrow(hit, tasqar_motionEvent);
                        }
                        else if(mode.equals("BL"))
                        {
                            SpawnBlinkingLight(hit, tasqar_motionEvent);
                        }
                        break;
                    }
                }
            }
        }
    }


    private void handleLocalTap()
    {
        synchronized (localSyncObject)
        {
            while (motionEventsLocal.size() > 0)
            {
                TASQAR_MotionEvent tasqar_motionEvent = motionEventsLocal.get(0);

                String mode = tasqar_motionEvent.mode;

                MotionEvent tap = tasqar_motionEvent.motionEvent;
                motionEventsLocal.remove(0);

                if (tap != null)
                {
                    switch (mode)
                    {
                        case "LN":
                            DrawOnCanvas(tap.getX(), tap.getY());
                            break;
                        case "BR":

                            break;
                        case "SAR":
                            DrawArrowTwoD(tap.getX(), tap.getY());
                            break;
                        case "SBL":
                            DrawBlinkerTwoD(tap.getX(), tap.getY());
                            break;
                        default:
                            Log.d(TAG, "Tap handler doesn't exist");
                            break;
                    }
                }
            }
        }
    }

    private void handleRemoteTap()
    {
        synchronized (remoteSyncObject)
        {
            while (motionEventsRemote.size() > 0)
            {
                TASQAR_MotionEvent tasqar_motionEvent = motionEventsRemote.get(0);

                String mode = tasqar_motionEvent.mode;

                MotionEvent tap = tasqar_motionEvent.motionEvent;
                motionEventsRemote.remove(0);

                if (tap != null)
                {
                    switch (mode)
                    {
                        case "LN":
                            DrawOnCanvas(tap.getX(), tap.getY());
                            break;
                        case "SAR":
                            DrawArrowTwoD(tap.getX(), tap.getY());
                            break;
                        case "SBL":
                            DrawBlinkerTwoD(tap.getX(), tap.getY());
                            break;
                    }
                }
            }
        }
    }


    private void DrawOnCanvas (float x, float y)
    {
        remoteRenderer.PaintMove(x, y);
        remoteRenderer.invalidate();
        Log.d(TAG, "Drawing on canvas");
    }

    private void DrawArrowTwoD (float x, float y)
    {
        remoteRenderer.DrawArrow(x, y);
    }

    private void DrawBlinkerTwoD (float x, float y)
    {
        remoteRenderer.DrawBlinker(x, y);
    }


    public  void TogglePointPlaneSpawn()
    {
        pointsOrPlaneSpawn = !pointsOrPlaneSpawn;
    }

    private void SpawnArrow(HitResult hit, TASQAR_MotionEvent motionEvent)
    {
        pointRenderer.AddPoint(hit, motionEvent.userName, 1);
    }

    private void SpawnBlinkingLight(HitResult hit, TASQAR_MotionEvent motionEvent)
    {
        pointRenderer.AddPoint(hit, motionEvent.userName, 2);
    }

    private void SpawnPoint(HitResult hit, TASQAR_MotionEvent motionEvent)
    {
        pointRenderer.AddPoint(hit, motionEvent.userName, 0);
    }

    public void SetLocalRendererMirror()
    {
        if(WebRTCMediaProvider.cameraID == 0)
        {
            if (VideoCapturerAndroid.arCorePresent)
            {
                localRenderer.setMirror(true);
            }
            else
            {
                localRenderer.setMirror(false);
            }
        }
        else
        {
            if (VideoCapturerAndroid.arCorePresent)
            {
                localRenderer.setMirror(true);
            }
            else
            {
                localRenderer.setMirror(true);
            }
        }
    }

    public void ToggleCamera()
    {
        if(WebRTCMediaProvider.cameraID == 1)
        {
            WebRTCMediaProvider.cameraID = 0;
        }
        else
        {
            if(VideoCapturerAndroid.arCorePresent)
                StopARCORE();
            WebRTCMediaProvider.cameraID = 1;
        }

        Log.d(TAG, VideoCapturerAndroid.arCorePresent ? "Present" : "Absent");
        SetLocalRendererMirror();

        WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;

        if (videoCapturerAndroid == null)
            return;

        videoCapturerAndroid.switchCamera(cameraSwitchHandler);

        Log.d(TAG, "No activity can handle picking a file. Showing alternatives.");
    }

    boolean openingARCORE = false;
    boolean arcoreRunning = false;

    public void StopARCORE()
    {
        if (session != null)
        {
            session.pause();
            arcoreRunning = false;
        }
    }

    public void StartARCORE()
    {
        if(session != null)
        {
            try {
                session.resume();
            } catch (CameraNotAvailableException e) {
                e.printStackTrace();
            }

            return;
        }

        if (openingARCORE)
            return;

        openingARCORE = true;

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();

                    while (true) {
                        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;

                        if (videoCapturerAndroid != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (session == null) {
                                        Exception exception = null;
                                        String message = null;

                                        try {
                                            session = new Session(/* context= */ getApplicationContext());
                                        } catch (UnavailableApkTooOldException e) {
                                            message = "Please update ARCore";
                                            exception = e;
                                            VideoCapturerAndroid.arCorePresent = false;
                                            ShowToast("AR CORE NOT PRESENT", applicationContext);
                                            OpenBackCamera();
                                        } catch (UnavailableSdkTooOldException e) {
                                            message = "Please update this app";
                                            exception = e;
                                            VideoCapturerAndroid.arCorePresent = false;
                                            ShowToast("AR CORE NOT PRESENT", applicationContext);
                                            OpenBackCamera();
                                        } catch (Exception e) {
                                            message = "Failed to create AR session";
                                            exception = e;
                                            VideoCapturerAndroid.arCorePresent = false;
                                            ShowToast("AR CORE NOT PRESENT", applicationContext);
                                            OpenBackCamera();
                                        }
                                    }

                                    if (session != null) {
                                        try {
                                            session.resume();
                                        } catch (CameraNotAvailableException e) {
                                            // In some cases (such as another camera app launching) the camera may be given to
                                            // a different app instead. Handle this properly by showing a message and recreate the
                                            // session at the next iteration.
                                            session = null;
                                            return;
                                        }

                                        glsurfaceView.onResume();
                                        displayRotationHelper.onResume();
                                    }

                                    openingARCORE = false;
                                    arcoreRunning = true;
                                }
                            });

                            break;
                        }

                        sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    public void OpenBackCamera()
    {
        WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;

        if (videoCapturerAndroid == null)
            return;

        WebRTCMediaProvider.cameraID = 0;
        videoCapturerAndroid.id = 1;

        videoCapturerAndroid.switchCamera(cameraSwitchHandler);
    }

    public void Connect()
    {
        if (connected)
            return;

        if (loginName.getText().toString().length() == 0)
        {
            ShowToast("Login Name is empty", this.applicationContext);
            return;
        }

        permissionGiven = false;
        /**
         * The connection options are set.
         * WCS server URL and user name are passed when RoomManagerOptions object is created.
         */

        final RoomManagerOptions roomManagerOptions = new RoomManagerOptions(wcsURL, loginName.getText().toString());

        /**
         * RoomManager object is created with method createRoomManager().
         * Connection session is created when RoomManager object is created.
         */
        roomManager = Flashphoner.createRoomManager(roomManagerOptions);

        /**
         * Callback functions for connection status events are added to make appropriate changes in controls of the interface when connection is established and closed.
         */

        roomManager.on(new RoomManagerEvent()
        {
            @Override
            public void onConnected(final Connection connection) {
                connected = true;

                RoomOptions roomOptions = new RoomOptions();
                roomOptions.setName(roomName);

                /**
                 * The participant joins a video chat room with method RoomManager.join().
                 * RoomOptions object is passed to the method.
                 * Room object is created and returned by the method.
                 */
                room = roomManager.join(roomOptions);

                if (ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    permissionGiven = true;
                }

                if (!permissionGiven) {
                    ActivityCompat.requestPermissions(VideoChatActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                            PUBLISH_REQUEST_CODE);
                }

                /**
                 * Callback functions for events occurring in video chat room are added.
                 * If the event is related to actions performed by one of the other participants, Participant object with data of that participant is passed to the corresponding function.
                 */

                room.on(new RoomEvent()
                {
                    @Override
                    public void onState(final Room room) {

                        Log.d(TAG, "ON STATE");

                        if (permissionGiven && stream == null)
                        {
                            stream = room.publish(localRenderer, VideoChatActivity.this);
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
                        for (final Participant participant : room.getParticipants())
                        {
                            /**
                             * A player view is assigned to each of the other participants in the room.
                             */
                            if (participantView != null)
                            {
                                /**
                                 * Playback of the stream being published by the other participant is started with method Participant.play().
                                 * SurfaceViewRenderer to be used to display the video stream is passed when the method is called.
                                 */
                                publishedParticipant = participant;
                                participantPublishing = true;
                                publishedParticipant.play(participantView.surfaceViewRenderer);
                            }

                            if(participantPublishing)
                            {
                                SelectedOrange();
                                uiHandler.StartTimer();
                                if(publishedParticipant.getName().contains("ODG"))
                                {
                                    isODG = true;
                                    uiHandler.selectedElement = MainUIHandler.SelectedElement.SURFACE_LINE;
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                    remoteRenderer.setMirror(false);
                                    Log.d(TAG, "Partner is an ODG device");
                                }
                            }
                        }

                        SetCurrentColor(currentColor);
                    }

                    @Override
                    public void onJoined(final Participant participant)
                    {
                        Log.d(TAG, "ON JOINED " + participant.getName());

                        if(participant.getName().contains("ODG"))
                        {
                            isODG = true;
                            uiHandler.selectedElement = MainUIHandler.SelectedElement.SURFACE_LINE;
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            remoteRenderer.setMirror(false);
                            Log.d(TAG, "An ODG device has joined");
                        }

                        uiHandler.StartTimer();
                        SelectedBlue();
                        /**
                         * When a new participant joins the room, a player view is assigned to that participant.
                         */

                        SetCurrentColor(currentColor);
                    }

                    @Override
                    public void onLeft(final Participant participant) {
                        /**
                         * When one of the other participants leaves the room, player view assigned to that participant is freed.
                         */
                        Log.d(TAG, "ON onLeft " + participant.getName());

                        participantPublishing = false;
                    }

                    @Override
                    public void onPublished(final Participant participant) {

                        Log.d(TAG, "ON onPublished " + participant.getName());
                        publishedParticipant = participant;
                        /**
                         * When one of the other participants starts publishing, playback of the stream published by that participant is started.
                         */
                        if (participantView != null)
                        {
                            participantPublishing = true;
                            remoteStream = publishedParticipant.play(participantView.surfaceViewRenderer);
                        }
                    }

                    @Override
                    public void onFailed(Room room, final String info)
                    {
                        connected = false;
                        room.leave(null);

                        Connect();
                    }

                    @Override
                    public void onMessage(final Message message)
                    {
                        /**
                         * When one of the participants sends a text message, the received message is added to the messages log.
                         */
                        Log.d(TAG, "ON MESSAGE " + message.getText());

                        String messageReceived = message.getText();

                        if(messageReceived.contains(":FUC-"))
                        {
                            StopFTP();
                            Log.d(TAG, "Cancelled");
                            File partialFile = new File(fileName);
                            if(partialFile.exists())
                                partialFile.delete();
                            return;
                        }

                        if(fileDownloading)
                        {
                            if(messageReceived.contains(":FUF"))
                            {
                                fileDownloadingFinishing = true;
                                messageReceived = messageReceived.replace(":FUF", "");
                                lastByteSize = Integer.parseInt(messageReceived);
                                currentDataSize = lastByteSize;

                                return;
                            }

                            try
                            {
                                Charset charset = Charset.forName("ISO-8859-1");

                                byte[] data = new byte[currentDataSize];
                                ByteBuffer byteBuffer = charset.encode(messageReceived);
                                byteBuffer.get(data);

                                fileWriteStream.write(data);

                                float percentage = ((float) downloadCount/(float) targetDownloadCount);

                                if(downloadCount == 0)
                                {
                                    uiHandler.showNotification(getApplicationContext(), "Downloading", fileName, new Intent ());
                                }
                                else
                                {
                                    uiHandler.UpdateNotification((int) (percentage * 100));
                                }

                                uiHandler.SetProgress(percentage, fileName);

                                if(downloadCount == targetDownloadCount)
                                {
                                    uiHandler.SetProgress(0, "");
                                    uiHandler.StopNotification();
                                }
                                downloadCount++;
                                if(fileDownloadingFinishing)
                                {
                                    fileDownloading = false;
                                    fileDownloadingFinishing = false;
                                }
                            }
                            catch (UnsupportedEncodingException e)
                            {
                                e.printStackTrace();
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (messageReceived.contains(":FU"))
                        {
                            String[] messageWithSize = messageReceived.split("@@");
                            messageReceived = messageWithSize[0];
                            lengthInBytes = Integer.parseInt(messageWithSize[1]);

                            targetDownloadCount = lengthInBytes / uploadBlockSize;
                            targetDownloadCount = (0 == (targetDownloadCount % uploadBlockSize)) ? targetDownloadCount : targetDownloadCount + 1;

                            String messageAndroidID = messageReceived.substring(messageReceived.indexOf(":FU") + 3, messageReceived.indexOf("-/"));

                            if(messageAndroidID.equals(android_id))
                            {
                                Log.d(TAG, "Android ID is The SAME");
                                return;
                            }

                            Log.d(TAG, "Message android ID" + messageAndroidID);
                            messageReceived = messageReceived.replace(":FU", "");
                            messageReceived = messageReceived.replace(messageAndroidID, "");
                            messageReceived = messageReceived.replace("-/", "");
                            fileName = messageReceived;
                            DownloadFile(messageReceived);
                        }
                        else if(messageReceived.contains("CTRL:-"))
                        {
                            messageReceived = messageReceived.replace("CTRL:-", "");
                            if(messageReceived.equals("DC"))
                            {
                                FloatingActionButton mEndButton = findViewById(R.id.EndCallButton);
                                mEndButton.callOnClick();
                            }
                            else if(messageReceived.equals("LS"))
                            {
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            }
                            else if (messageReceived.equals("PT"))
                            {
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            }
                            else if(messageReceived.equals("UC"))
                            {
                                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                            }
                        }
                        else
                        {
                            synchronized (remoteSyncObject)
                            {
                                remoteMessages.add(messageReceived);
                            }
                        }
                    }
                });
            }

            @Override
            public void onDisconnection(final Connection connection)
            {
                connected = false;
                Log.d(TAG, "ON DISCCONEASd");

                CallHistoryDatabaseHelper callHistoryDatabaseHelper = new CallHistoryDatabaseHelper(getApplicationContext());
                Cursor data = callHistoryDatabaseHelper.showData();
                data.moveToLast();

                callHistoryDatabaseHelper.UpdateSpecificData(CallHistoryDatabaseHelper.DataType.DURATION, uiHandler.timerText.getText().toString(), data.getString(0));
                uiHandler.StopTimer();
                stream = null;
            }
        });
    }

    public void SendMessage(String message)
    {
        if (room == null)
            return;

        for (Participant participant : room.getParticipants())
        {
            participant.sendMessage(message);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // TODO Fix no activity available
        if (data == null)
            return;

        switch (requestCode)
        {
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK)
                {
                    ContentResolver contentResolver = getApplicationContext().getContentResolver();
                    try
                    {
                        fileReadStream = (FileInputStream) contentResolver.openInputStream(data.getData());
                        String FilePath = data.getData().getPath();
                        Log.d(TAG, " The File Path is " + FilePath);
                        try
                        {
                            UploadFile(FilePath);
                        }
                        catch (RuntimeException e)
                        {
                            Log.d(TAG, e.getMessage());
                        }
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
                break;

            case ScreenRecorder.PERMISSION_CODE:
                if (resultCode == RESULT_OK)
                {
                    final Intent localDataCopy = data;
                    Handler handler = new Handler();
                    Runnable runnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            screenRecorder.ActivityResult(RESULT_OK, localDataCopy);
                        }
                    };

                    handler.postDelayed(runnable, 200);
                }
                break;
        }
    }

    public  void SendScreenshot()
    {
        try
        {
            String filePath = "/sdcard/scr.png";
            fileReadStream = new FileInputStream(filePath);

            UploadFile(filePath);

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private String friendName = "";
    public  static  int uploadBlockSize = 10000;
    public void UploadFile(final String filePath)
    {
        friendName = "";

        if(fileUploading)
            return;

        for (Participant participant : room.getParticipants())
        {
            if(participant.getName() != roomManager.getUsername())
            {
                friendName = participant.getName();
                break;
            }
        }

        if(friendName.length() == 0)
        {
            ShowToast("No participants present", this);
            return;
        }

        fileUploading = true;

        int fileLengthInBytes = 0;
        try
        {
            fileLengthInBytes = fileReadStream.available();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        final int lengthInBytes = fileLengthInBytes;

        SendMessage(":FU" + VideoChatActivity.getInstance().android_id + "-" + GetFileName(filePath) + "@@" + fileLengthInBytes);

        fileUploadThread = new Thread(new Runnable()
        {
            Charset charset = Charset.forName("ISO-8859-1");

            boolean acceptedMessage = true;
            int uploadCount;
            int targetUploadCount;
            @Override
            public void run()
            {
                targetUploadCount = lengthInBytes / uploadBlockSize;
                targetUploadCount = (0 == (targetUploadCount % uploadBlockSize)) ? targetUploadCount : targetUploadCount + 1;
                cancelled = false;

                boolean sendingEndMark = false;
                boolean sentEndMark = false;
                while (true)
                {
                    try
                    {
                        if(acceptedMessage)
                        {
                            int available = fileReadStream.available();
                            int uploadSize = uploadBlockSize;

                            Message message = new Message();
                            message.setTo(friendName);
                            message.getRoomConfig().put("name", room.getName());

                            if (available < uploadSize || available == 0)
                            {
                                uploadSize = available;

                                if (!sentEndMark)
                                {
                                    message.setText(":FUF" + uploadSize);
                                    sentEndMark = true;
                                    sendingEndMark = true;
                                }
                            }

                            if(uploadSize > 0 && !sendingEndMark)
                            {
                                byte[] data = new byte[uploadSize];

                                fileReadStream.read(data);

                                ByteBuffer byteBuffer = ByteBuffer.wrap(data);

                                String stringData = charset.decode(byteBuffer).toString();

                                message.setText(stringData);

                                if(sentEndMark)
                                {
                                    fileUploading = false;
                                }
                            }

                            acceptedMessage = false;

                            room.sendAppCommand("sendMessage", message, new RestAppCommunicator.Handler()
                            {
                                @Override
                                public void onAccepted(Data data)
                                {
                                    if(cancelled)
                                    {
                                        acceptedMessage = false;

                                        return;
                                    }

                                    float percentage = ((float) uploadCount/(float) targetUploadCount);

                                    if(uploadCount == 0)
                                    {
                                        uiHandler.showNotification(getApplicationContext(), "Uploading", filePath, new Intent ());
                                    }
                                    else
                                    {
                                        uiHandler.UpdateNotification((int)(percentage * 100));
                                    }

                                    uiHandler.SetProgress(percentage, filePath);

                                    if(uploadCount == targetUploadCount)
                                    {
                                        uiHandler.SetProgress(0, "");
                                        uiHandler.StopNotification();
                                    }

                                    acceptedMessage = true;
                                    uploadCount++;
                                }

                                @Override
                                public void onRejected(Data data)
                                {

                                }
                            });
                            sendingEndMark = false;

                            if(!fileUploading)
                            {
                                String[] fileNames = filePath.split("/");
                                uiHandler.fileButtonHelper.AddData(fileNames[fileNames.length - 1], filePath, "SENT", uiHandler.GetDate());

                                fileReadStream.close();

                                StopFTP();
                                break;
                            }
                        }
                        Thread.sleep(10);
                    }
                    catch (IOException | InterruptedException | ArrayIndexOutOfBoundsException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
        fileUploadThread.start();
    }

    public void DownloadFile(String fileName)
    {
        if(fileDownloading)
            return;

        fileDownloading = true;

        currentDataSize = uploadBlockSize;

        downloadThread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    final String fileDate = uiHandler.GetDate();
                    String filePath = "/sdcard/ReceivedFiles/" + fileName;

                    File downloadDir = new File("/sdcard/ReceivedFiles");
                    if (!downloadDir.exists())
                    {
                        downloadDir.mkdirs();
                    }

                    File downloadedFile = new File(filePath);
                    if (!downloadedFile.exists())
                    {
                        downloadedFile.createNewFile();
                    }

                    fileWriteStream = new FileOutputStream(filePath);
                    while (fileDownloading)
                    {
                        Thread.sleep(100);
                    }

                    File thisFile = new File(filePath);
                    String pathName = "";
                    if (thisFile.exists())
                    {
                        pathName = "/sdcard/ReceivedFiles/" + uiHandler.AddTimeStampToName(GetFileName(filePath), fileDate);

                        thisFile.renameTo(new File(pathName));
                    }

                    uiHandler.fileButtonHelper.AddData(fileName, "/sdcard/ReceivedFiles/" + uiHandler.AddTimeStampToName(fileName, fileDate), "RECEIVED", fileDate);

                    fileWriteStream.close();
                    uiHandler.SetProgress(0, "");
                    OpenFile(pathName);
                    StopFTP();
                }
                catch (FileNotFoundException e1)
                {
                    e1.printStackTrace();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        });

        downloadThread.start();
    }

    public void StopFTP()
    {
        fileDownloading = false;
        fileDownloadingFinishing = false;
        fileUploading = false;
        cancelled = true;
        downloadCount = 0;
        targetDownloadCount = 0;


        if(fileUploadThread != null)
        {
            fileUploadThread.interrupt();
            fileUploadThread = null;
        }

        if(downloadThread != null)
        {
            downloadThread.interrupt();
            downloadThread = null;
        }

        uiHandler.SetProgress(0, "");
        uiHandler.StopNotification();

        Log.d(TAG, "STOP FTP CALLED");

        try
        {
            if(fileReadStream != null)
                fileReadStream.close();
            if(fileWriteStream != null)
                fileWriteStream.close();

            fileReadStream = null;
            fileWriteStream = null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public  String GetFileName(String filePath)
    {
        filePath = filePath.replace("primary:", "");

        return filePath.substring(filePath.lastIndexOf("/"));
    }

    public void OpenFile(String filePath)
    {
        Log.d(TAG, filePath);
        uiHandler.Minimise();
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
            ShowToast("No handler for this type of file.", this.getApplicationContext());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case PUBLISH_REQUEST_CODE: {
                if (grantResults.length == 0 ||
                        grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                        grantResults[1] != PackageManager.PERMISSION_GRANTED)
                {

                    Log.i(TAG, "Permission has been denied by user");
                } else {
                    /**
                     * Stream is created and published with method Room.publish().
                     * SurfaceViewRenderer to be used to display video from the camera is passed to the method.
                     */

                    if (stream == null && room != null)
                    {
                        stream = room.publish(localRenderer, VideoChatActivity.this);
                        stream.unmuteAudio();
                    }

                    /**
                     * Callback function for stream status change is added to make appropriate changes in controls of the interface when stream is being published.
                     */

                    Log.i(TAG, "Permission has been granted by user");
                }
            }
        }
    }

    public boolean isPaused;
    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "App Paused");
        isPaused = true;
        if (session != null)
        {
            if(room != null)
                room.unpublish();
            displayRotationHelper.onPause();
            glsurfaceView.onPause();
            session.pause();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (roomManager != null)
        {
            roomManager.disconnect();
        }
        uiHandler.StopNotification();
    }

    public void Disconnect()
    {
        connected = false;

        if(stream != null)
        {
            stream.stop();
            stream = null;
        }

        if(room != null)
        {
            room.unpublish();
            room = null;
        }

        if(roomManager != null)
        {
            roomManager.disconnect();
            roomManager = null;
        }

        if(screenRecorder != null)
        {
            screenRecorder.StopRecording();
            screenRecorder = null;
        }
    }

    private class ParticipantView
    {
        SurfaceViewRenderer surfaceViewRenderer;
        TextView login;

        public ParticipantView(SurfaceViewRenderer surfaceViewRenderer, TextView login)
        {
            this.surfaceViewRenderer = surfaceViewRenderer;
            this.login = login;
        }
    }

    public void SetLandscapeParams ()
    {
        ViewGroup.LayoutParams layoutParams = glsurfaceView.getLayoutParams();
        layoutParams.width = 1280;
        layoutParams.height = 720;
        glsurfaceView.setLayoutParams(layoutParams);
        glsurfaceView.invalidate();

        resetLocalCam = true;
        uiHandler.AssignUIElements(false);
        screenSize = GetScreeenSize();
    }

    public void SetPortraitParams ()
    {
        ViewGroup.LayoutParams layoutParams = glsurfaceView.getLayoutParams();
        layoutParams.width = 720;
        layoutParams.height = 1280;
        glsurfaceView.setLayoutParams(layoutParams);
        glsurfaceView.invalidate();

        resetLocalCam = true;
        uiHandler.AssignUIElements(false);
        screenSize = GetScreeenSize();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        adjustFullScreen(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            Log.d(TAG, "ScreenSize : " + screenSize);
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            isLandscape = true;
            SetLandscapeParams();
            if(uiHandler.backCam && VideoCapturerAndroid.arCorePresent)
                SendMessage("CTRL:-LS");
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            Log.d(TAG, "ScreenSize : " + screenSize);
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            isLandscape = false;
            SetPortraitParams();
            if(uiHandler.backCam && VideoCapturerAndroid.arCorePresent)
                SendMessage("CTRL:-PT");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            adjustFullScreen(getResources().getConfiguration());
        }
    }

    private void adjustFullScreen(Configuration config)
    {
//        final View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        Point size = GetScreeenSize();
        Log.d(TAG, "Size : " + size + "ScreenSize : " + screenSize);
        if((size.x == screenSize.y && size.y == screenSize.x))
            Log.d(TAG, "Screen Rotated");
        else
            uiHandler.setUItoPiP(!screenSize.toString().equals(size.toString()));
    }
}