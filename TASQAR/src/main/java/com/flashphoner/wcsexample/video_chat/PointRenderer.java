package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import processing.core.PVector;
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

public class PointRenderer {


    private static final String VERTEX_SHADER_NAME = "shaders/vert.glsl";
    private static final String FRAGMENT_SHADER_NAME = "shaders/frag.glsl";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 50000;

    private int vertexVboId;
    private int colorVboId;
    private int vboSize;

    private int programName;
    private int colorAttribute;
    private int positionAttribute;
    private int modelViewProjectionUniform;

    private int numPoints = 0;

    private ArrayList<ArrayList<Anchor>> anchors = new ArrayList<ArrayList<Anchor>>();
    private ArrayList<Anchor> currentAnchorList = new ArrayList<Anchor>();


    public void createOnGlThread(Context context) throws IOException
    {
        ShaderUtil.checkGLError(TAG, "before create");

        int[] buffers = new int[2];
        GLES20.glGenBuffers(2, buffers, 0);
        vertexVboId = buffers[0];
        colorVboId = buffers[1];

        ShaderUtil.checkGLError(TAG, "buffer alloc");

        int vertexShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader = ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        programName = GLES20.glCreateProgram();
        GLES20.glAttachShader(programName, vertexShader);
        GLES20.glAttachShader(programName, passthroughShader);
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        ShaderUtil.checkGLError(TAG, "program");

        positionAttribute = GLES20.glGetAttribLocation(programName, "vertex");
        colorAttribute = GLES20.glGetAttribLocation(programName, "color");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "transform");

        ShaderUtil.checkGLError(TAG, "program  params");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        anchors.add(currentAnchorList);
    }

    public void RemoveAllZeroAnchors()
    {
        int numAnchorsList = anchors.size();

        for(int i = 0; i < numAnchorsList; i++)
        {
            if(anchors.get(i).size() < 5)
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
        Log.d(VideoChatActivity.TAG, "Adding Break");

//        RemoveAllZeroAnchors();
        currentAnchorList = new ArrayList<Anchor>();
        anchors.add(currentAnchorList);
    }

    int count = 0;

    public void AddPoint(Anchor anchor, Pose hitPose)
    {
        count += 1;

//        if(count == 3)
        {
            anchor.getPose().compose(hitPose);
            currentAnchorList.add(anchor);
            count = 0;
        }
    }

    public class Building extends Contour {

        public Building(PVector[] c) {
            this.contour = c;
        }
    }

    public Contour getBuildingContour() {

        float scale = 0.01f;

        int numPoints = 20;

        PVector[] points = new PVector[numPoints];

        float angleInc = 2 * 3.14f / numPoints;

        for(int i = 0; i < numPoints; i++)
        {
            points[i] = new PVector((float)Math.sin(i * angleInc) * scale, (float)Math.cos(i * angleInc) * scale);
        }

        return new Building(points);
    }

    float[] vertices = new float[0];
    float[] colors = new float[0];

    FloatBuffer vertexBuffer = null;
    FloatBuffer colorBuffer = null;
    public void draw(float[] cameraView, float[] cameraPerspective) {

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(colorAttribute);

        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        GLES20.glLineWidth(1.0f);

        int numAnchorsList = anchors.size();

        for(int anchorsListCount = 0; anchorsListCount < numAnchorsList; anchorsListCount++)
        {
            ArrayList<Anchor> anchorsList = anchors.get(anchorsListCount);

            if(anchorsList.size() == 0)
                continue;

            int numAnchors = anchorsList.size();

            ArrayList<PVector> listOfPoints = new ArrayList<>();

            for (int i = 0; i < numAnchors; i++) {
                Pose pose = anchorsList.get(i).getPose();
                float[] originPoint = pose.getTranslation();
                listOfPoints.add(new PVector(originPoint[0], originPoint[1], originPoint[2]));
            }

            PVector[] pointVectors = listOfPoints.toArray(new PVector[listOfPoints.size()]);
            Path path = new P_BezierSpline(pointVectors);

            Contour contour = getBuildingContour();
            ContourScale conScale = new CS_ConstantScale();
            conScale.scale(1f);
            contour.make_u_Coordinates();

            Extrusion e = new Extrusion(null, path, 50, contour, conScale);
            e.drawMode(S3D.TEXTURE );

            MakeExtrusionVerticesArray(e);

            final int vertexStride = 3 * Float.BYTES;

            { // VERTEX
                // bind VBO
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexVboId);
                // fill VBO with data
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, Float.BYTES * vertices.length, vertexBuffer, GLES20.GL_DYNAMIC_DRAW);
                // associate currently bound VBO with shader attribute
                GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, vertexStride, 0);
            }

            { // COLOR
                // bind VBO
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorVboId);
                // fill VBO with data
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, Float.BYTES * colors.length, colorBuffer, GLES20.GL_DYNAMIC_DRAW);
                // associate currently bound VBO with shader attribute
                GLES20.glVertexAttribPointer(colorAttribute, 3, GLES20.GL_FLOAT, false, vertexStride, 0);
            }

                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numVertices);
            }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(colorAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }

    int numVertices = 0;
    void MakeExtrusionVerticesArray(Extrusion e)
    {
        Mesh2DCore mesh2DCore = (Mesh2DCore)e;

        MeshSection var1 = mesh2DCore.fullShape;

        numVertices = ( var1.eNS - 2) * var1.eEW * 3;

        vertices = new float[numVertices * 3];
        colors = new float[numVertices * 3];

        Arrays.fill(colors, 0.5f);

        vertexBuffer = allocateDirectFloatBuffer(numVertices * 3);
        colorBuffer = allocateDirectFloatBuffer(numVertices * 3);

        int pointCounter = 0;

        for(int var4 = var1.sNS; var4 < var1.eNS - 2; ++var4) {

            for(int var5 = var1.sEW; var5 < var1.eEW; ++var5) {
                PVector p1 = mesh2DCore.coord[var5][var4];
                vertices[pointCounter] = p1.x;
                vertices[pointCounter + 1] = p1.y;
                vertices[pointCounter + 2] = p1.z;
                pointCounter += 3;

                PVector p2 = mesh2DCore.coord[var5][var4 + 1];
                vertices[pointCounter] = p2.x;
                vertices[pointCounter + 1] = p2.y;
                vertices[pointCounter + 2] = p2.z;
                pointCounter += 3;

                PVector p3 = mesh2DCore.coord[var5][var4 + 2];
                vertices[pointCounter] = p3.x;
                vertices[pointCounter + 1] = p3.y;
                vertices[pointCounter + 2] = p3.z;
                pointCounter += 3;
            }
        }

        vertexBuffer.rewind();
        vertexBuffer.put(vertices);
        vertexBuffer.rewind();

        colorBuffer.rewind();
        colorBuffer.put(colors);
        colorBuffer.rewind();
    }

    FloatBuffer allocateDirectFloatBuffer(int n) {
        return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
