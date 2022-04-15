package com.example.emcb.BLE;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emcb.R;

import java.util.ArrayList;


public class ListviewFragment extends Fragment {

    private static final String DEVICE_DATA_LIST = "device_data_list";
    private static final String GRID_COUNT = "grid_count";

    private RecyclerView recyclerView;
    private ArrayList<DeviceData> mDeviceDataList;
    private int mGridCount;
    private MyAllViewAdapter myListViewAdapter;

    public ListviewFragment() {
        // Required empty public constructor
    }

    public static ListviewFragment newInstance(ArrayList<DeviceData> deviceDataList, int gridCount) {
        ListviewFragment fragment = new ListviewFragment();
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
        View view = inflater.inflate(R.layout.fragment_listview, container, false);
        recyclerView = view.findViewById(R.id.gridRecyclerView);
        myListViewAdapter = new MyAllViewAdapter(getContext(), mDeviceDataList);
        recyclerView.setLayoutManager(new LinearLayoutManager((getActivity())));
        recyclerView.setAdapter(myListViewAdapter);
        return view;
    }

    public void updateTabData(int position) {
        myListViewAdapter.updateDeviceData(position);
    }
}