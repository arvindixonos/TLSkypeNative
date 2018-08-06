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
    private static final int INITIAL_BUFFER_POINTS = 4000;

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

    public void AddPoint(Anchor anchor)
    {
        Log.d(VideoChatActivity.TAG, "Adding Point");
        currentAnchorList.add(anchor);
    }

    public float VecMagnitude(float[] p1, float[] p2)
    {
        return (float) Math.sqrt(   (p2[0] - p1[0]) *  (p2[0] - p1[0]) +
                            (p2[1] - p1[1]) *  (p2[1] - p1[1]) +
                            (p2[2] - p1[2]) *  (p2[2] - p1[2]));
    }

    public float[] VecNormalized(float[] p1, float[] p2)
    {
        float magnitude = VecMagnitude(p1, p2);

        float[] normalized = new float[3];

        normalized[0] = (p2[0] - p1[0]) / magnitude;
        normalized[1] = (p2[1] - p1[1]) / magnitude;
        normalized[2] = (p2[2] - p1[2]) / magnitude;

        return  normalized;
    }

    public float[] VecNormalized(float[] p1)
    {
        float magnitude = VecMagnitude(p1);

        float[] normalized = new float[3];

        normalized[0] = (p1[0]) / magnitude;
        normalized[1] = (p1[1]) / magnitude;
        normalized[2] = (p1[2]) / magnitude;

        return  normalized;
    }

    public float VecDot(float[] p1, float[] p2)
    {
        return  p1[0] * p2[0] + p1[1] * p2[1] + p1[2] * p2[2];
    }

    public float VecMagnitude(float[] p)
    {
        return (float) Math.sqrt(   (p[0]) *  (p[0]) +
                                    (p[1]) *  (p[1]) +
                                    (p[2]) *  (p[2]));
    }

    public float[] VecRotate(float[] vec, float[] axis, float angle)
    {
        float[] rotatedVec = new float[3];

        float[] part1 = new float[3];
        part1[0] = (float) (vec[0] * Math.cos(Math.PI / 2.0f));
        part1[1] = (float) (vec[1] * Math.cos(Math.PI / 2.0f));
        part1[2] = (float) (vec[2] * Math.cos(Math.PI / 2.0f));

        float[] part2 = new float[3];
        part2[0] = (float) (axis[0] * VecDot(axis, vec) * (1.0f - Math.cos(Math.PI / 2.0f)));
        part2[1] = (float) (axis[1] * VecDot(axis, vec) * (1.0f - Math.cos(Math.PI / 2.0f)));
        part2[2] = (float) (axis[2] * VecDot(axis, vec) * (1.0f - Math.cos(Math.PI / 2.0f)));

        float[] part3 = new float[3];
        part3[0] = (float) (VecCross(vec, axis)[0] * Math.sin(Math.PI / 2.0f));
        part3[1] = (float) (VecCross(vec, axis)[1] * Math.sin(Math.PI / 2.0f));
        part3[2] = (float) (VecCross(vec, axis)[2] * Math.sin(Math.PI / 2.0f));

        rotatedVec[0] = part1[0] + part2[0] + part3[0];
        rotatedVec[1] = part1[1] + part2[1] + part3[1];
        rotatedVec[2] = part1[2] + part2[2] + part3[2];

        return rotatedVec;
    }

    public float VecAngle(float[] p1, float[] p2)
    {
        float angle = 0.0f;

        angle = (float) Math.atan2(VecMagnitude(VecCross(p1, p2)), VecDot(p1, p2));

        float[] crossProduct = VecCross(p1, p2);

        float sign = -Math.signum(crossProduct[2]);

        return  angle;
    }

    public float[] VecCross(float[] p1, float[] p2)
    {
        float[] cross = new float[3];               //a × b = {aybz - azby; azbx - axbz; axby - aybx}

        cross[0] = p1[1] * p2[2] - p1[2] * p2[1];
        cross[1] = p1[2] * p2[0] - p1[0] * p2[2];
        cross[2] = p1[0] * p2[1] - p1[1] * p2[0];

        return cross;
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

            int totalPoints = numAnchors * 3; // ((numAnchors - 1) * 20 + numAnchors) * 3;

            float[] verticesFloatArray = new float[totalPoints];

            int k = 0;
            int j = 0;

            float maxAngleDeviation = 0.3f;

            boolean nextPointSet = true;
            Pose pose = anchorsList.get(0).getPose();
            float[] nextPoint = pose.getTranslation();

            for (int i = 0; i < totalPoints; i += 3)
            {
                pose = anchorsList.get(k).getPose();

//                if(!nextPointSet)
                {
//                    verticesFloatArray[i] = pose.tx();
//                    verticesFloatArray[i + 1] = pose.ty();
//                    verticesFloatArray[i + 2] = pose.tz();
                }
//                else
//                {
                    verticesFloatArray[i]     = nextPoint[0];
                    verticesFloatArray[i + 1] = nextPoint[1];
                    verticesFloatArray[i + 2] = nextPoint[2];
//                    verticesFloatArray[i + 3] = nextPoint[0] - 0.2f;
//                    verticesFloatArray[i + 4] = nextPoint[1] + 0.2f;
//                    verticesFloatArray[i + 5] = nextPoint[2] - 0.2f;
//                    verticesFloatArray[i + 6] = nextPoint[0] + 0.2f;
//                    verticesFloatArray[i + 7] = nextPoint[1] - 0.2f;
//                    verticesFloatArray[i + 8] = nextPoint[2] + 0.2f;
//
//                    nextPointSet = false;
//                }

                if(k < numAnchors - 1)
                {
                    Pose nextPose = anchorsList.get(k + 1).getPose();

                    nextPose = new Pose(nextPose.getTranslation(), pose.getRotationQuaternion());

                    nextPose = Pose.makeInterpolated(pose, nextPose, 0.1f);

                    nextPoint = nextPose.getTranslation();
                }

//                if(k < numAnchors - 1)
//                {
//                    Pose newPose = anchorsList.get(k + 1).getPose();
//
//                    float[] p0 = new float[3];
//                    p0[0] = verticesFloatArray[i];
//                    p0[1] = verticesFloatArray[i + 1];
//                    p0[2] = verticesFloatArray[i + 2];
//
//                    float[] p1 = new float[3];
//                    p1[0] = newPose.tx();
//                    p1[1] = newPose.ty();
//                    p1[2] = newPose.tz();
//
//                    float[] normalizedVector = VecNormalized(p0);
//
//                    float mag = VecMagnitude(normalizedVector);
//
//                    normalizedVector[0] = 0;
//                    normalizedVector[1] = 0;
//                    normalizedVector[2] = 0;
//
//                    normalizedVector = pose.rotateVector(normalizedVector);
//
//                    mag = VecMagnitude(normalizedVector);
//
//                    p0 = VecNormalized(p0);
//
//                    mag = VecMagnitude(normalizedVector);
//
//                    float angle = VecAngle(p0, normalizedVector);
//
//                    Log.d(TAG, "Asd");
////                    if(Math.abs(angle) > maxAngleDeviation)
////                    {
////                        angle = Math.signum(angle) * maxAngleDeviation;
////
////                        float magnitude = VecMagnitude(p0, p1);
////                        float[] vecNorm = VecNormalized(p0, p1);
////
////                        float[] axis = VecCross(p0, p1);
////
////                        float[] rotatedVector = VecRotate(vecNorm, axis, angle);
////
////                        p1[0] = p0[0] + rotatedVector[0] * magnitude;
////                        p1[1] = p0[1] + rotatedVector[1] * magnitude;
////                        p1[2] = p0[2] + rotatedVector[2] * magnitude;
////
////                        nextPointSet = true;
////
////                        nextPoint[0] = p1[0];
////                        nextPoint[1] = p1[1];
////                        nextPoint[2] = p1[2];
////                    }
//
////                    int start = i + 3;
////
////                    j = 0;
////                    for (float t = 0f; t < 1f; t += 0.05f)
////                    {
////                        float newX = p0[0] + t * (p1[0] - p0[0]);
////                        float newY = p0[1] + t * (p1[1] - p0[1]);
////                        float newZ = p0[2] + t * (p1[2] - p0[2]);
////
////                        verticesFloatArray[start + j * 3] = newX;
////                        verticesFloatArray[start + (j * 3) + 1] = newY;
////                        verticesFloatArray[start + (j * 3) + 2] = newZ;
////
////                        j++;
////                    }
//                }

                k++;
            }

            FloatBuffer verticesBuffer = FloatBuffer.wrap(verticesFloatArray);
            verticesBuffer.put(verticesFloatArray).position(0);

            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, (totalPoints / 3) * BYTES_PER_POINT, verticesBuffer);

            GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, (totalPoints / 3));

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}
