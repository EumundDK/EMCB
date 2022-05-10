package com.example.emcb.BLE;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import com.example.emcb.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

public class DeviceDataActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private ArrayList<String> existingReader;
    private ArrayList<DeviceData> deviceDataList;
    private String rawData;
    private int totalTag = 100;

    private TextView mConnectionState;
    private TextView mRawData;
    private TextView mDeviceAddress;
    private TextView mDeviceName;
    private RecyclerView recyclerView;

    private String nDeviceName;
    private String nDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    RecyclerViewAdapter recyclerViewAdapter;

    CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            // recyclerViewAdapter.updateDeviceData(deviceDataList);
            countDownTimer.start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_data_2);

        existingReader = new ArrayList<>();
        deviceDataList = new ArrayList<>();

        Intent intent = getIntent();
        nDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        nDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mDeviceName = findViewById(R.id.device_name);
        mDeviceAddress = findViewById(R.id.device_address);
        mConnectionState = findViewById(R.id.connection_state);
        mRawData = findViewById(R.id.gatt_services_raw_data);

        mDeviceName.setText(nDeviceName);
        mDeviceAddress.setText(nDeviceAddress);

        recyclerView = findViewById(R.id.dataRecyclerView);

        initialiseData();

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
            updateConnectionState(R.string.connecting);
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
            case R.id.menu_connect:
                mBluetoothLeService.connect(nDeviceAddress);
                updateConnectionState(R.string.connecting);
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
            updateConnectionState(R.string.connecting);
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
                recyclerViewAdapter = new RecyclerViewAdapter(DeviceDataActivity.this, deviceDataList, 1);
                recyclerView.setAdapter(recyclerViewAdapter);
                recyclerView.setLayoutManager(new GridLayoutManager(DeviceDataActivity.this, getSpanCount()));
                mBluetoothLeService.setCharacteristicNotificationData();
//                countDownTimer.start();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                rawData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                updateData(rawData);
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
        recyclerView.setAdapter(null);
        mRawData.setText(R.string.no_data);
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

    private void updateData(String data) {
        int deviceNumber;
        double totalCurrent;
        int duplicateValue = 0;

        if (data != null) {
            mRawData.setText(data);
            String[] filterData = data.split(" ");
            String readerNo = String.valueOf(Integer.parseUnsignedInt(filterData[1]));

            if(existingReader.contains(readerNo)){
                for(int i = 0; i < totalTag; i++){
                    if((deviceDataList.get(i).getReader().equals(readerNo)) && (deviceDataList.get(i).getDuplicate() != 0)) {
                        deviceDataList.get(i).setDuplicate(0);
                        //recyclerViewAdapter.notifyItemChanged(i);
                    }
                }
            } else {
                existingReader.add(readerNo);
            }

            for(int i = 2; i < (filterData.length - 2); i+=4) {
                deviceNumber = Integer.parseUnsignedInt(filterData[i], 16);
                if(deviceNumber > 0 && deviceNumber <= totalTag) {
                    deviceNumber = deviceNumber - 1;
                    duplicateValue = deviceDataList.get(deviceNumber).getDuplicate() + 1;
                    totalCurrent = calculateTotalCurrent(Integer.parseUnsignedInt(filterData[i+3], 16) * 256, Integer.parseUnsignedInt(filterData[i+2], 16));

                    deviceDataList.get(deviceNumber).setReader(String.valueOf(Integer.parseUnsignedInt(filterData[1], 16)));
                    deviceDataList.get(deviceNumber).setName(String.valueOf(Integer.parseUnsignedInt(filterData[i], 16)));
                    deviceDataList.get(deviceNumber).setStatus(String.valueOf(Integer.parseUnsignedInt(filterData[i + 1], 16)));
                    deviceDataList.get(deviceNumber).setCurrent(String.valueOf(totalCurrent));
                    deviceDataList.get(deviceNumber).setDuplicate(duplicateValue);
                    recyclerViewAdapter.notifyItemChanged(deviceNumber);
                }

            }
            //recyclerViewAdapter.updateDeviceData(deviceDataList);

        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void initialiseData() {
        for(int i = 0; i < totalTag; i++) {
            DeviceData mDeviceData = new DeviceData();
            mDeviceData.setReader(String.valueOf(0));
            mDeviceData.setName(String.valueOf(i + 1));
            mDeviceData.setStatus(String.valueOf(64));
            mDeviceData.setCurrent(getString(R.string.blank));
            mDeviceData.setDuplicate(0);
            deviceDataList.add(mDeviceData);
        }
    }

    private void resetDuplicateValue() {
        for(int i = 0; i < totalTag; i++) {
            deviceDataList.get(i).setDuplicate(0);
        }
    }

    private double calculateTotalCurrent(int current1, int current2) {
        double totalCurrent;
        double maxCurrent = 99.9;

        totalCurrent = ((current1 + current2) / 10.0);
        if(totalCurrent > maxCurrent) {
            totalCurrent = 99.9;
        }

        return totalCurrent;
    }

    private int getSpanCount() {
        int columnWidth;
        int minWidth = 800;
        int spanCount;
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        columnWidth = Math.round(recyclerView.getWidth() / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
         if(columnWidth >= minWidth) {
             spanCount = 10;
         } else {
             spanCount = 5;
         }
         return spanCount;
    }

    private void displayData(String data) {
        double totalCurrent;

        if (data != null) {
            mRawData.setText(data);
            //deviceData.clear();

            String[] filterData = data.split(" ");
            String readerNo = String.valueOf(Integer.parseUnsignedInt(filterData[1], 16));

            if(existingReader.contains(readerNo)){
                deviceDataList.removeIf(e -> (e.getReader().equals(readerNo)));
            } else {
                existingReader.add(readerNo);
            }

            for(int i = 2; i < (filterData.length - 4); i+=4) {
                DeviceData mDeviceData = new DeviceData();
                mDeviceData.setReader(String.valueOf(Integer.parseUnsignedInt(filterData[1], 16)));
                mDeviceData.setName(String.valueOf(Integer.parseUnsignedInt(filterData[i], 16)));
                mDeviceData.setStatus(String.valueOf(Integer.parseUnsignedInt(filterData[i + 1], 16)));
                totalCurrent = calculateTotalCurrent(Integer.parseUnsignedInt(filterData[i+2], 16) * 256, Integer.parseUnsignedInt(filterData[i+3], 16));
                mDeviceData.setCurrent(String.valueOf(totalCurrent));
                for(int j = i + 4; j < (filterData.length - 4); j+=4) {
                    if(mDeviceData.name.equals(String.valueOf(Integer.parseUnsignedInt(filterData[j], 16)))) {
                        //mDeviceData.setDuplicate(1);
                        break;
                    }
                }
            }
//            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

}