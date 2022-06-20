package com.example.emcb.BLE;

public class DeviceGatt {

    public static String DEVICE_CUSTOM_SERVICE = "36a125be-49c7-462f-8e34-8d5b9dc883ea";
    public static String DEVICE_BLE_SEND = "44122a22-fc66-48e6-92c8-7d02cc9d16fd";
    public static String DEVICE_BLE_RECEIVE = "3dad49ff-61f8-4c7c-a474-74ad70b7c81a";
    public static String DEVICE_EEP_DATA = "6a26d1e2-e8b5-438a-b9f7-a470cdc58ff2";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static int EEP_COMMAND = 65;
    public static int ON_COMMAND = 241;
    public static int OFF_COMMAND = 242;
    public static int SELECT_COMMAND = 248;
    public static int UPDATE_CURRENT_COMMAND = 250;
    public static int UPDATE_CUTOFF_COMMAND = 251;

    public static int STATUS_ON = 16;
    public static int STATUS_OFF = 0;
    public static int STATUS_SELECT = 8;
    public static int STATUS_ONOFF = 16;

}
