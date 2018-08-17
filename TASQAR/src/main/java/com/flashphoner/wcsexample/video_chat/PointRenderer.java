package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import processing.core.PApplet;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;
import shapes3d.Extrusion;
import shapes3d.Mesh2DCore;
import shapes3d.S3D;
import shapes3d.utils.CS_ConstantScale;
import shapes3d.utils.Contour;
import shapes3d.utils.ContourScale;
import shapes3d.utils.MeshSection;
import shapes3d.utils.P_BezierSpline;
import shapes3d.utils.Path;

import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

/**
 * Created by TakeLeap05 on 02-08-2018.
 */

public class PointRenderer{


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

    private ArrayList<ArrayList<Anchor>> anchors = new ArrayList<ArrayList<Anchor>>();
    private ArrayList<Anchor> currentAnchorList = new ArrayList<Anchor>();
    private ArrayList<float[]> verticesList = new ArrayList<float[]>();
    private ArrayList<float[]> normalsList = new ArrayList<float[]>();

    private Pose previousPose = null;

    private float[] firstTranslation = new float[3];
    private boolean firstTranslationSet = false;


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

        anchors.add(currentAnchorList);
    }

    public void RemoveAllZeroAnchors()
    {
        int numAnchorsList = anchors.size();

        for(int i = 0; i < numAnchorsList; i++)
        {
            if(anchors.get(i).size() < 2)
            {
                ArrayList<Anchor> removedAnchors = anchors.remove(i);
                int numRemovedAnchors = removedAnchors.size();

                for(int j = 0; j < numRemovedAnchors; j++)
                {
                    removedAnchors.get(j).detach();
                }

                numAnchorsList = anchors.size();

                i = 0;
            }
        }
    }

    public void AddBreak()
    {
//        Log.d(VideoChatActivity.TAG, "Adding Break " + pointerCounter / 3);

        previousPose = null;

        RemoveAllZeroAnchors();
        currentAnchorList = new ArrayList<Anchor>();
        anchors.add(currentAnchorList);

//        prevPointAddTime = 0;

        pointerCounter = 0;

        MakeExtrusionVerticesArrays();
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
            if(!firstTranslationSet)
            {
                firstTranslationSet = true;
                firstTranslation[0] = hitPose.tx();
                firstTranslation[1] = hitPose.ty();
                firstTranslation[2] = hitPose.tz();
            }

            AddAnchor(hitResult, hitPose);
        }

        previousPose = hitPose;

//        Log.d(TAG, "NEW POSE " + anchor.getPose().tx() + " " + anchor.getPose().ty() + " " + anchor.getPose().tz());
    }

    public void AddAnchor(HitResult hitResult, Pose hitPose)
    {
        Anchor anchor = hitResult.getTrackable().createAnchor(hitPose);
        currentAnchorList.add(anchor);
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

    public Contour getBuildingContour() {

        float scale = 0.0023f;

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
        if(anchors.size() > 0)
        {
            ArrayList<Anchor> anchorsList = anchors.get(0);

            if(anchorsList.size() > 0)
            {
                Anchor anchor = anchorsList.get(0);

                float[] translationFirst = anchor.getPose().getTranslation();

                if( Math.abs(translationFirst[0] - firstTranslation[0]) > 0.2f ||
                    Math.abs(translationFirst[1] - firstTranslation[1]) > 0.2f ||
                    Math.abs(translationFirst[2] - firstTranslation[2]) > 0.2f )
                {
                    return  true;
                }
            }
        }

        return false;
    }

    void MakeExtrusionVerticesArrays()
    {
        verticesList.clear();
        normalsList.clear();

        int numAnchorsList = anchors.size();
        for(int anchorsListCount = 0; anchorsListCount < numAnchorsList; anchorsListCount++) {
            ArrayList<Anchor> anchorsList = anchors.get(anchorsListCount);

            if (anchorsList.size() == 0)
                continue;

            int numAnchors = anchorsList.size();

            ArrayList<PVector> listOfPoints = new ArrayList<>();

            for (int i = 0; i < numAnchors; i++) {
                Pose pose = anchorsList.get(i).getPose();
                float[] originPoint = pose.getTranslation();
                listOfPoints.add(new PVector(originPoint[0], originPoint[1], originPoint[2]));
            }

            if (listOfPoints.size() < 2)
                continue;

            PVector[] pointVectors = listOfPoints.toArray(new PVector[listOfPoints.size()]);

//            if (pointVectors.length < 2)
//                continue;

            Path path = new P_BezierSpline(pointVectors);

            Contour contour = getBuildingContour();
            ContourScale conScale = new CS_ConstantScale();
            conScale.scale(1f);
            contour.make_u_Coordinates();

            Extrusion e = new Extrusion(null, path, 50, contour, conScale);
            e.drawMode(S3D.TEXTURE);

            Mesh2DCore mesh2DCore = (Mesh2DCore)e;

            MeshSection var1 = mesh2DCore.fullShape;

            int numVertices = (var1.eNS - 2) * var1.eEW * 3;

            float[] vertices = new float[numVertices * 3];
            float[] normals = new float[numVertices * 3];

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

            verticesList.add(vertices);
            normalsList.add(normals);
        }
    }

    public void draw(float[] cameraView, float[] cameraPerspective, float[] colorCorrectionRgba) {

        if(isWorldReferenceChanged())
        {
            Log.d(TAG, "WORLD REFERENCE CHANGED");

            MakeExtrusionVerticesArrays();
        }

        GLES20.glEnable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(normalAttribute);

        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        GLES20.glLineWidth(1.0f);

        int numVerticesList = verticesList.size();

        for(int verticesListCount = 0; verticesListCount < numVerticesList; verticesListCount++)
        {
            float[] vertices = verticesList.get(verticesListCount);
            float[] normals = normalsList.get(verticesListCount);

            FloatBuffer vertexBuffer = FloatBuffer.wrap(vertices);
            FloatBuffer normalBuffer = FloatBuffer.wrap(normals);
//            normalBuffer = allocateDirectFloatBuffer(normals.length * 3);
//
//            vertexBuffer.rewind();
//            vertexBuffer.put(vertices);
//            vertexBuffer.rewind();
//
//            normalBuffer.rewind();
//            normalBuffer.put(normals);
//            normalBuffer.rewind();

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
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, numVertices);
            }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(normalAttribute);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        GLES20.glDisable(GLES20.GL_BLEND);

        ShaderUtil.checkGLError(TAG, "Draw");
    }

    FloatBuffer allocateDirectFloatBuffer(int n) {
        return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
