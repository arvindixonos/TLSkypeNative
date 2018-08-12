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

import processing.core.PVector;
import shape3d.BezTube;
import shapes3d.Extrusion;
import shapes3d.S3D;
import shapes3d.utils.CS_ConstantScale;
import shapes3d.utils.Contour;
import shapes3d.utils.ContourScale;
import shapes3d.utils.MeshSection;
import shapes3d.utils.P_Bezier3D;
import shapes3d.utils.P_BezierSpline;
import shapes3d.utils.P_LinearPath;
import shapes3d.utils.Path;

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
    private static final int INITIAL_BUFFER_POINTS = 50000;

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

    float lineLength = 0.015f;

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

    public void draw(float[] cameraView, float[] cameraPerspective) {

        ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);

        GLES20.glUniform4f(colorUniform, 31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f);
        float[] modelMatrix = new float[16];
        float[] modelViewMatrix = new float[16];
        float[] modelViewProjectionMatrix = new float[16];
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelViewMatrix, 0, cameraView, 0, modelMatrix, 0);
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, cameraPerspective, 0, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);
        GLES20.glUniform1f(pointSizeUniform, 5.0f);

        GLES20.glLineWidth(1.0f);

        int numAnchorsList = anchors.size();
        int numFaces = 0;

        for(int anchorsListCount = 0; anchorsListCount < numAnchorsList; anchorsListCount++)
        {
            ArrayList<Anchor> anchorsList = anchors.get(anchorsListCount);

            if(anchorsList.size() == 0 || anchorsListCount == 0)
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
//            e.setTexture("wall.png", 1, 1);
            e.drawMode(S3D.TEXTURE );
            // Extrusion end caps
//            e.setTexture("grass.jpg", S3D.E_CAP);
//            e.setTexture("sky.jpg", S3D.S_CAP);
            e.drawMode(S3D.TEXTURE, S3D.BOTH_CAP);

            MeshSection meshSection = e.fullShape;

            int numPoints = (meshSection.eNS - 3) * meshSection.eEW * 12;
            float[] points = new float[numPoints * 3];

            int pointCounter = 0;
            for(int i = meshSection.sNS; i < meshSection.eNS - 3; i++) {

                for(int j = meshSection.sEW; j < meshSection.eEW; j++) {

                    PVector p1 = e.coord[j][i];
                    PVector p2 = e.coord[j][i + 1];
                    PVector p3 = e.coord[j][i + 2];
                    PVector p4 = e.coord[j][i + 3];

                    for(int k = 0; k < 3; k++)
                    {
                        if(k == 0)
                        {
                            points[pointCounter] = p1.x;
                            points[pointCounter + 1] = p1.y;
                            points[pointCounter + 2] = p1.z;

                            points[pointCounter + 3] = p2.x;
                            points[pointCounter + 4] = p2.y;
                            points[pointCounter + 5] = p2.z;

                            points[pointCounter + 6] = p3.x;
                            points[pointCounter + 7] = p3.y;
                            points[pointCounter + 8] = p3.z;
                        }
                        else if(k == 1)
                        {
                            points[pointCounter] = p2.x;
                            points[pointCounter + 1] = p2.y;
                            points[pointCounter + 2] = p2.z;

                            points[pointCounter + 3] = p3.x;
                            points[pointCounter + 4] = p3.y;
                            points[pointCounter + 5] = p3.z;

                            points[pointCounter + 6] = p4.x;
                            points[pointCounter + 7] = p4.y;
                            points[pointCounter + 8] = p4.z;
                        }
                        else if(k == 2)
                        {
                            points[pointCounter] = p3.x;
                            points[pointCounter + 1] = p3.y;
                            points[pointCounter + 2] = p3.z;

                            points[pointCounter + 3] = p4.x;
                            points[pointCounter + 4] = p4.y;
                            points[pointCounter + 5] = p4.z;

                            points[pointCounter + 6] = p1.x;
                            points[pointCounter + 7] = p1.y;
                            points[pointCounter + 8] = p1.z;
                        }
                        else
                        {
                            points[pointCounter] = p4.x;
                            points[pointCounter + 1] = p4.y;
                            points[pointCounter + 2] = p4.z;

                            points[pointCounter + 3] = p1.x;
                            points[pointCounter + 4] = p1.y;
                            points[pointCounter + 5] = p1.z;

                            points[pointCounter + 6] = p2.x;
                            points[pointCounter + 7] = p2.y;
                            points[pointCounter + 8] = p2.z;
                        }

                        pointCounter += 9;
                    }
                }
            }

            if (numPoints * BYTES_PER_POINT > vboSize) {
                while (numPoints * BYTES_PER_POINT > vboSize) {
                    vboSize *= 2;
                }
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
            }

            FloatBuffer verticesBuffer = FloatBuffer.allocate(points.length);
            verticesBuffer.put(points).position(0);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            GLES20.glVertexAttribPointer(positionAttribute, 3, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, verticesBuffer.capacity() * BYTES_PER_FLOAT, verticesBuffer);
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);

                int numFacesPoint = 9 == 0 ? 12 : 24;
//
//                for(int faceID = 0; faceID < numFacesPoint; faceID++)
//                {
//                    int fetchFaceID = -1;
//
//                    if(i == 0)
//                    {
//                        fetchFaceID = faceID;
//
//                        int[] indices = getCubeIndices(fetchFaceID);
//
//                        for(int index = 0; index < 3; index++)
//                        {
//                            float[] point = getCubePoint(originPoint, indices[index]);
//
//                            System.arraycopy(point, 0, cubeArray, index * 3, 3);
//                        }
//
//                        System.arraycopy(getCubePoint(originPoint, 0), 0, edgePoints, 0, 3);
//                        System.arraycopy(getCubePoint(originPoint, 1), 0, edgePoints, 3, 3);
//                        System.arraycopy(getCubePoint(originPoint, 2), 0, edgePoints, 6, 3);
//                        System.arraycopy(getCubePoint(originPoint, 3), 0, edgePoints, 9, 3);
//                    }
//                    else
//                    {
//                        if(faceID >= 12)
//                        {
//                            fetchFaceID = faceID - 12;
//
//                            int[] indices = getCubeIndices(fetchFaceID);
//
//                            for(int index = 0; index < 3; index++)
//                            {
//                                float[] point = getCubePoint(originPoint, indices[index]);
//
//                                System.arraycopy(point, 0, cubeArray, index * 3, 3);
//                            }
//
//                            if(fetchFaceID == 2)
//                            {
//                                System.arraycopy(getCubePoint(originPoint, 0), 0, edgePoints, 0, 3);
//                                System.arraycopy(getCubePoint(originPoint, 1), 0, edgePoints, 3, 3);
//                                System.arraycopy(getCubePoint(originPoint, 2), 0, edgePoints, 6, 3);
//                                System.arraycopy(getCubePoint(originPoint, 3), 0, edgePoints, 9, 3);
//                            }
//                        }
//                        else
//                        {
//                            if(faceID == 0)
//                            {
//                                System.arraycopy(getCubePoint(originPoint, 4), 0, edgePoints, 12, 3);
//                                System.arraycopy(getCubePoint(originPoint, 5), 0, edgePoints, 15, 3);
//                                System.arraycopy(getCubePoint(originPoint, 6), 0, edgePoints, 18, 3);
//                                System.arraycopy(getCubePoint(originPoint, 7), 0, edgePoints, 21, 3);
//                            }
//
//                            int[] indices = getCubeIndices(faceID);
//
//                            for(int indicesIndex = 0; indicesIndex < 3; indicesIndex++)
//                            {
//                                System.arraycopy(edgePoints, indices[indicesIndex] * 3, cubeArray, indicesIndex * 3, 3);
//                            }
//                        }
//                    }
//
//                    verticesBuffer = FloatBuffer.wrap(cubeArray);
//                    verticesBuffer.put(cubeArray).position(0);
//
//                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
//
//                    GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, 3 * BYTES_PER_POINT, verticesBuffer);
//
//                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
//                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
//
//                    numFaces++;
//                }
//            }
        }

        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGLError(TAG, "Draw");
    }
}
