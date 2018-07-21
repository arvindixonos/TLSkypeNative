package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CustomImageButton extends AppCompatImageButton implements View.OnTouchListener
{
    private static String TAG = "CustomButton";
    public CustomImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        this.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
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
            }
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
