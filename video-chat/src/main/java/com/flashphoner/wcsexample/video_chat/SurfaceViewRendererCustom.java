package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;

import org.webrtc.SurfaceViewRenderer;

/**
 * Created by TakeLeap05 on 04-07-2018.
 */

public class SurfaceViewRendererCustom extends SurfaceViewRenderer
{
    public SurfaceViewRendererCustom(Context context) {
        super(context);
    }

    @Override
    public void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        Log.d(VideoChatActivity.TAG, "CAlled Draw");
    }
}
