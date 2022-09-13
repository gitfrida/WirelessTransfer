package com.mce.wirelesstransfer.network;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.ui.MainActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ContentSender {

    private MainActivity activity;
    private Socket socket;
    private static ContentSender instance;

    private ContentSender() {
    }

    public static ContentSender getInstance() {
        if (instance == null)
            instance = new ContentSender();
        return instance;
    }

    public void init(MainActivity activity) {
        this.activity = activity;
    }

    public void openSocket() {


        Thread thread = new Thread(() -> {
            try {
                Log.d(MainActivity.TAG, "Listen to server");
                ServerSocket serverSocket = new ServerSocket(ConnectionManager.PORT);
                Log.d(MainActivity.TAG, "Open Socket Sender");
                socket = serverSocket.accept();

                activity.showMessage(R.string.ready_send);
                activity.setTransferBtnVisible();
                Log.d(MainActivity.TAG, "Sender socket - readu " + socket.isConnected());

            } catch (Exception e) {
                Log.d(MainActivity.TAG, e.toString());
            }
        });
        thread.start();
    }

    public void sendContent(Uri param, String jsonPhoneBook, String host) {

        Log.d(MainActivity.TAG, "JSON TO SEND " + jsonPhoneBook);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    outputStream = socket.getOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                    sendPhonebook(dataOutputStream, jsonPhoneBook);
                    sendFile(param, dataOutputStream, activity);
                    activity.showMessage(R.string.finished);
                    inputStream.close();
                    outputStream.close();
                    Log.d(MainActivity.TAG, "Finished sending phonebook" + jsonPhoneBook);
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, "Sending exception " + e.getMessage());

                }
            }
        });

        thread.start();


    }


    private void sendPhonebook(DataOutputStream dataOutputStream, String jsonPhoneBook) {
        activity.showMessage(R.string.sending_phonebook);
        try {
            byte[] data = jsonPhoneBook.getBytes("UTF-8");
            dataOutputStream.writeInt(data.length);
            dataOutputStream.write(data);
        } catch (Exception e) {

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
        activity.showMessage(R.string.sending_image);
        byte buf[] = new byte[1024];
        int len;
        try {
            ContentResolver contentResolver = activity.getContentResolver();
            long size = getFileSize(param);
            InputStream inputStream = contentResolver.openInputStream(param);
            Log.d(MainActivity.TAG, "sending file size " + size);
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


