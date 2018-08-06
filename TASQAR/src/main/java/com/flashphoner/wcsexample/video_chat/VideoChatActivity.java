package com.flashphoner.wcsexample.video_chat;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import com.flashphoner.fpwcsapi.MediaDeviceList;
import com.flashphoner.fpwcsapi.bean.Connection;
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
import com.flashphoner.fpwcsapi.webrtc.MediaDevice;
import com.flashphoner.fpwcsapi.webrtc.WebRTCMediaProvider;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.obsez.android.lib.filechooser.tool.DirAdapter;

import org.android.opensource.libraryyuv.Libyuv;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturerAndroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


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
    String wcsURL = "ws://123.176.34.172:8080";
//    String roomName = "room-cd696c";
    String roomName = "TLSkypeRoom-CoolRoom";
//    UI references.

    private ImageButton mConnectButton;
    private ImageButton mFileUploadButton;
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

    public MainUIHandler uiHandler;
    public ScreenRecorder  screenRecorder;


    private ParticipantView participantView;

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
    private Stream stream;
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
    public GLSurfaceView glsurfaceView;
    private DisplayRotationHelper displayRotationHelper;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PointRenderer      pointRenderer = new PointRenderer();

    private final ObjectRenderer virtualObject = new ObjectRenderer();
//    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final float[] anchorMatrix = new float[16];

    // Anchors created from taps used for object placing with a given color.
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;

        public ColoredAnchor(Anchor a, float[] color4f, Pose finalPose) {
            this.anchor = a;
            this.color = color4f;
            this.anchor.getPose().compose(finalPose);
        }
    }

    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();

    private int mWidth;
    private int mHeight;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Log.d(TAG, "SURFACE ASD PARAMS " + glsurfaceView.getWidth() + " " + glsurfaceView.getHeight());

        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(/*context=*/ this);
            pointRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(/*context=*/ this);

            virtualObject.createOnGlThread(/*context=*/ this, "models/sphere.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

//            virtualObjectShadow.createOnGlThread(this, "models/andy_shadow.obj", "models/andy_shadow.png");
//            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
//            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

//        width = 1280;
//        height = 720;

        screenWidth = width;
        screenHeight = height;

        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);

        mWidth = 720; //width;
        mHeight = 1280; //height;

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
            public void run() {

                Toast toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    public static VideoChatActivity getInstance() {
        if (Instance == null) {
            Instance = new VideoChatActivity();
        }
        return Instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Instance = this;

        currentActivityIntent = getIntent();
        String Message = currentActivityIntent.getStringExtra("MIN");
        screenSize = GetScreeenSize();

        WebRTCMediaProvider.cameraID = 1;

        SetupCallScreen();

        android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (ActivityCompat.checkSelfPermission(VideoChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(VideoChatActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    FILEWRITE_REQUEST_CODE);
        }

        applicationContext = getApplicationContext();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /**
         * Initialization of the API.
         */
        Flashphoner.init(this);

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
    protected void onResume() {
        super.onResume();

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

        if(screenRecorder != null)
            screenRecorder.ResumeRecording();
    }

    public void AddBreak()
    {
        motionEvents.clear();
        pointRenderer.AddBreak();
    }

    void SetupCallScreen ()
    {
        setContentView(R.layout.activity_ui);
        localRenderer = findViewById(R.id.CurrentRender);
        remoteRenderer = findViewById(R.id.StreamRender);
        participantView = new ParticipantView(remoteRenderer, mParticipantName);
        mConnectButton = findViewById(R.id.CallExpertButton);
        mFileUploadButton = findViewById(R.id.UploadFileButton);
        mPlaneOrPointButton = findViewById(R.id.pointorplanebutton);

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

        uiHandler = new MainUIHandler(Instance);

        loginName = findViewById(R.id.UserName);
        loginName.setText(Build.MODEL);

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
            public void onCameraSwitchDone(boolean frontCamera) {

                if(!frontCamera && VideoCapturerAndroid.arCorePresent)
                {
                    if (!openingARCORE && !arcoreRunning)
                        StartARCORE();
                }
            }

            @Override
            public void onCameraSwitchError(String s) {

            }
        };

        mPlaneOrPointButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TogglePointPlaneSpawn();
            }
        });

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
                        .withAdapterSetter(new ChooserDialog.AdapterSetter() {
                            @Override
                            public void apply(DirAdapter adapter) {
                                //
                            }
                        })
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                Toast.makeText(ctx, "FILE: " + path, Toast.LENGTH_SHORT).show();
                                InputStream fileInputStream = null;
                                try {
                                    fileInputStream = new FileInputStream(pathFile);
                                    UploadFile(path, fileInputStream);

                                } catch (FileNotFoundException e) {
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

//                nHandler.post(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        Intent fileintent = new Intent(Intent.ACTION_GET_CONTENT);
//                        fileintent.setType("gagt/sdf");
//                        try
//                        {
//                            startActivityForResult(fileintent, PICKFILE_RESULT_CODE);
//                        }
//                        catch (ActivityNotFoundException e)
//                        {
//                            Log.e("tag", "No activity can handle picking a file. Showing alternatives.");
//                        }
//                    }
//                });
            }
        });

//        uiHandler.ToggleVideoView();
        Connect();

        mConnectButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                {
                    uiHandler.ToggleVideoView();
//                    Connect();
                }
            }
        });

        SetLocalRendererMirror();
    }

    @Override
    public void onDrawFrame(GL10 gl) {

//        Log.d(TAG, "ON DRAW");

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }

        displayRotationHelper.updateSessionIfNeeded(session);

        try
        {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            Frame frame = session.update();
            Camera camera = frame.getCamera();


            handleTap(frame, camera);

            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                byte[] ardata = GetScreenPixels();
                onPreviewFrame(ardata, null);
                return;
            }

//            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);
//
//            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            if(pointsOrPlaneSpawn)
            {
                // Visualize tracked points.
                PointCloud pointCloud = frame.acquirePointCloud();
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);

                // Application is responsible for releasing the point cloud resources after
                // using it.
                pointCloud.release();
            }
            else
            {
                // Visualize planes.
                planeRenderer.drawPlanes(session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);
            }

            // Visualize anchors created by touch.
//            float scaleFactor = 0.05f;
//            for (ColoredAnchor coloredAnchor : anchors) {
//                if (coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING) {
//                    continue;
//                }
//                // Get the current pose of an Anchor in world space. The Anchor pose is updated
//                // during calls to session.update() as ARCore refines its estimate of the world.
//                coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);
//
//                // Update and draw the model and its shadow.
//                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
////                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
//                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
////                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
//            }

            pointRenderer.draw(viewmtx, projmtx);

            byte[] ardata = GetScreenPixels();
            onPreviewFrame(ardata, null);
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
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

    public byte[] GetScreenPixels() throws IOException {
        buf.position(0);

        GLES20.glReadPixels(0, 0, screenWidth, screenHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);

//        Libyuv.ARGBScale(pixelData, screenWidth * 4, screenWidth, screenHeight, pixelData2, mWidth * 4, mWidth, mHeight);

//        Libyuv.ABGRToARGB(pixelData, mWidth * 4, argbBuffer, mWidth * 4, mWidth, -mHeight);
//        Libyuv.ARGBMirror(argbBuffer, mWidth * 4, argbBufferMirror, mWidth * 4, mWidth, mHeight);

        Libyuv.ARCORETONV21(pixelData, screenWidth * 4, tempData, argbBuffer, mWidth * 4, mWidth, -mHeight, ybuffer, uvbuffer, frameData);
//        Libyuv.ARGBToNV21(argbBuffer, mWidth * 4, mWidth, mHeight, ybuffer, uvbuffer);

//        System.arraycopy(ybuffer,0, frameData,0, mWidth * mHeight);
//        System.arraycopy(uvbuffer,0, frameData, mWidth * mHeight, mWidth * mHeight / 2);

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
    public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {

        WebRTCMediaProvider webRTCMediaProvider = WebRTCMediaProvider.getInstance();
        VideoCapturerAndroid videoCapturerAndroid = webRTCMediaProvider.videoCapturer;

        if (videoCapturerAndroid == null)
            return;

        if(VideoCapturerAndroid.arCorePresent && WebRTCMediaProvider.cameraID == 0)
        {
            long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            if (videoCapturerAndroid.eventsHandler != null && !videoCapturerAndroid.firstFrameReported) {
                videoCapturerAndroid.eventsHandler.onFirstFrameAvailable();
                videoCapturerAndroid.firstFrameReported = true;
            }

            int frameOrientation = 180; //videoCapturerAndroid.getFrameOrientation();

//            Log.d(TAG, "WITH AR CORE :" + videoCapturerAndroid.getFrameOrientation());

            if (videoCapturerAndroid.frameObserver != null)
                videoCapturerAndroid.frameObserver.onByteBufferFrameCaptured(data, videoCapturerAndroid.captureFormat.width,
                        videoCapturerAndroid.captureFormat.height, frameOrientation, captureTimeNs);
        }
        else
        {
//            try {
//                SaveBitmap(data, videoCapturerAndroid.captureFormat.width, videoCapturerAndroid.captureFormat.height);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            if (videoCapturerAndroid.eventsHandler != null && !videoCapturerAndroid.firstFrameReported) {
                videoCapturerAndroid.eventsHandler.onFirstFrameAvailable();
                videoCapturerAndroid.firstFrameReported = true;
            }

//           Log.d(TAG, "NO AR CORE :" + videoCapturerAndroid.getFrameOrientation() + " " + videoCapturerAndroid.captureFormat.width);

            int frameOri = videoCapturerAndroid.getFrameOrientation();

            videoCapturerAndroid.cameraStatistics.addFrame();
            videoCapturerAndroid.frameObserver.onByteBufferFrameCaptured(data, videoCapturerAndroid.captureFormat.width, videoCapturerAndroid.captureFormat.height, videoCapturerAndroid.getFrameOrientation(), captureTimeNs);
            if (videoCapturerAndroid.camera != null) {
                videoCapturerAndroid.camera.addCallbackBuffer(data);
            }
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

        MotionEvent motionEvent = MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, xVal, yVal, 0);
        TapHandle(motionEvent, width, height);
    }

    public void TapSend(int x, int y, int width, int height)
    {
        SendMessage("TAP: " + x + " " + y + " " + width + " " + height);

        DecodeTapMessage("TAP: " + x + " " + y + " " + width + " " + height);
    }

    ArrayList<MotionEvent> motionEvents = new ArrayList<MotionEvent>();

    public void TapHandle(MotionEvent event, float width, float height)
    {
        float xMul = event.getX() / width;
        float yMul = event.getY() / height;

        float xVal = screenWidth * xMul;
        float yVal = screenHeight * yMul;

        event.setLocation(xVal, yVal);

        motionEvents.add(event);
    }

    public  boolean pointsOrPlaneSpawn = false;

    private void handleTap(Frame frame, Camera camera)
    {
        while (motionEvents.size() > 0)
        {
            MotionEvent tap = motionEvents.get(0);
            motionEvents.remove(0);

            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING)
            {
//                Log.d(TAG, "MOVE TAP " + tap.getX() + " " + tap.getY());

                for (HitResult hit : frame.hitTest(tap)) {
//                if (anchors.size() >= 20) {
//                    anchors.get(0).anchor.detach();
//                    anchors.remove(0);
//                }

                    Trackable currentTrackable = hit.getTrackable();

//                    Log.d(TAG, "HIT FOUND");

                    SpawnPoint(hit);

//                if(pointsOrPlaneSpawn)
//                {
//                    if(currentTrackable instanceof com.google.ar.core.Point)
//                    {
//                        SpawnArrow(hit, camera);
//                    }
//                }
//                else
//                {
//                    if(currentTrackable instanceof com.google.ar.core.Plane)
//                    {
//                        SpawnArrow(hit, camera);
//                    }
//                }

                    break;
                }
            }
        }
    }


    public  void TogglePointPlaneSpawn()
    {
        pointsOrPlaneSpawn = !pointsOrPlaneSpawn;
    }

    private void SpawnArrow(HitResult hit, Camera camera)
    {
        float[] objColor = new float[]{66.0f, 133.0f, 244.0f, 255.0f};

        anchors.add(new ColoredAnchor(hit.createAnchor(), objColor, camera.getDisplayOrientedPose()));
    }

    private void SpawnPoint(HitResult hit)
    {
        pointRenderer.AddPoint(hit.createAnchor());
    }

    public void SetLocalRendererMirror()
    {
        if(WebRTCMediaProvider.cameraID == 0) {
            if (VideoCapturerAndroid.arCorePresent) {
                localRenderer.setMirror(true);
            } else {
                localRenderer.setMirror(false);
            }
        }
        else
        {
            if (VideoCapturerAndroid.arCorePresent) {
                localRenderer.setMirror(true);
            } else {
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

    public void StopARCORE() {
        if (session != null) {
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

    public void Connect() {
        if (connected)
            return;

        if (loginName.getText().toString().length() == 0) {
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

        roomManager.on(new RoomManagerEvent() {
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

                room.on(new RoomEvent() {
                    @Override
                    public void onState(final Room room) {

                        Log.d(TAG, "ON STATE");

                        if (permissionGiven && stream == null) {
                            stream = room.publish(localRenderer, VideoChatActivity.this);
                            stream.unmuteAudio();
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
                            if (participantView != null)
                            {
                                /**
                                 * Playback of the stream being published by the other participant is started with method Participant.play().
                                 * SurfaceViewRenderer to be used to display the video stream is passed when the method is called.
                                 */

                                participantPublishing = true;
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

                        /**
                         * When one of the other participants starts publishing, playback of the stream published by that participant is started.
                         */
                        if (participantView != null) {
                            participantPublishing = true;
                            Stream remoteStream = participant.play(participantView.surfaceViewRenderer);
                            remoteStream.unmuteAudio();
                        }
                    }

                    @Override
                    public void onFailed(Room room, final String info) {
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

                        if (messageReceived.contains(":FU"))
                        {
                            String messageAndroidID = messageReceived.substring(messageReceived.indexOf(":FU") + 3, messageReceived.indexOf("-/"));

                            if(messageAndroidID == android_id)
                            {
                                return;
                            }

                            messageReceived = messageReceived.replace(":FU", "");
                            messageReceived = messageReceived.replace(messageAndroidID, "");
                            messageReceived = messageReceived.replace("-/", "");
                            DownloadFile(messageReceived);
                        }
                        else if(messageReceived.contains("Disconnect"))
                        {
                            FloatingActionButton mEndButton = findViewById(R.id.EndCallButton);
                            mEndButton.callOnClick();
                        }
                        else if (messageReceived.contains("TAP: ") && WebRTCMediaProvider.cameraID == 0)
                        {
                            DecodeTapMessage(messageReceived);
                        }
                    }
                });
            }

            @Override
            public void onDisconnection(final Connection connection) {
                connected = false;
                Log.d(TAG, "ON DISCCONEASd");

                stream = null;
            }
        });
    }

    public void SendMessage(String message) {
        if (room == null)
            return;

        for (Participant participant : room.getParticipants()) {
            participant.sendMessage(message);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // TODO Fix no activity available
        if (data == null)
            return;
//        Log.d(TAG, "Data is" + data.getData().getPath());
        switch (requestCode)
        {
            case PICKFILE_RESULT_CODE:
            if (resultCode == RESULT_OK)
            {
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                try
                {
                    InputStream fileInputStream = contentResolver.openInputStream(data.getData());
                    String FilePath = data.getData().getPath();
                    Log.d(TAG, " The File Path is " + FilePath);
                     try
                     {
                         UploadFile(FilePath, fileInputStream);
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
                    Runnable runnable = new Runnable() {
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
            InputStream inputStream = new FileInputStream(filePath);

            UploadFile(filePath, inputStream);

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String filePath = "/sdcard/scr.png";
//                    InputStream inputStream = new FileInputStream(filePath);
//
//                    UploadFile(filePath, inputStream);
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    public void UploadFile(final String filePath, final InputStream inputStream) {

        final FTPManager ftpManager = new FTPManager();

        ftpManager.fileInputStream = inputStream;
        ftpManager.uploadORdownload = 1;
        ftpManager.applicationContext = getApplicationContext();
        ftpManager.filePath = filePath;
        if (ftpManager.running)
        {
            ShowToast("FTP Manager Running Already", this.getApplicationContext());
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper looper = mHandler.getLooper();
                looper.prepare();
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ftpManager.execute();
                    }
                });
            }
        }).start();
    }

    public void DownloadFile(String fileName) {

        String filePath = "/sdcard/ReceivedFiles/" + fileName;
        final FTPManager ftpManager = new FTPManager();

        if (ftpManager.running) {
            ShowToast("FTP Manager Running Already", this.getApplicationContext());
            return;
        }

        ftpManager.uploadORdownload = 2;
        ftpManager.applicationContext = getApplicationContext();
        ftpManager.filePath = filePath;
        new Thread(new Runnable() {
            @Override
            public void run() {

                mHandler.getLooper().prepare();
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ftpManager.execute();
                    }
                });
            }
        }).start();

    }

    public void OpenFile(String filePath) {
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
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

    @Override
    protected void onPause()
    {
        super.onPause();

        if (session != null) {

            displayRotationHelper.onPause();
            glsurfaceView.onPause();
            session.pause();
        }

        if(screenRecorder != null)
            screenRecorder.PauseRecording();;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (roomManager != null)
        {
            roomManager.disconnect();
        }
    }

    public void Disconnect()
    {
        connected = false;

        if(stream != null)
        {
            stream.stop();
            stream = null;
        }

        room.unpublish();
        roomManager.disconnect();

        room = null;

        if(screenRecorder != null)
        {
            screenRecorder.StopRecording();
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


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustFullScreen(newConfig);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
        uiHandler.setUItoPiP(!screenSize.toString().equals(size.toString()));
    }

}