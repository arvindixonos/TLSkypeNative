package com.takeleap.tasqar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
    public String emailID;
    public String participantName;
    public String userRole;

    public CircularImageView profilePic;
    public TextView nameField;
    public TextView dateField;
    public TextView durationField;
    public TextView roleField;
    public FloatingActionButton callButton;

    private Context currentContext;
    private Handler mHandler = new Handler();
    private boolean profilePicPresent;

    public HistoryButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        currentContext = context;
    }

    public void Initialise (String ID, String name, String date, String duration, String role, String email_ID)
    {
        userID = ID;
        emailID = email_ID;
        participantName = name;
        userRole = role;

        nameField = (TextView) this.getChildAt(1);
        dateField = (TextView) this.getChildAt(2);
        durationField = (TextView) this.getChildAt(3);
        roleField = (TextView) this.getChildAt(4);
        callButton = (FloatingActionButton) this.getChildAt(5);
        profilePic = (CircularImageView) getChildAt(6);

        roleField.setText(role);
        nameField.setText(name);
        durationField.setText(duration);
        dateField.setText(date);

//        if(!new File(LoginUIHandler.filePath + "/" + userID + "_PIC.jpg").exists())
//            DownloadFile(userID + "_PIC.jpg");
//        else {
//            Log.d(TAG, "PIC There");
//            AssignProfilePIC();
//        }

        callButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AppManager.getInstance().ChangeActivity(email_ID, true);
                SetData();
            }
        });
    }

    public void SetData ()
    {
        CallHistoryDatabaseHelper callHistoryDatabaseHelper = new CallHistoryDatabaseHelper(currentContext);
        callHistoryDatabaseHelper.addData(userID, emailID, participantName, userRole, GetDate(), "01:36");
    }

    public String GetDate()
    {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-YYYY_hh-mm-ss");

        return dateFormat.format(date);
    }
    private void DownloadFile (final String fileName)
    {
        final FTPManager ftpManager = new FTPManager();
        ftpManager.uploadORdownload = 0;
        ftpManager.applicationContext = currentContext.getApplicationContext();
        ftpManager.filePath = Environment.getExternalStorageDirectory().getPath() + "/TASQAR/ReceivedFiles/UserData/";
        ftpManager.fileName = fileName;

        new Thread(new Runnable()
        {
            @Override
            public void run() {

                Looper looper = mHandler.getLooper();
                looper.prepare();
                mHandler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        ftpManager.execute();
                        while (!ftpManager.transferSuccess && !ftpManager.serverReply.contains("550"))
                        {
                            try
                            {
                                Thread.sleep(10);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                        if(ftpManager.serverReply.contains("550"))
                        {
                            Log.d(TAG, "No Image");
                            profilePicPresent = false;
                            String fileName = userID + "_PIC.jpg";
                            File profilePic = new File(LoginUIHandler.filePath + "/" + fileName);
                            if(profilePic.exists())
                            {
                                profilePic.delete();
                            }
                            return;
                        }
                        else
                        {
                            profilePicPresent = true;
                        }
                        Log.d(TAG, "Calling Assign Profile PIC");
                        ((Activity)currentContext).runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Log.d(TAG, "Calling Assign Profile PIC");
                                AssignProfilePIC();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void AssignProfilePIC ()
    {
        Log.d(TAG, "Assign Profile PIC");
        if(profilePicPresent)
        {
            Log.d(TAG, "Assign Profile PIC 2");
            profilePic.setImageBitmap(GetUserProfilePhoto(userID));
        }
    }

    private Bitmap GetUserProfilePhoto (String userID)
    {
        String imageName = userID + "_PIC.jpg";

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Log.d(TAG, LoginUIHandler.filePath + " : " + imageName);
        return BitmapFactory.decodeFile(LoginUIHandler.filePath + "/" + imageName, options);
    }
}