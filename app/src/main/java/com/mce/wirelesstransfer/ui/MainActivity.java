package com.mce.wirelesstransfer.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.contacts.PhoneBookManager;
import com.mce.wirelesstransfer.network.ConnectionManager;
import com.mce.wirelesstransfer.network.ContentSender;
import java.util.ArrayList;

public class MainActivity extends Activity {

    public static final String TAG = "FileTransferDebug";
    final int PICKFILE_REQUEST_CODE = 100;
    final int PERMISSIONS_REQUEST_CODE_ALL = 103;
    public ConnectionManager connectionManager;
    private TextView status;
    private View rolesLayout;
    private String phoneBook = "";
    private RecyclerView listView;
    private DeviceListAdapter deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectionManager = new ConnectionManager(this);
        connectionManager.setupWifiManager();
        setupUI();
        checkPermissions();
    }

    /**
     * Check for permissions and if any is not granted request it.
     * If all is granted prepare phonebook info
     */
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

    @Override
    protected void onPause() {
        super.onPause();
        connectionManager.unregister();
    }

    /**
     * A callback when file has been picked. Start sending content if all is okay
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKFILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                ContentSender.getInstance().sendContent(imageUri,phoneBook,connectionManager.getDeviceAddress());
                Log.d(TAG, "File is picked");
            }
        }
    }


    /**
     * Receives permissions results. If any permission is denied app can't continue
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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

            } else
                preparePhoneBook();
        }
        return;
    }


    /**
     * Prepares UI components
     */
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

        // Setup nearby devices list
        listView = findViewById(R.id.devicesList);
        listView.setLayoutManager(new LinearLayoutManager(this));
        deviceListAdapter = new DeviceListAdapter(this);
        listView.setAdapter(deviceListAdapter);
    }

    /**
     * Calls PhoneBookManager to get the entire phonebook as a json string to be
     * sent later on
     */
    private void preparePhoneBook() {
       phoneBook = PhoneBookManager.getInstance().getJsonPhoneBook(this);
       findViewById(R.id.progressBar).setVisibility(View.GONE);
       status.setText(R.string.choose_role);
       rolesLayout.setVisibility(View.VISIBLE);
    }


    /**
     * Starts a new intent to select an image.
     */
    public void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
    }

    /**
     * Enable Transfer button by showing it
     */
    public void setTransferBtnVisible()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.transferBtn).setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Show message on the screen to update user with the app's progress
     * @param stringId
     */
    public void showMessage(int stringId) {
        runOnUiThread(() -> status.setText(stringId));
    }

    /**
     * User has selected a device to connect to, remove peers list and request connection
     * @param wifiP2pDevice
     */
    public void deviceSelected(WifiP2pDevice wifiP2pDevice)
    {
        connectionManager.connectToDevice(wifiP2pDevice);
        listView.setVisibility(View.GONE);
    }

    /**
     * Show a list with peers info
     * @param devices
     */
   public void showDevicesList(ArrayList<WifiP2pDevice> devices)
   {
       listView.setVisibility(View.VISIBLE);
       deviceListAdapter.setDevices(devices);
   }
}
