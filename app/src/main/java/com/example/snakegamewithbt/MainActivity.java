package com.example.snakegamewithbt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "ConnectActivity";


    //BUTTONS
    Button btnBTOn;
    Button btnFindDevices;
    Button btnConnect;
    Button btnPlay;
    Button btnSend;

    //TEXT VIEW
    TextView tvScore;

    //LIST VIEW
    ListView lvDevices;

    //BOOLEAN
    boolean isConnected = false;

    //BLUETOOTH COMPONENTS
    BluetoothAdapter mBluetoothAdapter;
    BluetoothConnectionService mBluetoothConnection;
    public DeviceListAdapter mDeviceListAdapter;

    //ARDUINO UUID
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //LIST TO STORE ALL AVAILABLE DEVICES
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();

    //BLUETOOTH DEVICE WHICH THE PHONE WILL CONNECT TO
    BluetoothDevice mBTDevice;


    //Button btnConnect, btnPlay, btnSend;

   // EditText etTest;

    //boolean isConnected = false;
    String status;

    //UUID
   // static final UUID MY_UUID_INSECURE =
    //        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String score;
    //BluetoothConnectionService mBluetoothConnection;
    //int score = 5;


    //    -----------------------------------------------------------------------------

    // BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    /*
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    /*
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;

                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
        //mBluetoothAdapter.cancelDiscovery();
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //BUTTONS
        btnBTOn = (Button) findViewById(R.id.btnBTOn);
        btnFindDevices = (Button) findViewById(R.id.btnFindDevices);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnSend = (Button) findViewById(R.id.btnSend);

        //TEXT VIEW
        tvScore = (TextView) findViewById(R.id.tvScore);

        //LIST VIEW
        lvDevices = (ListView) findViewById(R.id.lvDevices);

        //LIST TO HOLD FOUND DEVICES
        mBTDevices = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //MAKE ITEMS IN THE LIST CLICKABLE
        lvDevices.setOnItemClickListener(MainActivity.this);

        btnBTOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                turnBTOn();
            }
        });

        btnFindDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findDevices();
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();

            }
        });


        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected == true) {
                    Intent connect = new Intent(MainActivity.this, GameActivity.class);
                    startActivity(connect);
                } else {
                    Toast.makeText(MainActivity.this, "You are not connected to the device", Toast.LENGTH_SHORT).show();
                }

                //connectBT();

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String scoreString = Integer.toString(score);
                String test = "test";
                byte[] bytes = test.getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });

        //btnConnect = (Button) findViewById(R.id.btnConnect);
        //btnPlay = (Button) findViewById(R.id.btnPlay);
        //tvStatus = (TextView) findViewById(R.id.tvStatus);
       // etTest = (EditText) findViewById(R.id.etTest);
        //btnSend = (Button) findViewById(R.id.btnSend);

        Intent getScore = getIntent();
        Bundle b = getScore.getExtras();
        if (b != null) {
            score = (String) b.get("SCORE");
            tvScore.setText(score);
        }


//
//        btnSend.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //String scoreString = Integer.toString(score);
//                byte[] bytes = score.getBytes(Charset.defaultCharset());
//                mBluetoothConnection.write(bytes);
//            }
//        });

    }

//    public void navigateBack() {
//        if(isConnected == true){
//            //Intent back = new Intent(ConnectActivity.this, MainActivity.class);
//            //String status = "Connected!";
//            //back.putExtra("STATUS", isConnected);
//            //startActivity(back);
////            if(BluetoothAdapter.STATE_CONNECTED){
////
////            }
//        }
//    }

    public void startGame(){
        if(isConnected == true){
            Intent game = new Intent(MainActivity.this, GameActivity.class);
            startActivity(game);
        }
    }

    public void startConnection(){

        startBTConnection(mBTDevice,MY_UUID_INSECURE);
        //IF success then set connected to true
        //isConnected = true;
        //navigateBack();
        if(mBTDevice.getName().equals("TEST")){
            System.out.println("Success");
            isConnected = true;
        }

        startGame();
    }

    /*
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }

    public void turnBTOn(){
        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    public void findDevices() {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }

    /*
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            }
            if (permissionCheck != 0) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                }
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }

//    public void sendScore(){
//       // byte[] bytes = etTest.getText().toString().getBytes(Charset.defaultCharset());
//       // mBluetoothConnection.write(bytes);
//        //etTest.setText("");
//    }

//    public void connectBT() {
//
//        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//        System.out.println(btAdapter.getBondedDevices());
//
//        BluetoothDevice arduino = btAdapter.getRemoteDevice("20:16:12:27:53:86");
//        System.out.println(arduino.getName());
//
//        //---SOCKET FOR COMMUNICATION
//        BluetoothSocket btSocket = null;
//        int counter = 0;
//        do {
//            try {
//                btSocket = arduino.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
//                System.out.println(btSocket);
//                //Establish connection
//                btSocket.connect();
//                System.out.println(btSocket.isConnected());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            counter++;
//        } while (!btSocket.isConnected() && counter < 3);
//
//        OutputStream outputStream = null;
//        try {
//            outputStream = btSocket.getOutputStream();
//            //outputStream.write(score.getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        try {
////            InputStream inputStream = btSocket.getInputStream();
////            //Flush the buffer
////            inputStream.skip(inputStream.available());
////            for(int i = 0; i < 26; i++){
////
////                byte b = (byte)inputStream.read();
////                System.out.println((char) b);
////            }
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//
//
//        //---CLOSING THE CONNECTION
//        try {
//            btSocket.close();
//            System.out.println(btSocket.isConnected());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}



