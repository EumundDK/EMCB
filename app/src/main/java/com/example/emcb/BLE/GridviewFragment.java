package com.example.emcb.BLE;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emcb.R;

import java.util.ArrayList;

public class GridviewFragment extends Fragment {

    private static final String DEVICE_DATA_LIST = "device_data_list";
    private static final String HAS_EXTRA_DATA = "has_extra_data";
    private static final String GRID_COUNT = "grid_count";

    private RecyclerView recyclerView;
    private ArrayList<DeviceData> mDeviceDataList = new ArrayList<>();
    private int mGridCount;
    private MyGridViewAdapter myGridViewAdapter;

    public GridviewFragment() {
        // Required empty public constructor
    }

    public static GridviewFragment newInstance(ArrayList<DeviceData> deviceDataList, int gridCount) {
        GridviewFragment fragment = new GridviewFragment();
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
        myGridViewAdapter = new MyGridViewAdapter(getContext(), mDeviceDataList);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), mGridCount));
        recyclerView.setAdapter(myGridViewAdapter);

        return view;
    }

    public void updateDeviceData(ArrayList<DeviceData> newDeviceDataList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BleDiffUtilCallbacks(mDeviceDataList, newDeviceDataList));
        mDeviceDataList = newDeviceDataList;
        diffResult.dispatchUpdatesTo(myGridViewAdapter);

    }
}