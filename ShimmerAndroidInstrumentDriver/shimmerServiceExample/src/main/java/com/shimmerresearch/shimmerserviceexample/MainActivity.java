package com.shimmerresearch.shimmerserviceexample;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;
import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog;
import com.shimmerresearch.android.guiUtilities.ShimmerDialogConfigurations;
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid;
import com.shimmerresearch.android.shimmerService.ShimmerService;
import com.shimmerresearch.bluetooth.ShimmerBluetooth;
import com.shimmerresearch.driver.CallbackObject;
import com.shimmerresearch.driver.ObjectCluster;
import com.shimmerresearch.driver.ShimmerDevice;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ConnectedShimmersListFragment.OnShimmerDeviceSelectedListener {

    ShimmerDialogConfigurations dialog;
    BluetoothAdapter btAdapter;
    ShimmerService mService;
    SensorsEnabledFragment sensorsEnabledFragment;
    ConnectedShimmersListFragment connectedShimmersListFragment;
    DeviceConfigFragment deviceConfigFragment;
    PlotFragment plotFragment;
    SignalsToPlotFragment signalsToPlotFragment;

    //Drawer stuff
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;
    public String selectedDeviceAddress, selectedDeviceName;
    private boolean showPlotFragments = false;

    XYPlot dynamicPlot; //TODO: Remove this when done testing


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter1 mSectionsPagerAdapter1;
    private SectionsPagerAdapter2 mSectionsPagerAdapter2;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;


    //Extra for intent from ShimmerBluetoothDialog
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    boolean isServiceStarted = false;

    final static String LOG_TAG = "Shimmer";
    final static String SERVICE_TAG = "ShimmerService";
    final static int REQUEST_CONNECT_SHIMMER = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.content_frame);
        mActivityTitle = getTitle().toString();

        String[] startArray = {"Configuration", "Plot"};
        addDrawerItems(startArray);
        setupDrawer();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter1 = new SectionsPagerAdapter1(getSupportFragmentManager());
        mSectionsPagerAdapter2 = new SectionsPagerAdapter2(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter1);
        mViewPager.setOffscreenPageLimit(4);    //Ensure none of the fragments has their view destroyed when off-screen

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        dialog = new ShimmerDialogConfigurations();

        sensorsEnabledFragment = SensorsEnabledFragment.newInstance(null, null);
        connectedShimmersListFragment = ConnectedShimmersListFragment.newInstance();
        deviceConfigFragment = DeviceConfigFragment.newInstance();
        plotFragment = PlotFragment.newInstance();
        signalsToPlotFragment = SignalsToPlotFragment.newInstance();

        //Check if Bluetooth is enabled
        if (!btAdapter.isEnabled()) {
            int REQUEST_ENABLE_BT = 1;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            //Start the Shimmer service
            Intent intent = new Intent(this, ShimmerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            Log.d(LOG_TAG, "Shimmer Service started");
            Toast.makeText(this, "Shimmer Service started", Toast.LENGTH_SHORT).show();
        }





    }

    private void addDrawerItems(String[] stringArray) {
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stringArray);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

//                String viewText = (String) mDrawerList.getItemAtPosition(position);
//                if (viewText.contains("\n")) {
//                    selectedDeviceAdd = viewText.substring(viewText.indexOf("\n"));
//                    Toast.makeText(MainActivity.this, "Selected device: " + viewText, Toast.LENGTH_SHORT).show();
//                }
                if(position == 0) {
                    mSectionsPagerAdapter1.setPlotView(false);
                    mSectionsPagerAdapter1.notifyDataSetChanged();

                } else {
                    mSectionsPagerAdapter1.setPlotView(true);
                    mSectionsPagerAdapter1.notifyDataSetChanged();
                }
                //Highlight the selected item
                view.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.holo_orange_light));
                //Set all other backgrounds to white (clearing previous highlight, if any)
                for (int i = 0; i < mDrawerList.getAdapter().getCount(); i++) {
                    if (i != position) {
                        View v = mDrawerList.getChildAt(i);
                        v.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), android.R.color.white));
//                        Log.e(SERVICE_TAG, "Cleared background color...");
                    }
                }
            }
        });
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
//                String[] list = getStringListOfDevicesConnected();
//                if (list != null) {
//                    addDrawerItems(list);
//                }
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("Navigation");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mActivityTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
    }

    private String[] getStringListOfDevicesConnected() {
        List<ShimmerDevice> deviceList = mService.getListOfConnectedDevices();
        if (deviceList != null) {
            String[] nameList = new String[deviceList.size()];
            for (int i = 0; i < deviceList.size(); i++) {
                ShimmerDevice device = deviceList.get(i);
                nameList[i] = device.getShimmerUserAssignedName() + "\n" + device.getMacId();
            }
            return nameList;
        } else {
            Log.w(SERVICE_TAG, "Error! No Shimmers connected. Cannot retrieve List of devices");
            return null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.paired_devices:
                Intent serverIntent = new Intent(getApplicationContext(), ShimmerBluetoothDialog.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_SHIMMER);
                return true;
            case R.id.test_button:
                ShimmerBluetoothManagerAndroid btManager2 = mService.getBluetoothManager();
                btManager2.startStreamingAllDevices();
                return true;
            case R.id.connect_shimmer:
                if (isServiceStarted) {
                    mService.connectShimmer("00:06:66:66:96:86");
                } else {
                    Toast.makeText(this, "ERROR! Service not started.", Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.add_handler:
                ShimmerBluetoothManagerAndroid btManager = mService.getBluetoothManager();
                btManager.addHandler(mHandler);
                return true;
            case R.id.sensors_enabled_fragment_test:
                ShimmerDevice device = mService.getShimmer("00:06:66:66:96:86");
                sensorsEnabledFragment.setShimmerService(mService);
                sensorsEnabledFragment.buildSensorsList(device, getApplicationContext());
                return true;
            case R.id.connected_shimmers_fragment_test:
                List<ShimmerDevice> deviceList = mService.getListOfConnectedDevices();
                connectedShimmersListFragment.buildShimmersConnectedListView(deviceList, getApplicationContext());
                return true;
            case R.id.device_configuration_fragment_test:
                ShimmerDevice device2 = mService.getShimmer("00:06:66:66:96:86");
                deviceConfigFragment.buildDeviceConfigList(device2, getApplicationContext());
                return true;
            case R.id.signals_to_plot_fragment_test:
                ShimmerDevice device3 = mService.getShimmer("00:06:66:66:96:86");
                signalsToPlotFragment.buildSignalsToPlotList(getApplicationContext(), mService, "00:06:66:66:96:86", plotFragment.getDynamicPlot());
                return true;
            case R.id.start_streaming:
                if(selectedDeviceAddress != null) {
                    ShimmerDevice mDevice1 = mService.getShimmer(selectedDeviceAddress);
                    mDevice1.startStreaming();
                    signalsToPlotFragment.buildSignalsToPlotList(this, mService, selectedDeviceAddress, dynamicPlot);
                }
                return true;
            case R.id.stop_streaming:
                if(selectedDeviceAddress != null) {
                    ShimmerDevice mDevice2 = mService.getShimmer(selectedDeviceAddress);
                    mDevice2.stopStreaming();
                    sensorsEnabledFragment.buildSensorsList(mDevice2, this);
                    deviceConfigFragment.buildDeviceConfigList(mDevice2, this);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onDestroy() {
        //Stop the Shimmer service
        super.onDestroy();
        mHandler = null;
        if(isServiceStarted) {
            this.unbindService(mConnection);
        }
        Intent intent = new Intent(this, ShimmerService.class);
        stopService(intent);
        Log.d(LOG_TAG, "Shimmer Service stopped");
        Toast.makeText(this, "Shimmer Service stopped", Toast.LENGTH_SHORT).show();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mService = ((ShimmerService.LocalBinder) service).getService();
            isServiceStarted = true;
            //Add this activity's Handler to the service's list of Handlers so we know when a Shimmer is connected/disconnected
            mService.addHandlerToList(mHandler);
            Log.d(SERVICE_TAG, "Shimmer Service Bound");
            //TODO: connectedShimmersListFragment.buildShimmersConnectedListView(mService.getListOfConnectedDevices(), getApplicationContext());

//            mHandler = mService.getHandler();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mService = null;
            isServiceStarted = false;
            Log.d(SERVICE_TAG, "Shimmer Service Disconnected");
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) { //The system Bluetooth enable dialog has returned a result
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, ShimmerService.class);
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                Log.d(LOG_TAG, "Shimmer Service started");
                Toast.makeText(this, "Shimmer Service started", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Please enable Bluetooth to proceed.", Toast.LENGTH_LONG).show();
                int REQUEST_ENABLE_BT = 1;
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Toast.makeText(this, "Unknown Error! Your device may not support Bluetooth!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) { //The devices paired list has returned a result
            if (resultCode == Activity.RESULT_OK) {
                //Get the Bluetooth mac address of the selected device:
                String macAdd = data.getStringExtra(EXTRA_DEVICE_ADDRESS);
                mService.connectShimmer(macAdd);    //Connect to the selected device
            }
        }
    }

    boolean checkBtEnabled() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            return false;
        }
        return true;
    }

    void testButton(View view) {
        ShimmerDevice sDevice = mService.getShimmer("00:06:66:66:96:86");
        dialog.buildShimmersConnectedList(mService.getBluetoothManager().getListOfConnectedDevices(), this);
        //dialog.buildShimmerConfigOptions(sDevice, this);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
/*
    public class SectionsPagerAdapter1 extends FragmentPagerAdapter {

        private boolean isPlotSet = false;

        public SectionsPagerAdapter1(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.i("JOS", "Position is: " + position);
            if(isPlotSet) {
                if(position == 0) {
                    if(isServiceStarted) {
                        connectedShimmersListFragment.buildShimmersConnectedListView(mService.getListOfConnectedDevices(), getApplicationContext());
                    } else {
                        connectedShimmersListFragment.buildShimmersConnectedListView(null, getApplicationContext());
                    }
                    return connectedShimmersListFragment;
                } else if(position == 1) {
                    plotFragment.setShimmerService(mService);
                    dynamicPlot = plotFragment.getDynamicPlot();
                    return plotFragment;
                } else {
                    return signalsToPlotFragment;
                }
            } else {
                if(position == 0) {
                    if(isServiceStarted) {
                        connectedShimmersListFragment.buildShimmersConnectedListView(mService.getListOfConnectedDevices(), getApplicationContext());
                    } else {
                        connectedShimmersListFragment.buildShimmersConnectedListView(null, getApplicationContext());
                    }
                    return connectedShimmersListFragment;
                } else if(position == 1) {
                    return sensorsEnabledFragment;
                } else {
                    return deviceConfigFragment;
                }
            }

        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (isPlotSet) {
                switch (position) {
                    case 0:
                        return "Connected Devices";
                    case 1:
                        return "Signals to Plot";
                    case 2:
                        return "Plot";
                }
                return null;
            } else {
                switch (position) {
                    case 0:
                        return "Connected Devices";
                    case 1:
                        return "Plot";
                    case 2:
                        return "Signals to Plot";
                }
                return null;
            }
        }

        public void setPlotView(boolean plotSet) {
            isPlotSet = plotSet;
        }
    }
*/

    public class SectionsPagerAdapter1 extends FragmentPagerAdapter {

        private boolean isPlotSet = false;

        public SectionsPagerAdapter1(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.i("JOS", "Position is: " + position);
                if(position == 0) {
                    if(isServiceStarted) {
                        connectedShimmersListFragment.buildShimmersConnectedListView(mService.getListOfConnectedDevices(), getApplicationContext());
                    } else {
                        connectedShimmersListFragment.buildShimmersConnectedListView(null, getApplicationContext());
                    }
                    return connectedShimmersListFragment;
                }
                else if(position == 1) {
                    return sensorsEnabledFragment;
                }
                else if (position == 2) {
                    return deviceConfigFragment;
                }
                else if (position == 3) {
                    return plotFragment;
                }
                else if (position == 4) {
                    return signalsToPlotFragment;
                }
                else {
                    return null;
                }
        }

        @Override
        public int getCount() {
            // Show 5 total pages.
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Connected Devices";
                case 1: return "Enable Sensors";
                case 2: return "Device Configuration";
                case 3: return "Plot";
                case 4: return "Signals to Plot";
                default: return "";
            }
        }

        public void setPlotView(boolean plotSet) {
            isPlotSet = plotSet;
        }
    }


    public class SectionsPagerAdapter2 extends FragmentPagerAdapter {

        public SectionsPagerAdapter2(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return PlotFragment.newInstance();
            } else if (position == 1) {
                if(isServiceStarted) {
                    connectedShimmersListFragment.buildShimmersConnectedListView(mService.getListOfConnectedDevices(), getApplicationContext());
                    Log.e("JOS", "Service started, returning fragment");
                } else {
                    connectedShimmersListFragment.buildShimmersConnectedListView(null, getApplicationContext());
                    Log.e("JOS", "Service not started, can't return fragment");
                }
                return connectedShimmersListFragment;
            } else {
                //Sensors fragment
                return sensorsEnabledFragment;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Plot";
                case 1:
                    return "Connected Shimmers";
                case 2:
                    return "Plot Signals";
            }
            return null;
        }
    }


    public boolean handleMessage(Message msg) {
        Toast.makeText(this, "Message received", Toast.LENGTH_SHORT).show();
        return true;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if(msg.what == ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE) {
                ShimmerBluetooth.BT_STATE state = null;
                String macAddress = "";
                String shimmerName = "";
                if (msg.obj instanceof ObjectCluster){
                    state = ((ObjectCluster)msg.obj).mState;
                    macAddress = ((ObjectCluster)msg.obj).getMacAddress();
                    shimmerName = ((ObjectCluster) msg.obj).getShimmerName();
                } else if(msg.obj instanceof CallbackObject){
                    state = ((CallbackObject)msg.obj).mState;
                    macAddress = ((CallbackObject)msg.obj).mBluetoothAddress;
                    shimmerName = "";
                }
                switch (state) {
                    case CONNECTED:
                        //Toast.makeText(getApplicationContext(), "Device connected: " + shimmerName + " " + macAddress, Toast.LENGTH_SHORT).show();
                        List<ShimmerDevice> deviceList = mService.getListOfConnectedDevices();
                        connectedShimmersListFragment.buildShimmersConnectedListView(deviceList, getApplicationContext());
                        //TODO: connectedShimmersListFragment.addDeviceToList(macAddress, shimmerName);
                        Log.e("JOS", "Device now connected");
                        break;
                    case CONNECTING:
                        break;
                    case STREAMING:
                        Toast.makeText(getApplicationContext(), "Device streaming: " + shimmerName + " " + macAddress, Toast.LENGTH_SHORT).show();
                        if(selectedDeviceAddress.contains(macAddress) && dynamicPlot != null) {
                            //If the selected device is the one that is now streaming, then show the list of signals available to be plotted
                            signalsToPlotFragment.buildSignalsToPlotList(getApplicationContext(), mService, macAddress, dynamicPlot);
                        }
                        Log.e("JOS", "Device now streaming");
                        break;
                    case STREAMING_AND_SDLOGGING:
                        if(selectedDeviceAddress.contains(macAddress) && dynamicPlot != null) {
                            signalsToPlotFragment.buildSignalsToPlotList(getApplicationContext(), mService, macAddress, dynamicPlot);
                        }
                        break;
                    case SDLOGGING:
                        break;
                    case DISCONNECTED:
                        Toast.makeText(getApplicationContext(), "Device disconnected: " + shimmerName + " " + macAddress, Toast.LENGTH_SHORT).show();
                        //TODO: connectedShimmersListFragment.removeDeviceFromList(macAddress);
                        break;
                }

            }

            if(msg.what == ShimmerBluetooth.NOTIFICATION_SHIMMER_STOP_STREAMING) {
                Log.e("JOS", "Shimmer has stopped streaming using ShimmerBluetooth class");
            }

            if(msg.arg1 == Shimmer.MSG_STATE_STOP_STREAMING) {
                Log.e("JOS", "Shimmer has stopped streaming using Shimmer class");
                signalsToPlotFragment.setDeviceNotStreamingView();
            }



        }
    };

    /**
     * This method is called when the ConnectedShimmersListFragment returns a selected Shimmer
     * @param macAddress
     */
    @Override
    public void onShimmerDeviceSelected(String macAddress, String deviceName) {
        Toast.makeText(this, "Selected Shimmer: " + deviceName + "\n" + macAddress, Toast.LENGTH_SHORT).show();
        selectedDeviceAddress = macAddress;
        selectedDeviceName = deviceName;

        //Pass the selected device to the fragments
        ShimmerDevice device = mService.getShimmer(selectedDeviceAddress);

        sensorsEnabledFragment.setShimmerService(mService);
        sensorsEnabledFragment.buildSensorsList(device, this);

        deviceConfigFragment.buildDeviceConfigList(device, this);

        plotFragment.setShimmerService(mService);
        dynamicPlot = plotFragment.getDynamicPlot();

        //signalsToPlotFragment.buildSignalsToPlotList(this, mService, selectedDeviceAddress, dynamicPlot);

    }





}
