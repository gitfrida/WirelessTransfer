package com.mce.wirelesstransfer.network;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.PrintStreamPrinter;

import com.google.gson.Gson;
import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.contacts.PhoneBook;
import com.mce.wirelesstransfer.contacts.PhoneBookManager;
import com.mce.wirelesstransfer.ui.MainActivity;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public  class ContentReceiver {

    String host;
    int port;
    MainActivity activity;

    public ContentReceiver(String host, MainActivity activity)
    {
        this.activity = activity;
        this.host = host;
    }


    public void openSocket() {

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {


                try{

                    Socket socket = new Socket();
                    Log.d(MainActivity.TAG, "Connecting to Server socket Receiver- ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, ConnectionManager.PORT)), 1000000);
                    Log.d(MainActivity.TAG, "Connected - Receiver");

                    InputStream inputstream = socket.getInputStream();

                    DataInputStream dataInputStream = new DataInputStream(inputstream);

                    activity.sendMessage(R.string.wait_content);

                    receivePhoneBook(dataInputStream);
                    activity.sendMessage(R.string.receive_image);

                    receiveFile(dataInputStream);

                    dataInputStream.close();
                    inputstream.close();
                    socket.close();
                } catch (FileNotFoundException e) {
                    Log.d(MainActivity.TAG, "Exception1 " + e.getMessage());

                } catch (IOException e) {
                    Log.d(MainActivity.TAG, "Exception2 " + e.getMessage());
                }

            }
        });

        thread.start();


    }

    private void receivePhoneBook(DataInputStream dataInputStream) {

        try {
            Log.d(MainActivity.TAG, "receivePhoneBook" );

            int length=dataInputStream.readInt();
            Log.d(MainActivity.TAG, "Read json "+length);

            byte[] data=new byte[length];
            dataInputStream.readFully(data);
            String inputStr=new String(data,"UTF-8");
            Log.d(MainActivity.TAG, "Read json done "+inputStr);
            PhoneBookManager.getInstance().writePhoneBook(activity,new Gson().fromJson(inputStr, PhoneBook.class));
            activity.sendMessage(R.string.phonebook_saved);
        }catch (Exception e)
        {
            Log.d(MainActivity.TAG, "Exception receive phonebook: " + e.getMessage());

        }
    }

    private void receiveFile(DataInputStream dataInputStream) {

        int len;
        byte buf[] = new byte[1024];
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = formatter.format(now) + ".jpg";
        File file = new File("/storage/emulated/0/Pictures/"+fileName);

        try {
            FileOutputStream outputStream = new FileOutputStream(file);

            long size = dataInputStream.readLong();
            Log.d(MainActivity.TAG, "File size to receive is " + size);

            while ((len = dataInputStream.read(buf)) != -1 && size>0) {
                Log.d(MainActivity.TAG, "Receiving file parts "+size);
                size -= len;
                outputStream.write(buf, 0, len);
                if(size<=0)
                    activity.sendMessage(R.string.finished);

            }

            outputStream.close();
        }
        catch (IOException e) {
            Log.d(MainActivity.TAG, "Exception receive file: " + e.getMessage());

        }
    }


}
