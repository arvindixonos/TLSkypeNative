package com.takeleap.tasqar;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import java.util.List;

import processing.core.PVector;

/**
 * Created by TakeLeap05 on 28-08-2018.
 */

public class TASQAR_Anchor
{
    public Anchor anchor;

    public AnchorList parentAnchorList;

    public TrackingState previousAnchorTrackingState;

    public  boolean     isPointVisibleToCamera = false;

    public boolean      isHitCameraRay = false;

    public TASQAR_Anchor(Anchor anchor, AnchorList parentAnchorList)
    {
        this.anchor = anchor;
        this.parentAnchorList = parentAnchorList;
        previousAnchorTrackingState = anchor.getTrackingState();
    }

    public void CheckDirty()
    {
        TrackingState currentTrackingState = anchor.getTrackingState();

        if(currentTrackingState != previousAnchorTrackingState)
        {
            parentAnchorList.isDirty = true;

            previousAnchorTrackingState = currentTrackingState;
        }
    }

    public  float   CAM_RAY_CAST_DISTANCE_THRESHOLD = 0.3f;
    public void CheckHitCameraRay(float[] camPosition, float[] camAxis, int anchorIndex, int numAnchors)
    {
        isHitCameraRay = false;

        if(VideoChatActivity.getInstance() != null) {
            List<HitResult> hitResults = VideoChatActivity.getInstance().frame.hitTest(camPosition, 0, camAxis, 0);

            if (hitResults.size() == 0)
                return;

            int hitCount = 0;
            for (HitResult hitResult : hitResults) {
                float[] hitPosition = hitResult.getHitPose().getTranslation();

                float[] ourPosition = getPose().getTranslation();

                if (PVector.dist(new PVector(ourPosition[0], ourPosition[1], ourPosition[2]),
                        new PVector(hitPosition[0], hitPosition[1], hitPosition[2])) < CAM_RAY_CAST_DISTANCE_THRESHOLD) {
                    hitCount += 1;
                }
            }

            if (hitResults.size() > 0)
            {
                float percentHit = (float) hitCount / (float) hitResults.size();

//                    Log.d(TAG, "LOCAL CAM HIT PER:" + anchorIndex + " " + percentHit * 100 + " " + numAnchors);

                isHitCameraRay = percentHit > 0.2f;
            }
        }
    }

    public Pose getPose() {
        return anchor.getPose();
    }

    public void detach() {
        anchor.detach();
    }

    public void UpdateAnchor(FrustumVisibilityTester frustumVisibilityTester) {

        float[] ourPosition = getPose().getTranslation();

        isPointVisibleToCamera = frustumVisibilityTester.isPointInFrustum(ourPosition[0], ourPosition[1], ourPosition[2]);

        parentAnchorList.isDirty = true;
    }
}