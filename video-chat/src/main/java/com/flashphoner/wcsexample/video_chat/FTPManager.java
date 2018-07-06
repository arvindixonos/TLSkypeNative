package com.flashphoner.wcsexample.video_chat;
import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

    public class FTPManager extends AsyncTask {

    FTPClient ftpClient = new FTPClient();

    String server = "123.176.34.172";
    int port = 21;
    String user = "maxi";
    String pass = "asdfghjk";

    int uploadORdownload = 1;
    String filePath = "";
    Context applicationContext;

    InputStream fileInputStream = null;

    Handler mHandler = new Handler();

    public  boolean running = false;

    @Override
    protected Object doInBackground(Object[] objects)
    {
        running = true;

        try {
            ftpClient.connect(server, port);
            showServerReply(ftpClient);

            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                Log.d(VideoChatActivity.TAG, "Connect failed");
                return null;
            }

            boolean success = ftpClient.login(user, pass);
            showServerReply(ftpClient);

            if (!success) {
                Log.d(VideoChatActivity.TAG,"Could not login to the server");
                return null;
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            if(uploadORdownload == 1) {

                Log.d(VideoChatActivity.TAG, fileInputStream.available() + " ");

                success = ftpClient.storeFile(GetFileName(filePath), fileInputStream);

                if(success)
                {
                    ToastFunction("Successfully Uploaded File " + GetFileName(filePath));

                    VideoChatActivity.getInstance().SendMessage(":FU" + VideoChatActivity.getInstance().android_id + "-" + GetFileName(filePath));
                }
                else
                {
                    ToastFunction(" Failed to Upload File " + GetFileName(filePath));
                }

                fileInputStream.close();
                showServerReply(ftpClient);
            }
            else {
                 File downloadDir = new File("/sdcard/ReceivedFiles");
                if (!downloadDir.exists())
                {
                    downloadDir.mkdirs();
                }

                File downloadedFile = new File(filePath);
                if (!downloadedFile.exists())
                {
                    downloadedFile.createNewFile();
                }

                FileOutputStream fos = new FileOutputStream(filePath);
                success = ftpClient.retrieveFile(GetFileName(filePath), fos);
                if(success)
                {
                    ToastFunction("Successfully Downloaded File " + GetFileName(filePath));

                    VideoChatActivity.getInstance().OpenFile(filePath);
                }
                else
                {
                    ToastFunction(" Failed to Downloaded File " + GetFileName(filePath));
                }

                fos.close();
                showServerReply(ftpClient);
            }

            ftpClient.logout();
            ftpClient.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }

        running = false;

        return null;
    }

        public void ToastFunction(final String toastMessage)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    try {
                        // Thread.sleep(10000);
                        mHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                VideoChatActivity.ShowToast(toastMessage);
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }).start();
        }

    public  String GetFileName(String filePath)
    {
       filePath = filePath.replace("primary:", "");

       return filePath.substring(filePath.lastIndexOf("/"));
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies)
            {
                Log.d(VideoChatActivity.TAG, "Server " + aReply);
            }
        }
    }
}
