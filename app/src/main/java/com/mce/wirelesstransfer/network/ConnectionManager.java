package com.mce.wirelesstransfer.network;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.ui.MainActivity;
import java.util.ArrayList;

public class ConnectionManager {
    public static final int PORT = 8882;
    private MainActivity mainActivity;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private String deviceAddress = null;
    private boolean isConnected = false;
    private boolean isInfoReady = false;
    private boolean isSender;
    public WifiP2pManager.PeerListListener peerListListener;
    public WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    /**
     * public constructor to create ConnecitonManager
     * @param mainActivity
     */
    public ConnectionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        setupPeersListener();
        setupConnectionListener();
    }

    /**
     * Sets up peers listener and holds the callback when peers are ready to show
     */
    private void setupPeersListener()
    {
        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                mainActivity.showMessage(R.string.found_nearby);
                if (!isConnected) {
                    Log.d(MainActivity.TAG, "onPeersAvailable: found peers " +wifiP2pDeviceList.getDeviceList().size());
                    mainActivity.showDevicesList(new ArrayList<>(wifiP2pDeviceList.getDeviceList()));
                }
            }
        };
    }

    /**
     * Sets up Connection Listener and holds the callback when connection info is ready
     */
    private void setupConnectionListener()
    {
        connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                {
                    Log.d(MainActivity.TAG, "onConnectionInfoAvailable formed: " + wifiP2pInfo.groupFormed + "  owner: " + wifiP2pInfo.isGroupOwner +
                            "  address: " + wifiP2pInfo.groupOwnerAddress + " isSender: " + isSender + " isConnected: " + isConnected);
                    if (!isSender) {
                        if (!isInfoReady && wifiP2pInfo.groupFormed) {
                            connectionReady(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                            isInfoReady = true;
                        }
                    } else {
                        if (wifiP2pInfo.groupOwnerAddress != null && !isInfoReady && wifiP2pInfo.groupFormed) {
                            connectionReady(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                            isInfoReady = true;
                        }
                    }
                }
            }
        };

    }


    /**
     * Initiates connect attempt to a selected device
     * @param device  selected device to connect to
     */
    public void connectToDevice(WifiP2pDevice device) {

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (!isConnected) {
            Log.d(MainActivity.TAG, "Try to connect to " + device.deviceName);
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            isConnected = true;
                            Log.d(MainActivity.TAG, "Connect Succeeded" + device.deviceName);
                            mainActivity.showMessage(R.string.connected_wait);
                            manager.requestConnectionInfo(channel, connectionInfoListener);
                        }

                        @Override
                        public void onFailure(int i) {
                            mainActivity.showMessage(R.string.connection_failed);
                        }
                    });
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, "Connect exception " + e.getMessage());
                }
            }
        }

    }


    /**
     * Discovers nearby peers
     */
    public void discoverPeers() {
        if (!isConnected) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(MainActivity.TAG, "Discover Succeeded");
                        mainActivity.showMessage(R.string.discover_succeeded);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(MainActivity.TAG, "Discover Failed");
                        mainActivity.showMessage(R.string.discover_failed);
                        discoverPeers();
                    }
                });
            }

        }

    }

    /**
     * Sets up wifi P2P communication managr and intents receiver
     */
    public void setupWifiManager() {

        manager = (WifiP2pManager) mainActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(mainActivity, mainActivity.getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, mainActivity);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    }

    /**
     * Registers the receiver
     */
    public void register() {
        mainActivity.registerReceiver(receiver, intentFilter);
    }

    /**
     * Unregisters the receiver
     */
    public void unregister() {
        mainActivity.unregisterReceiver(receiver);
    }


    /**
     * Handles connection is setup to start transfering content
     * If sender, it notifies activity and sets up button visible to transfer image
     * and opens socket
     * If receiver initate a socket to listen to incoming data
     * @param address the connected device's ip address
     */
    public void connectionReady(String address) {
        this.deviceAddress = address;
        if (isSender) {

            ContentSender.getInstance().init(mainActivity);
            ContentSender.getInstance().openSocket();
        } else {
            new ContentReceiver(deviceAddress, mainActivity).openSocket();
        }
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }


    public boolean isConnected() {
        return isConnected;
    }

    public boolean isInfoReady() {
        return isInfoReady;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }


}
