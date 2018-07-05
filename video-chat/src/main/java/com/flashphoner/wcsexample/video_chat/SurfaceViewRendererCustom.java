package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.webrtc.SurfaceViewRenderer;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by TakeLeap05 on 04-07-2018.
 */

public class SurfaceViewRendererCustom extends SurfaceViewRenderer
{
    private Bitmap mBitmap;
    private Canvas  mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;

    public SurfaceViewRendererCustom(Context context) {
        super(context);
        this.setWillNotDraw(false);

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
    }

    public SurfaceViewRendererCustom(Context context, AttributeSet attrs){
        super(context, attrs);
        this.setWillNotDraw(false);

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
    }

    private     int  newWidth, newHeight;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.d(VideoChatActivity.TAG, w + " " + h + " " + oldw + " " + oldh);

        if(w > 0 && h > 0)
        {
            newWidth = w;
            newHeight = h;

            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    private Paint       mPaint;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        Log.d(VideoChatActivity.TAG, "ON DRAW");

        canvas.drawColor(0Xcbf442);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                Log.d(VideoChatActivity.TAG, "ACTION DOWN");
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
//                Log.d(VideoChatActivity.TAG, "ACTION_MOVE");
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
//                Log.d(VideoChatActivity.TAG, "ACTION_UP");
                touch_up();

                Bitmap screenShotBitmap = loadBitmapFromView(this, newWidth, newHeight);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream("/sdcard/scr.png");
                    screenShotBitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                invalidate();
                break;
        }
        return true;
    }

    public Bitmap loadBitmapFromView(View v, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width , height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
        v.draw(c);
        return b;
    }
}
