package com.mce.wirelesstransfer.network;


import android.util.Log;
import com.google.gson.Gson;
import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.contacts.PhoneBook;
import com.mce.wirelesstransfer.contacts.PhoneBookManager;
import com.mce.wirelesstransfer.ui.MainActivity;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public  class ContentReceiver {

    String host;
    MainActivity activity;

    /**
     * creates a new receiver with host address to connect to and activity for updates
     * @param host
     * @param activity
     */
    public ContentReceiver(String host, MainActivity activity)
    {
        this.activity = activity;
        this.host = host;
    }

    /**
     * Opens socket with the connected device's address and waits for content
     */
    public void openSocket() {
        Thread thread = new Thread(() -> {
            try{
                Socket socket = new Socket();
                Log.d(MainActivity.TAG, "Connecting to Server socket Receiver- ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, ConnectionManager.PORT)), 1000000);
                Log.d(MainActivity.TAG, "Connected - Receiver");
                activity.showMessage(R.string.ready);
                InputStream inputstream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputstream);
                activity.showMessage(R.string.wait_content);
                receivePhoneBook(dataInputStream);
                activity.showMessage(R.string.receive_image);
                receiveFile(dataInputStream);
                dataInputStream.close();
                inputstream.close();
                socket.close();
            } catch (FileNotFoundException e) {
                Log.d(MainActivity.TAG, "Exception1 " + e.getMessage());

            } catch (IOException e) {
                Log.d(MainActivity.TAG, "Exception2 " + e.getMessage());
            }
        });

        thread.start();
    }

    /**
     * Receives phonebook json info from inputstream and saves it
     * @param dataInputStream
     */
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
            activity.showMessage(R.string.phonebook_saved);
        }
        catch (Exception e)
        {
            Log.d(MainActivity.TAG, "Exception receive phonebook: " + e.getMessage());
        }
    }

    /**
     * Receives a file from inputStream and saves it
     * @param dataInputStream
     */
    private void receiveFile(DataInputStream dataInputStream) {
        int len;
        byte buf[] = new byte[1024];
        File file = new File(getFilePath());
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            long size = dataInputStream.readLong();
            Log.d(MainActivity.TAG, "File size to receive is " + size);
            while ((len = dataInputStream.read(buf)) != -1 && size>0) {
                Log.d(MainActivity.TAG, "Receiving file parts "+size);
                size -= len;
                outputStream.write(buf, 0, len);
                if(size<=0)
                    activity.showMessage(R.string.finished);
            }
            outputStream.close();
        }
        catch (IOException e) {
            Log.d(MainActivity.TAG, "Exception receive file: " + e.getMessage());
        }
    }

    /**
     * Create a new name for the received image, add it to the path and return it.
     * @return
     */
    private String getFilePath() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = formatter.format(now) + ".jpg";
        return "/storage/emulated/0/Pictures/"+fileName;
    }


}
