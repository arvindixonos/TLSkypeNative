package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import processing.core.PVector;
import shapes3d.EndCapContour;
import shapes3d.Extrusion;
import shapes3d.Mesh2DCore;
import shapes3d.S3D;
import shapes3d.utils.CS_ConstantScale;
import shapes3d.utils.Contour;
import shapes3d.utils.ContourScale;
import shapes3d.utils.MeshSection;
import shapes3d.utils.P_Bezier3D;

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

    private int colorUniform;

    private ArrayList<ArrayList<Anchor>> anchors = new ArrayList<ArrayList<Anchor>>();
    private ArrayList<Anchor> currentAnchorList = new ArrayList<Anchor>();
    private ArrayList<float[]> verticesList = new ArrayList<float[]>();
    private ArrayList<float[]> normalsList = new ArrayList<float[]>();

    private ArrayList<float[]> verticesListStartCap = new ArrayList<float[]>();
    private ArrayList<float[]> normalsListStartCap = new ArrayList<float[]>();

    private ArrayList<float[]> verticesListEndCap = new ArrayList<float[]>();
    private ArrayList<float[]> normalsListEndCap = new ArrayList<float[]>();

    private PVector firstPoint = new PVector();
    private PVector secondPoint = new PVector();

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

    Object  syncObject = new Object();

    public void AddBreak() {
        synchronized (syncObject) {
            RemoveAllZeroAnchors();
            currentAnchorList = new ArrayList<Anchor>();
            anchors.add(currentAnchorList);
            MakeExtrusionVerticesArrays();
            pointerCounter = 0;
            firstPoint = new PVector();
            secondPoint = new PVector();
        }
    }

    PVector SlerpVector(PVector aVec, PVector bVec, float t)
    {
        PVector slerpVector = new PVector();

        float angle = PVector.angleBetween(aVec, bVec);
        slerpVector = aVec.mult((float) (Math.sin(angle * (1f - t)) / Math.sin(angle))).add(bVec.mult((float) (Math.sin(angle * t) / Math.sin(angle))));

        return slerpVector;
    }

    public static float maxAngleDeviation = 10f;
    PVector FixPoint(PVector point2)
    {
        PVector previousVector = PVector.sub(secondPoint, firstPoint);
        previousVector = previousVector.normalize();

        PVector currentVector = PVector.sub(point2, secondPoint);
        float currentVectorMagnitude = PVector.dist(currentVector, new PVector());
        currentVector = currentVector.normalize();

        float angle = (float) Math.toDegrees(PVector.angleBetween(previousVector, currentVector));

        Log.d(TAG, "POINTS " + firstPoint + " " + secondPoint + " " + point2);

        float angleDeviation = (maxAngleDeviation / angle);

        Log.d(TAG, "OLD ANGLE IS: " + angle + " " + angleDeviation + " " + currentVectorMagnitude);

        if (angle < maxAngleDeviation)
            angleDeviation = 1f;

//        angleDeviation = 0.3f;

        PVector newVector = SlerpVector(previousVector, currentVector, angleDeviation);
//        Log.d(TAG, "NEW VEC LENGTH: " + PVector.dist(newVector, new PVector()) + " " + PVector.dist(currentVector, new PVector())  + " " + PVector.dist(previousVector, new PVector()));
        newVector = newVector.normalize();
        newVector.mult(currentVectorMagnitude);
        newVector = PVector.add(secondPoint, newVector);

        // Check code

        PVector aVec = PVector.sub(secondPoint, firstPoint);
        aVec = aVec.normalize();

        PVector bVec = PVector.sub(newVector, secondPoint);
        bVec = bVec.normalize();

        angle = (float) Math.toDegrees(PVector.angleBetween(aVec, bVec));

        Log.d(TAG, "NEW ANGLE IS " + newVector + " " + angle);

        firstPoint = secondPoint;
        secondPoint = newVector;

        return newVector;
    }


    int pointerCounter = 0;
    public void AddPoint(HitResult hitResult)
    {
        synchronized (syncObject)
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

            if(pointerCounter > 1)//PVector.dist(firstPoint, new PVector()) > Float.MIN_VALUE)
            {
                float hx = hitPose.tx();
                float hy = hitPose.ty();
                float hz = hitPose.tz();

                PVector currentPoint = new PVector(hx, hy, hz);

//                if(PVector.dist(secondPoint, new PVector()) < Float.MIN_VALUE)
//                {
//                    secondPoint = currentPoint;
//                }
//                else
//                {
//                    currentPoint = FixPoint(currentPoint);
//                }

                hitPose = new Pose(new float[]{currentPoint.x,  currentPoint.y, currentPoint.z}, new float[]{0, 0, 0, 0});
                AddAnchor(hitResult, hitPose);
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

                Log.d(TAG, "FIRST POINT");

                firstPoint = new PVector(hitPose.tx(), hitPose.ty(), hitPose.tz());
                AddAnchor(hitResult, hitPose);
            }


//        Log.d(TAG, "NEW POSE " + anchor.getPose().tx() + " " + anchor.getPose().ty() + " " + anchor.getPose().tz());
        }

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
        verticesListStartCap.clear();
        normalsListStartCap.clear();
        verticesListEndCap.clear();
        normalsListEndCap.clear();

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

            P_Bezier3D bezierCurve = new P_Bezier3D(pointVectors, pointVectors.length);

            Contour contour = getSphericalContour();
            ContourScale conScale = new CS_ConstantScale();
            conScale.scale(0.1f);
            contour.make_u_Coordinates();

            Extrusion e = new Extrusion(null, bezierCurve, 50, contour, conScale);
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

            EndCapContour capContour = (EndCapContour)e.startEC;

            vertices = new float[capContour.triangles.length * 9];
            normals = new float[capContour.triangles.length * 9];

            for(int i = 0, j = 0; i < capContour.triangles.length; i += 3, j += 9) {
                int v1 = capContour.triangles[i];
                int var2 = capContour.triangles[i + 1];
                int var3 = capContour.triangles[i + 2];

                normals[j] = capContour.n.x;
                normals[j + 1] = capContour.n.y;
                normals[j + 2] = capContour.n.z;
                normals[j + 3] = capContour.n.x;
                normals[j + 4] = capContour.n.y;
                normals[j + 5] = capContour.n.z;
                normals[j + 6] = capContour.n.x;
                normals[j + 7] = capContour.n.y;
                normals[j + 8] = capContour.n.z;

                vertices[j] = capContour.edge[v1].x;
                vertices[j + 1] = capContour.edge[v1].y;
                vertices[j + 2] = capContour.edge[v1].z;
                vertices[j + 3] = capContour.edge[var2].x;
                vertices[j + 4] = capContour.edge[var2].y;
                vertices[j + 5] = capContour.edge[var2].z;
                vertices[j + 6] = capContour.edge[var3].x;
                vertices[j + 7] = capContour.edge[var3].y;
                vertices[j + 8] = capContour.edge[var3].z;
            }

            verticesListStartCap.add(vertices);
            normalsListStartCap.add(normals);

            capContour = (EndCapContour)e.endEC;

            vertices = new float[capContour.triangles.length * 9];
            normals = new float[capContour.triangles.length * 9];

            for(int i = 0, j = 0; i < capContour.triangles.length; i += 3, j += 9) {
                int v1 = capContour.triangles[i];
                int var2 = capContour.triangles[i + 1];
                int var3 = capContour.triangles[i + 2];

                normals[j] = capContour.n.x;
                normals[j + 1] = capContour.n.y;
                normals[j + 2] = capContour.n.z;
                normals[j + 3] = capContour.n.x;
                normals[j + 4] = capContour.n.y;
                normals[j + 5] = capContour.n.z;
                normals[j + 6] = capContour.n.x;
                normals[j + 7] = capContour.n.y;
                normals[j + 8] = capContour.n.z;

                vertices[j] = capContour.edge[v1].x;
                vertices[j + 1] = capContour.edge[v1].y;
                vertices[j + 2] = capContour.edge[v1].z;
                vertices[j + 3] = capContour.edge[var2].x;
                vertices[j + 4] = capContour.edge[var2].y;
                vertices[j + 5] = capContour.edge[var2].z;
                vertices[j + 6] = capContour.edge[var3].x;
                vertices[j + 7] = capContour.edge[var3].y;
                vertices[j + 8] = capContour.edge[var3].z;
            }

            verticesListEndCap.add(vertices);
            normalsListEndCap.add(normals);
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

        GLES20.glLineWidth(5.0f);

        int numVerticesList = verticesList.size();

        for(int verticesListCount = 0; verticesListCount < numVerticesList; verticesListCount++)
        {
            float[] vertices = verticesList.get(verticesListCount);
            float[] normals = normalsList.get(verticesListCount);
            GLES20.glUniform3f(colorUniform, 0.2f, 0.9f, 0.3f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLE_STRIP);

            vertices = verticesListStartCap.get(verticesListCount);
            normals = normalsListStartCap.get(verticesListCount);
            GLES20.glUniform3f(colorUniform, 0.9f, 0.2f, 0.3f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);

            vertices = verticesListEndCap.get(verticesListCount);
            normals = normalsListEndCap.get(verticesListCount);
            GLES20.glUniform3f(colorUniform, 0.2f, 0.3f, 0.9f);
            DrawVertices(vertices, normals, GLES20.GL_TRIANGLES);
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
}
