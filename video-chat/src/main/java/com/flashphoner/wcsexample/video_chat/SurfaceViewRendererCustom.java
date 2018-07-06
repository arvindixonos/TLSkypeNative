package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.graphics.ImageFormat.NV21;

/**
 * Created by TakeLeap05 on 04-07-2018.
 */

public class SurfaceViewRendererCustom extends SurfaceViewRenderer {
    private static final float TOUCH_TOLERANCE = 4;
    public boolean savePic = false;
    public YuvFrame yuvFrame;
    Bitmap screenShotBitmap;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private int newWidth, newHeight;
    private float mX, mY;
    private Paint mPaint;

    public  boolean touchEnabled = false;

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

        setDrawingCacheEnabled(true);
    }

    public SurfaceViewRendererCustom(Context context, AttributeSet attrs) {
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

        setDrawingCacheEnabled(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.d(VideoChatActivity.TAG, w + " " + h + " " + oldw + " " + oldh);

        if (w != 0 && h != 0) {
            newWidth = w;
            newHeight = h;
        }

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

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
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
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

//        Log.d(VideoChatActivity.TAG, "ON DRAW");
        canvas.drawColor(0Xcbf442);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame frame) {
//        synchronized (previousFrame)
//        {
//            previousFrame = frame;
//        }

//        if(frame.yuvPlanes.length == 0)
//            Log.d(VideoChatActivity.TAG, "YUV PLANES ARE NULL");
//        else
//            Log.d(VideoChatActivity.TAG, "NUM YUV PLANES " + frame.yuvPlanes.length);

//        screenShotBitmap = captureBitmapFromYuvFrame(frame);
        super.renderFrame(frame);

        if (savePic) {
//            Log.d(VideoChatActivity.TAG, "NUM YUV PLANES " + frame.yuvPlanes.length);
            yuvFrame = new YuvFrame(frame);
            if(!yuvFrame.hasData())
            {
                return;
            }

            Log.d(VideoChatActivity.TAG, yuvFrame.height + " HEIGHT OF FRAME");

            Bitmap drawingCacheBitmap = loadBitmapFromView(this, newWidth, newHeight);
            Bitmap yuvFrameBitmap = yuvFrame.getBitmap();
//            yuvFrameBitmap = FlipBitmap(yuvFrameBitmap);

            drawingCacheBitmap = Bitmap.createScaledBitmap(drawingCacheBitmap, yuvFrameBitmap.getWidth(),
                    yuvFrameBitmap.getHeight(), false);

            screenShotBitmap = AddBitmaps(yuvFrameBitmap, drawingCacheBitmap);

            FileOutputStream out = null;
            try {
                out = new FileOutputStream("/sdcard/scr.png");
                Log.d(VideoChatActivity.TAG, "" + screenShotBitmap.isMutable());
                screenShotBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                // bmp is your Bitmap instance
                out.close();

                VideoChatActivity.getInstance().SendScreenshot();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ClearDrawingCache();

            savePic = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(!touchEnabled)
            return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                Bitmap drawingCache = getDrawingCache();
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

                if(!VideoChatActivity.getInstance().participantPublishing)
                {
                    ClearDrawingCache();
                }
                else
                {
                    savePic = true;
                }

                invalidate();
                break;
        }
        return true;
    }

    public void ClearDrawingCache()
    {
        Bitmap drawingCache = getDrawingCache();
        this.destroyDrawingCache();
        this.buildDrawingCache();
        drawingCache = getDrawingCache();

        mPath.rewind();
        mPath.reset();
        mBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        draw(mCanvas);

        postInvalidate();
    }

    public Bitmap AddBitmaps(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    public Bitmap loadBitmapFromView(View v, int width, int height) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    public Bitmap FlipBitmap(Bitmap d)
    {
        Matrix m = new Matrix();
        m.preScale(-1, 1);
        Bitmap src = d;
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return dst;
    }
}
