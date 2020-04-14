package com.example.snakegamewithbt;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.UUID;


public class ConnectActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
    private static final String TAG = "ConnectActivity";

    //BUTTONS
    Button btnBTOn;
    Button btnFindDevices;
    Button btnConnect;

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

//    -----------------------------------------------------------------------------

    // BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state)
                {
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
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND))
            {
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
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_activity);

        //BUTTONS
        btnBTOn = (Button) findViewById(R.id.btnBTOn);
        btnFindDevices = (Button) findViewById(R.id.btnFindDevices);
        btnConnect = (Button) findViewById(R.id.btnConnect);

        //LIST VIEW
        lvDevices = (ListView) findViewById(R.id.lvDevices);

        //LIST TO HOLD FOUND DEVICES
        mBTDevices = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //MAKE ITEMS IN THE LIST CLICKABLE
        lvDevices.setOnItemClickListener(ConnectActivity.this);

        btnBTOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
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
    }

    //create method for starting connection
//***remember the conncction will fail and app will crash if you haven't paired first

    public void navigateBack()
    {
        if(isConnected == true)
        {
            Intent back = new Intent(ConnectActivity.this, MainActivity.class);
            back.putExtra("STATUS", isConnected);
            startActivity(back);
        }
    }
    public void startConnection()
    {

        startBTConnection(mBTDevice,MY_UUID_INSECURE);
        //IF success then set connected to true
       isConnected = true;
       navigateBack();
    }

    /*
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);

    }

    public void turnBTOn()
    {
        if(mBluetoothAdapter == null)
        {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled())
        {
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    public void findDevices()
    {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering())
        {

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
    private void checkBTPermissions()
    {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            }
            if (permissionCheck != 0)
            {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                                         Manifest.permission.ACCESS_COARSE_LOCATION},
                                                        1001); //Any number
                }
            }
        }
        else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(ConnectActivity.this);
        }
    }
}
