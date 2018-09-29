package com.takeleap.tasqar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import org.apache.commons.math3.complex.Quaternion;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Set;

import processing.core.PVector;

/**
 * Created by TakeLeap05 on 02-08-2018.
 */

public class PointRenderer
{
    private static final String VERTEX_SHADER_NAME = "shaders/object.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/object.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 50000;

    private int vertexVboId;
    private int normalVboId;

    private int programName;
    private int positionAttribute;
    private int normalAttribute;

    private int modelViewProjectionUniform;

    private int colorUniform;

    private Thread makeExtrusionVerticesThread = null;
    private Thread updateAnchorsThread = null;

    private final ObjectRenderer arrowObject = new ObjectRenderer();
    private final ObjectRenderer blinkingLightObject = new ObjectRenderer();

    public  TASQAR_PoseInfoList poseInfoList = new TASQAR_PoseInfoList();

    public void UpdateCurrentColor(String userName, String currentColor)
    {
        poseInfoList.UpdateCurrentColor(userName, currentColor);
    }

    public void DestroyAll()
    {
        if(makeExtrusionVerticesThread != null)
            makeExtrusionVerticesThread.interrupt();

        makeExtrusionVerticesThread = null;

        if(updateAnchorsThread != null)
            updateAnchorsThread.interrupt();

        updateAnchorsThread = null;

        poseInfoList.DestroyAll();
    }

    PointRenderer()
    {
        MakeExtrusionVerticesThreadLoop();

        UpdateAnchorsThreadLoop();
    }

    public void createOnGlThread(Context context, String diffuseTextureAssetName) throws IOException
    {
        ShaderUtil.checkGLError(VideoChatActivity.TAG, "before create");

        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        vertexVboId = buffers[0];
        normalVboId = buffers[1];

        ShaderUtil.checkGLError(VideoChatActivity.TAG, "buffer alloc");

        int vertexShader = ShaderUtil.loadGLShader(VideoChatActivity.TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader = ShaderUtil.loadGLShader(VideoChatActivity.TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        programName = GLES20.glCreateProgram();
        GLES20.glAttachShader(programName, vertexShader);
        GLES20.glAttachShader(programName, passthroughShader);
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        ShaderUtil.checkGLError(VideoChatActivity.TAG, "program");

        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        normalAttribute = GLES20.glGetAttribLocation(programName, "a_Normal");
        colorUniform = GLES20.glGetUniformLocation(programName, "color");
//        textureUniform = GLES20.glGetUniformLocation(programName, "u_Texture");
//        tileCountUniform = GLES20.glGetUniformLocation(programName, "tileCount");
//        tileSizeUniform = GLES20.glGetUniformLocation(programName, "tileSize");

        ShaderUtil.checkGLError(VideoChatActivity.TAG, "program  params");

        // Read the texture.
        Bitmap textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(diffuseTextureAssetName));

//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glGenTextures(textures.length, textures, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
//        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

//        textureBitmap.recycle();

        arrowObject.createOnGlThread(/*context=*/ context, "models/arrow_v3.obj", "models/arrow_tex.png");
        arrowObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

        blinkingLightObject.createOnGlThread(/*context=*/ context, "models/circle_v2.obj", "models/arrow_tex.png");
        blinkingLightObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void AddBreak(String userName)
    {
        poseInfoList.AddBreak(userName);
        poseInfoList.RemoveAllZeroAnchorsList();
    }

    public void AddPoint(HitResult hitResult, String userName, int anchorListType)
    {
        poseInfoList.AddPoint(hitResult, userName, anchorListType);
    }

    public static float clamp(float val, float min, float max)
    {
        return Math.max(min, Math.min(max, val));
    }

    public void UpdateAnchorsThreadLoop()
    {
        updateAnchorsThread = new Thread(new Runnable() {
            @Override
            public void run() {

                long sleepTime = 1000;

                while (true)
                {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(VideoChatActivity.getInstance() != null)
                    {
                        if(VideoChatActivity.getInstance().camera != null)
                        {
                            if (isCameraMovedRotatedALot())
                            {
                                poseInfoList.UpdateAllAnchors(frustumVisibilityTester);
                            }

                            previousCameraPose = VideoChatActivity.getInstance().camera.getPose();
                        }
                    }
                }
            }
        });

        updateAnchorsThread.start();
    }

    public void MakeExtrusionVerticesThreadLoop()
    {
        makeExtrusionVerticesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    poseInfoList.MakeExtrusionVerticesArrays(frustumVisibilityTester);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        makeExtrusionVerticesThread.start();
    }

    public static   float   CAM_DISTANCE_THRESHOLD = 0.1f;
    public static   float   CAM_ANGLE_THRESHOLD = 5.0f;
    public boolean  isCameraMovedRotatedALot()
    {
        if(VideoChatActivity.getInstance().camera == null)
            return false;

        float[] previousCameraTranslation = previousCameraPose.getTranslation();
        float[] currentCameraTranslation = VideoChatActivity.getInstance().camera.getPose().getTranslation();

        float camDistanceDelta = PVector.dist(new PVector(previousCameraTranslation[0], previousCameraTranslation[1], previousCameraTranslation[2]),
                new PVector(currentCameraTranslation[0], currentCameraTranslation[1], currentCameraTranslation[2]));

        if(camDistanceDelta > CAM_DISTANCE_THRESHOLD)
        {
//            Log.d(TAG, "DISTANCE CHANGE " + camDistanceDelta);
            return true;
        }

        float[] previousCameraQuaternion = previousCameraPose.getRotationQuaternion();
        float[] currentCameraQuaternion = VideoChatActivity.getInstance().camera.getPose().getRotationQuaternion();

        Quaternion quatPreviousCameraQuaternion = new Quaternion(previousCameraQuaternion[0], previousCameraQuaternion[1], previousCameraQuaternion[2], previousCameraQuaternion[3]);
        Quaternion quatCurrentCameraQuaternion = new Quaternion(currentCameraQuaternion[0], currentCameraQuaternion[1], currentCameraQuaternion[2], currentCameraQuaternion[3]);

        double previousAngle = Math.toDegrees(Math.acos(quatPreviousCameraQuaternion.getScalarPart()) * 2.0f);
        double currentAngle = Math.toDegrees(Math.acos(quatCurrentCameraQuaternion.getScalarPart()) * 2.0f);

        if(Math.abs(previousAngle - currentAngle) > CAM_ANGLE_THRESHOLD) {

//            Log.d(TAG, "ANGLE CHANGE " + Math.abs(previousAngle - currentAngle));
            return true;
        }

        return false;
    }

    public void DrawAnchorList(AnchorList list, float[] cameraView, float[] cameraPerspective)
    {
        if(list != null)
        {
            if(list.numAnchors < 3)
                return;

            list.calcVertices(frustumVisibilityTester);

            float[] modelMatrix = list.modelMatrix;

            if(modelMatrix == null)
                return;

            float[] modelViewMatrix = new float[16];
            float[] modelViewProjectionMatrix = new float[16];
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
            GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

            float[] vertices = list.vertices;
            float[] normals = list.normals;

            if(vertices == null)
                return;

            float[] anchorListColor = list.listColor;
            GLES20.glUniform3f(colorUniform, anchorListColor[0], anchorListColor[1], anchorListColor[2]);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLE_STRIP);

            vertices = list.verticesStartCap;
            normals = list.normalsStartCap;
            GLES20.glUniform3f(colorUniform, 0.1f, 0.8f, 0.2f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);

            vertices = list.verticesEndCap;
            normals = list.normalsEndCap;
            GLES20.glUniform3f(colorUniform, 0.8f, 0.1f, 0.2f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);
        }
    }

    void DrawArrow (Anchor anchor, float[] viewmtx, float[] projmtx, float[] objColor)
    {
        float scaleFactor = 0.3f;
        if (anchor.getTrackingState() != TrackingState.TRACKING)
        {
            return;
        }

        float[] anchorMatrix = new float[16];

        Pose anchorPose = anchor.getPose();
        Pose newPose = new Pose(anchorPose.getTranslation(), new float[]{0.0f, 0.0f, 0.0f, 1.0f});

        newPose.toMatrix(anchorMatrix, 0);

        arrowObject.updateModelMatrix(anchorMatrix, scaleFactor);
        arrowObject.draw(viewmtx, projmtx, objColor);
    }

    float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }

    void DrawBlinkingLight (Anchor anchor, float[] viewmtx, float[] projmtx, float scaleCounter)
    {
        float[] objColor = new float[4];

        float[] redColor = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
        float[] orangeColor = new float[]{0.0f, 0.647f, 1.0f, 0.0f};

        objColor[0] = lerp(redColor[0], orangeColor[0], scaleCounter);
        objColor[1] = lerp(redColor[1], orangeColor[1], scaleCounter);
        objColor[2] = lerp(redColor[2], orangeColor[2], scaleCounter);
        objColor[3] = lerp(redColor[3], orangeColor[3], scaleCounter);

        if (anchor.getTrackingState() != TrackingState.TRACKING)
        {
            return;
        }

        float[] anchorMatrix = new float[16];

        float minScale = 0f;
        float maxScale = 0.03f;

        float currentScale = lerp(minScale, maxScale, scaleCounter);

        Pose displayOrientedPose = VideoChatActivity.getInstance().camera.getDisplayOrientedPose();
        Pose anchorPose = anchor.getPose();

        Pose cameraFacingPose = new Pose(anchorPose.getTranslation(), displayOrientedPose.getRotationQuaternion());

        cameraFacingPose.toMatrix(anchorMatrix, 0);

        blinkingLightObject.updateModelMatrix(anchorMatrix, currentScale);
        blinkingLightObject.draw(viewmtx, projmtx, objColor);
    }

    public Pose previousCameraPose = Pose.IDENTITY;
    public FrustumVisibilityTester frustumVisibilityTester = new FrustumVisibilityTester();
    public void draw(float[] cameraView, float[] cameraPerspective) {

        try
        {
        frustumVisibilityTester.calculateFrustum(cameraPerspective, cameraView);

        if(poseInfoList.firstTranslationSet && poseInfoList.isWorldReferenceChanged())
        {
            poseInfoList.firstTranslationSet = false;

            poseInfoList.UpdateAllAnchors(frustumVisibilityTester);

            poseInfoList.DirtyAllAnchorLists();
        }

        GLES20.glEnable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(VideoChatActivity.TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(normalAttribute);

        Set<String> poseInfoKeys = poseInfoList.GetKeySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        while (poseInfoKeysIterator.hasNext())
        {
            PoseInfo poseInfo = poseInfoList.GetUserPoseInfo(poseInfoKeysIterator.next());

            if(poseInfo.currentUserAnchorList != null)
                DrawAnchorList(poseInfo.currentUserAnchorList, cameraView, cameraPerspective);

            int numAnchorsLists = poseInfo.anchorsLists.size();

            for(int anchorListCount = 0; anchorListCount < numAnchorsLists; anchorListCount++)
            {
                AnchorList anchorList = poseInfo.anchorsLists.get(anchorListCount);

                if(anchorList.getAnchorListType() == 1)
                {
                    TASQAR_Anchor tasqar_anchor = anchorList.anchors.get(0);

                    DrawArrow(tasqar_anchor.anchor, cameraView, cameraPerspective, anchorList.listColor);

                    continue;
                }
                else if(anchorList.getAnchorListType() == 2)
                {
                    TASQAR_Anchor tasqar_anchor = anchorList.anchors.get(0);

                    DrawBlinkingLight(tasqar_anchor.anchor, cameraView, cameraPerspective, anchorList.scaleCounter);

                    continue;
                }

                GLES20.glUseProgram(programName);

                float[] modelMatrix = anchorList.modelMatrix;

                if(modelMatrix == null)
                    continue;

                float[] modelViewMatrix = new float[16];
                float[] modelViewProjectionMatrix = new float[16];
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
                Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
                GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

                float[] vertices = anchorList.vertices;
                float[] normals = anchorList.normals;

                if(vertices == null)
                    continue;

                float[] anchorListColor = anchorList.listColor;
                GLES20.glUniform3f(colorUniform, anchorListColor[0], anchorListColor[1], anchorListColor[2]);
                DrawVertices(vertices, normals, GLES20.GL_TRIANGLE_STRIP);

                vertices = anchorList.verticesStartCap;
                normals = anchorList.normalsStartCap;
                GLES20.glUniform3f(colorUniform, 0.1f, 0.8f, 0.2f);
                DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);

                vertices = anchorList.verticesEndCap;
                normals = anchorList.normalsEndCap;
                GLES20.glUniform3f(colorUniform, 0.8f, 0.1f, 0.2f);
                DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);
            }
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(normalAttribute);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDisable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(VideoChatActivity.TAG, "Draw");

        }catch (IndexOutOfBoundsException ex)
        {
            ex.printStackTrace();
        }
    }

    void DrawVertices(float[] vertices, float[] normals, int shape)
    {
        FloatBuffer vertexBuffer = FloatBuffer.wrap(vertices);
        FloatBuffer normalBuffer = FloatBuffer.wrap(normals);

        final int vertexStride = 3 * Float.BYTES;

        int numVertices = vertices.length / 3;

        { // VERTEX
            // bind VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexVboId);
            // fill VBO with data
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, Float.BYTES * vertices.length, vertexBuffer, GLES20.GL_DYNAMIC_DRAW);
            // associate currently bound VBO with shader attribute
            GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, vertexStride, 0);
        }

        { // NORMALS
            // bind VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalVboId);
            // fill VBO with data
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, Float.BYTES * normals.length, normalBuffer, GLES20.GL_DYNAMIC_DRAW);
            // associate currently bound VBO with shader attribute
            GLES20.glVertexAttribPointer(normalAttribute, 3, GLES20.GL_FLOAT, false, vertexStride, 0);
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glDrawArrays(shape, 0, numVertices);
    }

    FloatBuffer allocateDirectFloatBuffer(int n)
    {
        return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void Undo(String username)
    {
        boolean undoResult = poseInfoList.Undo(username);

        if(undoResult)
        {
            poseInfoList.DirtyAllAnchorLists();
        }
    }
}
