package com.mce.wirelesstransfer.network;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;

import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.ui.MainActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ContentSender {

    private MainActivity activity;
    private Socket socket;
    private static ContentSender instance;
    private ContentSender() {
    }

    public static ContentSender getInstance()
    {
        if(instance==null)
            instance = new ContentSender();
        return instance;
    }

    public void init(MainActivity activity)
    {
        this.activity = activity;
    }

    public void openSocket(String host)
    {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    Log.d(MainActivity.TAG, "Listen to server");
               //     socket = new Socket();
               //     socket.bind(null);
                //    socket.connect((new InetSocketAddress(host, 8888)), 1000000);

                    ServerSocket serverSocket = new ServerSocket(ConnectionManager.PORT);
                     Log.d(MainActivity.TAG, "Open Socket Sender");
                      socket = serverSocket.accept();

                    Log.d(MainActivity.TAG, "Sender socket - readu " + socket.isConnected());

                } catch (Exception e) {
                    Log.d(MainActivity.TAG, e.toString());
                }
            }
        });
        thread.start();
    }

    public void sendContent(Uri param, String jsonPhoneBook,String host) {

        Log.d(MainActivity.TAG,"JSON TO SEND "+ jsonPhoneBook);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {

                    outputStream = socket.getOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    outputStream = socket.getOutputStream();
                    dataOutputStream = new DataOutputStream(outputStream);

                    sendPhonebook(dataOutputStream,jsonPhoneBook);
                    sendFile(param, dataOutputStream,activity);

                    activity.sendMessage(R.string.finished);

                    inputStream.close();
                    outputStream.close();
                  //  socket.close();
                    Log.d(MainActivity.TAG,"Finished sending phonebook" +jsonPhoneBook);


                }
                catch (Exception e)
                {
                    Log.d(MainActivity.TAG,"Sending exception " + e.getMessage());

                }

            }
        });

        thread.start();


    }


    private void sendPhonebook(DataOutputStream dataOutputStream, String jsonPhoneBook) {
        activity.sendMessage(R.string.sending_phonebook);
        try {

            byte[] data = jsonPhoneBook.getBytes("UTF-8");
            dataOutputStream.writeInt(data.length);
            dataOutputStream.write(data);
        }
        catch (Exception e)
        {

        }
    }

    private long getFileSize(Uri param) {
        Cursor returnCursor =
                activity.getContentResolver().query(param, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long size = returnCursor.getLong(sizeIndex);
        return size;
    }


    private boolean sendFile(Uri param, DataOutputStream dataOutputStream, MainActivity activity) {
        activity.sendMessage(R.string.sending_image);

        byte buf[] = new byte[1024];
        int len;
        try {
            ContentResolver contentResolver = activity.getContentResolver();
            long size = getFileSize(param);
            InputStream inputStream = contentResolver.openInputStream(param);

            Log.d(MainActivity.TAG, "sending file size "+size);
            dataOutputStream.writeLong(size);
            while ((len = inputStream.read(buf)) != -1) {
                dataOutputStream.write(buf, 0, len);

            }
            inputStream.close();

        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }


}


