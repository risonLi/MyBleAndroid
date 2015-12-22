package song.myble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 2015/12/11.
 */
public class BluetoothLeService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private BluetoothManager bluetoothManager;

    private int mConnectionState = STATE_DISCONNECTED;

    //连接状态
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    //用于发送广播报文（作为广播的唯一标识符）
    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "STATE_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";

    private static final String TAG = "song";

    //HEART_RATE_MEASUREMENT这个UUID为蓝牙公用的心跳检测UUID，类似于Socket连接的心跳包
    private static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static final UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(HEART_RATE_MEASUREMENT);

    /**
     * 初始化服务
     * @return
     */
    public boolean initialize(){
        if (bluetoothManager == null){
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (bluetoothManager == null){
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null)
            return false;

        return true;
    }

    /**
     * 连接BLE设备
     * @param address BLE设备的MAC地址
     * @return 是否连接成功
     */
    public boolean connect(final String address){

        if (bluetoothAdapter == null || address == null){
            Log.i(TAG, "bluetoothAdapter未初始化，或者address未获得");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && bluetoothGatt != null){
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;   //连接BLE，成功返回true
            }else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null){
            Log.i(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        //获取bluetoothGatt，从而实现蓝牙的控制
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);

        mBluetoothDeviceAddress = address;

        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * 断开蓝牙连接
     */
    public void disconnect(){
        if (bluetoothAdapter == null || bluetoothGatt == null){
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * 回调函数
     * bluetoothGatt可以操作该回调函数中的方法
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        //连接蓝牙
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            //如果状态为已经连接
            if (newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

                bluetoothGatt.discoverServices();   //搜索该蓝牙所有的服务
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){ //如果未连接
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_CONNECTING;

                mBluetoothDeviceAddress = null;
                bluetoothGatt = null;

                broadcastUpdate(intentAction);
            }
        }

        //搜索服务
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else
                Log.i(TAG, "onServicesDiscovered received: " + status);
        }

        //读取数据
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.i("song", "onCharacteristicRead读取数据中");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        //写入数据，用于发送给蓝牙设备
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                Log.i(TAG, "onCharacteristicWrite status：" + status + " 写入成功");
            }else {
                Log.i(TAG, "onCharacteristicWrite status：" + status + " 写入失败");
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

    };

    /**
     * 发送广播更新数据信息
     * @param action
     */
    private void broadcastUpdate(final String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }
    //?????????????????????????????????????????????????????????????????????
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);

        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())){
            int flage = characteristic.getProperties();
            int format = -1;
            if ((flage & 0x01) != 0){   //用于判断编码格式，16进制和8进制
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            }else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            //将数据放入intent，用于发送广播
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }else {
            final byte[] data = characteristic.getValue();

            if (data != null && data.length > 0){
                StringBuffer sb = new StringBuffer(data.length);
                String sTemp;
                for (int i = 0; i < data.length; i++) {
//                    Log.i("song", i + "收到的原始数据为：" + data[i]);
                    sTemp = Integer.toHexString(0xFF & data[i]);
                    if (sTemp.length() < 2)
                        sb.append(0);
                    sb.append(sTemp.toUpperCase());
//                    Log.i("ble", "sb:" + sb.toString());
                }
                intent.putExtra(EXTRA_DATA, sb.toString());
            }

            /*//将二进制数组转换为string字符串
            if (data != null && data.length > 0){
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data){
                    //这里的%x指将数据转换为16进制
                    stringBuilder.append(String.format("%x ", byteChar));
                }
                //将数据放入intent，用于发送广播
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }*/
        }
        sendBroadcast(intent); //发送广播
    }

    /**
     * LocalBinder类和iBinder方法可以保证其他地方获取的都是同一个BluetoothLeService
     */
    public class LocalBinder extends Binder{
        BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }
    private final IBinder iBinder = new LocalBinder();

    //启动和绑定Service
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    //关闭和解绑Service
    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    //关闭bluetoothGatt连接
    public void close(){
        if (bluetoothGatt == null){
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * 读取数据
     * @param characteristic 指定读取的接口
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        if (bluetoothAdapter == null || bluetoothGatt == null)
            return;

        //bluetoothGatt利用回调函数进行数据读取
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * 设置characteristic接口是否接收数据（接收蓝牙传递过来的数据）
     * @param characteristic
     * @param enable 是否接收数据
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enable){
        if (bluetoothAdapter == null || bluetoothGatt == null)
            return;

        //????????????????????????????????????????????????????????????????
        bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
               UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null){
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * 发送数据给蓝牙模块
     * @param characteristic 蓝牙模块的接口
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic){
        if (bluetoothAdapter == null || bluetoothGatt == null)
            return;

        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public List<BluetoothGattService> getSupportedGattServices(){
        if (bluetoothGatt == null)
            return null;

        return bluetoothGatt.getServices();
    }
}
