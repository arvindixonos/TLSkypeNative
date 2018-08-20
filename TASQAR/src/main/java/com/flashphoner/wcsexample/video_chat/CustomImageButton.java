package com.flashphoner.wcsexample.video_chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CustomImageButton extends AppCompatImageButton implements View.OnTouchListener
{
    private static String TAG = "CustomButton";
    @SuppressLint("ClickableViewAccessibility")
    public CustomImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.setOnTouchListener((v, event) ->
        {
            switch(event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    OnTouchDown(v);
                    break;
                case MotionEvent.ACTION_UP:
                    OnTouchUp(v);
                    break;
            }
            return false;
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        return false;
    }

    public void OnTouchDown(View v)
    {
        CustomImageButton imgBtn = findViewById(v.getId());
        imgBtn.setImageResource(R.drawable.pin_num_buttons);
        RelativeLayout parent = (RelativeLayout) imgBtn.getParent();
        TextView textComponent = (TextView) parent.getChildAt(1);
        textComponent.setTextColor(getResources().getColor(R.color.blueBG));
    }

    public void OnTouchUp(View v)
    {
        CustomImageButton imgBtn = findViewById(v.getId());
        imgBtn.setImageResource(R.drawable.pin_num_buttons_outline);
        RelativeLayout parent = (RelativeLayout) imgBtn.getParent();
        TextView textComponent = (TextView) parent.getChildAt(1);
        textComponent.setTextColor(getResources().getColor(R.color.blueGlow));
    }
}

class TempButton extends AppCompatImageButton implements View.OnTouchListener
{
    private float startXPos;
    private float startYPos;
    private float xPos;
    private float yPos;

    private Context currentContext;

    private boolean moved = false;
    private boolean movedDown = false;

    public ConstraintLayout layoutHolder;
    public Button   mPointOrPlaneButton;
    public Button   mDrawModeButton;
    public Button   mArrowModeButton;

    @SuppressLint("ClickableViewAccessibility")
    public TempButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        currentContext = context;
        this.setOnTouchListener((v, event) ->
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    Log.d(VideoChatActivity.TAG, "Tapped");
                    startXPos = event.getX();
                    startYPos = event.getY();
                    moved = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(!movedDown)
                    {
                        if ((startYPos + 50) < event.getY())
                        {
                            moved = true;
                            Log.d(VideoChatActivity.TAG, "Moved BY :" + startYPos + "Y :" + event.getY());

                            MoveDrawer(event.getX(), event.getY());
                        }
                    }
                    else {
                        if ((startYPos + 50) > event.getY())
                        {
                            moved = true;
                            Log.d(VideoChatActivity.TAG, "Moved");
                            MoveDrawer(event.getX(), event.getY());
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:

                    Log.d(VideoChatActivity.TAG, "Released");
                    if(moved)
                    {
                        AdjustParams();
                    }
                    else
                    {
                        Toast.makeText(currentContext, "Undo", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            return false;
        });
    }

    public void InitialiseButtons ()
    {
        mPointOrPlaneButton.setOnClickListener(v ->
        {
            Toast.makeText(currentContext, "Point Or Plane", Toast.LENGTH_SHORT).show();
            VideoChatActivity.getInstance().pointsOrPlaneSpawn = true;
        });

        mDrawModeButton.setOnClickListener(v ->
        {
            Toast.makeText(currentContext, "Draw Mode", Toast.LENGTH_SHORT).show();
            VideoChatActivity.getInstance().arrowMode = false;
        });

        mArrowModeButton.setOnClickListener(v ->
        {
            Toast.makeText(currentContext, "Arrow Mode", Toast.LENGTH_SHORT).show();
            VideoChatActivity.getInstance().arrowMode = true;
        });
    }

    public void AdjustParams ()
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutHolder.getLayoutParams();
        if(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, params.height, getResources().getDisplayMetrics()) > 200)
        {
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
            movedDown = true;
        }
        else
        {
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
            movedDown = false;
        }
        layoutHolder.setLayoutParams(params);
    }

    public void MoveDrawer (float xPosition, float yPosition)
    {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutHolder.getLayoutParams();
        params.height = (int) (yPosition);
        Log.d("TLSKYPE", " Moved " + params.height);
        layoutHolder.setLayoutParams(params);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        return false;
    }
}