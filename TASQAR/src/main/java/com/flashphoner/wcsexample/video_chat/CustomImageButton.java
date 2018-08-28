package com.flashphoner.wcsexample.video_chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
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

import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

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

    private boolean arrowMode = false;
    private boolean p2pMode;

    private Context currentContext;
    public VideoChatActivity chatActivity;

    private boolean moved = false;
    private boolean movedDown = false;

    public ConstraintLayout layoutHolder;

    public FloatingActionButton mPointOrPlaneModeFloatingButton;
    public FloatingActionButton mDrawModeFloatingButton;
    public FloatingActionButton mArrowModeFloatingButton;

    public Button   mPointOrPlaneButton;
    public Button   mDrawModeButton;
    public Button   mArrowModeButton;

    public TempButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        currentContext = context;
        this.setOnTouchListener((v, event) ->
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "Tapped");
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
                            Log.d(TAG, "Moved BY :" + startYPos + "Y :" + event.getY());

                            MoveDrawer(event.getX(), event.getY());
                        }
                    }
                    else {
                        if ((startYPos + 50) > event.getY())
                        {
                            moved = true;
                            Log.d(TAG, "Moved");
                            MoveDrawer(event.getX(), event.getY());
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:

                    Log.d(TAG, "Released");
                    if(moved)
                    {
                        AdjustParams();
                    }
                    else
                    {
                        Toast.makeText(currentContext, "Undo", Toast.LENGTH_SHORT).show();

                        VideoChatActivity.getInstance().UndoClicked();
                    }
                    break;
            }
            return false;
        });
    }

    public void InitialiseButtons ()
    {
//        mPointOrPlaneButton.setOnClickListener(v ->
//        {
//            TogglePointOrPlaneMode(true);
//        });

        mDrawModeButton.setOnClickListener(v ->
        {
            ToggleDrawMode(true);
            arrowMode = !arrowMode;
            AdaptButtons();
        });

        mArrowModeButton.setOnClickListener(v ->
        {
            ToggleArrowMode(true);
            arrowMode = !arrowMode;
            AdaptButtons();
        });

//        mPointOrPlaneModeFloatingButton.setOnClickListener(v ->
//        {
//            TogglePointOrPlaneMode(true);
//            mPointOrPlaneModeFloatingButton.setBackgroundColor(currentContext.getResources().getColor(R.color.redLight));
//        });

        mDrawModeFloatingButton.setOnClickListener(v ->
        {
            ToggleArrowMode(true);
            arrowMode = !arrowMode;
            AdaptButtons();
        });

        mArrowModeFloatingButton.setOnClickListener(v ->
        {
            ToggleArrowMode(true);
            arrowMode = !arrowMode;
            AdaptButtons();
        });
    }

    private void AdaptButtons ()
    {
        if(arrowMode)
        {
            mArrowModeFloatingButton.setBackgroundTintList(ColorStateList.valueOf(currentContext.getResources().getColor(R.color.redLight)));
            mDrawModeFloatingButton.setBackgroundTintList(ColorStateList.valueOf(currentContext.getResources().getColor(R.color.blueDark)));
        }
        else
        {
            mDrawModeFloatingButton.setBackgroundTintList(ColorStateList.valueOf(currentContext.getResources().getColor(R.color.redLight)));
            mArrowModeFloatingButton.setBackgroundTintList(ColorStateList.valueOf(currentContext.getResources().getColor(R.color.blueDark)));
        }
    }

    public void ToggleArrowMode (boolean isSender)
    {
        if(isSender)
        {
            Toast.makeText(currentContext, "Arrow Mode", Toast.LENGTH_SHORT).show();
            chatActivity.arrowMode = !chatActivity.arrowMode;
//            chatActivity.SendMessage("CTRL:-AR");
        }
        else
        {
            Toast.makeText(currentContext, "Arrow Mode", Toast.LENGTH_SHORT).show();
            chatActivity.arrowMode = !chatActivity.arrowMode;
        }
    }

    public void ToggleDrawMode (boolean isSender)
    {
        if(isSender)
        {
            Toast.makeText(currentContext, "Draw Mode", Toast.LENGTH_SHORT).show();
            chatActivity.arrowMode = false;
//            chatActivity.SendMessage("CTRL:-DR");
        }
        else
        {
            Toast.makeText(currentContext, "Draw Mode", Toast.LENGTH_SHORT).show();
            chatActivity.arrowMode = false;
        }
    }

    public void TogglePointOrPlaneMode (boolean isSender)
    {
        if(isSender)
        {
            Toast.makeText(currentContext, "Point Or Plane", Toast.LENGTH_SHORT).show();
            chatActivity.pointsOrPlaneSpawn = !chatActivity.pointsOrPlaneSpawn;
//            chatActivity.SendMessage("CTRL:-PP");
        }
        else
        {
            Toast.makeText(currentContext, "Point Or Plane", Toast.LENGTH_SHORT).show();
            chatActivity.pointsOrPlaneSpawn = !chatActivity.pointsOrPlaneSpawn;
        }
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
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            movedDown = false;
        }
        layoutHolder.setLayoutParams(params);
    }

    public void Close ()
    {
        if(!movedDown)
            return;

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutHolder.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        layoutHolder.setLayoutParams(params);
        movedDown = false;
    }

    public void StartClose()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {


                for(int i = 150; i >= 4; i -= 2)
                {
                    final int count = i;
                    chatActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Log.d(TAG, "Count :" + count);
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Close();
                        }
                    });
                }

            }
        }).start();
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

class CircularImageView extends AppCompatImageButton
{

    public CircularImageView( Context context )
    {
        super( context );
    }

    public CircularImageView( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }

    public CircularImageView( Context context, AttributeSet attrs, int defStyle )
    {
        super( context, attrs, defStyle );
    }

    @Override
    protected void onDraw( @NonNull Canvas canvas )
    {

        Drawable drawable = getDrawable( );

        if ( drawable == null )
        {
            return;
        }

        if ( getWidth( ) == 0 || getHeight( ) == 0 )
        {
            return;
        }
        Bitmap b = ( (BitmapDrawable) drawable ).getBitmap( );
        Bitmap bitmap = b.copy( Bitmap.Config.ARGB_8888, true );

        int w = getWidth( )/*, h = getHeight( )*/;

        Bitmap roundBitmap = getCroppedBitmap( bitmap, w );
        canvas.drawBitmap( roundBitmap, 0, 0, null );

    }

    private static Bitmap getCroppedBitmap( @NonNull Bitmap bmp, int radius )
    {
        Bitmap bitmap;

        if ( bmp.getWidth( ) != radius || bmp.getHeight( ) != radius )
        {
            float smallest = Math.min( bmp.getWidth( ), bmp.getHeight( ) );
            float factor = smallest / radius;
            bitmap = Bitmap.createScaledBitmap( bmp, ( int ) ( bmp.getWidth( ) / factor ), ( int ) ( bmp.getHeight( ) / factor ), false );
        }
        else
        {
            bitmap = bmp;
        }

        Bitmap output = Bitmap.createBitmap( radius, radius,
                Bitmap.Config.ARGB_8888 );
        Canvas canvas = new Canvas( output );

        final Paint paint = new Paint( );
        final Rect rect = new Rect( 0, 0, radius, radius );

        paint.setAntiAlias( true );
        paint.setFilterBitmap( true );
        paint.setDither( true );
        canvas.drawARGB( 0, 0, 0, 0 );
        paint.setColor( Color.parseColor( "#BAB399" ) );
        canvas.drawCircle( radius / 2 + 0.7f,
                radius / 2 + 0.7f, radius / 2 + 0.1f, paint );
        paint.setXfermode( new PorterDuffXfermode( PorterDuff.Mode.SRC_IN ) );
        canvas.drawBitmap( bitmap, rect, rect, paint );

        return output;
    }

}