package com.example.ble_scoreboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.ble_scoreboard.utils.BLECommandUtil;

import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class ControlPanelActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_SCAN = 3;

    private static final String TAG = "BLE_HM10";

    // HM-10 UUIDs
    private static final UUID HM10_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothLeScanner bluetoothLeScanner;
    private BLECommandUtil bleCommandUtil;

    private TextView tv_result;
    private Button btn_connect;

    private String currentDeviceAddress;
    private String currentDeviceName;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        // Initialize TextView and Buttons
        initializeViews();

        // Load device address from preferences
        loadDeviceAddress();

        // Initialize Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            tv_result.setText("Bluetooth is OFF. Please enable it.");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Request location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_FINE_LOCATION);
        }

        // Connect to BLE on button click
        btn_connect.setOnClickListener(v -> {
            if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
                tv_result.setText("Scanning for BLE device: " +
                        (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName
                                : currentDeviceAddress));
                startScan();
            } else {
                tv_result.setText("No device address configured. Please go to Settings.");
            }
        });
    }

    private void loadDeviceAddress() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentDeviceAddress = prefs.getString("deviceAddress1", "");
        currentDeviceName = prefs.getString("deviceName1", "");

        // Update button text
        if (currentDeviceName != null && !currentDeviceName.isEmpty()) {
            btn_connect.setText("Connect to " + currentDeviceName);
        } else if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
            btn_connect.setText("Connect to " + currentDeviceAddress);
        } else {
            btn_connect.setText("No Device Configured");
            btn_connect.setEnabled(false);
        }
    }

    private void initializeViews() {
        tv_result = findViewById(R.id.tv_result);
        btn_connect = findViewById(R.id.btn_connect);

        // Game Control Buttons
        Button btnNewGame = findViewById(R.id.btn_new_game);
        Button btnHorn = findViewById(R.id.btn_horn);
        Button btnShotclock14 = findViewById(R.id.btn_shotclock_14);
        Button btnShotclock24 = findViewById(R.id.btn_shotclock_24);
        Button btnArrowLeft = findViewById(R.id.btn_arrow_left);
        Button btnArrowRight = findViewById(R.id.btn_arrow_right);

        // Team A Control Buttons
        Button btnTeamAScorePlus1 = findViewById(R.id.btn_team_a_score_plus_1);
        Button btnTeamAScorePlus2 = findViewById(R.id.btn_team_a_score_plus_2);
        Button btnTeamAScoreMinus1 = findViewById(R.id.btn_team_a_score_minus_1);
        Button btnTeamAFoulPlus1 = findViewById(R.id.btn_team_a_foul_plus_1);
        Button btnTeamAFoulMinus1 = findViewById(R.id.btn_team_a_foul_minus_1);
        Button btnTeamATolPlus1 = findViewById(R.id.btn_team_a_tol_plus_1);
        Button btnTeamATolMinus1 = findViewById(R.id.btn_team_a_tol_minus_1);

        // Team B Control Buttons
        Button btnTeamBScorePlus1 = findViewById(R.id.btn_team_b_score_plus_1);
        Button btnTeamBScorePlus2 = findViewById(R.id.btn_team_b_score_plus_2);
        Button btnTeamBScoreMinus1 = findViewById(R.id.btn_team_b_score_minus_1);
        Button btnTeamBFoulPlus1 = findViewById(R.id.btn_team_b_foul_plus_1);
        Button btnTeamBFoulMinus1 = findViewById(R.id.btn_team_b_foul_minus_1);
        Button btnTeamBTolPlus1 = findViewById(R.id.btn_team_b_tol_plus_1);
        Button btnTeamBTolMinus1 = findViewById(R.id.btn_team_b_tol_minus_1);

        // Null command
        Button btnNull = findViewById(R.id.btn_null);

        // Set click listeners for game control buttons
        btnNewGame.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_NEW_GAME));
        btnHorn.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_GAMETIME_SHOTCLOCK_HORN));
        btnShotclock14.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_SHOTCLOCK_RESET_14));
        btnShotclock24.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_SHOTCLOCK_RESET_24));
        btnArrowLeft.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_LEFT_ARROW));
        btnArrowRight.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_RIGHT_ARROW));

        // Set click listeners for Team A buttons
        btnTeamAScorePlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_SCORE_PLUS_1));
        btnTeamAScorePlus2.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_SCORE_PLUS_2));
        btnTeamAScoreMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_SCORE_MINUS_1));
        btnTeamAFoulPlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_FOUL_PLUS_1));
        btnTeamAFoulMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_FOUL_MINUS_1));
        btnTeamATolPlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_TOL_PLUS_1));
        btnTeamATolMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_A_TOL_MINUS_1));

        // Set click listeners for Team B buttons
        btnTeamBScorePlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_SCORE_PLUS_1));
        btnTeamBScorePlus2.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_SCORE_PLUS_2));
        btnTeamBScoreMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_SCORE_MINUS_1));
        btnTeamBFoulPlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_FOUL_PLUS_1));
        btnTeamBFoulMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_FOUL_MINUS_1));
        btnTeamBTolPlus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_TOL_PLUS_1));
        btnTeamBTolMinus1.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_TEAM_B_TOL_MINUS_1));

        // Set click listener for null command
        btnNull.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_NULL));
    }

    private void sendCommandIfConnected(byte commandByte) {
        if (bleCommandUtil != null) {
            bleCommandUtil.processCommand(commandByte);
        } else {
            tv_result.setText("Not connected to BLE device");
        }
    }

    private void startScan() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            tv_result.setText("Location permission required for BLE scan.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Bluetooth scan
                startScan();
            } else {
                tv_result.setText("Bluetooth scan permission required.");
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getAddress().equals(currentDeviceAddress)) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothLeScanner.stopScan(scanCallback);
                tv_result.setText("Connecting to device...");
                connectToDevice(device);
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
        } else {
            tv_result.setText("Bluetooth connect permission required.");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected to device, discovering services...");
                runOnUiThread(() -> tv_result.setText("Connected! Discovering services..."));
                if (ContextCompat.checkSelfPermission(ControlPanelActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from device");
                bleCommandUtil = null;
                runOnUiThread(() -> tv_result.setText("Disconnected from device"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered!");
                for (BluetoothGattService service : gatt.getServices()) {
                    if (service.getUuid().equals(HM10_SERVICE_UUID)) {
                        BluetoothGattCharacteristic characteristic = service
                                .getCharacteristic(HM10_CHARACTERISTIC_UUID);
                        if (characteristic != null) {
                            Log.d(TAG, "HM-10 characteristic found!");
                            runOnUiThread(() -> {
                                tv_result.setText("Device characteristic found! Ready to send commands.");
                                bleCommandUtil = new BLECommandUtil(bluetoothGatt, ControlPanelActivity.this,
                                        tv_result);
                            });

                            // Enable notifications
                            if (ContextCompat.checkSelfPermission(ControlPanelActivity.this,
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt.setCharacteristicNotification(characteristic, true);
                            }

                            // Read initial data
                            if (ContextCompat.checkSelfPermission(ControlPanelActivity.this,
                                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                gatt.readCharacteristic(characteristic);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final String receivedData = characteristic.getStringValue(0);
                Log.d(TAG, "Data Received: " + receivedData);
                runOnUiThread(() -> tv_result.setText("Received: " + receivedData));
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Reload device address in case it was changed in settings
        loadDeviceAddress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close GATT client
        if (bluetoothGatt != null) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt.close();
            }
            bluetoothGatt = null;
        }
    }
}
