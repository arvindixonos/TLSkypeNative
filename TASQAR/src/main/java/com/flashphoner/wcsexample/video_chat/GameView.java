package com.flashphoner.wcsexample.video_chat;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.SurfaceView;

import android.content.*;
import android.view.*;
import android.graphics.*;

import org.webrtc.SurfaceViewRenderer;

import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

public class GameView extends SurfaceViewRenderer
{
    private SurfaceHolder holder;
    private Bitmap bmp;
    private float xPos, yPos;
    private Context appContext;
    private Canvas canvas;
    private Drawable draw;
    private float xStart, yStart, xCurrent, yCurrent;

    public GameView(Context context) {
        super(context);
        appContext = context;
        this.bmp = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow);
        holder = getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { }

            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {
                canvas = holder.lockCanvas();
                holder.unlockCanvasAndPost(canvas);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {

            }

        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event.getX(), event.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(event.getX(), event.getY());
                break;
        }
        return true;
    }



    public void onTouchDown (float x, float y)
    {
        Log.d(TAG, x + " : " + y);
        xStart = x;
        yStart = y;
//        draw = getResources().getDrawable(R.drawable.draw_arrow);
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_t);
        bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
//        canvas = new Canvas(mutable);
    }
    Rect dst;
    public void onTouchMove (float x, float y)
    {
        holder.lockCanvas();
        dst = new Rect((int) xStart, (int) yStart, (int) x, (int) y);
        canvas.drawBitmap(bmp, null, dst, null);
        holder.unlockCanvasAndPost(canvas);
//        Rect dst = new Rect(0, 0, 100, 100);
    }

//    public void onTouchMove (float x, float y)
//    {
//        dst = new Rect((int) xStart, (int) yStart, (int) x, (int) y);
////        canvas.drawBitmap(bmp, null, dst, null);
//        draw.setBounds(dst);
//        draw.draw(canvas);
//    }

    public void onTouchUp (float x, float y)
    {
//        holder.lockCanvas();
//        Log.d(TAG, "clearing");
////        canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
//        canvas.save();
//        Rect canvRect = new Rect (0, 0, canvas.getWidth(), canvas.getHeight());
//        Paint clearPaint = new Paint();
//        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        canvas.drawRect(0, 0, canvRect.width(), canvRect.height(), clearPaint);
//        canvas.drawRect(canvRect, clearPaint);
//        holder.unlockCanvasAndPost(canvas);
        Bitmap bmp1 = Bitmap.createBitmap(getDisplay().getWidth(), getDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        canvas.drawRect(0, 0, getDisplay().getWidth(), getDisplay().getHeight(), null);
        this.destroyDrawingCache();
        this.buildDrawingCache();
        draw(canvas);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bmp, null, dst, null);
        invalidate();
    }

    protected void drawThis(Canvas canvas)
    {
        canvas.drawBitmap(this.bmp, xPos, yPos, null);
    }

}
