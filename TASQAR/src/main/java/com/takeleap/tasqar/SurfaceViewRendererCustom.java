package com.takeleap.tasqar;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.takeleap.tasqar.R;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

import java.util.ArrayList;
import java.util.List;


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
    public boolean drawEnabled = true; //TODO
    private boolean drawReset;
    private float xStart;
    private float yStart;
    List<Drawing> drawPictures = new ArrayList<>();
    Picture drawPicture;
    Rect dst = new Rect(0, 0, 173, 485);
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor = Color.RED;
    public Activity currentActivity;
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
        drawPicture = getPicture(false);
        dst = new Rect(- 25,  - 25, 25, 25);

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
        drawPicture = getPicture(false);
        dst = new Rect(- 25,  - 25, 25, 25);

        setDrawingCacheEnabled(true);
    }


    public void DrawBlinker (float x, float y)
    {
        drawPictures.add(new Drawing(new Point(25, 25), new Point((int) x, (int) y), getResources(), currentActivity, Drawing.Art.BLINK, this));
        invalidate();
    }

    public void DrawArrow (float x, float y)
    {
        drawPictures.add(new Drawing(new Point(25, 70), new Point((int) x, (int) y), getResources(), currentActivity, Drawing.Art.ARROW, this));
        invalidate();
    }

    public void ClearCanvas ()
    {
        drawPictures.clear();
        mPath.rewind();
        mPath.reset();
        mBitmap.recycle();
        mBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPath = null;
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

    public void drawTouch_move(float x, float y)
    {
        if(!drawEnabled)
            return;

        dst = new Rect((int) x - 25, (int) y - 25, (int) x + 25, (int) y + 25);
        invalidate();
    }

    public void PaintStart (float x, float y)
    {
        Log.d("PAINT", "Paint Start");

        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, 20,  mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private boolean paintStarted = false;
    public void PaintMove (float x, float y)
    {
        if(!paintStarted)
        {
            PaintStart(x, y);
            paintStarted = true;
            return;
        }
        Log.d("PAINT", "Paint Move");

        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
        {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    public void PaintUp ()
    {
        Log.d("PAINT", "Paint Up");

        mPath.lineTo(mX, mY);
        paintStarted = false;
    }


    public Picture getPicture(boolean clear)
    {
        Bitmap bitmap;
        if(!clear)
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.button_record);
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

        canvas.drawPicture(drawPicture, dst);

        if(drawPictures.size() != 0)
        {
            for (Drawing drawnPicture : drawPictures)
            {
                canvas.drawBitmap(drawnPicture.mDrawingBitmap, null, drawnPicture.drawnRect, drawnPicture.mDrawingPaint);
            }
        }
        if(drawEnabled && mPath != null)
        {
            canvas.save();
            mCanvas.drawPath(mPath, mPaint);

            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.restore();
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

                if(VideoChatActivity.getInstance().arrowMode)
                    VideoChatActivity.getInstance().TapSend(xPos, yPos, width, height);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:

                xPos = (int)event.getX();
                yPos = (int)event.getY();
                width = getWidth();
                height = getHeight();

                if(!VideoChatActivity.getInstance().arrowMode)
                    VideoChatActivity.getInstance().TapSend(xPos, yPos, width, height);

                invalidate();
                break;
            case MotionEvent.ACTION_UP:

                if(VideoChatActivity.getInstance().isODG)
                {
                    VideoChatActivity.getInstance().BreakSend();
                }
                if(!VideoChatActivity.getInstance().arrowMode)
                    VideoChatActivity.getInstance().BreakSend();
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

class FingerPath
{

    public int color;
    public int strokeWidth;
    public Path path;

    public FingerPath(int color, int StrokeWidth, Path path)
    {
        this.color = color;
        this.strokeWidth = StrokeWidth;
        this.path = path;
    }
}

class Drawing
{
    Bitmap mDrawingBitmap;
    Paint mDrawingPaint;
    Rect drawnRect;

    private Resources res;
    private Rect drawnRectStart;
    private Point position;
    private Point size;
    private Activity currentActivity;
    private Art thisArt;
    private Thread fadeThread;
    private ColorFilter mDrawingColorFilter;
    private SurfaceViewRendererCustom thisRenderer;
    private int expansionSize = 10;

    public enum Art
    {
        ARROW,
        BLINK
    }

    public Drawing(Point size, Point position, Resources resources, Activity activity, Art currentArt, SurfaceViewRendererCustom currentRenderer)
    {
        this.position = position;
        this.size = size;
        thisArt = currentArt;
        currentActivity = activity;
        res = resources;
        thisRenderer = currentRenderer;

        switch(thisArt)
        {
            case ARROW:
                mDrawingBitmap = BitmapFactory.decodeResource(res, R.drawable.draw_arrow_t);
                drawnRect = new Rect(position.x - size.x, position.y - (size.y * 2), position.x + size.x, position.y);
                break;
            case BLINK:
                mDrawingBitmap = BitmapFactory.decodeResource(res, R.drawable.button_record);
                drawnRect = new Rect(position.x - size.x, position.y - size.y, position.x + size.x, position.y + size.y);
                drawnRectStart = new Rect(drawnRect);
                StartAnimation();
                break;
        }

        mDrawingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDrawingColorFilter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        mDrawingPaint.setColorFilter(mDrawingColorFilter);
    }

    public int getLeft()
    {
        return position.x - (mDrawingBitmap.getWidth() / 2);
    }

    public int getTop()
    {
        return position.y + (mDrawingBitmap.getHeight() / 2);
    }

    public void StartAnimation ()
    {
        fadeThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(true)
                {
                    for (int i = 0; i < expansionSize; i++)
                    {
                        drawnRect.bottom = drawnRect.bottom + i;
                        drawnRect.top = drawnRect.top - i;
                        drawnRect.left = drawnRect.left - i;
                        drawnRect.right = drawnRect.right + i;
                        if(mDrawingPaint != null)
                            mDrawingPaint.setAlpha(i * 25);

                        try
                        {
                            Thread.sleep(100);
                            thisRenderer.invalidate();
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    drawnRect.set(drawnRectStart);
                }
            }
        });

        fadeThread.start();
    }
}