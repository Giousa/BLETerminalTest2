package com.giousa.bleterminaltest.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.giousa.bleterminaltest.UIUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Description:
 * Author:Giousa
 * Date:2017/2/6
 * Email:65489469@qq.com
 */
public class BlueToothLeManager extends Thread implements BluetoothLeTool.BluetoothLeDataListener,
        BluetoothLeTool.BluetoothLeDiscoveredListener,
        BluetoothLeTool.BluetoothLeStatusListener{


    public interface HeartBeatChangedListener {
        void onHeartBeatChanged(int heartBeat);
    }

    private HeartBeatChangedListener mHeartBeatChangedListener;


    public void setHeartBeatChangedListener(HeartBeatChangedListener heartBeatChangedListener) {
        mHeartBeatChangedListener = heartBeatChangedListener;
    }

    private final String TAG = BlueToothLeManager.class.getSimpleName();
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private ArrayList<BluetoothDevice> mLeDevices;
    private Map<String, BluetoothDevice> mDeviceMap = new HashMap<>();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothLeTool mBluetoothTool;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    private int mCharProp;
    private String mDeviceName;

    public BlueToothLeManager(Context pContext) {
        mContext = pContext;
    }


    public void initBlueToothInfo() {
        blueToothInit();
        scanLeDevice(true);
    }

    private boolean blueToothInit() {

        mHandler = new Handler();
        mLeDevices = new ArrayList<>();

        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        Log.i(TAG, "mBluetoothAdapter=" + String.valueOf(mBluetoothAdapter));

        if (mBluetoothAdapter == null) {
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                if (mContext instanceof Activity)
                    ((Activity) mContext).startActivityForResult(enableBtIntent, 1);
            }
        }

        return true;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    UIUtils.runInMainThread(new Runnable() {
                        @Override
                        public void run() {
                            String deviceName = device.getName();

                            if (deviceName == null) {
                                return;
                            }

                            if (!mDeviceMap.containsKey(deviceName)) {
                                mDeviceMap.put(deviceName, device);

                                if (mDeviceName.equals(deviceName)) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    connectDevice();
                                }
                            }
                        }
                    });
                }
            };

    public boolean connectDevice() {

        mBluetoothDevice = mDeviceMap.get(mDeviceName);

        if (mBluetoothDevice == null) {
            return false;
        }

        Log.d(TAG + "蓝牙设备名称：", String.valueOf(mBluetoothDevice.getName()) + "***");
        Log.d(TAG + "蓝牙设备名称：", "***" + mBluetoothDevice.getName());

        mBluetoothTool = new BluetoothLeTool();
        if (!mBluetoothTool.initialize()) {
            Log.i(TAG, "Unable to initialize Bluetooth");
            return false;
        }

        mBluetoothTool.connect(mBluetoothDevice.getAddress());
        mBluetoothTool.setBluetoothLeDataListener(this);
        mBluetoothTool.setBluetoothLeDiscoveredListener(this);
        mBluetoothTool.setBluetoothLeStatusListener(this);

        if (mBluetoothTool != null) {
            final boolean result = mBluetoothTool.connect(mBluetoothDevice.getAddress());
            Log.i(TAG, "Connect request result=" + result);
            return result;
        }
        return false;
    }

    /**
     * 断开连接
     */
    public void disconnectDevice() {
        if (mBluetoothDevice != null){
            mBluetoothTool.disconnect();
            mBluetoothTool = null;
        }
    }

    public void sendData(int value) {

        if(mBluetoothTool == null && mBluetoothGattCharacteristic == null){
            Toast.makeText(UIUtils.getContext(),"蓝牙未连接",Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothTool.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
        mBluetoothGattCharacteristic.setValue(""+value);
        mBluetoothTool.writeCharacteristic(mBluetoothGattCharacteristic);
    }

    private void setCharacteristicNotification() {
        mBluetoothTool.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
    }

    /**
     * 解析数据
     * @param value
     */
    public void onDataAvailable(byte[] value) {

        byte b = value[0];
        Log.d(TAG,"getdata byte-------"+b);
        if (mHeartBeatChangedListener != null) {
            mHeartBeatChangedListener.onHeartBeatChanged(b);
        }
    }


    public void onConnected() {
        Log.d(TAG, "****ACTION_GATT_CONNECTED********");
    }

    public void onDisconnected() {
        Log.d(TAG, "*********ACTION_GATT_DISCONNECTED***********");
    }

    public void onDiscovered(List<BluetoothGattService> supportedService) {
        List<BluetoothGattService> mGattServices = supportedService;

        for (int i = 0; i < mGattServices.size(); i++) {
            Log.d(TAG, "**********" + mGattServices.get(i).getUuid().toString());
            if (mGattServices.get(i).getUuid().toString().equals("0000ffe0-0000-1000-8000-00805f9b34fb")) {
                BluetoothGattService mBluetoothGattService = mGattServices.get(i);
                List<BluetoothGattCharacteristic> ListBlueChar = mBluetoothGattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : ListBlueChar) {
                    Log.d(TAG, "--------------" + characteristic.getProperties() + "");
                    if (characteristic != null) {
                        mBluetoothGattCharacteristic = characteristic;
                        setCharacteristicNotification();
                        mCharProp = characteristic.getProperties();
                    }
                }
            }
        }
    }

    public void setDeviceName(String name) {
        mDeviceName = name;
    }

}
