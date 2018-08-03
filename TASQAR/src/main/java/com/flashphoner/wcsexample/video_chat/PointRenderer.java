package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

/**
 * Created by TakeLeap05 on 02-08-2018.
 */

public class PointRenderer {


    private static final String VERTEX_SHADER_NAME = "shaders/point_cloud.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/point_cloud.frag";

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 1000;

    private int vbo;
    private int vboSize;

    private int programName;
    private int positionAttribute;
    private int modelViewProjectionUniform;
    private int colorUniform;
    private int pointSizeUniform;

    private int numPoints = 0;

    private ArrayList<ArrayList<Anchor>> anchors = new ArrayList<ArrayList<Anchor>>();
    private ArrayList<Anchor> currentAnchorList = new ArrayList<Anchor>();

    public void createOnGlThread(Context context) throws IOException
    {
        ShaderUtil.checkGLError(TAG, "before create");

        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        vbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "buffer alloc");

        int vertexShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int passthroughShader =
                ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        programName = GLES20.glCreateProgram();
        GLES20.glAttachShader(programName, vertexShader);
        GLES20.glAttachShader(programName, passthroughShader);
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        ShaderUtil.checkGLError(TAG, "program");

        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        colorUniform = GLES20.glGetUniformLocation(programName, "u_Color");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
        pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize");

        ShaderUtil.checkGLError(TAG, "program  params");

        anchors.add(currentAnchorList);
    }

    public void AddBreak()
    {
        currentAnchorList = new ArrayList<Anchor>();
        anchors.add(currentAnchorList);
    }

    public void AddPoint(Anchor anchor)
    {
        currentAnchorList.add(anchor);
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {

        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjection, 0);
        GLES20.glUniform1f(pointSizeUniform, 5.0f);

        GLES20.glLineWidth(6.0f);


        int numAnchorsList = anchors.size();

        for(int anchorsListCount = 0; anchorsListCount < numAnchorsList; anchorsListCount++)
        {
            ArrayList<Anchor> anchorsList = anchors.get(anchorsListCount);

            if(anchorsList.size() == 0)
                continue;

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

            int numAnchors = anchorsList.size();

            float[] verticesFloatArray = new float[numAnchors * 3];

            int k = 0;
            for (int i = 0; i < numAnchors * 3; i += 3)
            {
                Pose pose = anchorsList.get(k).getPose();

                verticesFloatArray[i] = pose.tx();
                verticesFloatArray[i + 1] = pose.ty();
                verticesFloatArray[i + 2] = pose.tz();

                k++;
            }

            FloatBuffer verticesBuffer = FloatBuffer.wrap(verticesFloatArray);
            verticesBuffer.put(verticesFloatArray).position(0);

            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, numAnchors * BYTES_PER_POINT, verticesBuffer);

            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, numAnchors);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}
