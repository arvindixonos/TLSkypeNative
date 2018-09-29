package com.takeleap.tasqar;

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
import java.io.OutputStream;

public class FTPManager extends AsyncTask {

    FTPClient ftpClient = new FTPClient();

    String server = "13.127.231.176";
    int port = 21;
    String user = "anonymous";
    String pass = "";

    int progress;
    int uploadORdownload = 1;
    String filePath = "";
    String fileName = "";
    Context applicationContext;
    private CopyStreamAdapter streamListener;
    boolean transferSuccess = false;
    static String serverReply = "";

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

            if(uploadORdownload == 1)
            {
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
                Log.d(TAG, fileInputStream.available() + " ");

                success = ftpClient.storeFile(fileName, buffIn);

                if(success)
                {
                    transferSuccess = true;
                }

                fileInputStream.close();
                showServerReply(ftpClient);
            }
            else
            {
                File downloadDir = new File(filePath);
                if (!downloadDir.exists())
                {
                    downloadDir.mkdirs();
                }

                File downloadedFile = new File(filePath + "/" + fileName);
                if (!downloadedFile.exists())
                {
                    downloadedFile.createNewFile();
                }

                FileOutputStream fos = new FileOutputStream(filePath + "/" + fileName);
                success = ftpClient.retrieveFile(fileName, fos);
                if(success)
                {
                    transferSuccess = true;
                    File thisFile = new File(filePath);
                    String pathName = "";
                    if (thisFile.exists())
                    {

                    }
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
                serverReply = aReply;
                Log.d(TAG, "Server " + aReply);
            }
        }
    }
}
