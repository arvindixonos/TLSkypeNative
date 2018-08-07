package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by TakeLeap05 on 04-07-2018.
 */

public class SurfaceViewRendererCustom extends SurfaceViewRenderer
{
    private static final float TOUCH_TOLERANCE = 4;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private int newWidth, newHeight;
    private float mX, mY;
    private Paint mPaint;
    public YuvFrame yuvFrame;
    public boolean savePic = false;
    Bitmap screenShotBitmap;

    //DrawPicture Vars
    public boolean drawEnabled;
    private boolean drawReset;
    private float xStart;
    private float yStart;
    Picture drawPicture;
    Rect dst = new Rect(0, 0, 173, 485);
    Rect nullRect = new Rect(0,0,0,0);
    //End DrawPicture Vars

    public SurfaceViewRendererCustom(Context context)
    {
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

    private void drawTouch_start(float x, float y)
    {
        if(!drawEnabled)
            return;

        xStart = x;
        yStart = y;
        drawPicture = getPicture(false);
        drawReset = false;
    }

    private void drawTouch_move(float x, float y)
    {
        if(!drawEnabled)
            return;

        dst = new Rect((int) xStart, (int) yStart, (int) x, (int) y);
    }

    private void drawTouch_up(float x, float y)
    {
        if(!drawEnabled)
            return;

    }

    public Picture getPicture(boolean clear)
    {
        Bitmap bitmap;
        if(!clear)
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_t);
        else
        {
            bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        }
        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, null, new RectF(0f, 0f, (float) bitmap.getWidth(), (float) bitmap.getHeight()), null);
        picture.endRecording();
        return picture;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.d(VideoChatActivity.TAG, w + " " + h + " " + oldw + " " + oldh);

        if (w != 0 && h != 0)
        {
            newWidth = w;
            newHeight = h;
        }

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        if(!drawEnabled)
        {
            canvas.drawColor(0Xcbf442);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }
        else
        {
            canvas.drawPicture(drawPicture, dst);
        }
        if(drawReset)
        {
            canvas.drawPicture(drawPicture, nullRect);
        }
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame frame)
    {
        super.renderFrame(frame);

        if (savePic)
        {
            yuvFrame = new YuvFrame(frame);
            if(!yuvFrame.hasData())
            {
                return;
            }

            Log.d(VideoChatActivity.TAG, yuvFrame.height + " HEIGHT OF FRAME");

            Bitmap drawingCacheBitmap = loadBitmapFromView(this, newWidth, newHeight);
            Bitmap yuvFrameBitmap = yuvFrame.getBitmap();

            drawingCacheBitmap = Bitmap.createScaledBitmap(drawingCacheBitmap, yuvFrameBitmap.getWidth(),
                    yuvFrameBitmap.getHeight(), false);

            screenShotBitmap = AddBitmaps(yuvFrameBitmap, drawingCacheBitmap);

            FileOutputStream out = null;
            try
            {
                out = new FileOutputStream("/sdcard/scr.png");
                Log.d(VideoChatActivity.TAG, "" + screenShotBitmap.isMutable());
                screenShotBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                // bmp is your Bitmap instance
                out.close();

                VideoChatActivity.getInstance().SendScreenshot();

            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            if(drawEnabled) {
                ClearDrawingCache();
                drawPicture = getPicture(true);
                drawReset = true;
            }

            savePic = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:

                int xPos = (int)event.getX();
                int yPos = (int)event.getY();
                int width = getWidth();
                int height = getHeight();

//                VideoChatActivity.getInstance().TapSend(xPos, yPos, width, height);
                if(drawEnabled)
                    drawTouch_start(event.getX(), event.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:

                xPos = (int)event.getX();
                yPos = (int)event.getY();
                width = getWidth();
                height = getHeight();

//                Log.d(VideoChatActivity.TAG, "MOVE RECORDERD");

//                Log.d(VideoChatActivity.TAG, "SURF " + xPos + " " + yPos);

                VideoChatActivity.getInstance().TapSend(xPos, yPos, width, height);

                if(drawEnabled)
                    drawTouch_move(event.getX(), event.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:

                VideoChatActivity.getInstance().AddBreak();

                drawTouch_up(event.getX(), event.getY());
                invalidate();
                if(!VideoChatActivity.getInstance().participantPublishing)
                {
                    ClearDrawingCache();
                }
                else if(drawEnabled)
                {
                    savePic = true;
                }
                break;
        }

        return true;
    }

    public void ClearDrawingCache()
    {
        this.destroyDrawingCache();
        this.buildDrawingCache();

        mPath.rewind();
        mPath.reset();
        mBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        draw(mCanvas);

        postInvalidate();
    }

    public Bitmap AddBitmaps(Bitmap bmp1, Bitmap bmp2)
    {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    public Bitmap loadBitmapFromView(View v, int width, int height)
    {
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