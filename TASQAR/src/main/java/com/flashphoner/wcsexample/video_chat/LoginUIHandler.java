package com.flashphoner.wcsexample.video_chat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class LoginUIHandler
{
    private Activity currentActivity;
    private static String TAG = "TLSKYPE";
    private AppManager  manager;

    //SI UI Elements
    private EditText SI_PasswordField;
    private EditText    SI_UserIDField;

    private Button SI_SubmitButton;
    private Button      SI_SignUpButton;
    //SI UI Elements

    //SU UI Elements
    private EditText    SU_NameField;
    private EditText    SU_EmailField;
    private EditText    SU_PhoneField;
    private EditText    SU_RoleField;
    private EditText    SU_HeirarchyField;
    private EditText    SU_OTPField;
    private EditText    SU_PasswordText;
    private EditText    SU_ReEnterText;

    private Button      SU_NextButton;
    private Button      SU_SubmitButton;
    private Button      SU_SubmitPasswordButton;

    private ImageButton SU_UserImageButton;

    private ImageView SU_NameFieldImage;
    private ImageView   SU_EmailFieldImage;
    private ImageView   SU_PhoneFieldImage;
    private ImageView   SU_ReEnterErrorImage;
    private ImageView   SU_PasswordErrorImage;
    private ImageView   SU_OTPErrorImage;

    //SU UI Elements

    //SU Variables
    private boolean isNamePresent;
    private boolean isEmailPresent;
    private boolean isPhonePresent;
    private boolean isPasswordPresent;
    private boolean isReEnterPresent;
    private boolean isOTPPresent;
    //SU Variables

    //All Screens
    private ConstraintLayout mLoginScreen;
    private ConstraintLayout    mSignUpScreen;
    private ConstraintLayout    mDetailsScreen;
    private ConstraintLayout    mPasswordScreen;
    //All Screens

    private Thread  emailIDCheckThread = null;

    private Thread loginThread = null;
    public int lastFetchedUserID = -1;
    public String photoftpLink = "";
    private Handler mHandler = new Handler();

    public LoginUIHandler (Activity appContext, AppManager managerClass)
    {
        currentActivity = appContext;
        manager = managerClass;
        getNextUserId();

        //Screens
        mLoginScreen = currentActivity.findViewById(R.id.SignInScreen);
        mSignUpScreen = currentActivity.findViewById(R.id.SignUpScreen);
        mDetailsScreen = currentActivity.findViewById(R.id.SU_DetailsScreen);
        mPasswordScreen = currentActivity.findViewById(R.id.SU_PasswordScreen);
        //Screens

        //SI Screen
        SI_PasswordField = currentActivity.findViewById(R.id.SI_PasswordField);
        SI_UserIDField = currentActivity.findViewById(R.id.SI_UserIDField);

        SI_SubmitButton = currentActivity.findViewById(R.id.SI_SignInButton);
        SI_SignUpButton = currentActivity.findViewById(R.id.SI_SignUpButton);

        //SU Screen
        SU_NameField = currentActivity.findViewById(R.id.SU_NameField);
        SU_EmailField = currentActivity.findViewById(R.id.SU_EmailField);
        SU_PhoneField = currentActivity.findViewById(R.id.SU_PhoneNoField);
        SU_RoleField = currentActivity.findViewById(R.id.SU_RoleField);
        SU_HeirarchyField = currentActivity.findViewById(R.id.SU_HeirarchyField);
        SU_OTPField = currentActivity.findViewById(R.id.SU_OTPField);
        SU_PasswordText = currentActivity.findViewById(R.id.SU_PasswordField);
        SU_ReEnterText = currentActivity.findViewById(R.id.SU_ReEnterField);

        SU_UserImageButton = currentActivity.findViewById(R.id.SU_ProfilePic);

        SU_NextButton = currentActivity.findViewById(R.id.SU_NextButton);

        SU_NameFieldImage = currentActivity.findViewById(R.id.SU_NameErrorImage);
        SU_EmailFieldImage = currentActivity.findViewById(R.id.SU_EmailErrorImage);
        SU_PhoneFieldImage = currentActivity.findViewById(R.id.SU_PhoneErrorImage);
        //SU Screen

        if(SI_SignUpButton != null)
        {
            Log.d(TAG, "TAG");
//            Log.d(TAG, SI_SignUpButton.getTransitionName());
        }

        if(SU_SubmitPasswordButton != null)
        {
            Log.d(TAG, "Works");
        }

        SI_SignUpButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mLoginScreen.setVisibility(View.GONE);
                mSignUpScreen.setVisibility(View.VISIBLE);
                mDetailsScreen.setVisibility(View.VISIBLE);
                mPasswordScreen.setVisibility(View.GONE);
            }
        });

        SI_SubmitButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                String userID = SI_UserIDField.getText().toString();
                String password = SI_PasswordField.getText().toString();

                Login(userID, password);

//            Login("arvindsemail@gmail.com", "asaadfghjk");
            }
        });

        Log.d(TAG, "TAG1");
        SU_NextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "TAG2");
                OnSubmitClicked();
            }
        });

        SU_NameField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_NameFieldImage.setVisibility(View.INVISIBLE);
                    }
                });

                isNamePresent = !(SU_NameField.getText().toString().length() == 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_EmailField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                SU_EmailFieldImage.setVisibility(View.INVISIBLE);
                String email = SU_EmailField.getText().toString();

                if (isValidEmail(email))
                {
                    isEmailPresent(email);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        SU_PhoneField.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_PhoneFieldImage.setVisibility(View.INVISIBLE);
                    }
                });

                isPhonePresent = !(SU_PhoneField.getText().toString().length() == 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_UserImageButton = currentActivity.findViewById(R.id.SU_ProfilePic);
        SU_UserImageButton.setOnClickListener(v -> new ChooserDialog(currentActivity)
                .withStartFile(null)
//                        .withResources(R.string.title_choose_any_file, R.string.title_choose, R.string.dialog_cancel)
//                        .withFileIconsRes(false, R.mipmap.ic_my_file, R.mipmap.ic_my_folder)
                .withAdapterSetter(adapter ->
                {
                    //
                })
                .withChosenListener((path, pathFile) ->
                {

                    if(!path.contains(".jpg"))
                        return;

                    Log.d(TAG, path);

                    try
                    {
                        FileInputStream fileReadStream = new FileInputStream(pathFile);

                        Bitmap bitmap = BitmapFactory.decodeFile(path);

                        SU_UserImageButton.setImageBitmap(bitmap);

                        UploadFile(path, fileReadStream);
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (RuntimeException e)
                    {
                        Log.d(TAG, e.getMessage());
                    }
                })
                .build()
                .show());
    }

    public void UploadFile(final String filePath, final InputStream inputStream)
    {

        final FTPManager ftpManager = new FTPManager();
        ftpManager.fileInputStream = inputStream;
        ftpManager.uploadORdownload = 1;
        ftpManager.applicationContext = currentActivity.getApplicationContext();
        ftpManager.filePath = filePath;
        ftpManager.fileName = lastFetchedUserID + "_PIC" + ".jpg";
        if (ftpManager.running)
        {
            Log.d(TAG, "FTP Manager Running Already");
            return;
        }
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
                        while (!ftpManager.transferSuccess)
                        {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        photoftpLink = lastFetchedUserID + "_PIC.jpg";

                        Log.d(TAG, "Uploaded File");
                    }
                });
            }
        }).start();
    }

    public void isEmailPresent(final String emailId)
    {

        if(emailIDCheckThread != null && emailIDCheckThread.isAlive())
        {
            emailIDCheckThread.interrupt();
            emailIDCheckThread = null;
        }

        emailIDCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/IsEmailIDPresent?emailID="+emailId);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        isEmailPresent = !(Integer.parseInt(value) == 1);

                        currentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(!isEmailPresent)
                                    SU_EmailFieldImage.setVisibility(View.VISIBLE);
                                else
                                    SU_EmailFieldImage.setVisibility(View.INVISIBLE);
                            }
                        });

                        break;
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        emailIDCheckThread.start();
    }

    Thread submitNewUserThread = null;
    public void SubmitNewUser(final int userID, final  String photoftpLink, final String name, final String emailId, final String phoneNumber, final String role, final String hierarchy)
    {
        if(submitNewUserThread != null && submitNewUserThread.isAlive())
        {
            submitNewUserThread.interrupt();
            submitNewUserThread = null;
        }
        Log.d(TAG, "Submit New User");

        submitNewUserThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/SubmitUserInfo?userID=" + userID + "&" + "name=" + name
                            + "&" + "emailID=" + emailId + "&" + "phone=" + phoneNumber + "&" + "role=" + role + "&" + "photoURL=" + photoftpLink + "&" + "hierarchy=" + hierarchy);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        boolean submitted = Integer.parseInt(value) == 1 ? true : false;

                        if(submitted)
                        {
                            currentActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    OpenOTPPasswordWindow();
                                }
                            });
                        }

                        break;
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        submitNewUserThread.start();
    }

    Thread submitNextThread = null;
    public void SubmitNext(final int userID, final String otp, final String password)
    {

        if(submitNextThread != null && submitNextThread.isAlive())
        {
            submitNextThread.interrupt();
            submitNextThread = null;
        }

        Log.d(TAG, "Submit Next");

        submitNextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/UpdatePassword?userID=" + userID + "&" + "otp=" + otp
                            + "&" + "password=" + password);

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        boolean submitted = Integer.parseInt(value) == 1 ? true : false;
                        Log.d(TAG, submitted ? "Submitted" : "Not Submitted");
                        if(submitted)
                        {
                                currentActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPasswordScreen.setVisibility(View.GONE);
                                        mDetailsScreen.setVisibility(View.VISIBLE);
                                        mSignUpScreen.setVisibility(View.GONE);
                                        mLoginScreen.setVisibility(View.VISIBLE);
                                    }
                                });
                        }
                        break;
                    }
                }
                catch (IOException | ParserConfigurationException | SAXException e)
                {
                    e.printStackTrace();
                }
            }
        });

        submitNextThread.start();
    }

    public void getNextUserId()
    {
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/GetNextUserID");

                    URLConnection con = (URLConnection) url.openConnection();
                    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    Document doc = docBuilder.parse(con.getInputStream());

                    Node userIDNode = doc.getElementsByTagName("int").item(0);
                    NodeList list = userIDNode.getChildNodes();

                    for (int i = 0; i < list.getLength(); i++)
                    {
                        Node node = list.item(i);
                        String value = node.getNodeValue();

                        lastFetchedUserID = Integer.parseInt(value);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }
            }
        });

        networkThread.start();
    }

    public void OnSubmitClicked()
    {
        Log.d(TAG, "Working");
        String name = SU_NameField.getText().toString();
        String phone = SU_PhoneField.getText().toString();
        String email = SU_EmailField.getText().toString();
        String role = SU_RoleField.getText().toString();
        String hierarchy = SU_HeirarchyField.getText().toString();

        if (isNamePresent && isPhonePresent && isEmailPresent)
        {
            if (lastFetchedUserID != -1)
            {
                Log.d(TAG, "OnSubmitClicked");
                SubmitNewUser(lastFetchedUserID, photoftpLink, name, email, phone , role, hierarchy);

//                if (result) {
//                    Log.d(TAG, "ALL FINE BRO 1");
//
//                    OpenOTPPasswordWindow();
//                } else {
//                    Log.d(TAG, "NOT ALL FIND 1");
//                }
            }
        }
    }

    public void OnSubmitPasswordClicked()
    {
        String otp = SU_OTPField.getText().toString();
        String password = SU_PasswordText.getText().toString();
        String passwordText = SU_PasswordText.getText().toString();
        String reEnterText = SU_ReEnterText.getText().toString();


        Log.d(TAG, "OnSubmitPasswordClicked");

        if(isOTPPresent && isPasswordPresent && isReEnterPresent)
        {
            if(!passwordText.equals(reEnterText))
            {
                Toast.makeText(currentActivity, "Password Mismatch", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Password Text : " + SU_PasswordText.getText() + " ReEnter Text : " + SU_ReEnterText.getText());
            }
            else
            {
                SubmitNext(lastFetchedUserID, otp, password);
            }
        }
    }

    void OpenOTPPasswordWindow()
    {
        mDetailsScreen.setVisibility(View.GONE);
        mPasswordScreen.setVisibility(View.VISIBLE);

        Log.d(TAG, "OpenOTPPasswordWindow");

        SU_PasswordErrorImage = currentActivity.findViewById(R.id.SU_PasswordError);
        SU_PasswordText = currentActivity.findViewById(R.id.SU_PasswordField);
        SU_PasswordText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                isPasswordPresent = isValidPassword(SU_PasswordText.getText().toString());

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_PasswordErrorImage.setVisibility(isPasswordPresent ? View.INVISIBLE : View.VISIBLE);
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_ReEnterErrorImage = currentActivity.findViewById(R.id.SU_ReEnterError);
        SU_ReEnterText = currentActivity.findViewById(R.id.SU_ReEnterField);
        SU_ReEnterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_ReEnterErrorImage.setVisibility(View.INVISIBLE);

                    }
                });

                String password = SU_PasswordText.getText().toString();

                isPasswordPresent = isValidPassword(password);

                if (isPasswordPresent) {
                    String reenter = SU_ReEnterText.getText().toString();

                    isReEnterPresent = reenter.equals(password);

                    currentActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SU_ReEnterErrorImage.setVisibility(isReEnterPresent ? View.INVISIBLE : View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_OTPErrorImage = currentActivity.findViewById(R.id.SU_OTPError);
        SU_OTPField = currentActivity.findViewById(R.id.SU_OTPField);
        SU_OTPField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_OTPErrorImage.setVisibility(View.INVISIBLE);
                    }
                });

                isOTPPresent = (s.toString().length() > 0);

                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SU_OTPErrorImage.setVisibility(isOTPPresent ? View.INVISIBLE : View.VISIBLE);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        SU_SubmitPasswordButton = currentActivity.findViewById(R.id.SU_SubmitPasswordButton);
        SU_SubmitPasswordButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                OnSubmitPasswordClicked();
            }
        });

        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SU_PasswordErrorImage.setVisibility(View.INVISIBLE);
                SU_OTPErrorImage.setVisibility(View.INVISIBLE);
                SU_ReEnterErrorImage.setVisibility(View.INVISIBLE);
            }
        });
    }

    boolean isValidEmail(String email)
    {

        Log.d(TAG, "IS CVALUD");

        if (email.length() > 5)
        {
            if (email.contains("@") && email.contains("."))
            {
                return true;
            }
        }

        return false;
    }

    boolean isValidPassword(String password)
    {
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.{8,})(?=.*[!@#$%^&*])";//"^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\$%\^&\*])(?=.{8,})"

        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(password);

        if (m.find()) {
            return true;
        }

        return false;
    }

    public  void Login(final String userID, final String Password)
    {
        if (loginThread != null && loginThread.isAlive())
        {
            loginThread.interrupt();
            loginThread = null;
        }

        loginThread = new Thread(() ->
        {
            URL url = null;
            try {
                url = new URL("http://13.127.231.176/Tasqar/TasqarWebService.asmx/Login?emailID=" + userID + "&" + "Password=" + Password);
                URLConnection con = url.openConnection();
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(con.getInputStream());

                Node userIDNode = doc.getElementsByTagName("int").item(0);
                NodeList list = userIDNode.getChildNodes();

                for (int i = 0; i < list.getLength(); i++)
                {
                    Node node = list.item(i);
                    String value = node.getNodeValue();

                    boolean submitted = Integer.parseInt(value) == 1;

                    if (submitted)
                        currentActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                manager.SetupUserScreen();
                            }
                        });
                    else
                        Log.e(TAG, "NotDone");

                    break;
                }

            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
            }
        });

        loginThread.start();
    }
}
