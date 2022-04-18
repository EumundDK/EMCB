package com.example.emcb.BLE;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.emcb.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AllFragment extends Fragment {

    private static final String DEVICE_DATA_LIST = "device_data_list";
    private static final String BLUETOOTH_SERVICE = "bluetooth_service";
    private static final String GRID_COUNT = "grid_count";

    private RecyclerView recyclerView;
    private ArrayList<DeviceData> mDeviceDataList;
    private int mGridCount;
    private MyAllViewAdapter myAllViewAdapter;
    private BluetoothLeService mBluetoothLeService;

    public AllFragment() {
        // Required empty public constructor
    }

    public static AllFragment newInstance(ArrayList<DeviceData> deviceDataList, int gridCount, BluetoothLeService bluetoothLeService) {
        AllFragment fragment = new AllFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(DEVICE_DATA_LIST, deviceDataList);
        args.putInt(GRID_COUNT, gridCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mDeviceDataList = getArguments().getParcelableArrayList(DEVICE_DATA_LIST);
            mGridCount = getArguments().getInt(GRID_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_all, container, false);
        recyclerView = view.findViewById(R.id.gridRecyclerView);
        myAllViewAdapter = new MyAllViewAdapter(getContext(), mDeviceDataList, mBluetoothLeService);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 10));
        recyclerView.setAdapter(myAllViewAdapter);
        return view;
    }

    public void updateTabData(int position) {
       myAllViewAdapter.updateDeviceData(position);
    }

}