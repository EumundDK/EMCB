package com.example.emcb.BLE;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class DeviceData implements Parcelable {
    //rawData for GridView
    String reader;
    String name;
    String status;
    String current;
    int duplicate;
    //extra rawData for ListView
    double currentSetting;
    int cutoffPeriod;
    int onOffSetting;
    int autoReconnect;
    String ownerName;

    public DeviceData() {

    }

    public DeviceData(Parcel source) {
        reader = source.readString();
        name = source.readString();
        status = source.readString();
        current = source.readString();
        duplicate = source.readInt();

        currentSetting = source.readDouble();
        cutoffPeriod = source.readInt();
        onOffSetting = source.readInt();
        autoReconnect = source.readInt();
        ownerName = source.readString();
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCurrent(String current) {
        this.current = current;
    }

    public void setDuplicate(int duplicate) {
        this.duplicate = duplicate;
    }

    public void setCurrentSetting(double currentSetting) {
        this.currentSetting = currentSetting;
    }

    public void setCutoffPeriod(int cutoffPeriod) {
        this.cutoffPeriod = cutoffPeriod;
    }

    public void setOnOffSetting(int onOffSetting) {
        this.onOffSetting = onOffSetting;
    }

    public void setAutoReconnect(int autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getReader() {
        return reader;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrent() {
        return current;
    }

    public int getDuplicate() {
        return duplicate;
    }

    public double getCurrentSetting() {
        return currentSetting;
    }

    public int getCutoffPeriod() {
        return cutoffPeriod;
    }

    public int getOnOffSetting() {
        return onOffSetting;
    }

    public int getAutoReconnect() {
        return autoReconnect;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel data, int i) {
        data.writeString(reader);
        data.writeString(name);
        data.writeString(status);
        data.writeString(current);
        data.writeInt(duplicate);

        data.writeDouble(currentSetting);
        data.writeInt(cutoffPeriod);
        data.writeInt(onOffSetting);
        data.writeInt(autoReconnect);
        data.writeString(ownerName);
    }

    public static final Creator<DeviceData> CREATOR = new Creator<DeviceData>() {

        @Override
        public DeviceData createFromParcel(Parcel source) {
            return new DeviceData(source);
        }

        @Override
        public DeviceData[] newArray(int size) {
            return new DeviceData[size];
        }
    };
}
