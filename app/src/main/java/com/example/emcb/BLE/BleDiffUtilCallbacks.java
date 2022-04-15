package com.example.emcb.BLE;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;

public class BleDiffUtilCallbacks extends DiffUtil.Callback {
    private ArrayList<DeviceData> oldList;
    private ArrayList<DeviceData> newList;

    public BleDiffUtilCallbacks(ArrayList<DeviceData> oldList, ArrayList<DeviceData> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        String oldReader = oldList.get(oldItemPosition).getReader();
        String newReader = newList.get(newItemPosition).getReader();
        String oldName = oldList.get(oldItemPosition).getName();
        String newName = newList.get(oldItemPosition).getName();
        return oldReader.equals(newReader) && oldName.equals(newName);
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        String oldCurrent = oldList.get(oldItemPosition).getCurrent();
        String newCurrent = newList.get(newItemPosition).getCurrent();
        String oldStatus = oldList.get(oldItemPosition).getStatus();
        String newStatus = newList.get(oldItemPosition).getStatus();

        return oldCurrent.equals(newCurrent) && oldStatus.equals(newStatus);
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
