package com.flashphoner.wcsexample.video_chat;

import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Pose;

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

public class AnchorList
{
    private   int     anchorListType = 0;

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

    public  Thread      blinkingThread = null;

    public  float[]     listColor = new float[]{1.0f, 1.0f, 1.0f, 1.0f};

    public float       scaleCounter = 0f;

    public  void    setAnchorListColor(float[] newColor)
    {
        listColor = newColor;
    }

    public void setAnchorListType(int type)
    {
        anchorListType = type;

        if(type == 2)
        {
            scaleCounter = 0f;

            if(blinkingThread != null)
            {
                blinkingThread.interrupt();
                blinkingThread = null;
            }

            blinkingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true)
                    {
                        scaleCounter += 0.01f;

                        if(scaleCounter > 1f)
                        {
                            scaleCounter = 0f;
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });

            blinkingThread.start();
        }
    }

    public int getAnchorListType()
    {
        return  anchorListType;
    }


    public void updateModelMatrix(float[] modelMatrix, float scaleFactor) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(this.modelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    public void UpdateAllAnchors(FrustumVisibilityTester frustumVisibilityTester)
    {
        int numAnchors = anchors.size();

        for(int i = 0; i < numAnchors; i++)
        {
            anchors.get(i).UpdateAnchor(frustumVisibilityTester);
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
        try
        {
            numAnchors = anchors.size();
            for(int i = 0; i < numAnchors; i++)
            {
                TASQAR_Anchor tasqar_anchor = anchors.get(i);
                tasqar_anchor.CheckDirty();
            }
        }
        catch (Exception ex)
        {

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

    public void calcVertices(FrustumVisibilityTester frustumVisibilityTester)
    {
        RemoveAllVertices();

        UpdateAllAnchors(frustumVisibilityTester);

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

        if(blinkingThread != null)
        {
            blinkingThread.interrupt();
            blinkingThread = null;
        }
    }
}
