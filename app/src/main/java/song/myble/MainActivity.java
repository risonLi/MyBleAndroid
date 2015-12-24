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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private Handler handler, handlerStart;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;

    private BluetoothDevice myDevice;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000; //5秒

    private boolean mConnected = false;
    private boolean getData = false;
    private boolean openOrNot = false;
//    private Thread myConnectThread;

    /*private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();*/
    private List<BluetoothGattService> mGattServices;

    private Button btn_enter, btnSsck, btnGps, btnWlzt, btnSbbm;
    private EditText et_enterData;
    private TextView tv_showData;

    public final String OBD_WRITE2 = "0000fff2-0000-1000-8000-00805f9b34fb";
    public final String OBD_WRITE5 = "0000fff5-0000-1000-8000-00805f9b34fb";
    public final String OBD_READ1 = "0000fff1-0000-1000-8000-00805f9b34fb";
    public final String OBD_READ4 = "0000fff4-0000-1000-8000-00805f9b34fb";

    private final String OPEN_SSCK = "55AA0004800401100D0A";
    private final String CLOSE_SSCK = "55AA0004800400110D0A";
    private final String WLZT = "55AA0004800501100D0A";
    private final String SBBM = "55AA0004800505100D0A";
    private final String OPEN_GPS = "55AA0004800601110D0A";
    private final String CLOSE_GPS = "55AA0004800600100D0A";

    private Intent gattServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_enter = (Button) findViewById(R.id.btn_enter);
        et_enterData = (EditText) findViewById(R.id.et_enterData);
        tv_showData = (TextView) findViewById(R.id.tv_showData);
        btnSsck = (Button) findViewById(R.id.btn_ssck);
        btnGps = (Button) findViewById(R.id.btn_gps);
        btnWlzt = (Button) findViewById(R.id.btn_wlzt);
        btnSbbm = (Button) findViewById(R.id.btn_sbbm);

        handler = new Handler();
        handlerStart = new Handler();

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

        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /*handlerStart.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (bluetoothLeService != null) {
                    final boolean result = bluetoothLeService.connect(myDevice.getAddress());
                    Log.i("song", "bluetoothLeService服务连接：" + result);
                }
            }
        }, SCAN_PERIOD);   //延迟5秒后执行*/


        /*myConnectThread = new Thread(){
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

        myConnectThread.start();*/

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

        btnSsck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] send = hex2ByteArray(OPEN_SSCK.toUpperCase(Locale.ENGLISH));
                senDataToBle(send);
            }
        });

        btnSbbm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] send = hex2ByteArray(SBBM.toUpperCase(Locale.ENGLISH));
                senDataToBle(send);
            }
        });

        btnWlzt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] send = hex2ByteArray(WLZT.toUpperCase(Locale.ENGLISH));
                senDataToBle(send);
            }
        });

        btnGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] send = hex2ByteArray(OPEN_GPS.toUpperCase(Locale.ENGLISH));
                senDataToBle(send);
            }
        });
    }

    private void startServiceAndBroadCast(){
        //启动BluetoothLeService
//            Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        /*if (myDevice != null && bluetoothLeService!= null){
            Log.i("song", "myDevice不为空");
            bluetoothLeService.disconnect();
            bluetoothLeService.connect(myDevice.getAddress());
        }else {*/
//            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//            Log.i("song", "broadcastReceiver创建广播：" + broadcastReceiver);
            /*//注册广播
            registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());*/

            if (bluetoothLeService != null){
                final boolean result = bluetoothLeService.connect(myDevice.getAddress());
                Log.i("song", "bluetoothLeService服务连接：" + result);
            }
//        }
    }

    private void stopServiceAndBroadCast(){
        if (bluetoothLeService != null){
            Log.i("song", "stopServiceAndBroadCast执行了");
            unregisterReceiver(broadcastReceiver);
            unbindService(mServiceConnection);
            bluetoothLeService = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData = false;
        bluetoothAdapter.startLeScan(mLeScanCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnected){
            Log.i("song", "onPause执行了");
            unregisterReceiver(broadcastReceiver);
            unbindService(mServiceConnection);
            bluetoothLeService = null;
        }
        bluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 扫描蓝牙设备
     * @param enable 控制扫描开关
     */
    private void scanBleDevice(final boolean enable){
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            /*handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    mScanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
//                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);    //延迟10秒后执行*/

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
//                    Log.i("song", "myDevice:" + myDevice + "  bluetoothDevice:" + bluetoothDevice);
                    if (bluetoothDevice.getName().equals("BLE to UART_2")){
//                            stopServiceAndBroadCast();
                        Log.i("song", "扫描到的BLE：" + bluetoothDevice.getName());
                        myDevice = bluetoothDevice;
                        bluetoothAdapter.stopLeScan(mLeScanCallback);
//                            stopServiceAndBroadCast();
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
            if (bluetoothLeService == null){
                Log.i("ble", "bluetoothLeService创建了");
                bluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
                //注册广播
                registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());
            }

            if (!bluetoothLeService.initialize()){
                finish();
            }

            //连接服务
            /*if (myDevice != null){
                bluetoothLeService.connect(myDevice.getAddress());
            }*/

        }

        //断开服务
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
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
                    getData = false;
//                    stopServiceAndBroadCast();
                    bluetoothAdapter.startLeScan(mLeScanCallback);

                    /*if (mConnected){
                        bluetoothLeService.connect(myDevice.getAddress());
                    }*/
                    Log.i("song", "蓝牙未连接");
                    break;
                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    mGattServices = bluetoothLeService.getSupportedGattServices();
                    Log.i("song", "获取蓝牙服务");

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mConnected == true && getData == false) {
                                readBleData4();
                            }
                        }
                    }, SCAN_PERIOD);   //延迟10秒后执行
                    readBleData();

//                    Log.i("song", "onServiceConnected开始读取数据");
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
        Log.i("ble", "获取到的数据：" + data);
        if (data != null){
            getData = true;
            decodeBleData(data);
            tv_showData.setText(data);
        }
    }

    private void readBleData(){
        if (mConnected){
            for (BluetoothGattService gattService : mGattServices){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics){
                    if (OBD_READ1.equals(characteristic.getUuid().toString())) {
                        Log.i("song", "readBleData开始读取OBD_READ1的数据");
                        bluetoothLeService.readCharacteristic(characteristic);
//                        if (!openOrNot){
                            bluetoothLeService.setCharacteristicNotification(characteristic, true);
//                            openOrNot = true;
//                        }
                    }
                }
            }
        }
    }

    private void readBleData4(){
        if (mConnected){
            for (BluetoothGattService gattService : mGattServices){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics){
                    if (OBD_READ4.equals(characteristic.getUuid().toString())) {
                        Log.i("song", "readBleData开始读取OBD_READ4的数据");
                        bluetoothLeService.readCharacteristic(characteristic);
//                        if (!openOrNot){
                            bluetoothLeService.setCharacteristicNotification(characteristic, true);
//                            openOrNot = true;
//                        }
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
        byte[] send = hex2ByteArray(et_enterData.getText().toString().toUpperCase(Locale.ENGLISH));
        senDataToBle(send);
    }

    private void senDataToBle(byte[] sendData){
        if (mConnected){
            for (BluetoothGattService gattService : mGattServices){
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : gattCharacteristics){
                    if (OBD_WRITE2.equals(characteristic.getUuid().toString())){
                        Log.i("song", "senDataToBle写入OBD_WRITE2的数据");
                        characteristic.setValue(sendData);
                        bluetoothLeService.writeCharacteristic(characteristic);
                    }else if (OBD_WRITE5.equals(characteristic.getUuid().toString())){
                        Log.i("song", "senDataToBle写入OBD_WRITE5的数据");
                        characteristic.setValue(sendData);
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

        if (mConnected){
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
        }else {
            data = "";
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
        int dataL = data.length();
        if (dataL >= 22){
            String dataD = data.substring(12, dataL - 5); //数据N位
            byte[] myData = hexStringToByte(dataD); //获取的数据
            if (checkNum(data, myData, dataL)){
                String order = data.substring(8, 12); //获取命令码4位
                DecimalFormat mdf = new DecimalFormat("#.##");
                switch (order){
                    case "8001":
                        Log.i("song", "8001实时车况数据：");
                        /*Log.i("song", "电瓶电压：" + (mdf.format((myData[0] & 0xFF) * 0.1) + "V"));
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
                        Log.i("song", "故障码数量(最多10个)：" + (myData[22]&0xFF));*/
                        break;
                    case "8002":
                        Log.i("song", "8002实时车速数据：");
                        /*Log.i("song", "车速：" + (myData[0]&0xFF));
                        Log.i("song", "水温：" + (myData[1]&0xFF - 40));*/
                        break;
                    case "8003":
                        /*String length = data.substring(4, 8); //数据部分的长度（命令码+内容+校验码）
                        if (length.equals("0010")){
                            Log.i("song", "8003系统状态信息打印：网络状态输出");
                            Log.i("song", "系统网络状态：" + (myData[0]&0xFF));
                            Log.i("song", "是否有SIM卡：" + (myData[1]&0xFF));
                            Log.i("song", "是否有IMEI号：" + (myData[2]&0xFF));
                            Log.i("song", "GSM信号强度：" + (myData[3]&0xFF));
                            Log.i("song", "status：" + (myData[4]&0xFF));
                            Log.i("song", "GSM_state ：" + (myData[5]&0xFF));
                            Log.i("song", "GPRS_state ：" + (myData[6]&0xFF));
                            Log.i("song", "GPRS_states ：" + (myData[7]&0xFF));
                            Log.i("song", "lac ：" + "高位：" + (myData[8]&0xFF) + " 低位：" + (myData[9]&0xFF));
                            Log.i("song", "rac ：" + (myData[10]&0xFF));
                            Log.i("song", "Cell_id ：" + "高位：" + (myData[11]&0xFF) + " 低位：" + (myData[12]&0xFF));
                        }else if (length.equals("0018")){
                            Log.i("song", "8003系统状态信息打印：设备编号输出");
                            String imei = "";
                            try {
                                imei = new String(myData,"ASCII");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            *//*for (byte bi : myData){
                                imei +=
                                imei += bi;
                            }*//*
                            Log.i("song", "设备编号：" + imei);
                        }*/
                        break;
                    case "8010":
                        Log.i("song", "8010GPS状态数据：");
//                        if ((myData[]))
                        break;
                }
            }
        }

    }

    //数据长度和校验码检验
    private Boolean checkNum(String data, byte[] myData, int longD){
        String length = data.substring(4, 8); //数据部分的长度（命令码+内容+校验码）
        if (data.length() == ((Integer.valueOf(length,16))*2+12)) { //数据长度检验
            //添加校验码
            String checkNum = data.substring(longD - 6, longD - 4); //获取校验码
            int x = 0;
            for (byte checkB : myData) {
                x += (checkB & 0xFF);
            }
            int check = ((x + 0x80 + Integer.valueOf(data.substring(10, 12), 16) + (Integer.valueOf(length, 16))) ^ 0xFF);
            if (Integer.toHexString(check).length() > 2){
                check = Integer.valueOf((Integer.toHexString(check).substring(1, 3)), 16);
            }
//            Log.i("song", "校验码：" + checkNum + "值为：" + check + " 校验码转换后：" + Integer.toHexString(check));
            if (check == (Integer.valueOf(checkNum, 16))) { //校验码检验
                return true;
            }else
                return false;
        }else
            return false;
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
