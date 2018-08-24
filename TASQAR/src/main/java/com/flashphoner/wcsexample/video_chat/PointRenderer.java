package com.flashphoner.wcsexample.video_chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import processing.core.PApplet;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import shapes3d.EndCapContour;
import shapes3d.Extrusion;
import shapes3d.Mesh2DCore;
import shapes3d.S3D;
import shapes3d.utils.CS_ConstantScale;
import shapes3d.utils.Contour;
import shapes3d.utils.ContourScale;
import shapes3d.utils.MeshSection;
import shapes3d.utils.P_Bezier3D;
import shapes3d.utils.P_BezierSpline;
import shapes3d.utils.Path;

import static android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

/**
 * Created by TakeLeap05 on 02-08-2018.
 */

public class PointRenderer{

    public class TASQAR_Anchor
    {
        public  Anchor  anchor;

        public AnchorList   parentAnchorList;

        public TrackingState previousAnchorTrackingState;

        public  boolean     isPointVisibleToCamera = false;

        public boolean      isHitCameraRay = false;

        public TASQAR_Anchor(Anchor anchor, AnchorList parentAnchorList)
        {
            this.anchor = anchor;
            this.parentAnchorList = parentAnchorList;
            previousAnchorTrackingState = anchor.getTrackingState();
        }

        public void CheckDirty()
        {
            TrackingState currentTrackingState = anchor.getTrackingState();

            if(currentTrackingState != previousAnchorTrackingState)
            {
                parentAnchorList.isDirty = true;

                previousAnchorTrackingState = currentTrackingState;
            }
        }

        public  float   CAM_RAY_CAST_DISTANCE_THRESHOLD = 0.3f;
        public void CheckHitCameraRay(float[] camPosition, float[] camAxis, int anchorIndex, int numAnchors)
        {
            isHitCameraRay = false;

            if(VideoChatActivity.getInstance() != null) {
                List<HitResult> hitResults = VideoChatActivity.getInstance().frame.hitTest(camPosition, 0, camAxis, 0);

                if (hitResults.size() == 0)
                    return;

                int hitCount = 0;
                for (HitResult hitResult : hitResults) {
                    float[] hitPosition = hitResult.getHitPose().getTranslation();

                    float[] ourPosition = getPose().getTranslation();

//                    Log.d(TAG, "DISTANCE IS " + PVector.dist(new PVector(ourPosition[0], ourPosition[1], ourPosition[2]),
//                            new PVector(hitPosition[0], hitPosition[1], hitPosition[2])));

                    if (PVector.dist(new PVector(ourPosition[0], ourPosition[1], ourPosition[2]),
                            new PVector(hitPosition[0], hitPosition[1], hitPosition[2])) < CAM_RAY_CAST_DISTANCE_THRESHOLD) {
                        hitCount += 1;
                    }
                }

                if (hitResults.size() > 0)
                {
                    float percentHit = (float) hitCount / (float) hitResults.size();

//                    Log.d(TAG, "LOCAL CAM HIT PER:" + anchorIndex + " " + percentHit * 100 + " " + numAnchors);

                    isHitCameraRay = percentHit > 0.4f;
                }
            }
        }

        public Pose getPose() {
            return anchor.getPose();
        }

        public void detach() {
            anchor.detach();
        }

        public void UpdateAnchor() {

            Pose ourPose = getPose();

            float[] ourPosition = ourPose.getTranslation();

            isPointVisibleToCamera = frustumVisibilityTester.isPointInFrustum(ourPosition[0], ourPosition[1], ourPosition[2]);

            parentAnchorList.isDirty = true;
        }
    }

    public static  int nextAnchorListID = 1;

    public class AnchorList
    {
        public  int     anchorListID = 0;

        public ArrayList<TASQAR_Anchor> anchors = new ArrayList<TASQAR_Anchor>();
        public float[] vertices = null;
        public float[] normals = null;

        public float[] verticesStartCap = null;
        public float[] normalsStartCap = null;

        public float[] verticesEndCap = null;
        public float[] normalsEndCap = null;

        public  boolean     isDirty = true;
        public  int         numAnchors = 0;

        float[] modelMatrix = null;

        public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
            float[] scaleMatrix = new float[16];
            Matrix.setIdentityM(scaleMatrix, 0);
            scaleMatrix[0] = scaleFactor;
            scaleMatrix[5] = scaleFactor;
            scaleMatrix[10] = scaleFactor;
            Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
        }

        public void UpdateAllAnchors()
        {
            int numAnchors = anchors.size();

            for(int i = 0; i < numAnchors; i++)
            {
                anchors.get(i).UpdateAnchor();
            }
        }

        public void RemoveAllVertices()
        {
            vertices = null;
            normals = null;
            verticesEndCap = null;
            normalsEndCap = null;
            verticesStartCap = null;
            normalsStartCap = null;
            modelMatrix = null;
        }

        public void CheckDirty()
        {
            for(int i = 0; i < numAnchors; i++)
            {
                TASQAR_Anchor tasqar_anchor = anchors.get(i);
                tasqar_anchor.CheckDirty();
            }
        }

        public boolean isPointEqual(int index, float[] translation)
        {
            float[] translationFirst = anchors.get(index).getPose().getTranslation();

            if( Math.abs(translationFirst[0] - translation[0]) > 0.2f ||
                    Math.abs(translationFirst[1] - translation[1]) > 0.2f ||
                    Math.abs(translationFirst[2] - translation[2]) > 0.2f )
            {
                return  true;
            }

            return  false;
        }

        public void AddAnchor(TASQAR_Anchor anchor)
        {
            anchors.add(anchor);

            numAnchors = anchors.size();
        }

        public void calcVertices()
        {
            RemoveAllVertices();

            UpdateAllAnchors();

            ArrayList<PVector> listOfPoints = new ArrayList<>();

            float[] currentCameraPosition = VideoChatActivity.getInstance().camera.getPose().getTranslation();
            float[] currentCameraQuaternion = VideoChatActivity.getInstance().camera.getPose().getRotationQuaternion();
            float[] currentCameraAxis = new float[]{currentCameraQuaternion[0], currentCameraQuaternion[1], currentCameraQuaternion[2]};
            PVector vecCurrentCameraAxis = new PVector(currentCameraAxis[0], currentCameraAxis[1], currentCameraAxis[2]);
            vecCurrentCameraAxis.normalize();

            int numHitAnchors = 0;
            int numVisibleAnchors = 0;
            for (int i = 0; i < numAnchors; i++)
            {
//                Log.d(TAG, "Anchor Visiblity " + (anchors.get(i).isPointVisibleToCamera ? "YES" : "NO"));

                if(anchors.get(i).isPointVisibleToCamera)// || anchors.get(i).previousAnchorTrackingState != TrackingState.TRACKING)
                {
                    numVisibleAnchors += 1;
                }

                PVector toAnchor = new PVector( anchors.get(i).getPose().tx() - currentCameraPosition[0],
                        anchors.get(i).getPose().ty() - currentCameraPosition[1],
                        anchors.get(i).getPose().tz() - currentCameraPosition[2]);
                toAnchor.normalize();

                currentCameraAxis = new float[]{toAnchor.x, toAnchor.y, toAnchor.z};

                anchors.get(i).CheckHitCameraRay(currentCameraPosition, currentCameraAxis, i, numAnchors);

                if(anchors.get(i).isHitCameraRay)
                {
                    numHitAnchors += 1;
                }

                Pose pose = anchors.get(i).getPose();
                float[] originPoint = pose.getTranslation();
                listOfPoints.add(new PVector(originPoint[0], originPoint[1], originPoint[2]));
            }

            float visibleAccPercentage = (float)numVisibleAnchors / (float)numAnchors;
            float hitAccPercentage = (float)numHitAnchors / (float)numAnchors;

//            Log.d(TAG, "HIT PERCENTAGE: " + hitAccPercentage * 100 + "%" + " VISIBLE PERCENTAGE: " + visibleAccPercentage * 100 + "%");

            if(hitAccPercentage < 0.9f || visibleAccPercentage < 0.9f)
            {
                isDirty = false;
                return;
            }

            PVector[] pointVectors = listOfPoints.toArray(new PVector[listOfPoints.size()]);

            P_Bezier3D bezierCurve = new P_Bezier3D(pointVectors, pointVectors.length);

            Contour contour = getSphericalContour();
            ContourScale conScale = new CS_ConstantScale();
            conScale.scale(1f);
            contour.make_u_Coordinates();

            Extrusion e = new Extrusion(null, bezierCurve, 50, contour, conScale);
            e.drawMode(S3D.TEXTURE);

            Mesh2DCore mesh2DCore = (Mesh2DCore)e;

            MeshSection var1 = mesh2DCore.fullShape;

            int numVertices = (var1.eNS - 2) * var1.eEW * 3;

            vertices = new float[numVertices * 3];
            normals = new float[numVertices * 3];

            int pointCounter = 0;

            for(int var4 = var1.sNS; var4 < var1.eNS - 2; var4++) {

                for(int var5 = var1.sEW; var5 < var1.eEW; var5++) {
                    PVector p1 = mesh2DCore.coord[var5][var4];
                    PVector n1 = mesh2DCore.norm[var5][var4];
                    vertices[pointCounter] = p1.x;
                    vertices[pointCounter + 1] = p1.y;
                    vertices[pointCounter + 2] = p1.z;
                    normals[pointCounter] = n1.x;
                    normals[pointCounter + 1] = n1.y;
                    normals[pointCounter + 2] = n1.z;
                    pointCounter += 3;

                    PVector p2 = mesh2DCore.coord[var5][var4 + 1];
                    PVector n2 = mesh2DCore.norm[var5][var4 + 1];
                    vertices[pointCounter] = p2.x;
                    vertices[pointCounter + 1] = p2.y;
                    vertices[pointCounter + 2] = p2.z;
                    normals[pointCounter] = n2.x;
                    normals[pointCounter + 1] = n2.y;
                    normals[pointCounter + 2] = n2.z;
                    pointCounter += 3;

                    PVector p3 = mesh2DCore.coord[var5][var4 + 2];
                    PVector n3 = mesh2DCore.norm[var5][var4 + 2];
                    vertices[pointCounter] = p3.x;
                    vertices[pointCounter + 1] = p3.y;
                    vertices[pointCounter + 2] = p3.z;
                    normals[pointCounter] = n3.x;
                    normals[pointCounter + 1] = n3.y;
                    normals[pointCounter + 2] = n3.z;
                    pointCounter += 3;
                }
            }

            EndCapContour capContour = (EndCapContour)e.startEC;

            verticesStartCap = new float[capContour.triangles.length * 9];
            normalsStartCap = new float[capContour.triangles.length * 9];

            for(int i = 0, j = 0; i < capContour.triangles.length; i += 3, j += 9) {
                int v1 = capContour.triangles[i];
                int var2 = capContour.triangles[i + 1];
                int var3 = capContour.triangles[i + 2];

                normalsStartCap[j] = capContour.n.x;
                normalsStartCap[j + 1] = capContour.n.y;
                normalsStartCap[j + 2] = capContour.n.z;
                normalsStartCap[j + 3] = capContour.n.x;
                normalsStartCap[j + 4] = capContour.n.y;
                normalsStartCap[j + 5] = capContour.n.z;
                normalsStartCap[j + 6] = capContour.n.x;
                normalsStartCap[j + 7] = capContour.n.y;
                normalsStartCap[j + 8] = capContour.n.z;

                verticesStartCap[j] = capContour.edge[v1].x;
                verticesStartCap[j + 1] = capContour.edge[v1].y;
                verticesStartCap[j + 2] = capContour.edge[v1].z;
                verticesStartCap[j + 3] = capContour.edge[var2].x;
                verticesStartCap[j + 4] = capContour.edge[var2].y;
                verticesStartCap[j + 5] = capContour.edge[var2].z;
                verticesStartCap[j + 6] = capContour.edge[var3].x;
                verticesStartCap[j + 7] = capContour.edge[var3].y;
                verticesStartCap[j + 8] = capContour.edge[var3].z;
            }

            capContour = (EndCapContour)e.endEC;

            verticesEndCap = new float[capContour.triangles.length * 9];
            normalsEndCap = new float[capContour.triangles.length * 9];

            for(int i = 0, j = 0; i < capContour.triangles.length; i += 3, j += 9) {
                int v1 = capContour.triangles[i];
                int var2 = capContour.triangles[i + 1];
                int var3 = capContour.triangles[i + 2];

                normalsEndCap[j] = capContour.n.x;
                normalsEndCap[j + 1] = capContour.n.y;
                normalsEndCap[j + 2] = capContour.n.z;
                normalsEndCap[j + 3] = capContour.n.x;
                normalsEndCap[j + 4] = capContour.n.y;
                normalsEndCap[j + 5] = capContour.n.z;
                normalsEndCap[j + 6] = capContour.n.x;
                normalsEndCap[j + 7] = capContour.n.y;
                normalsEndCap[j + 8] = capContour.n.z;

                verticesEndCap[j] = capContour.edge[v1].x;
                verticesEndCap[j + 1] = capContour.edge[v1].y;
                verticesEndCap[j + 2] = capContour.edge[v1].z;
                verticesEndCap[j + 3] = capContour.edge[var2].x;
                verticesEndCap[j + 4] = capContour.edge[var2].y;
                verticesEndCap[j + 5] = capContour.edge[var2].z;
                verticesEndCap[j + 6] = capContour.edge[var3].x;
                verticesEndCap[j + 7] = capContour.edge[var3].y;
                verticesEndCap[j + 8] = capContour.edge[var3].z;
            }

            modelMatrix = new float[16];
            float[] anchorMatrix = new float[16];
            anchors.get(0).getPose().toMatrix(anchorMatrix, 0);

            updateModelMatrix(anchorMatrix, 1.0f);

            isDirty = false;
        }

        public  AnchorList()
        {
            anchorListID = nextAnchorListID;

            nextAnchorListID += 1;

            this.isDirty = true;
        }

        public void DetachAllAnchors()
        {
            numAnchors = anchors.size();

            for(int i = 0; i < numAnchors; i++)
            {
                anchors.get(i).detach();
            }

            anchors.clear();

            numAnchors = anchors.size();

            vertices = null;
            normals = null;
        }
    }

    public void UpdateAllAnchors()
    {
        int numAnchorList = anchorsLists.size();

        for(int i = 0; i < numAnchorList; i++)
        {
            AnchorList anchorList = anchorsLists.get(i);
            anchorList.UpdateAllAnchors();
        }
    }

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

    private ArrayList<AnchorList> anchorsLists = new ArrayList<AnchorList>();

    private Pose previousPose = null;

    private float[] firstTranslation = new float[3];
    private boolean firstTranslationSet = false;

    private Thread makeExtrusionVerticesThread = null;
    private Thread updateAnchorsThread = null;

    public void DestroyAll()
    {
        if(makeExtrusionVerticesThread != null)
            makeExtrusionVerticesThread.interrupt();

        makeExtrusionVerticesThread = null;

        if(updateAnchorsThread != null)
            updateAnchorsThread.interrupt();

        updateAnchorsThread = null;

        int numAnchorLists = anchorsLists.size();

        for(int i = 0; i < numAnchorLists; i++)
        {
            anchorsLists.get(i).DetachAllAnchors();
        }
    }

    PointRenderer()
    {
        MakeExtrusionVerticesThreadLoop();

        UpdateAchorsThreadLoop();
    }

    public void createOnGlThread(Context context, String diffuseTextureAssetName) throws IOException
    {
        ShaderUtil.checkGLError(TAG, "before create");

        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        vertexVboId = buffers[0];
        normalVboId = buffers[1];

        ShaderUtil.checkGLError(TAG, "buffer alloc");

        int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        programName = GLES20.glCreateProgram();
        GLES20.glAttachShader(programName, vertexShader);
        GLES20.glAttachShader(programName, passthroughShader);
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        ShaderUtil.checkGLError(TAG, "program");

        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        normalAttribute = GLES20.glGetAttribLocation(programName, "a_Normal");
        colorUniform = GLES20.glGetUniformLocation(programName, "color");
//        textureUniform = GLES20.glGetUniformLocation(programName, "u_Texture");
//        tileCountUniform = GLES20.glGetUniformLocation(programName, "tileCount");
//        tileSizeUniform = GLES20.glGetUniformLocation(programName, "tileSize");

        ShaderUtil.checkGLError(TAG, "program  params");

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

        textureBitmap.recycle();

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void RemoveAllZeroAnchorsList()
    {
        int numAnchorsLists = anchorsLists.size();

        for(int i = 0; i < numAnchorsLists; i++)
        {
            AnchorList anchorList = anchorsLists.get(i);

            if(anchorList.numAnchors < 2)
            {
                anchorList.DetachAllAnchors();

                anchorsLists.remove(i);

                numAnchorsLists = anchorsLists.size();

                i = 0;
            }
        }
    }

    public void AddBreak()
    {
        previousPose = null;

//        synchronized (anchorsLists)
        {
            if(currentAnchorList != null)
                anchorsLists.add(currentAnchorList);
        }

        RemoveAllZeroAnchorsList();

        currentAnchorList = null;

        pointerCounter = 0;
    }

    int pointerCounter = 0;
    public void AddPoint(HitResult hitResult)
    {
//        if(pointerCounter != 0)
//        {
//            if (pointerCounter % 3 != 0)
//            {
//                anchor.detach();
//                pointerCounter += 1;
//                return;
//            }
//        }

        Pose hitPose = hitResult.getHitPose();

        if(previousPose != null)
        {
            float threshold = 0.01f;

            float px = previousPose.tx();
            float py = previousPose.ty();
            float pz = previousPose.tz();

            float hx = hitPose.tx();
            float hy = hitPose.ty();
            float hz = hitPose.tz();

            Vector3D currentPoint = new Vector3D(hx, hy, hz);
            Vector3D previousPoint = new Vector3D(px, py, pz);
            float distance = (float) Vector3D.distance(currentPoint, previousPoint);

            if(distance > threshold)
            {
                double controlPointHeight = 0.001;
                Vector3D controlPoint = previousPoint.add(currentPoint).scalarMultiply(0.5);
                controlPoint = new Vector3D(controlPoint.getX(), controlPoint.getY() + controlPointHeight, controlPoint.getZ());

                int numPointsToAdd = (int) (distance / threshold);
                int totalPoints = numPointsToAdd + 2;
                float inc = 1.0f / totalPoints;

                for (float t = 0.0f; t <= 1; t += inc)
                {
                    double newX = (1.0f - t) * (1.0f - t) * previousPoint.getX() + 2.0f * (1.0f - t) * t * controlPoint.getX() + t * t * currentPoint.getX();
                    double newY = (1.0f - t) * (1.0f - t) * previousPoint.getY() + 2.0f * (1.0f - t) * t * controlPoint.getY() + t * t * currentPoint.getY();
                    double newZ = (1.0f - t) * (1.0f - t) * previousPoint.getZ() + 2.0f * (1.0f - t) * t * controlPoint.getZ() + t * t * currentPoint.getZ();

                    Pose newPose = new Pose(new float[]{(float) newX, (float) newY, (float) newZ}, new float[]{0, 0, 0, 0});
                    AddAnchor(hitResult, newPose);
                }
            }
            else
            {
                AddAnchor(hitResult, hitPose);
            }

//            Log.d(TAG, "NEW POINT " + px + " " + py + " " + pz + " " + hx + " " + hy + " " + hz + " " + currentPoint.getX() + " " + currentPoint.getY() + " " + currentPoint.getZ() + " " + distance);
        }
        else
        {
            AddAnchor(hitResult, hitPose);
        }

        previousPose = hitPose;

//        Log.d(TAG, "NEW POSE " + anchor.getPose().tx() + " " + anchor.getPose().ty() + " " + anchor.getPose().tz());
    }

    public AnchorList   currentAnchorList = null;
    public void AddAnchor(HitResult hitResult, Pose hitPose)
    {
        if(currentAnchorList == null)
        {
            currentAnchorList = new AnchorList();
        }

        Anchor anchor = hitResult.getTrackable().createAnchor(hitPose);
        currentAnchorList.AddAnchor(new TASQAR_Anchor(anchor, currentAnchorList));
        pointerCounter += 1;
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public class Building extends Contour {

        public Building(PVector[] c) {
            this.contour = c;
        }
    }

    public Contour getSphericalContour() {

        float scale = 0.005f;

        int numPoints = 30;

        PVector[] points = new PVector[numPoints];

        float angleInc = (float) (2 * Math.PI / numPoints);

        for(int i = 0; i < numPoints; i++)
        {
            points[i] = new PVector((float)Math.sin(i * angleInc), (float)Math.cos(i * angleInc));
            points[i].mult(scale);
        }

        return new Building(points);
    }

//    FloatBuffer vertexBuffer = null;
//    FloatBuffer normalBuffer = null;

    boolean isWorldReferenceChanged()
    {
        if(anchorsLists.size() > 0)
        {
            AnchorList anchorsList = anchorsLists.get(0);

            if (anchorsList.numAnchors > 0) {
                boolean firstPointChanged = anchorsList.isPointEqual(0, firstTranslation);

                if (firstPointChanged) {
                    return true;
                }
            }
        }

        return false;
    }

    void MakeExtrusionVerticesArrays()
    {
        int numAnchorsLists = anchorsLists.size();
        for(int anchorsListCount = 0; anchorsListCount < numAnchorsLists; anchorsListCount++) {

            AnchorList anchorList = anchorsLists.get(anchorsListCount);

            anchorList.CheckDirty();

            if (anchorList.isDirty)
            {
//                Log.d(TAG, "Calculating Vertices for " + anchorList.anchorListID);
                anchorList.calcVertices();
            }
        }

        if(numAnchorsLists > 0 && !firstTranslationSet)
        {
            AnchorList anchorList = anchorsLists.get(0);
            TASQAR_Anchor tasqar_anchor = anchorList.anchors.get(0);

            firstTranslationSet = true;
            firstTranslation[0] = tasqar_anchor.getPose().tx();
            firstTranslation[1] = tasqar_anchor.getPose().ty();
            firstTranslation[2] = tasqar_anchor.getPose().tz();
        }
    }

    public void DirtyAllAnchorLists() {

        int numAnchorLists = anchorsLists.size();

        for (int i = 0; i < numAnchorLists; i++) {
            anchorsLists.get(i).isDirty = true;
        }
    }


    public void UpdateAchorsThreadLoop()
    {
        updateAnchorsThread = new Thread(new Runnable() {
            @Override
            public void run() {

                long sleepTime = 1000;

                while (true)
                {
                    if(VideoChatActivity.getInstance() != null)
                    {
                        if(VideoChatActivity.getInstance().camera != null)
                        {
                            if (isCameraMovedRotatedALot())
                            {
                                UpdateAllAnchors();

//                                Log.d(TAG, "CAMERA CHANGED A LOT JI");
                            }

                            previousCameraPose = VideoChatActivity.getInstance().camera.getPose();
                        }
                    }

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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

                    MakeExtrusionVerticesArrays();

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


    public Pose previousCameraPose = Pose.IDENTITY;
    public FrustumVisibilityTester frustumVisibilityTester = new FrustumVisibilityTester();
    public void draw(float[] cameraView, float[] cameraPerspective) {

        frustumVisibilityTester.calculateFrustum(cameraPerspective, cameraView);

        if(firstTranslationSet && isWorldReferenceChanged())
        {
//            Log.d(TAG, "WORLD REFERENCE CHANGED");

            firstTranslationSet = false;

            UpdateAllAnchors();

            DirtyAllAnchorLists();
        }

        GLES20.glEnable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(normalAttribute);

        int numAnchorsLists = anchorsLists.size();

        for(int anchorListCount = 0; anchorListCount < numAnchorsLists; anchorListCount++)
        {
            AnchorList anchorList = anchorsLists.get(anchorListCount);

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

            GLES20.glUniform3f(colorUniform, 0.2f, 0.9f, 0.3f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLE_STRIP);

            vertices = anchorList.verticesStartCap;
            normals = anchorList.normalsStartCap;
            GLES20.glUniform3f(colorUniform, 0.9f, 0.2f, 0.3f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);

            vertices = anchorList.verticesEndCap;
            normals = anchorList.normalsEndCap;
            GLES20.glUniform3f(colorUniform, 0.2f, 0.3f, 0.9f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(normalAttribute);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDisable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(TAG, "Draw");
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

    FloatBuffer allocateDirectFloatBuffer(int n) {
        return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
