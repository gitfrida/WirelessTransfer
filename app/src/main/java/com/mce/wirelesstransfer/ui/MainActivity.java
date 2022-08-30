package com.mce.wirelesstransfer.ui;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.contacts.PhoneBookManager;
import com.mce.wirelesstransfer.network.ConnectionManager;
import com.mce.wirelesstransfer.network.ContentReceiver;
import com.mce.wirelesstransfer.network.ContentSender;
import com.mce.wirelesstransfer.network.WiFiDirectBroadcastReceiver;


public class MainActivity extends Activity {

    public static final String TAG = "FileTransferDebug";
    final int PICKFILE_REQUEST_CODE = 100;
    final int PERMISSIONS_REQUEST_CODE_ALL = 103;
    public ConnectionManager connectionManager;
    private TextView status;
    private View rolesLayout;
    String phoneBook = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up an Intent to send back to apps that request a file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionManager = new ConnectionManager(this);
        connectionManager.setupWifiManager();

        setupUI();

        // If any permission is denied app can't continue
        checkPermissions();



    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED)) {
            requestPermissions(new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.WRITE_CONTACTS

                    },
                    PERMISSIONS_REQUEST_CODE_ALL);
        }
        else
            preparePhoneBook();


    }


    @Override
    protected void onResume() {
        super.onResume();
        connectionManager.register();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        connectionManager.unregister();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) { // Activity.RESULT_OK
                Uri imageUri = data.getData();
                ContentSender.getInstance().sendContent(imageUri,phoneBook,connectionManager.deviceAddress);
                Log.d(TAG, "File is picked");
            }
        }

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED ||
                    grantResults[1] == PackageManager.PERMISSION_DENIED ||
                    grantResults[2] == PackageManager.PERMISSION_DENIED ||
                    grantResults[3] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permissions are missing Bye Bye!", Toast.LENGTH_SHORT);
                finish();

            } else {
                preparePhoneBook();

            }

        }

        return;
    }


    private void setupUI() {

        rolesLayout = findViewById(R.id.rolesLayout);
        status = findViewById(R.id.status);
        findViewById(R.id.senderBtn).setOnClickListener(view -> {
            connectionManager.setSender(true);
            connectionManager.discoverPeers();
            rolesLayout.setVisibility(View.INVISIBLE);

        });

        findViewById(R.id.receiverBtn).setOnClickListener(view -> {
            connectionManager.setSender(false);
            connectionManager.discoverPeers();
            rolesLayout.setVisibility(View.INVISIBLE);

        });

        findViewById(R.id.transferBtn).setOnClickListener(view -> {
            ContentSender.getInstance().init(this);
            view.setVisibility(View.GONE);
            pickFile();
        });


    }

    private void preparePhoneBook() {

       phoneBook = PhoneBookManager.getInstance().getJsonPhoneBook(this);
       findViewById(R.id.progressBar).setVisibility(View.GONE);
       status.setText(R.string.choose_role);
       rolesLayout.setVisibility(View.VISIBLE);
    }


    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");


        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    public void setTransferBtnVisible()
    {
        findViewById(R.id.transferBtn).setVisibility(View.VISIBLE);
    }



    public void sendMessage(int stringId) {
        runOnUiThread(() -> status.setText(stringId));
    }


}
