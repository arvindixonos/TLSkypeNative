package com.flashphoner.wcsexample.video_chat;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPManager extends AsyncTask {

    FTPClient ftpClient = new FTPClient();

    String server = "123.176.34.172";
    int port = 21;
    String user = "maxi";
    String pass = "asdfghjk";

    int progress;
    int uploadORdownload = 1;
    String filePath = "";
    String fileName = "";
    Context applicationContext;
    private CopyStreamAdapter streamListener;
    boolean transferSuccess = false;

    InputStream fileInputStream = null;

//    Handler mHandler = new Handler();

    public  boolean running = false;

    public  static  String  TAG = "TLSKYPE";

    @Override
    protected Object doInBackground(Object[] objects)
    {
        running = true;

        try
        {
            ftpClient.connect(server, port);
            showServerReply(ftpClient);

            int replyCode = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode))
            {
                Log.d(TAG, "Connect failed");
                return null;
            }

            boolean success = ftpClient.login(user, pass);
            showServerReply(ftpClient);

            if (!success) {
                Log.d(TAG,"Could not login to the server");
                return null;
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            BufferedInputStream buffIn = null;
            final File file = new File(filePath);

            buffIn = new BufferedInputStream(fileInputStream, (int)file.length());
            ftpClient.enterLocalPassiveMode();

            streamListener = new CopyStreamAdapter()
            {

                @Override
                public void bytesTransferred(long totalBytesTransferred,
                                             int bytesTransferred, long streamSize)
                {
                    progress = (int) (totalBytesTransferred * 100 / file.length());
                    if (totalBytesTransferred == file.length())
                    {
                        removeCopyStreamListener(streamListener);
                    }
                }

            };
            ftpClient.setCopyStreamListener(streamListener);

            if(uploadORdownload == 1)
            {
                Log.d(TAG, fileInputStream.available() + " ");

                success = ftpClient.storeFile(fileName, buffIn);

                if(success)
                {
                    ToastFunction("Successfully Uploaded File " + GetFileName(filePath));
                    transferSuccess = true;
                }
                else
                {
                    ToastFunction(" Failed to Upload File " + GetFileName(filePath));
                }

                fileInputStream.close();
                showServerReply(ftpClient);
            }
            else
            {
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
                    transferSuccess = true;
                    File thisFile = new File(filePath);
                    String pathName = "";
                    if (thisFile.exists())
                    {

                    }
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
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    // TODO Auto-generated method stub
//                    try {
//                        // Thread.sleep(10000);
//                        mHandler.post(new Runnable() {
//
//                            @Override
//                            public void run() {
//                            }
//                        });
//                    } catch (Exception e) {
//                        // TODO: handle exception
//                    }
//                }
//            }).start();
    }

    public String GetFileName(String filePath)
    {
        filePath = filePath.replace("primary:", "");

        return filePath.substring(filePath.lastIndexOf("/"));
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies)
            {
                Log.d(TAG, "Server " + aReply);
            }
        }
    }
}
