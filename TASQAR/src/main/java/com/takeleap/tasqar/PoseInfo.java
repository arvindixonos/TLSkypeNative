package com.takeleap.tasqar;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;

import java.util.ArrayList;

public class PoseInfo {

    public String   userName;
    public float[]  selectedColor = new float[]{1.0f, 0.0f, 0.0f, 0.0f};
    public Pose previousPose = null;
    public AnchorList currentUserAnchorList = null;
    public ArrayList<AnchorList> anchorsLists = new ArrayList<AnchorList>();

    public  PoseInfo(String userName)
    {
        this.userName = userName;
        this.previousPose = null;
        this.currentUserAnchorList = null;
    }

    public void AddPreviousPose(Pose previousPose)
    {
        this.previousPose = previousPose;
    }

    public void AddBreak()
    {
        previousPose = null;

        if(currentUserAnchorList != null)
            anchorsLists.add(currentUserAnchorList);

        currentUserAnchorList = null;
    }

    public void DestroyAllAnchors()
    {
        int numAnchorLists = anchorsLists.size();

        for(int i = 0; i < numAnchorLists; i++)
        {
            anchorsLists.get(i).DetachAllAnchors();
        }
    }

    public void RemoveAllZeroAnchorsList()
    {
        int numAnchorsLists = anchorsLists.size();

        for(int i = 0; i < numAnchorsLists; i++)
        {
            AnchorList anchorList = anchorsLists.get(i);

            if(anchorList.numAnchors < 2 && anchorList.getAnchorListType() == 0)
            {
                anchorList.DetachAllAnchors();

                anchorsLists.remove(i);

                numAnchorsLists = anchorsLists.size();

                i = 0;
            }
        }
    }

    public PoseInfo AddAnchor(HitResult hitResult, Pose hitPose, int anchorListType)
    {
        if(currentUserAnchorList == null)
        {
            currentUserAnchorList = new AnchorList();
            currentUserAnchorList.setAnchorListType(anchorListType);
            currentUserAnchorList.setAnchorListColor(selectedColor);
        }

        Anchor anchor = hitResult.getTrackable().createAnchor(hitPose);
        currentUserAnchorList.AddAnchor(new TASQAR_Anchor(anchor, currentUserAnchorList));

        return this;
    }

    public boolean Undo()
    {
        if(anchorsLists.size() > 0)
        {
            AnchorList removedList = anchorsLists.remove(anchorsLists.size() - 1);

            removedList.DetachAllAnchors();

            return  true;
        }

        return  false;
    }
}
