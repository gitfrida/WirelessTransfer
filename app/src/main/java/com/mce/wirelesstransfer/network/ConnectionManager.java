package com.mce.wirelesstransfer.network;

import android.Manifest;
import android.app.Activity;
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
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.mce.wirelesstransfer.R;
import com.mce.wirelesstransfer.ui.MainActivity;

public class ConnectionManager {
    public static final int PORT = 8881;
    private MainActivity mainActivity;


    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    public String deviceAddress = null;
    boolean isConnected = false;
    boolean isInfoReady = false;
    private boolean isSender;

    public WifiP2pManager.PeerListListener peerListListener;
    public WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    public ConnectionManager(MainActivity mainActivity) {
        this.mainActivity = mainActivity;


        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                        mainActivity.sendMessage(R.string.found_nearby);
                            if (!isConnected) {
                                Log.d(MainActivity.TAG, "onPeersAvailable: connect to" + device.deviceName + "  address " + device.deviceAddress);
                                connectToDevice(device);
                            }

                        break;


                }
            }

        };

        connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
               {
                   Log.d(MainActivity.TAG,"onConnectionInfoAvailable formed: "+wifiP2pInfo.groupFormed + "  owner: "+wifiP2pInfo.isGroupOwner +
                           "  address: "+wifiP2pInfo.groupOwnerAddress + " isSender: "+isSender + " isConnected: "+isConnected);
                    if(!isSender)
                    {
                           if(!isInfoReady && wifiP2pInfo.groupFormed) {
                               connectionReady(wifiP2pInfo.groupOwnerAddress.getHostAddress() );
                               isInfoReady = true;
                           }


                    }
                    else
                    {
                        if(wifiP2pInfo.groupOwnerAddress!=null && !isInfoReady && wifiP2pInfo.groupFormed) {
                            connectionReady(wifiP2pInfo.groupOwnerAddress.getHostAddress());
                            isInfoReady = true;
                        }
                    }


                }
            }
        };
    }


    private void connectToDevice(WifiP2pDevice device) {
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
                            mainActivity.sendMessage(R.string.connected_wait);
                            manager.requestConnectionInfo(channel,  connectionInfoListener);

                        }

                        @Override
                        public void onFailure(int i) {
                            mainActivity.sendMessage(R.string.connection_failed);
                        }
                    });
                } catch (Exception e) {
                    Log.d(MainActivity.TAG, "Connect exception " + e.getMessage());
                }
            }
        }

    }


    public void discoverPeers() {
        if (!isConnected) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(MainActivity.TAG, "Discover Succeeded");
                        mainActivity.sendMessage(R.string.discover_succeeded);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(MainActivity.TAG, "Discover Failed");
                        mainActivity.sendMessage(R.string.discover_failed);
                        discoverPeers();
                    }
                });
            }

        }

    }

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

    public void register() {
        mainActivity.registerReceiver(receiver, intentFilter);
    }

    public void unregister() {
        mainActivity.unregisterReceiver(receiver);
    }


    public void connectionReady(String address) {
        mainActivity.sendMessage(R.string.ready);
        this.deviceAddress = address;
        if (isSender) {
            mainActivity.sendMessage(R.string.ready_send);
            mainActivity.setTransferBtnVisible();
            ContentSender.getInstance().init(mainActivity);
            ContentSender.getInstance().openSocket(deviceAddress);
        }
         else
            new ContentReceiver(deviceAddress, mainActivity).openSocket();


    }

    public boolean isSender() {
        return isSender;
    }

    public void setSender(boolean sender) {
        isSender = sender;
    }

}
