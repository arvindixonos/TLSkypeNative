package com.flashphoner.wcsexample.video_chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
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

import org.webrtc.DataChannel;

import static com.flashphoner.wcsexample.video_chat.VideoChatActivity.TAG;

public class CustomImageButton extends AppCompatImageButton implements View.OnTouchListener
{
    private static String TAG = "UI_TEST";
    private Context currentContext;
    private AppManager manager;
    @SuppressLint("ClickableViewAccessibility")
    public CustomImageButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        currentContext = context;
        manager = AppManager.getInstance();

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
        String name = currentContext.getResources().getResourceName(v.getId());
        name = name.replace("com.flashphoner.wcsexample.video_chat:id/imgBtn", "");
        CheckView(name);
    }

    public void CheckView (String ViewName)
    {
        manager.ClickFunction(ViewName.charAt(0));
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

class HistoryButton extends ConstraintLayout
{
    private static String TAG = "UI_TEST";

    public String userID;

    public TextView nameField;
    public TextView dateField;
    public TextView durationField;
    public TextView roleField;
    public FloatingActionButton callButton;

    public HistoryButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void Initialise (String ID, String name, String date, String duration, String role)
    {
        userID = ID;

        nameField = (TextView) this.getChildAt(1);
        dateField = (TextView) this.getChildAt(2);
        durationField = (TextView) this.getChildAt(3);
        roleField = (TextView) this.getChildAt(4);
        callButton = (FloatingActionButton) this.getChildAt(5);

        roleField.setText(role);
        nameField.setText(name);
        durationField.setText(duration);
        dateField.setText(date);

        callButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AppManager.Instance.ChangeActivity(userID);
            }
        });
    }
}