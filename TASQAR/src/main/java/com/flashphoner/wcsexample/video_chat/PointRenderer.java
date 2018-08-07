package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

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

    int count = 0;

    public void AddPoint(Anchor anchor)
    {
        count += 1;

        if(count == 2) {
            currentAnchorList.add(anchor);
            count = 0;
        }
    }

    float lineLength = 0.01f;

    int[] unitCubeIndices = new int[] {         0, 1, 2, // 0
                                                0, 3, 2, // 1

                                                0, 4, 3, // 2
                                                4, 7, 3, // 3

                                                4, 5, 6, // 4
                                                4, 7, 6, // 5

                                                5, 1, 2, // 6
                                                5, 2, 6, // 7

                                                4, 5, 1, // 8
                                                4, 0, 1, // 9

                                                7, 6, 2, // 10
                                                7, 2, 3  // 11
                                                };

    float[] unitCubeVertices = new float[]
            {
            0, 0, 0, //0
            1, 0, 0, //1
            1, 1, 0, //2
            0, 1, 0, //3
            0, 0, 1, //4
            1, 0, 1, //5
            1, 1, 1, //6
            0, 1, 1  //7
            };

    float[] edgePoints = new float[3 * 4 * 2];

    float[] getCubePoint(float[] originPoint, int pointIndex)
    {
        float[] finalPoints = new float[3];

        finalPoints[0] = originPoint[0] + unitCubeVertices[pointIndex * 3 + 0] * lineLength;
        finalPoints[1] = originPoint[1] + unitCubeVertices[pointIndex * 3 + 1] * lineLength;
        finalPoints[2] = originPoint[2] + unitCubeVertices[pointIndex * 3 + 2] * lineLength;

        return  finalPoints;
    }

    int[] getCubeIndices(int index)
    {
        int[] finalPoints = new int[3];

        System.arraycopy(unitCubeIndices, index * 3, finalPoints, 0, 3);

        return  finalPoints;
    }

    public void draw(float[] cameraView, float[] cameraPerspective) {

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glUniform1f(pointSizeUniform, 5.0f);

        GLES20.glLineWidth(3.0f);

        int numAnchorsList = anchors.size();
        int numFaces = 0;

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
                float[] originPoint = pose.getTranslation();

                int numFacesPoint = i == 0 ? 12 : 24;

                for(int faceID = 0; faceID < numFacesPoint; faceID++)
                {
                    int fetchFaceID = -1;

                    if(i == 0)
                    {
                        fetchFaceID = faceID;

                        int[] indices = getCubeIndices(fetchFaceID);

                        for(int index = 0; index < 3; index++)
                        {
                            float[] point = getCubePoint(originPoint, indices[index]);

                            System.arraycopy(point, 0, cubeArray, index * 3, 3);
                        }

                        System.arraycopy(getCubePoint(originPoint, 0), 0, edgePoints, 0, 3);
                        System.arraycopy(getCubePoint(originPoint, 1), 0, edgePoints, 3, 3);
                        System.arraycopy(getCubePoint(originPoint, 2), 0, edgePoints, 6, 3);
                        System.arraycopy(getCubePoint(originPoint, 3), 0, edgePoints, 9, 3);
                    }
                    else
                    {
                        if(faceID >= 12)
                        {
                            fetchFaceID = faceID - 12;

                            int[] indices = getCubeIndices(fetchFaceID);

                            for(int index = 0; index < 3; index++)
                            {
                                float[] point = getCubePoint(originPoint, indices[index]);

                                System.arraycopy(point, 0, cubeArray, index * 3, 3);
                            }

                            if(fetchFaceID == 2)
                            {
                                System.arraycopy(getCubePoint(originPoint, 0), 0, edgePoints, 0, 3);
                                System.arraycopy(getCubePoint(originPoint, 1), 0, edgePoints, 3, 3);
                                System.arraycopy(getCubePoint(originPoint, 2), 0, edgePoints, 6, 3);
                                System.arraycopy(getCubePoint(originPoint, 3), 0, edgePoints, 9, 3);
                            }
                        }
                        else
                        {
                            if(faceID == 0)
                            {
                                System.arraycopy(getCubePoint(originPoint, 4), 0, edgePoints, 12, 3);
                                System.arraycopy(getCubePoint(originPoint, 5), 0, edgePoints, 15, 3);
                                System.arraycopy(getCubePoint(originPoint, 6), 0, edgePoints, 18, 3);
                                System.arraycopy(getCubePoint(originPoint, 7), 0, edgePoints, 21, 3);
                            }

                            int[] indices = getCubeIndices(faceID);

                            for(int indicesIndex = 0; indicesIndex < 3; indicesIndex++)
                            {
                                System.arraycopy(edgePoints, indices[indicesIndex] * 3, cubeArray, indicesIndex * 3, 3);
                            }
                        }
                    }

                    verticesBuffer = FloatBuffer.wrap(cubeArray);
                    verticesBuffer.put(cubeArray).position(0);

                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, 3 * BYTES_PER_POINT, verticesBuffer);

                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

                    numFaces++;
                }
            }
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}
