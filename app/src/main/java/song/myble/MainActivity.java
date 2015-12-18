package song.myble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;

    private BluetoothDevice myDevice = null;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000; //扫描10秒

    private boolean mConnected = false;
    private Thread myConnectThread;

    /*private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();*/
    private List<BluetoothGattService> mGattServices;

    private Button btn_enter;
    private EditText et_enterData;
    private TextView tv_showData;

    public static String OBD_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb";
    public static String OBD_READ = "0000fff1-0000-1000-8000-00805f9b34fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_enter = (Button) findViewById(R.id.btn_enter);
        et_enterData = (EditText) findViewById(R.id.et_enterData);
        tv_showData = (TextView) findViewById(R.id.tv_showData);

        handler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "您的设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //开启蓝牙
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        myConnectThread = new Thread(){
            @Override
            public void run() {
                try {
                    while (!mConnected){
                        scanBleDevice(true);    //开始扫描
                        sleep(60000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        myConnectThread.start();

        //字符串测试
        /*String sd = "438001123";
        if (sd.substring(2, 6).equals("8001"))
            Log.i("song", "进入了！");
        String kk = sd.substring(0, 4);
        Log.i("song", "KK:" + kk);
        if (kk.equals("4380")){
            Log.i("song", "if这里是4380");
        }
        if (kk == "4380"){ //该判断无法成立
            Log.i("song", "if==这里是4380");
        }
        switch (kk){
            case "4380":
                Log.i("song", "switch这里是4380");
                break;
        }
        Log.i("song", "valueOf:" + sd.substring(2, 6) + "  sd的长度为：" + sd.length() + "最后一位：" + sd.substring(8,9));*/
    }

    private void startServiceAndBroadCast(){
        //启动BluetoothLeService
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //注册广播
        registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());

        if (bluetoothLeService != null){
            final boolean result = bluetoothLeService.connect(myDevice.getAddress());
            Log.i("song", "bluetoothLeService服务连接：" + result);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnected)
            unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnected){
            unbindService(mServiceConnection);
            bluetoothLeService = null;
        }
        mConnected = true;  //起到终止线程的作用
    }

    /**
     * 扫描蓝牙设备
     * @param enable 控制扫描开关
     */
    private void scanBleDevice(final boolean enable){
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    mScanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);    //延迟10秒后执行

//            mScanning = true;
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }else {
//            mScanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 蓝牙扫描回调接口
     * startLeScan等会调用该函数
     * 扫描到的蓝牙信息可以在onLeScan方法中获得
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        //这里的bluetoothDevice就是扫描到的蓝牙设备的device
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("song", "扫描到的BLE：" + bluetoothDevice.getName());
                    if (bluetoothDevice.getName().equals("BLE to UART_2")){
                        myDevice = bluetoothDevice;
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
                        startServiceAndBroadCast();
                    }
                }
            });
        }
    };

    /**
     * 用于管理bindService的生命周期（这里是管理BluetoothLeService）
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        //连接服务
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //获取服务
            bluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();

            if (!bluetoothLeService.initialize()){
                finish();
            }

            //连接服务
            if (myDevice != null){
                bluetoothLeService.connect(myDevice.getAddress());
            }

        }

        //断开服务
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService.disconnect();
            bluetoothLeService = null;
        }
    };

    /**
     * 自定义广播
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            switch (action){
                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    mConnected = true;
                    Log.i("song", "蓝牙已经连接");
                    break;
                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    mConnected = false;
                    Log.i("song", "蓝牙未连接");
                    break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    mGattServices = bluetoothLeService.getSupportedGattServices();
                    Log.i("song", "获取蓝牙服务");
                    readBleData();
                    Log.i("song", "onServiceConnected开始读取数据");
                    break;
                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    //获取广播传递过来的数据
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    break;
            }
        }
    };

    /**
     * 添加自定义的广播名称（相当与现实中广播的频道）
     * 还有很多系统自带的广播名称
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //显示接收到的数据
    private void displayData(String data){
        if (data != null){
            Log.i("song", "获取到的数据：" + data);
            decodeBleData(data);
            tv_showData.setText(data);
        }
    }

    private void readBleData(){
        if (mConnected){
            for (BluetoothGattService gattService : mGattServices){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics){
                    if (OBD_READ.equals(characteristic.getUuid().toString())){
                        Log.i("song", "readBleData开始读取指定UUID的数据");
                        bluetoothLeService.readCharacteristic(characteristic);
                        bluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                }
            }
        }
    }

    /**
     * 发送数据给蓝牙设备
     * @param v
     */
    public void SendButtonTapped(View v){
        if (mConnected){
            byte[] send = hex2ByteArray(et_enterData.getText().toString().toUpperCase(Locale.ENGLISH));
//            byte[] send = 55AA0004800401010D0A;
            for (BluetoothGattService gattService : mGattServices){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics){
                    if (OBD_WRITE.equals(characteristic.getUuid().toString())){
                        //发送数据给蓝牙模块
                        characteristic.setValue(send);
                        bluetoothLeService.writeCharacteristic(characteristic);
                    }
                }
            }
        }
    }

    /**
     * 将字符串转换为16进制数
     * @param hexString
     * @return
     */
    public byte[] hex2ByteArray(String hexString){
        String hexVal = "0123456789ABCDEF"; //OBD里定义的开启蓝牙数据发送通信协议

        float StringLength = (hexString.length()/2.0f);
        byte[] out;

        if (StringLength == (int)StringLength) {
            out = new byte[hexString.length() / 2];
        }else {
            out = new byte[hexString.length() / 2 + 1];
        }

        int n = hexString.length();

        int hn = 0;
        int ln = 0;

        for (int i=0; i<n; i+=2){
            //make a bit representation in an int of the hex value
            if((n-i)<2 ) {
                hn = hexVal.indexOf( hexString.charAt( i ) );
                out[i/2] = (byte)(hn);
            }
            else {
                hn = hexVal.indexOf( hexString.charAt( i ) );
                ln = hexVal.indexOf( hexString.charAt( i + 1 ) );
                out[i/2] = (byte)( ( hn << 4 ) | ln );
            }
            //now just shift the high order nibble and add them together
        }

        return out;
    }

    /**
     * 处理蓝牙传递过来的数据：16进制字符串，进行组装
     * @param data
     */
//    StringBuffer bleDatas = null;
    String bleDatas = "";
    private void decodeBleData(String data){

        int dLong = data.length(); //获取该字符串的长度
        if (dLong>=4){
            if (data.substring(dLong-4, dLong).equals("0D0A")){
                if (data.substring(0, 4).equals("55AA")){
                    decodeData(data);
                    bleDatas = "";
                }else {
                    bleDatas += data;
                    decodeData(bleDatas);
                    bleDatas = "";
                }
            }else {
                bleDatas += data;
            }
        }else {
            bleDatas += data;
            decodeData(bleDatas);
            bleDatas = "";
        }
        /*if (dLong > 0){
            String dHead = data.substring(0, 3); //取出头部的4位数
            String dEnd = data.substring(dLong-4, dLong); //取出尾部4位数

            if (dHead.equals("55AA")){ //该组字符串的前四位为数据头部
                String dLength = data.substring(4, 7); //获取该组数据的长度
//                int length = Integer.parseInt(dLength, 16); //将16进制字符串转换为long型
                String dtype = data.substring(8, 11); //获取传递过来的数据的类型（GPS/车况/车速）
                int typeNum = Integer.parseInt(dtype, 16);


                bleDatas.setLength(0); //清空全局字符串
            }else { //该组数可能是前面一组的后续部分

            }
            if (dEnd.equals("0D0A")){   //如果最后的4位不是0D0A
                endData = data.substring(dLong-2, dLong); //保存该组数据的最后两位，以防下一组数据只有两个结束符0A

            }else {

            }

        }else {
            Log.i("song", "没有数据！");
        }*/

    }

    /**
     * 解析数据中的数据
     * 命令吗（车况/车速/GPS/系统状态）
     * @param data 数据
     *             根据通信协议剔除掉头和尾
     *             先取出其中的数据部分，将数据转换为10进制数
     */
    private void decodeData(String data){

        Log.i("song", "整条数据：" + data);

        int longD = data.length();
        String length = data.substring(4, 8); //数据部分的长度（命令码+内容+校验码）
        String order = data.substring(8, 12); //获取命令码4位
        String dataD = data.substring(12, longD-5); //数据N位
        byte[] myData = hexStringToByte(dataD); //获取的数据
        String checkCode = data.substring(longD - 5, longD - 3); //校验码2位
//        int lengthNum = Integer.valueOf(length, 16);
//        int orderNum = Integer.valueOf(order, 16); //例如“F4”->0xF4

        /*for (byte testb : myData){
            Log.i("song", "myData里的数据：" + (testb&0xFF));
        }*/

        switch (order){
            case "8001":
                Log.i("song", "电瓶电压：" + (myData[0]&0xFF)*0.1 + "V");
                Log.i("song", "发动机转速：" + ((myData[1]&0xFF)*256 + (myData[2]&0xFF)));
                Log.i("song", "车速：" + ((myData[3]&0xFF)) + "km/h");
                Log.i("song", "冷却液温度：" + ((myData[4]&0xFF) - 40) + "℃");
                Log.i("song", "油箱压力绝对值：" + (myData[5]&0xFF));
                Log.i("song", "此次点火之后行驶时间：" + ((myData[6]&0xFF)*65535 + (myData[7]&0xFF)*256 + (myData[8]&0xFF)) + "s");
                Log.i("song", "此次点火之后行驶里程：" + ((myData[9]&0xFF)*65536 +(myData[10]&0xFF)*256 +(myData[11]&0xFF)) + "m");
                Log.i("song", "此次点火之后燃油消耗：" + ((myData[12]&0xFF)*256 +(myData[13]&0xFF))*0.001 + "L");
                Log.i("song", "瞬时油耗标识：" + Integer.toHexString(myData[14] & 0xFF));
                Log.i("song", "瞬时油耗：" + ((myData[15]&0xFF)*256+(myData[16]&0xFF))*0.01 + "L/100KM");
                Log.i("song", "剩余油量：" + ((myData[17]&0xFF)*256+(myData[18]&0xFF))*0.001 + "%");
                Log.i("song", "急加速次数：" + (myData[19]&0xFF));
                Log.i("song", "急减速次数：" + (myData[20]&0xFF));
                Log.i("song", "急转弯次数：" + (myData[21]&0xFF));
                Log.i("song", "故障码数量(最多10个)：" + (myData[22]&0xFF));
                break;
            case "8002":
                Log.i("song", "8002实时车速数据：");
                Log.i("song", "车速：" + (myData[0]&0xFF));
                Log.i("song", "水温：" + (myData[1]&0xFF - 40));
                /*Log.i("song", "车速：" + Integer.parseInt(data.substring(12, 14), 16));
                Log.i("song", "水温：" + (Integer.parseInt(data.substring(14, 16), 16)-40));*/
                break;
            case "8003":
                Log.i("song", "8003系统状态信息打印：");
                break;
            case "8010":
                Log.i("song", "8010GPS状态数据：");
                break;

        }
    }

    /**
     * 将16进制字符串转换为byte数组
     * @param hex
     * @return
     */
    private byte[] hexStringToByte(String hex) {
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }
    private static int toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }
}
