package com.research.itsl.ism_d2d;

import android.app.Activity;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, WifiP2pManager.PeerListListener {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private TabHost mTabHost;
    private ListView listView;
    //CHARLEY: 0 - Wifi P2P,  1 - Bluetooth,  2 - Bluetooth Low Energy.  We start with Wifi P2P
    private int IsmType = 0;

    //CHARLEY: Declare WifiP2P variables
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver wifiReceiver = null;
    private IntentFilter intentFilter;
    private WifiP2pDevice selectedWifiDevice;
    public String groupOwnerAddress = new String();
    public String receivedDataString = new String();
    public int PACKET_LENGTH = 64;
    public int TIMEOUT_SECONDS = 5;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private WiFiPeerListAdapter peerListAdapter;

    //CHARLEY: Declare Classic Bluetooth variables
    final private int REQUEST_ENABLE_BT=1; //Request code used to start bluetooth enable activity
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mBluetoothReceiver;
    private BluetoothListAdapter mBluetoothArrayAdapter;
    private BluetoothDevice currentBluetoothDevice;

    Map<BluetoothDevice,Number> bluetoothRssi = new HashMap<BluetoothDevice, Number>();

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;


    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        listView.setVisibility(View.VISIBLE);
        findViewById(R.id.no_devices_available).setVisibility(View.INVISIBLE);
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        peerListAdapter.notifyDataSetChanged();
        if (peers.size() == 0) {
            Toast.makeText(MainActivity.this, "No devices available, search again",Toast.LENGTH_SHORT).show();
            return;
        }

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //CHARLEY: This code works with XML file to prevent cursor from starting in edit text for packet length and timeout, We turn on the cursor when the user actually edits the fields
        TextView.OnClickListener onClickListener = new TextView.OnClickListener(){
            @Override
            public void onClick(View v){
                if(v.getId() == R.id.packet_length_entry){
                    EditText packetEditText = (EditText) findViewById(R.id.packet_length_entry);
                    packetEditText.setCursorVisible(true);
                }else{
                    EditText timeoutEditText = (EditText) findViewById(R.id.timeout_entry);
                    timeoutEditText.setCursorVisible(true);
                }
            }
        };

        //CHARLEY: Listen for changes to edit text values and update the respective value, either packet length or receive socket timeout to allow users to set these values
        TextView.OnEditorActionListener enterKeyListener = new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if(exampleView.getId()==R.id.packet_length_entry){
                        EditText editText = (EditText) findViewById(R.id.packet_length_entry);
                        String packetLengthString = editText.getText().toString();
                        if(Pattern.compile("[0-9]+").matcher(packetLengthString).matches()){
                            MainActivity.this.PACKET_LENGTH = Integer.parseInt(packetLengthString);
                        }else{
                            editText.setText("");
                            Toast.makeText(MainActivity.this, "Please input a positive integer",Toast.LENGTH_SHORT).show();
                        }
                        editText.setCursorVisible(false);
                    }else{
                        EditText editText = (EditText) findViewById(R.id.timeout_entry);
                        String timeoutLengthString = editText.getText().toString();
                        if(Pattern.compile("[0-9]+").matcher(timeoutLengthString).matches()){
                            MainActivity.this.TIMEOUT_SECONDS = Integer.parseInt(timeoutLengthString);
                        }
                        else{
                            editText.setText("");
                            Toast.makeText(MainActivity.this, "Please input a positive integer",Toast.LENGTH_SHORT).show();
                        }
                        editText.setCursorVisible(false);
                    }
                }
                return true;
            }
        };

        EditText packetLengthEditText = (EditText) findViewById(R.id.packet_length_entry);
        packetLengthEditText.setOnEditorActionListener(enterKeyListener);
        packetLengthEditText.setOnClickListener(onClickListener);
        EditText timeoutEditText = (EditText) findViewById(R.id.timeout_entry);
        timeoutEditText.setOnEditorActionListener(enterKeyListener);
        timeoutEditText.setOnClickListener(onClickListener);

        //---------------------CHARLEY: Setting up the tabs---------------------------------
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        TabHost.TabSpec spec;
        spec = mTabHost.newTabSpec("device_list").setIndicator("Available Devices").setContent(R.id.device_list);
        mTabHost.addTab(spec);
        mTabHost.getTabWidget().getChildAt(0).setBackgroundColor(0xffffff);

        spec = mTabHost.newTabSpec("device_info").setIndicator("Device Information").setContent(R.id.device_info);
        mTabHost.addTab(spec);
        mTabHost.getTabWidget().getChildAt(1).setBackgroundColor(0xffffff);

        spec = mTabHost.newTabSpec("device_comm").setIndicator("Device Communication").setContent(R.id.device_comm);
        mTabHost.addTab(spec);
        mTabHost.getTabWidget().getChildAt(2).setBackgroundColor(0xffffff);


        //---------------------END: Setting up the tabs---------------------------------

        //CHARLEY: Getting rid of the dividers between tabs
        mTabHost.getTabWidget().setDividerDrawable(null);

        //CHARLEY: Change all the tab text colors to grey and change font style
        for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
            TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
            tv.setTypeface(Typeface.create("NORMAL", 0), 0);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP,11);
            tv.setTransformationMethod(null);
            tv.setGravity(Gravity.CENTER);
        }

        //CHARLEY: Change the initial tab to light grey
        TextView initialView = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
        initialView.setTextColor(Color.parseColor("#bababa"));


        //CHARLEY: Initialize Wifi Peer list
        peerListAdapter= new WiFiPeerListAdapter(this, R.layout.device_cell_layout, peers);

        //CHARLEY: Add necessary intent values to be matched
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //CHARLEY: Register the BroadcastReceiver with the intent values to be matched
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(MainActivity.this, getMainLooper(), null);
        wifiReceiver = new WiFiP2pBroadcastReceiver(manager, channel, MainActivity.this);
        registerReceiver(wifiReceiver, intentFilter);

        listView = (ListView)  findViewById(R.id.device_list_view);
        listView.setAdapter(peerListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //CHARLEY: Pass the selected device's information to the appropriate views to display device info
                selectedWifiDevice = peerListAdapter.getItem(position);
                TextView infoNameView = (TextView) findViewById(R.id.device_info_name);
                infoNameView.setText(selectedWifiDevice.deviceName);
                TextView infoTypeView=(TextView) findViewById(R.id.device_info_type);
                infoTypeView.setText(selectedWifiDevice.primaryDeviceType);
                TextView infoAddressView=(TextView) findViewById(R.id.device_info_address);
                infoAddressView.setText(selectedWifiDevice.deviceAddress);
                findViewById(R.id.connect_button).setVisibility(View.VISIBLE);
                //CHARLEY: Go to the device info tab
                mTabHost.setCurrentTab(1);
            }
        });

        if(mTabHost.getCurrentTabTag().equals("device_list")){
            //CHARLEY: Attempt to find nearby mobile devices to initiate D2D connection
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, "Discovery Initiated",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reasonCode) {
                    Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT).show();
                }
            });

        }

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                //CHARLEY: Change current tab's text color to light grey
                int currentTab = mTabHost.getCurrentTab();
                TextView view = (TextView) mTabHost.getTabWidget().getChildAt(currentTab).findViewById(android.R.id.title);
                view.bringToFront();
                view.setTextColor(Color.parseColor("#bababa"));
                //CHARLEY: Set all the rest of the tab's text color to grey
                for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
                    if (mTabHost.getTabWidget().getChildAt(i) != mTabHost.getTabWidget().getChildAt(currentTab)) {
                        TextView tv = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
                        tv.setTextColor(Color.parseColor("#000000"));
                    }
                }

                //CHARLEY: If current tab is Available devices,
                if(mTabHost.getCurrentTabTag().equals("device_list")){

                    switch(IsmType){

                        case 0: {
                            if (MainActivity.this.isWifiP2pEnabled) {
                                //CHARLEY: Attempt to find nearby mobile devices to initiate D2D connection
                                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(int reasonCode) {
                                        Toast.makeText(MainActivity.this, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            break;
                        }

                        case 1: {
                            if (!mBluetoothAdapter.isDiscovering()){
                                mBluetoothAdapter.startDiscovery();
                                Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case 2: {
                            break;
                        }
                    }


                }

                //CHARLEY: If current tab is Device Info,
                if(mTabHost.getCurrentTabTag().equals("device_info")){

                    switch(IsmType){
                        case 0: {

                            break;
                        }
                        case 1: {
                            break;
                        }
                        case 2: {
                            break;
                        }
                    }

                }

                //CHARLEY: If current tab is Device Communication,
                if(mTabHost.getCurrentTabTag().equals("device_comm")){

                }
            }
        });
    }

    public void deviceConnect(View view) {
        //CHARLEY: Attempt to create a P2P connection
        switch(IsmType){
            case 0: {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = selectedWifiDevice.deviceAddress;
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        //CHARLEY: Find the appropriate views and display them
                        TextView connectionStatus = (TextView) findViewById(R.id.device_comm_connection_status);
                        connectionStatus.setText("Connected to " + selectedWifiDevice.deviceName);
                        findViewById(R.id.send_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.receive_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.received_data_textview).setVisibility(View.VISIBLE);
                        findViewById(R.id.received_data_label).setVisibility(View.VISIBLE);
                        findViewById(R.id.disconnect_button).setVisibility(View.VISIBLE);
                        findViewById(R.id.set_packet_length_and_timeout).setVisibility(View.VISIBLE);

                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, "Unable to establish Wifi P2P connection", Toast.LENGTH_SHORT).show();

                    }
                });
                break;
            }
            case 1: {

                break;
            }
            case 2: {
                break;
            }
        }
    }

    public void deviceDisconnect(View view) {
        //CHARLEY: Remove P2P connection
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {

            }

            @Override
            public void onSuccess() {
                //CHARLEY: Find the appropriate views and display them, remove views that are no longer appropriate
                TextView connectionStatus = (TextView) findViewById(R.id.device_comm_connection_status);
                connectionStatus.setText("No connection");
                findViewById(R.id.send_button).setVisibility(View.INVISIBLE);
                findViewById(R.id.receive_button).setVisibility(View.INVISIBLE);
                findViewById(R.id.received_data_textview).setVisibility(View.INVISIBLE);
                findViewById(R.id.received_data_label).setVisibility(View.INVISIBLE);
                findViewById(R.id.disconnect_button).setVisibility(View.INVISIBLE);
                findViewById(R.id.set_packet_length_and_timeout).setVisibility(View.VISIBLE);
                TextView available = (TextView) findViewById(R.id.no_devices_available);
                available.setVisibility(View.VISIBLE);
                available.setText("No Devices Available");
                findViewById(R.id.device_list).setVisibility(View.INVISIBLE);
            }

        });
    }

    public void dataTransfer(View view) throws Exception{
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();
        switch(view.getId()){
            //CHARLEY: Send data in the background, pass the current device & target device MAC address and the group owner address to the async background task
            case R.id.send_button:
                new DataTransferAsyncTask(MainActivity.this,DataTransferAsyncTask.TRANSFER_SEND,macAddress,selectedWifiDevice.deviceAddress,groupOwnerAddress).execute();
                break;

            //CHARLEY: Attempt to receive data in the background, pass the current device & target device MAC address and the group owner address to the async background task
            case R.id.receive_button:
                //CHARLEY: Clear the Received Data section since we are attempting to received new set of data
                this.receivedDataString = "";
                TextView receivedData = (TextView) findViewById(R.id.received_data_textview);
                receivedData.setText(this.receivedDataString);
                //CHARLEY: Attempt to receive data in background.
                new DataTransferAsyncTask(MainActivity.this,DataTransferAsyncTask.TRANSFER_RECEIVE,macAddress,selectedWifiDevice.deviceAddress,groupOwnerAddress).execute();
                break;

            default:
                break;
        }

    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch(IsmType){
            case 0: {
                if (wifiReceiver != null){
                    unregisterReceiver(wifiReceiver);
                    wifiReceiver = null;
                }
                break;
            }
            case 1: {
                mBluetoothAdapter.cancelDiscovery();
                if (mBluetoothReceiver != null){
                    unregisterReceiver(mBluetoothReceiver);
                    wifiReceiver = null;
                }
                break;
            }
            case 2: {
                break;
            }
        }

        switch(position){
            //CHARLEY: Set up the Wifi P2P device list and register a receiver to look for devices nearby
            case 0: {
                getActionBar().setTitle("Wifi P2P");
                if (IsmType != position) {
                    mTabHost.setCurrentTab(0);
                    listView.setAdapter(peerListAdapter);
                    listView.setVisibility(View.INVISIBLE);
                    findViewById(R.id.no_devices_available).setVisibility(View.VISIBLE);
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            selectedWifiDevice = peerListAdapter.getItem(position);
                            TextView infoNameView = (TextView) findViewById(R.id.device_info_name);
                            infoNameView.setText(selectedWifiDevice.deviceName);
                            TextView infoTypeView = (TextView) findViewById(R.id.device_info_type);
                            infoTypeView.setVisibility(View.VISIBLE);
                            if (findViewById(R.id.device_info_rssi) != null)
                                findViewById(R.id.device_info_rssi).setVisibility(View.INVISIBLE);
                            infoTypeView.setText(selectedWifiDevice.primaryDeviceType);
                            TextView infoAddressView = (TextView) findViewById(R.id.device_info_address);
                            infoAddressView.setText(selectedWifiDevice.deviceAddress);
                            findViewById(R.id.connect_button).setVisibility(View.VISIBLE);
                            mTabHost.setCurrentTab(1);
                        }
                    });
                    if(wifiReceiver == null){
                        wifiReceiver = new WiFiP2pBroadcastReceiver(manager, channel, this);
                        registerReceiver(wifiReceiver, intentFilter);
                    }

                }

                IsmType = 0;
                break;
            }
            case 1: {
                getActionBar().setTitle("Bluetooth");
                mBluetoothArrayAdapter = new BluetoothListAdapter(this, R.layout.device_cell_layout);
                listView.invalidateViews();
                listView.setAdapter(mBluetoothArrayAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        currentBluetoothDevice = mBluetoothArrayAdapter.getItem(position);
                        TextView infoNameView = (TextView) findViewById(R.id.device_info_name);
                        infoNameView.setText(currentBluetoothDevice.getName());
                        findViewById(R.id.device_info_type).setVisibility(View.INVISIBLE);
                        findViewById(R.id.device_info_rssi).setVisibility(View.VISIBLE);
                        TextView infoRssiView = (TextView) findViewById(R.id.device_info_rssi);
                        infoRssiView.setText(bluetoothRssi.get(currentBluetoothDevice).toString());
                        TextView infoAddressView = (TextView) findViewById(R.id.device_info_address);
                        infoAddressView.setText(currentBluetoothDevice.getAddress());
                        findViewById(R.id.connect_button).setVisibility(View.VISIBLE);
                        mTabHost.setCurrentTab(1);
                    }
                });
                if (IsmType != position) {
                    mTabHost.setCurrentTab(0);
                    listView.setVisibility(View.INVISIBLE);
                    findViewById(R.id.no_devices_available).setVisibility(View.VISIBLE);
                }
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter != null) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        // Create a BroadcastReceiver for ACTION_FOUND
                        mBluetoothReceiver = new BroadcastReceiver() {
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();
                                // When discovery finds a device
                                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                                    findViewById(R.id.no_devices_available).setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.VISIBLE);
                                    // Get the BluetoothDevice object from the Intent
                                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                    bluetoothRssi.put(device, intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0));
                                    // Add the device to an array adapter to show in a ListView
                                    mBluetoothArrayAdapter.add(device);
                                    mBluetoothArrayAdapter.notifyDataSetChanged();
                                }
                            }
                        };
                        // Register the BroadcastReceiver
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(mBluetoothReceiver, filter); // Don't forget to unregister during onDestroy

                    }
                }
                if (!mBluetoothAdapter.isDiscovering()){
                    mBluetoothAdapter.startDiscovery();
                    Toast.makeText(MainActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                }

                IsmType = 1;
                break;
            }
            case 2: {
                getActionBar().setTitle("Bluetooth Low Energy");

                IsmType = 2;
                break;
            }
        }


    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(mBluetoothReceiver != null)
           unregisterReceiver(mBluetoothReceiver);
        if(wifiReceiver != null)
           unregisterReceiver(wifiReceiver);
    }


    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_MENU) {
            if(mNavigationDrawerFragment.isDrawerOpen()){
                //CHARLEY: Close the navigation drawer when User presses hardware menu button if the menu is open
                mNavigationDrawerFragment.closeDrawer();
                return true;
            }
            if(!mNavigationDrawerFragment.isDrawerOpen()){
                //CHARLEY: If the menu is closed, open the navigation drawer when User presses hardware menu button so that they can use the menu
                mNavigationDrawerFragment.openDrawer();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //-------------------------CHARLEY: Allow to user to open or close the left sidebar when the menu icon is pressed---------
            if(!mNavigationDrawerFragment.isDrawerOpen()){
                mNavigationDrawerFragment.openDrawer();
            }
            else{
                mNavigationDrawerFragment.closeDrawer();
            }

            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
