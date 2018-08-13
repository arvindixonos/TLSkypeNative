package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

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
    private int vboSize;

    private int programName;
    private int positionAttribute;
    private int normalAttribute;
//    private int texCoordAttribute;

    // Shader location: texture sampler.
//    private int textureUniform;

    private int modelViewUniform;
    private int modelViewProjectionUniform;

    // Shader location: environment properties.
    private int lightingParametersUniform;

    // Shader location: material properties.
    private int materialParametersUniform;

    // Shader location: color correction property
    private int colorCorrectionParameterUniform;

    // Shader location: object color property (to change the primary color of the object).
    private int colorUniform;


    private ArrayList<ArrayList<Anchor>> anchors = new ArrayList<ArrayList<Anchor>>();
    private ArrayList<Anchor> currentAnchorList = new ArrayList<Anchor>();

    private Pose previousPose = null;

    private static final float[] LIGHT_DIRECTION = new float[] {0.250f, 0.866f, 0.433f, 0.0f};
    private final float[] viewLightDirection = new float[4];

    private float ambient = 0.3f;
    private float diffuse = 1.0f;
    private float specular = 1.0f;
    private float specularPower = 6.0f;

    private final int[] textures = new int[1];


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

        modelViewUniform = GLES20.glGetUniformLocation(programName, "u_ModelView");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");

        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        normalAttribute = GLES20.glGetAttribLocation(programName, "a_Normal");
//        texCoordAttribute = GLES20.glGetAttribLocation(programName, "a_TexCoord");
//
//        textureUniform = GLES20.glGetUniformLocation(programName, "u_Texture");

        lightingParametersUniform = GLES20.glGetUniformLocation(programName, "u_LightingParameters");
        materialParametersUniform = GLES20.glGetUniformLocation(programName, "u_MaterialParameters");
        colorCorrectionParameterUniform = GLES20.glGetUniformLocation(programName, "u_ColorCorrectionParameters");
        colorUniform = GLES20.glGetUniformLocation(programName, "u_ObjColor");

        ShaderUtil.checkGLError(TAG, "program  params");

//        // Read the texture.
//        Bitmap textureBitmap = BitmapFactory.decodeStream(context.getAssets().open(diffuseTextureAssetName));
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glGenTextures(textures.length, textures, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
//
//        GLES20.glTexParameteri(
//                GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
//        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//
//        textureBitmap.recycle();

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

        previousPose = null;

//        RemoveAllZeroAnchors();
        currentAnchorList = new ArrayList<Anchor>();
        anchors.add(currentAnchorList);
    }

    public void AddPoint(Anchor anchor, Pose hitPose)
    {
        if(previousPose != null)
        {
            float threshold = 0.06f;

            float px = previousPose.tx();
            float py = previousPose.ty();
            float pz = previousPose.tz();

            float hx = hitPose.tx();
            float hy = hitPose.ty();
            float hz = hitPose.tz();

            float cx = (Math.abs(hx) - Math.abs(px)) > threshold ? (Math.abs(px) + threshold) * Math.signum(hx) : hx;
            float cy = (Math.abs(hy) - Math.abs(py)) > threshold ? (Math.abs(py) + threshold) * Math.signum(hy) : hy;
            float cz = (Math.abs(hz) - Math.abs(pz)) > threshold ? (Math.abs(pz) + threshold) * Math.signum(hz) : hz;

            hitPose = new Pose(new float[]{cx, cy, cz}, new float[]{hitPose.qx(), hitPose.qy(), hitPose.qz(), hitPose.qw()});
        }

        anchor.getPose().compose(hitPose);
        currentAnchorList.add(anchor);

        previousPose = hitPose;
    }

    public class Building extends Contour {

        public Building(PVector[] c) {
            this.contour = c;
        }
    }

    public Contour getBuildingContour() {

        float scale = 0.0045f;

        int numPoints = 20;

        PVector[] points = new PVector[numPoints];

        float angleInc = 2 * 3.14f / numPoints;

        for(int i = 0; i < numPoints; i++)
        {
            points[i] = new PVector((float)Math.sin(i * angleInc), (float)Math.cos(i * angleInc));
            points[i].mult(scale);
        }

        return new Building(points);
    }

    float[] vertices = new float[0];
    float[] normals = new float[0];

    FloatBuffer vertexBuffer = null;
    FloatBuffer normalBuffer = null;
    public void draw(float[] cameraView, float[] cameraPerspective, float[] colorCorrectionRgba) {

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
        GLES20.glUniformMatrix4fv(modelViewUniform, 1, false, modelViewMatrix, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0);

        Random rand = new  Random();

        float[] lightDirection = new float[4];
        lightDirection[0] = rand.nextFloat();
        lightDirection[1] = rand.nextFloat();
        lightDirection[2] = rand.nextFloat();
        lightDirection[3] = rand.nextFloat();

        Matrix.multiplyMV(viewLightDirection, 0, modelViewMatrix, 0, LIGHT_DIRECTION, 0);
        normalizeVec3(viewLightDirection);
        GLES20.glUniform4f(
                lightingParametersUniform,
                viewLightDirection[0],
                viewLightDirection[1],
                viewLightDirection[2],
                1.f);
        GLES20.glUniform4fv(colorCorrectionParameterUniform, 1, colorCorrectionRgba, 0);

        float[] objColor = new float[]{0.0f, 0.9f, 0.0f, 0.95f};

        // Set the object color property.
        GLES20.glUniform4fv(colorUniform, 1, objColor, 0);

        // Set the object material properties.
        GLES20.glUniform4f(materialParametersUniform, ambient, diffuse, specular, specularPower);

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

            Extrusion e = new Extrusion(null, path, 40, contour, conScale);
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

    private static void normalizeVec3(float[] v) {
        float reciprocalLength = 1.0f / (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        v[0] *= reciprocalLength;
        v[1] *= reciprocalLength;
        v[2] *= reciprocalLength;
    }

    int numVertices = 0;
    void MakeExtrusionVerticesArray(Extrusion e)
    {
        Mesh2DCore mesh2DCore = (Mesh2DCore)e;

        MeshSection var1 = mesh2DCore.fullShape;

        numVertices = (var1.eNS - 2) * var1.eEW * 3;

        vertices = new float[numVertices * 3];
        normals = new float[numVertices * 3];

        vertexBuffer = allocateDirectFloatBuffer(numVertices * 3);
        normalBuffer = allocateDirectFloatBuffer(numVertices * 3);

        int pointCounter = 0;

        for(int var4 = var1.sNS; var4 < var1.eNS - 2; ++var4) {

            for(int var5 = var1.sEW; var5 < var1.eEW; ++var5) {
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

        vertexBuffer.rewind();
        vertexBuffer.put(vertices);
        vertexBuffer.rewind();

        normalBuffer.rewind();
        normalBuffer.put(normals);
        normalBuffer.rewind();
    }

    FloatBuffer allocateDirectFloatBuffer(int n) {
        return ByteBuffer.allocateDirect(n * Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
