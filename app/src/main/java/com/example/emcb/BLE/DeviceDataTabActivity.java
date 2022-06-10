package com.example.emcb.BLE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.emcb.R;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

public class DeviceDataTabActivity extends AppCompatActivity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private byte[] getEepData = {0x41};
    private final int currentPtr = 1;
    private final int idPtr = 3;
    private final int cutoffPtr = 5;
    private final int onOffPtr = 7;
    private final int autoConnPtr = 8;
    private final int ownerPtr = 9;

    private int EepOn = 0;

    private ArrayList<String> existingReader;
    private ArrayList<DeviceData> deviceDataList;
    private int totalTag = 100;

    private TextView mConnectionState;
    private TextView mRawData;
    private TextView mEepData;
    private TextView mDeviceAddress;
    private TextView mDeviceName;

    private String nDeviceName;
    private String nDeviceAddress;
    public static BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;

    PagerAdapter mPagerAdapter;
    ViewPager2 viewPager2;
    TabLayout tabLayout;

    AllFragment allFragment;
    GridviewFragment gridviewFragment;
    ListviewFragment listviewFragment;

    CountDownTimer countDownTimer = new CountDownTimer(3000, 1000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            if(gridviewFragment != null) {
                gridviewFragment.refreshDeviceData();
            }
            if(listviewFragment != null) {
                listviewFragment.refreshDeviceData();
            }
            countDownTimer.start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_data_tab);

        existingReader = new ArrayList<>();
        deviceDataList = new ArrayList<>();

        Intent intent = getIntent();
        nDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        nDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mDeviceName = findViewById(R.id.device_name);
        mDeviceAddress = findViewById(R.id.device_address);
        mConnectionState = findViewById(R.id.connection_state);
        mRawData = findViewById(R.id.gatt_services_raw_data);
        mEepData = findViewById(R.id.gatt_services_eep_data);

        mDeviceName.setText(nDeviceName);
        mDeviceAddress.setText(nDeviceAddress);

        initialiseData();

        tabLayout = findViewById(R.id.tabLayout);
        viewPager2 = findViewById(R.id.viewPager);
        mPagerAdapter = new PagerAdapter(this);

        viewPager2.setAdapter(mPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch(position) {
//                    case 0:
//                        tab.setText("All");
//                        break;
                    case 0:
                        tab.setIcon(R.drawable.ic_baseline_grid_view_24);
                        break;
                    case 1:
                        tab.setIcon(R.drawable.ic_baseline_view_list_24);
                        break;
                }
            }
        }).attach();

        getSupportActionBar().setTitle(nDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startService(gattServiceIntent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(nDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();
        stopService(new Intent(this, BluetoothLeService.class));
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_refresh:
                if(gridviewFragment != null) {
                    gridviewFragment.refreshDeviceData();
                }
                if(listviewFragment != null) {
                    listviewFragment.refreshDeviceData();
                }
                break;
            case R.id.menu_readEep:
                if(listviewFragment != null) {
                    clearUI();
                    writeEepData(getEepData);
                }
                break;
            case R.id.menu_connect:
                mBluetoothLeService.connect(nDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                mBluetoothLeService.disconnect();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(nDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                mBluetoothLeService.setCharacteristicNotificationData(true);
                countDownTimer.start();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                updateData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_EEP_DATA_AVAILABLE.equals(action)) {
                updateEepData(intent.getStringExtra(BluetoothLeService.EEP_DATA));
            }
        }
    };

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void clearUI() {
        mRawData.setText(R.string.no_data);
        mEepData.setText(R.string.no_data);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_EEP_DATA_AVAILABLE);
        return intentFilter;
    }

    public void updateData(String data) {
        int deviceNumber;
        double totalCurrent;
        int duplicateValue;
        int header;

        if (data != null) {
            mRawData.setText(data);
            String[] filterData = data.split(" ");
            header = Integer.parseUnsignedInt(filterData[0], 16);
            resetDuplicateValue(String.valueOf(Integer.parseUnsignedInt(filterData[1])));

            if(header != 0) {
                for(int i = 2; i < (filterData.length - 2); i+=4) {
                    deviceNumber = Integer.parseUnsignedInt(filterData[i], 16);
                    if(deviceNumber > 0 && deviceNumber <= totalTag) {
                        deviceNumber = deviceNumber - 1;
                        duplicateValue = deviceDataList.get(deviceNumber).getDuplicate() + 1;
                        totalCurrent = calculateTotalCurrent(Integer.parseUnsignedInt(filterData[i+2], 16), Integer.parseUnsignedInt(filterData[i+3], 16));

                        deviceDataList.get(deviceNumber).setReader(String.valueOf(Integer.parseUnsignedInt(filterData[1], 16)));
                        deviceDataList.get(deviceNumber).setName(String.valueOf(Integer.parseUnsignedInt(filterData[i], 16)));
                        deviceDataList.get(deviceNumber).setStatus(String.valueOf(Integer.parseUnsignedInt(filterData[i + 1], 16)));
                        deviceDataList.get(deviceNumber).setCurrent(String.valueOf(totalCurrent));
                        deviceDataList.get(deviceNumber).setDuplicate(duplicateValue);
                        if(gridviewFragment != null) {
                            gridviewFragment.updateDeviceData(deviceNumber);
                        }
                        if(listviewFragment != null) {
                            listviewFragment.updateDeviceData(deviceNumber);
                        }
                    }
                }
            }
        }
    }

    public void updateEepData(String data) {
        int deviceNumber;
        double totalCurrent;
        int totalPeriod;
        int header;

        if (data != null) {
            mEepData.setText(data);
            String[] filterData = data.split(" ");
            header = Integer.parseUnsignedInt(filterData[0], 16);

            if(header == 0) {
                deviceNumber = Integer.parseUnsignedInt(filterData[idPtr], 16);
                if(deviceNumber > 0 && deviceNumber <= totalTag) {
                    deviceNumber = deviceNumber - 1;
                    totalCurrent = calculateTotalCurrent(Integer.parseUnsignedInt(filterData[currentPtr], 16), Integer.parseUnsignedInt(filterData[currentPtr + 1], 16));
                    totalPeriod = calculateTotalPeriod(Integer.parseUnsignedInt(filterData[cutoffPtr], 16), Integer.parseUnsignedInt(filterData[cutoffPtr + 1], 16));

                    deviceDataList.get(deviceNumber).setCurrentSetting(totalCurrent);
                    deviceDataList.get(deviceNumber).setName(String.valueOf(Integer.parseUnsignedInt(filterData[idPtr], 16)));
                    deviceDataList.get(deviceNumber).setCutoffPeriod(totalPeriod);
                    deviceDataList.get(deviceNumber).setOnOffSetting(Integer.parseUnsignedInt(filterData[onOffPtr], 16));
                    deviceDataList.get(deviceNumber).setAutoReconnect(Integer.parseUnsignedInt(filterData[autoConnPtr], 16));
                    deviceDataList.get(deviceNumber).setOwnerName(String.valueOf(Integer.parseUnsignedInt(filterData[ownerPtr], 16)));
                }
            }
        }

    }

    private void initialiseData() {
        for(int i = 0; i < totalTag; i++) {
            //GRID VIEW
            DeviceData mDeviceData = new DeviceData();
            mDeviceData.setReader(String.valueOf(0));
            mDeviceData.setName(String.valueOf(i + 1));
            mDeviceData.setStatus(String.valueOf(64));
            mDeviceData.setCurrent(getString(R.string.blank));
            mDeviceData.setDuplicate(0);
            //LIST VIEW
            mDeviceData.setCurrentSetting(0.0);
            mDeviceData.setCutoffPeriod(0);
            mDeviceData.setOnOffSetting(0);
            mDeviceData.setAutoReconnect(0);
            mDeviceData.setOwnerName("");

            deviceDataList.add(mDeviceData);
        }
    }

    private void resetDuplicateValue(String readerNo) {
        if(existingReader.contains(readerNo)){
            for(int i = 0; i < totalTag; i++){
                if(deviceDataList.get(i).getReader().equals(readerNo)) {
                    deviceDataList.get(i).setDuplicate(0);
                }
            }
        } else {
            existingReader.add(readerNo);
        }
    }
    private double calculateTotalCurrent(int current1, int current2) {
        double totalCurrent;
        double maxCurrent = 99.9;
        totalCurrent = ((current1 + (current2 * 256)) / 10.0);
        if(totalCurrent > maxCurrent) {
            totalCurrent = maxCurrent;
        }
        return totalCurrent;
    }

    private int calculateTotalPeriod(int period1, int period2) {
        int totalPeriod;
        int maxPeriod = 300;
        totalPeriod = (period1 + (period2 * 256));
        if(totalPeriod > maxPeriod) {
            totalPeriod = maxPeriod;
        }
        return totalPeriod;
    }

    public int getSpanCount() {
        int columnWidth;
        int minWidth = 800;
        int spanCount;
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        columnWidth = Math.round(tabLayout.getWidth() / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        if(columnWidth >= minWidth) {
            spanCount = 10;
        } else {
            spanCount = 5;
        }
        return spanCount;
    }

    public class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {

            switch (position) {
//                case 0:
//                    allFragment = AllFragment.newInstance(deviceDataList, getSpanCount());
//                    return allFragment;
                case 0:
                    gridviewFragment = GridviewFragment.newInstance(deviceDataList, getSpanCount());
                    return gridviewFragment;
                case 1:
                    listviewFragment = ListviewFragment.newInstance(deviceDataList, getSpanCount());
                    return listviewFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

    }

    public void readCharacteristicData() {
        mBluetoothLeService.readCharacteristicData();
    }

    public static void writeCharacteristicData(byte[] value) {
        mBluetoothLeService.writeCharacteristicData(value);
    }

    public void setCharacteristicNotificationData(boolean notify) {
        mBluetoothLeService.setCharacteristicNotificationData(notify);
    }

    public static void readEepData() {
        mBluetoothLeService.readEepCharacteristicData();
    }

    public static void writeEepData(byte[] value) {
        mBluetoothLeService.writeEepCharacteristicData(value);
    }

    public static void setEepNotificationData(boolean notify) {
        mBluetoothLeService.setEepCharacteristicNotificationData(notify);
    }



}
