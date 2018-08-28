package com.flashphoner.wcsexample.video_chat;

import android.util.Log;

import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import de.javagl.obj.Obj;

public class TASQAR_PoseInfoList
{
    public HashMap<String, PoseInfo> tasqar_poseInfos = new HashMap<String, PoseInfo>();

    public float[] firstTranslation = new float[3];
    public boolean firstTranslationSet = false;

    public PoseInfo GetUserPoseInfo(String userName)
    {
//        Log.d(VideoChatActivity.TAG, "GETTING " + userName);

        PoseInfo poseInfo = tasqar_poseInfos.get(userName);

        if(poseInfo == null)
        {
//            Log.d(VideoChatActivity.TAG, "NO USER FOR " + userName);
            poseInfo = SetUserPoseInfo(userName, null);
        }

        return poseInfo;
    }

    public PoseInfo  SetUserPoseInfo(String userName, PoseInfo poseInfo)
    {
        if(poseInfo == null)
        {
            poseInfo = new PoseInfo(userName);
            tasqar_poseInfos.put(userName, poseInfo);
        }
        else if(tasqar_poseInfos.containsKey(userName))
        {
            tasqar_poseInfos.replace(userName, poseInfo);
        }
        else
        {
            tasqar_poseInfos.put(userName, poseInfo);
        }

        return  poseInfo;
    }

    public void UpdateAllAnchors(FrustumVisibilityTester frustumVisibilityTester)
    {
        Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        while (poseInfoKeysIterator.hasNext())
        {
            String key = poseInfoKeysIterator.next();

            PoseInfo poseInfo = GetUserPoseInfo(key);

            ArrayList<AnchorList> anchorsLists = poseInfo.anchorsLists;

            int numAnchorList = anchorsLists.size();

            for(int j= 0; j < numAnchorList; j++)
            {
                AnchorList anchorList = anchorsLists.get(j);

                if(anchorList.anchorListType != 0)
                    continue;

                anchorList.UpdateAllAnchors(frustumVisibilityTester);

                anchorsLists.set(j, anchorList);
            }

            poseInfo.anchorsLists = anchorsLists;

            SetUserPoseInfo(key, poseInfo);
        }
    }

    public void DestroyAll()
    {
        Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        while (poseInfoKeysIterator.hasNext()) {

            String key = poseInfoKeysIterator.next();

            PoseInfo poseInfo = GetUserPoseInfo(key);
            poseInfo.DestroyAllAnchors();
            SetUserPoseInfo(key, poseInfo);
        }
    }

    public void RemoveAllZeroAnchorsList()
    {
        Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        while (poseInfoKeysIterator.hasNext()) {
            String key = poseInfoKeysIterator.next();
            PoseInfo poseInfo = GetUserPoseInfo(key);
            poseInfo.RemoveAllZeroAnchorsList();
            SetUserPoseInfo(key, poseInfo);
        }
    }

    public void AddBreak(String userName) {
        PoseInfo poseInfo = GetUserPoseInfo(userName);
        poseInfo.AddBreak();
        SetUserPoseInfo(userName, poseInfo);
    }

    public void AddPoint(HitResult hitResult, String userName, int anchorListType)
    {
        PoseInfo userPoseInfo = GetUserPoseInfo(userName);
        Pose hitPose = hitResult.getHitPose();

        if(anchorListType == 1)
        {
            userPoseInfo = AddAnchor(hitResult, hitPose, userName, anchorListType);
            userPoseInfo.currentUserAnchorList.anchorListType = anchorListType;
            userPoseInfo.AddBreak();
            SetUserPoseInfo(userName, userPoseInfo);
            return;
        }

        Pose previousPose = userPoseInfo.previousPose;

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
                    userPoseInfo = AddAnchor(hitResult, newPose, userName, anchorListType);
                }
            }
            else
            {
                userPoseInfo = AddAnchor(hitResult, hitPose, userName, anchorListType);
            }
        }
        else
        {
            userPoseInfo = AddAnchor(hitResult, hitPose, userName, anchorListType);
        }

        userPoseInfo.AddPreviousPose(hitPose);
        SetUserPoseInfo(userName, userPoseInfo);
    }

    public PoseInfo AddAnchor(HitResult hitResult, Pose hitPose, String userName, int anchorListType)
    {
        return GetUserPoseInfo(userName).AddAnchor(hitResult, hitPose, anchorListType);
    }

    boolean isWorldReferenceChanged()
    {
        Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        if(poseInfoKeysIterator.hasNext())
        {
            PoseInfo poseInfo = tasqar_poseInfos.get(poseInfoKeysIterator.next());

            if(poseInfo.anchorsLists.size() == 0) {
                firstTranslationSet = false;
                return false;
            }
            AnchorList anchorsList = poseInfo.anchorsLists.get(0);

            if (anchorsList.numAnchors > 0) {
                boolean firstPointChanged = anchorsList.isPointEqual(0, firstTranslation);

                if (firstPointChanged) {
                    return true;
                }
            }
        }

        return false;
    }

    Object makeVerticesSyncObject = new Object();

    void MakeExtrusionVerticesArrays(FrustumVisibilityTester frustumVisibilityTester)
    {
        synchronized (makeVerticesSyncObject)
        {
            Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
            Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

            while (poseInfoKeysIterator.hasNext())
            {
                String key = poseInfoKeysIterator.next();

                PoseInfo poseInfo = tasqar_poseInfos.get(key);

                int numAnchorsLists = poseInfo.anchorsLists.size();
                for(int anchorsListCount = 0; anchorsListCount < numAnchorsLists; anchorsListCount++)
                {
                    AnchorList anchorList = poseInfo.anchorsLists.get(anchorsListCount);

                    if(anchorList.anchorListType != 0)
                        continue;

                    anchorList.CheckDirty();

                    if (anchorList.isDirty)
                    {
                        anchorList.calcVertices(frustumVisibilityTester);
                    }
                }

                if(numAnchorsLists > 0 && !firstTranslationSet)
                {
                    AnchorList anchorList = poseInfo.anchorsLists.get(0);
                    TASQAR_Anchor tasqar_anchor = anchorList.anchors.get(0);

                    firstTranslationSet = true;
                    firstTranslation[0] = tasqar_anchor.getPose().tx();
                    firstTranslation[1] = tasqar_anchor.getPose().ty();
                    firstTranslation[2] = tasqar_anchor.getPose().tz();
                }

                SetUserPoseInfo(key, poseInfo);
            }
        }
    }

    public void DirtyAllAnchorLists() {

        Set<String> poseInfoKeys = tasqar_poseInfos.keySet();
        Iterator<String> poseInfoKeysIterator = poseInfoKeys.iterator();

        while (poseInfoKeysIterator.hasNext()) {
            String key = poseInfoKeysIterator.next();

            PoseInfo poseInfo = tasqar_poseInfos.get(key);

            int numAnchorLists = poseInfo.anchorsLists.size();

            for (int i = 0; i < numAnchorLists; i++) {
                poseInfo.anchorsLists.get(i).isDirty = true;
            }

            SetUserPoseInfo(key, poseInfo);
        }
    }

    public Set<String> GetKeySet()
    {
        return tasqar_poseInfos.keySet();
    }

    public boolean Undo(String userName) {
        synchronized (makeVerticesSyncObject) {
            PoseInfo poseInfo = GetUserPoseInfo(userName);
            boolean undoResult = poseInfo.Undo();
            SetUserPoseInfo(userName, poseInfo);

            return undoResult;
        }
    }
}
