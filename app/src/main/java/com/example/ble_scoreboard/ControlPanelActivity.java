package com.example.ble_scoreboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.ble_scoreboard.utils.BLECommandUtil;
import com.example.ble_scoreboard.utils.BLEManager;
import com.example.ble_scoreboard.utils.ClockManager;

import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class ControlPanelActivity extends AppCompatActivity
        implements BLEManager.ConnectionCallback, ClockManager.ClockUpdateListener {
    private static final int REQUEST_BLUETOOTH_SCAN = 3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    private static final String TAG = "ControlPanelActivity";

    private BLEManager bleManager;
    private ClockManager clockManager;
    private TextView tv_result;
    private Button btn_connect;

    // Clock Preview UI elements
    private TextView tvMin1;
    private TextView tvMin2;
    private TextView tvSec1;
    private TextView tvSec2;
    private TextView tvMSec;
    private TextView tvShot1;
    private TextView tvShot2;
    private TextView tvHornx;

    // Global variables for individual digits as seen in the block image
    private int globalMin1 = 0;
    private int globalMin2 = 0;
    private int globalSec1 = 0;
    private int globalSec2 = 0;
    private int globalMSec = 0;
    private int globalShot1 = 2;
    private int globalShot2 = 4;
    private int globalHornx = 0; // 0 = Off, 1 = On

    private String currentDeviceAddress;
    private String currentDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        // Initialize TextView and Buttons
        initializeViews();

        // Load device address from preferences
        loadDeviceAddress();

        // Get BLE Manager instance
        bleManager = BLEManager.getInstance();
        bleManager.initialize(getApplicationContext());
        bleManager.addConnectionCallback(this);

        // Initialize ClockManager
        clockManager = ClockManager.getInstance();
        clockManager.addClockUpdateListener(this);

        // Initialize global variables from ClockManager
        globalMin1 = clockManager.getMin1();
        globalMin2 = clockManager.getMin2();
        globalSec1 = clockManager.getSec1();
        globalSec2 = clockManager.getSec2();
        globalMSec = clockManager.getMSec();
        globalShot1 = clockManager.getShot1();
        globalShot2 = clockManager.getShot2();

        // Initialize clock display
        updateClockDisplay();

        // Check if Bluetooth is enabled
        if (!bleManager.isBluetoothEnabled()) {
            tv_result.setText("Bluetooth is OFF. Please enable it.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestBluetoothPermission();
            }
            return;
        }

        // Request necessary permissions
        checkAndRequestPermissions();

        // Connect to BLE on button click
        btn_connect.setOnClickListener(v -> {
            if (!bleManager.isDeviceConnected(currentDeviceAddress)) {
                if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
                    tv_result.setText("Connecting to: " +
                            (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName
                                    : currentDeviceAddress));
                    if (checkAndRequestPermissions()) {
                        bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                    }
                } else {
                    tv_result.setText("No device address configured. Please go to Settings.");
                }
            } else {
                tv_result.setText("Disconnecting...");
                bleManager.disconnectDevice(currentDeviceAddress);
                btn_connect.setText(
                        "Connect to " + (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName
                                : currentDeviceAddress));
            }
        });
    }

    private void loadDeviceAddress() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentDeviceAddress = prefs.getString("deviceAddress1", "");
        currentDeviceName = prefs.getString("deviceName1", "");

        // Update button text
        if (currentDeviceName != null && !currentDeviceName.isEmpty()) {
            btn_connect.setText(bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)
                    ? "Disconnect from " + currentDeviceName
                    : "Connect to " + currentDeviceName);
        } else if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
            btn_connect.setText(bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)
                    ? "Disconnect from " + currentDeviceAddress
                    : "Connect to " + currentDeviceAddress);
        } else {
            btn_connect.setText("No Device Configured");
            btn_connect.setEnabled(false);
        }
    }

    private boolean checkAndRequestPermissions() {
        // Check if Bluetooth is enabled
        if (!bleManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestBluetoothPermission();
            }
            return false;
        }

        // Check and request location permissions (required for scanning on Android
        // 6.0+)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_FINE_LOCATION);
            return false;
        }

        // Check Bluetooth connect permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission();
            return false;
        }

        return true;
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    }, REQUEST_BLUETOOTH_SCAN);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    REQUEST_FINE_LOCATION);
        }
    }

    private void initializeViews() {
        tv_result = findViewById(R.id.tv_result);
        btn_connect = findViewById(R.id.btn_connect);

        // Clock Preview UI elements
        tvMin1 = findViewById(R.id.tv_control_min1);
        tvMin2 = findViewById(R.id.tv_control_min2);
        tvSec1 = findViewById(R.id.tv_control_sec1);
        tvSec2 = findViewById(R.id.tv_control_sec2);
        tvMSec = findViewById(R.id.tv_control_msec);
        tvShot1 = findViewById(R.id.tv_control_shot1);
        tvShot2 = findViewById(R.id.tv_control_shot2);
        tvHornx = findViewById(R.id.tv_control_hornx);

        // Game Control Buttons
        Button btnNewGame = findViewById(R.id.btn_new_game);
        Button btnHorn = findViewById(R.id.btn_horn);
        Button btnResync = findViewById(R.id.btn_resync);
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

        // Clock control buttons in preview
        Button btnStartClock = findViewById(R.id.btn_start_clock);
        Button btnStopClock = findViewById(R.id.btn_stop_clock);

        // Set click listeners for game control buttons
        btnNewGame.setOnClickListener(v -> {
            // Execute initialization in a background thread to avoid UI freezing
            new Thread(() -> {
                // First, make sure we clear any previous state
                if (bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)) {
                    // Send reset command first to make sure the scoreboard is in a clean state
                    sendCommandIfConnected(BLECommandUtil.COMMAND_RESET_CLOCK);

                    // Short delay to ensure the reset is processed
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                }

                // Reset the clock to default values
                clockManager.resetToDefaults();

                // Run UI updates on the main thread
                runOnUiThread(() -> {
                    // Update global variables from ClockManager
                    globalMin1 = clockManager.getMin1();
                    globalMin2 = clockManager.getMin2();
                    globalSec1 = clockManager.getSec1();
                    globalSec2 = clockManager.getSec2();
                    globalMSec = clockManager.getMSec();
                    globalShot1 = clockManager.getShot1();
                    globalShot2 = clockManager.getShot2();

                    // Update clock display immediately
                    updateClockDisplay();
                    tv_result.setText("Initializing new game...");
                });

                // Send individual setup commands to ensure the scoreboard is properly
                // initialized
                if (bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)) {
                    try {
                        // First send the new game command
                        sendCommandIfConnected(BLECommandUtil.CMD_NEW_GAME);
                        Thread.sleep(150); // Delay for command to be processed

                        // Force send all digits immediately to setup the initial display
                        forceScoreboardUpdate();
                        Thread.sleep(150); // Delay between commands

                        // Ensure time digits are set correctly with redundant send
                        forceScoreboardUpdate();
                        Thread.sleep(150);

                        // Start the clock locally
                        runOnUiThread(() -> {
                            clockManager.startClock();
                            tv_result.setText("New game started - Clock running in real-time");
                        });

                        // Then send start command to device
                        sendCommandIfConnected(BLECommandUtil.COMMAND_START_CLOCK);
                        Thread.sleep(100);

                        // Force another update to make sure everything is in sync
                        forceScoreboardUpdate();

                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                        runOnUiThread(() -> tv_result.setText("Error during initialization: " + e.getMessage()));
                    }
                } else {
                    // If no device connected, just start the clock locally
                    runOnUiThread(() -> {
                        clockManager.startClock();
                        tv_result.setText("New game started - Clock running (no device connected)");
                    });
                }
            }).start();
        });
        btnHorn.setOnClickListener(v -> sendCommandIfConnected(BLECommandUtil.CMD_GAMETIME_SHOTCLOCK_HORN));
        
        // Resync button - force immediate update of all digits to the scoreboard
        btnResync.setOnClickListener(v -> {
            tv_result.setText("Resyncing scoreboard...");
            forceScoreboardUpdate();
            tv_result.setText("Scoreboard resynced");
        });

        // Set clock control listeners
        btnStartClock.setOnClickListener(v -> {
            clockManager.startClock();
            sendCommandIfConnected(BLECommandUtil.COMMAND_START_CLOCK);
            // Force immediate sync of current clock values
            forceScoreboardUpdate();
            tv_result.setText("Clock started");
        });

        // Track last stop click time for double-tap detection
        final long[] lastStopClickTime = { 0 };
        btnStopClock.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();

            // Check if this is a double tap (within 500ms)
            if (currentTime - lastStopClickTime[0] < 500) {
                // Double tap - reset to defaults
                clockManager.stopClock();
                clockManager.resetToDefaults();

                // Update global variables from ClockManager
                globalMin1 = clockManager.getMin1();
                globalMin2 = clockManager.getMin2();
                globalSec1 = clockManager.getSec1();
                globalSec2 = clockManager.getSec2();
                globalMSec = clockManager.getMSec();
                globalShot1 = clockManager.getShot1();
                globalShot2 = clockManager.getShot2();

                // Update display
                updateClockDisplay();

                // Send reset command and updated values
                sendCommandIfConnected(BLECommandUtil.COMMAND_RESET_CLOCK);
                forceScoreboardUpdate();

                tv_result.setText("Clock reset to defaults");
            } else {
                // Single tap - just stop the clock
                clockManager.stopClock();
                sendCommandIfConnected(BLECommandUtil.COMMAND_STOP_CLOCK);
                // Force immediate sync of stopped values
                forceScoreboardUpdate();
                tv_result.setText("Clock stopped");
            }

            // Update last click time
            lastStopClickTime[0] = currentTime;
        });
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
        if (bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)) {
            if (bleManager.sendCommand(currentDeviceAddress, commandByte)) {
                tv_result.setText("Sent command: " + BLECommandUtil.getCommandDescription(commandByte));
            } else {
                tv_result.setText("Failed to send command");
            }
        } else {
            tv_result.setText("Not connected to BLE device");
        }
    }

    // UUIDs from your block code image
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");

    // Cache these to avoid repeated lookups for better real-time performance
    private BluetoothGattService cachedService = null;
    private BluetoothGattCharacteristic cachedCharacteristic = null;
    private BluetoothGatt cachedGatt = null;
    private long lastStatusUpdateTime = 0; // To limit status updates for better UI performance
    private static final long STATUS_UPDATE_INTERVAL = 500; // Only update status text every 500ms
    private long lastSendTime = 0;
    private static final long MIN_SEND_INTERVAL = 200; // Increased interval to 200ms to reduce BLE traffic
    private String lastSentDigits = ""; // Track last sent value to avoid duplicate sends

    /**
     * Send all individual digit values over BLE as a joined string in real-time
     * This implements the approach shown in your BlocklyApp image with
     * BluetoothLE2.WriteBytes with high-performance optimizations
     */
    /**
     * Send all individual digit values over BLE as a joined string with
     * optimizations to prevent overloading the BLE device
     * This ensures the physical scoreboard is updated with the current time values
     * 
     * @return boolean indicating if the send was successful
     */
    private boolean sendAllDigitsOverBLE() {
        long currentTime = System.currentTimeMillis();

        // Update our rate-limiting timestamp
        lastSendTime = currentTime;

        // Early exit if not connected
        if (bleManager == null || !bleManager.isDeviceConnected(currentDeviceAddress)) {
            if (currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                tv_result.setText("Not connected to BLE device");
                lastStatusUpdateTime = currentTime;
                // Reset cache as connection changed
                cachedCharacteristic = null;
                cachedService = null;
                cachedGatt = null;
            }
            return false;
        }

        try {
            // Get or use cached gatt
            if (cachedGatt == null) {
                cachedGatt = bleManager.getBluetoothGatt(currentDeviceAddress);
                if (cachedGatt == null) {
                    if (currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                        tv_result.setText("Device not connected properly");
                        lastStatusUpdateTime = currentTime;
                    }
                    return false;
                }
            }

            // Get or use cached service
            if (cachedService == null) {
                cachedService = cachedGatt.getService(SERVICE_UUID);
                if (cachedService == null) {
                    if (currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                        tv_result.setText("Service not found");
                        lastStatusUpdateTime = currentTime;
                    }
                    return false;
                }
            }

            // Get or use cached characteristic
            if (cachedCharacteristic == null) {
                cachedCharacteristic = cachedService.getCharacteristic(CHARACTERISTIC_UUID);
                if (cachedCharacteristic == null) {
                    if (currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                        tv_result.setText("Characteristic not found");
                        lastStatusUpdateTime = currentTime;
                    }
                    return false;
                }
            }

            // Join all digit values into a compact string
            // Add check for valid digit range to avoid sending invalid data
            int safeMin1 = Math.min(Math.max(globalMin1, 0), 9);
            int safeMin2 = Math.min(Math.max(globalMin2, 0), 9);
            int safeSec1 = Math.min(Math.max(globalSec1, 0), 9);
            int safeSec2 = Math.min(Math.max(globalSec2, 0), 9);
            int safeMSec = Math.min(Math.max(globalMSec, 0), 9);
            int safeShot1 = Math.min(Math.max(globalShot1, 0), 9);
            int safeShot2 = Math.min(Math.max(globalShot2, 0), 9);
            int safeHornx = Math.min(Math.max(globalHornx, 0), 1);

            // Format for the scoreboard - ensure proper formatting for display
            String joinedDigits = "" + safeMin1 + safeMin2 + safeSec1 +
                    safeSec2 + safeMSec + safeShot1 +
                    safeShot2 + safeHornx;

            // Skip if sending the same data (optimization) - but not during force updates
            boolean isForcedUpdate = (lastSendTime == 0);
            if (!isForcedUpdate && joinedDigits.equals(lastSentDigits)) {
                return true; // No change needed, so consider it a success
            }
            lastSentDigits = joinedDigits;

            // Write the value to the characteristic
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                // Convert to byte array - most efficient way
                byte[] valueBytes = joinedDigits.getBytes();

                // Set value and write in a single operation with additional error handling
                cachedCharacteristic.setValue(valueBytes);
                boolean success = cachedGatt.writeCharacteristic(cachedCharacteristic);

                // Log every update in debug mode for verification
                Log.d(TAG, "Sending to scoreboard: " + joinedDigits + " - Success: " + success);
                
                // Compatibility refresh: Send CMD_NULL after successful digit push to trigger display refresh
                if (success) {
                    // Small delay to ensure digits are processed before the refresh command
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                    
                    // Send null command to trigger display refresh without using horn
                    sendCommandIfConnected(BLECommandUtil.CMD_NULL);
                    Log.d(TAG, "Sent compatibility refresh command");
                }

                // Only update status text occasionally to avoid UI freezing
                if (isForcedUpdate || currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                    if (success) {
                        tv_result.setText("Scoreboard showing: " + safeMin1 + safeMin2 + ":" +
                                safeSec1 + safeSec2 + "." + safeMSec);
                    } else {
                        tv_result.setText("Sync failed, will retry");
                    }
                    lastStatusUpdateTime = currentTime;
                }

                // Return success status
                return success;
            }
            // If we get here, we don't have required permissions
            return false;
        } catch (Exception e) {
            // Catch any unexpected exceptions to prevent app crashes
            Log.e(TAG, "Error in sendAllDigitsOverBLE: " + e.getMessage(), e);
            if (currentTime - lastStatusUpdateTime > STATUS_UPDATE_INTERVAL) {
                tv_result.setText("BLE error: " + e.getMessage());
                lastStatusUpdateTime = currentTime;
            }
            return false;
        } finally {
            // If horn status is on, reset it after sending
            if (globalHornx == 1) {
                globalHornx = 0;
                updateClockDisplay();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_SCAN || requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with connection
                if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
                    tv_result.setText("Connecting to " + currentDeviceName + "...");
                    bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                }
            } else {
                tv_result.setText("Bluetooth permission required.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled, proceed with connection
                if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
                    tv_result.setText("Connecting to " + currentDeviceName + "...");
                    if (checkAndRequestPermissions()) {
                        bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                    }
                }
            } else {
                // User declined to enable Bluetooth
                tv_result.setText("Bluetooth is required for device connection");
            }
        }
    }

    // BLEManager.ConnectionCallback Implementation
    @Override
    public void onDeviceConnected(String address, String name) {
        if (address.equals(currentDeviceAddress)) {
            runOnUiThread(() -> {
                tv_result.setText("Connected to " + name);
                btn_connect.setText("Disconnect from " + name);
            });
        }
    }

    @Override
    public void onDeviceDisconnected(String address) {
        if (address.equals(currentDeviceAddress)) {
            runOnUiThread(() -> {
                tv_result.setText("Disconnected from device");
                btn_connect.setText("Connect to "
                        + (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName : address));
            });
        }
    }

    @Override
    public void onConnectionError(String address, int status) {
        if (address.equals(currentDeviceAddress)) {
            runOnUiThread(() -> {
                tv_result.setText("Connection error: " + status);
                btn_connect.setText("Connect to "
                        + (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName : address));
            });
        }
    }

    @Override
    public void onScanComplete() {
        runOnUiThread(() -> {
            // If not connected after scan completed, update UI
            if (!bleManager.isDeviceConnected(currentDeviceAddress)) {
                tv_result.setText("Device not found. Try again.");
            }
        });
    }

    // onResume is implemented later in the file

    /**
     * Updates the clock display based on the current clock values
     */
    private void updateClockDisplay() {
        // Update UI with current digit values
        tvMin1.setText(String.valueOf(globalMin1));
        tvMin2.setText(String.valueOf(globalMin2));
        tvSec1.setText(String.valueOf(globalSec1));
        tvSec2.setText(String.valueOf(globalSec2));
        tvMSec.setText(String.valueOf(globalMSec));
        tvShot1.setText(String.valueOf(globalShot1));
        tvShot2.setText(String.valueOf(globalShot2));
        tvHornx.setText("Horn: " + (globalHornx == 1 ? "On" : "Off"));
    }

    /**
     * Handle the logic shown in the block-based code image to update individual
     * digits
     */
    private void processClockLogic(int key) {
        // Logic based on the block image provided
        if (key == 0) {
            // Case for key = 0 (tenths of second logic)
            if (globalMSec > 0) {
                globalMSec = globalMSec - 2;
            } else {
                if (globalSec2 > 0) {
                    globalSec2 = globalSec2 - 1;
                    globalMSec = 8;
                } else {
                    if (globalSec1 > 0) {
                        globalSec1 = globalSec1 - 1;
                        globalSec2 = 9;
                        globalMSec = 8;
                    } else {
                        if (globalMin2 > 0) {
                            globalMin2 = globalMin2 - 1;
                            globalMSec = 8;
                            globalSec2 = 9;
                            globalSec1 = 5;
                        } else {
                            if (globalMin1 > 0) {
                                globalMin1 = globalMin1 - 1;
                                globalMSec = 8;
                                globalSec2 = 9;
                                globalSec1 = 5;
                                globalMin2 = 9;
                            } else {
                                globalHornx = 1;
                            }
                        }
                    }
                }
            }
        }

        // Update UI after logic processing
        updateClockDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload device address in case it was changed in settings
        loadDeviceAddress();

        // Re-register as clock update listener
        clockManager.addClockUpdateListener(this);

        // Update UI based on connection state
        if (bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)) {
            tv_result.setText("Connected to " + currentDeviceName);
            btn_connect.setText("Disconnect from " + currentDeviceName);
        } else {
            tv_result.setText("Ready to connect");
            btn_connect.setText(
                    "Connect to " + (currentDeviceName != null && !currentDeviceName.isEmpty() ? currentDeviceName
                            : currentDeviceAddress));
        }

        // Update clock display
        updateClockDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove callbacks to prevent leaks
        if (bleManager != null) {
            bleManager.removeConnectionCallback(this);
        }
        if (clockManager != null) {
            clockManager.removeClockUpdateListener(this);
        }
    }

    // ClockManager.ClockUpdateListener Implementation
    @Override
    public void onClockUpdate(int minutes, int seconds, int milliseconds, int shotClock) {
        // We're already on the main thread from the ClockManager handler

        // Update our individual digit variables based on the clock values
        globalMin1 = minutes / 10;
        globalMin2 = minutes % 10;
        globalSec1 = seconds / 10;
        globalSec2 = seconds % 10;
        globalMSec = milliseconds / 100; // Convert to tenths of a second
        globalShot1 = shotClock / 10;
        globalShot2 = shotClock % 10;

        // Process additional logic
        processClockLogic(0);

        // Update display
        updateClockDisplay();

        // Only send BLE updates on specific intervals - reduces BLE traffic
        // We'll update every 200ms (every other tick) to avoid overloading the
        // scoreboard
        if (System.currentTimeMillis() - lastSendTime >= MIN_SEND_INTERVAL) {
            // Send updates to device - always update if connected regardless of previous
            // state
            if (bleManager != null && bleManager.isDeviceConnected(currentDeviceAddress)) {
                sendAllDigitsOverBLE();

                // Log at reduced frequency
                if (System.currentTimeMillis() - lastStatusUpdateTime >= STATUS_UPDATE_INTERVAL) {
                    Log.d("ClockSync", "Sent update: " + globalMin1 + globalMin2 + ":" +
                            globalSec1 + globalSec2 + "." + globalMSec);
                }
            }
        }
    }

    /**
     * Forces an immediate update to the scoreboard regardless of timing intervals.
     * Used when we need to ensure the scoreboard is synchronized at specific
     * moments.
     * This method also ensures that the last sent digits cache is cleared to force
     * a fresh send with multiple retry attempts for reliability.
     */
    private void forceScoreboardUpdate() {
        // Reset the timing to allow immediate send
        lastSendTime = 0;
        lastStatusUpdateTime = 0;

        // Clear the last sent digits to force a new update regardless of content
        lastSentDigits = "";

        // Log that we're forcing an update
        Log.d(TAG, "Forcing scoreboard update with values: " +
                globalMin1 + globalMin2 + ":" + globalSec1 + globalSec2 + "." + globalMSec);

        // Check connection status
        if (!bleManager.isDeviceConnected(currentDeviceAddress)) {
            Log.w(TAG, "Cannot force update - device not connected");
            return; // This is void method, so plain return is correct here
        }

        // For critical updates, send multiple times with small delays between to ensure
        // reliability
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                // Send all digits immediately - this will happen even if the values haven't
                // changed
                boolean success = sendAllDigitsOverBLE();

                if (i < maxRetries - 1) {
                    // Sleep between retries to allow the device to process
                    Thread.sleep(70);
                }

                Log.d(TAG, "Force update attempt " + (i + 1) + "/" + maxRetries +
                        " for values: " + globalMin1 + globalMin2 + ":" +
                        globalSec1 + globalSec2 + "." + globalMSec);
            } catch (InterruptedException e) {
                Log.e(TAG, "Sleep interrupted during forced update", e);
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error during forced update: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onClockStateChanged(int state) {
        // Handle clock state changes if needed
    }

    @Override
    public void onGameClockExpired() {
        runOnUiThread(() -> {
            globalHornx = 1; // Turn horn on
            updateClockDisplay();
            // Automatically send buzzer command to device
            sendCommandIfConnected(BLECommandUtil.COMMAND_BUZZER);
        });
    }

    @Override
    public void onShotClockExpired() {
        runOnUiThread(() -> {
            // Handle shot clock expiration
            sendCommandIfConnected(BLECommandUtil.COMMAND_SHOT_CLOCK_BUZZER);
        });
    }
}
