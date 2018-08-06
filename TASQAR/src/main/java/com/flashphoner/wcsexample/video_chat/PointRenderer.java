package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.text.InputFilter;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.microedition.khronos.opengles.GL;

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

    boolean first = true;

    public void AddPoint(Anchor anchor)
    {
        if(first)
            currentAnchorList.add(anchor);

        first = !first;
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

    float lineLength = 0.02f;
    float[] unitCubeVertices = new float[] {    0, 0, 0, 1, 0, 0, 0, 1, 0, // 0
                                                0, 1, 0, 1, 0, 0, 1, 1, 0, // 1

                                                0, 0, 0, 0, 0, 1, 0, 1, 0, // 2
                                                0, 0, 1, 0, 1, 1, 0, 1, 0, // 3

                                                0, 0, 1, 1, 0, 1, 1, 1, 1, // 4
                                                0, 0, 1, 0, 1, 1, 1, 1, 1, // 5

                                                1, 0, 1, 1, 0, 0, 1, 1, 0, // 6
                                                1, 0, 1, 1, 1, 0, 1, 1, 1, // 7

                                                0, 0, 1, 1, 0, 1, 1, 0, 0, // 8
                                                0, 0, 1, 0, 0, 0, 1, 0, 0, // 9

                                                0, 1, 1, 1, 1, 1, 1, 1, 0, // 10
                                                0, 1, 1, 1, 1, 0, 0, 1, 0  // 11
                                                };

    public float[] getMatrix(float[] rotationQuaternion) {

        // products
        float q0q0  = rotationQuaternion[0] * rotationQuaternion[0];
        float q0q1  = rotationQuaternion[0] * rotationQuaternion[1];
        float q0q2  = rotationQuaternion[0] * rotationQuaternion[2];
        float q0q3  = rotationQuaternion[0] * rotationQuaternion[3];
        float q1q1  = rotationQuaternion[1] * rotationQuaternion[1];
        float q1q2  = rotationQuaternion[1] * rotationQuaternion[2];
        float q1q3  = rotationQuaternion[1] * rotationQuaternion[3];
        float q2q2  = rotationQuaternion[2] * rotationQuaternion[2];
        float q2q3  = rotationQuaternion[2] * rotationQuaternion[3];
        float q3q3  = rotationQuaternion[3] * rotationQuaternion[3];

        // create the matrix
        float[] m = new float[9];

        m [0] = 2.0f * (q0q0 + q1q1) - 1.0f;
        m [1] = 2.0f * (q1q2 - q0q3);
        m [2] = 2.0f * (q1q3 + q0q2);

        m [3] = 2.0f * (q1q2 + q0q3);
        m [4] = 2.0f * (q0q0 + q2q2) - 1.0f;
        m [5] = 2.0f * (q2q3 - q0q1);

        m [6] = 2.0f * (q1q3 - q0q2);
        m [7] = 2.0f * (q2q3 + q0q1);
        m [8] = 2.0f * (q0q0 + q3q3) - 1.0f;

        return m;
    }

    float[] getCubeVertices(float[] originPoint, int faceID, Rotation rotation)
    {
        float[] cubeVertices = new float[9];

        cubeVertices[0] = originPoint[0] + unitCubeVertices[faceID * 9 + 0] * lineLength;
        cubeVertices[1] = originPoint[1] + unitCubeVertices[faceID * 9 + 1] * lineLength;
        cubeVertices[2] = originPoint[2] + unitCubeVertices[faceID * 9 + 2] * lineLength;
        cubeVertices[3] = originPoint[0] + unitCubeVertices[faceID * 9 + 3] * lineLength;
        cubeVertices[4] = originPoint[1] + unitCubeVertices[faceID * 9 + 4] * lineLength;
        cubeVertices[5] = originPoint[2] + unitCubeVertices[faceID * 9 + 5] * lineLength;
        cubeVertices[6] = originPoint[0] + unitCubeVertices[faceID * 9 + 6] * lineLength;
        cubeVertices[7] = originPoint[1] + unitCubeVertices[faceID * 9 + 7] * lineLength;
        cubeVertices[8] = originPoint[2] + unitCubeVertices[faceID * 9 + 8] * lineLength;

        return  cubeVertices;
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {



        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
//        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glUniform1f(pointSizeUniform, 5.0f);

        GLES20.glLineWidth(6.0f);

        int numAnchorsList = anchors.size();

        for(int anchorsListCount = 0; anchorsListCount < numAnchorsList; anchorsListCount++)
        {
            ArrayList<Anchor> anchorsList = anchors.get(anchorsListCount);

            if(anchorsList.size() == 0)
                continue;

            int numAnchors = anchorsList.size();

            float[] cubeArray = new float[9];
            FloatBuffer verticesBuffer = FloatBuffer.wrap(cubeArray);

            for (int i = 0; i < numAnchors; i++)
            {
                Pose pose  = anchorsList.get(i).getPose();
                float[] nextPoint = pose.getTranslation();

                for(int faceID = 0; faceID < 12; faceID++)
                {
                    float[] rotationQuaternion = pose.getRotationQuaternion();

                    Rotation rotation = new Rotation(rotationQuaternion[0], rotationQuaternion[1], rotationQuaternion[2], rotationQuaternion[3], true);

                    cubeArray = getCubeVertices(nextPoint, faceID, rotation);

                    verticesBuffer = FloatBuffer.wrap(cubeArray);
                    verticesBuffer.put(cubeArray).position(0);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

                    float[] modelMatrix = new float[16];
                    float[] modelViewMatrix = new float[16];
                    float[] modelViewProjectionMatrix = new float[16];
                    Matrix.setIdentityM(modelMatrix, 0);

                    Vector3D rotationAxis = rotation.getAxis(RotationConvention.FRAME_TRANSFORM);
                    double rotationAngle = rotation.getAngle();
                    Matrix.rotateM(modelMatrix, 0, (float)rotationAngle, (float)rotationAxis.getX(), (float)rotationAxis.getY(), (float)rotationAxis.getZ());

                    Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
                    Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);


                    GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, 3 * BYTES_PER_POINT, verticesBuffer);

                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
                }

                // pose.getRotationQuaternion();

//                if(k < numAnchors - 1)
//                {
//                    Pose nextPose = anchorsList.get(k + 1).getPose();
//
//                    nextPose = new Pose(nextPose.getTranslation(), pose.getRotationQuaternion());
//
//                    nextPose = Pose.makeInterpolated(pose, nextPose, 0.1f);
//
//                    nextPoint = nextPose.getTranslation();
//                }
            }
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}
