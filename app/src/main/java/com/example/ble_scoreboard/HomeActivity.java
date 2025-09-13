package com.example.ble_scoreboard;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.ble_scoreboard.utils.BLECommandUtil;
import com.example.ble_scoreboard.utils.BLEManager;

@RequiresApi(api = Build.VERSION_CODES.S)
public class HomeActivity extends AppCompatActivity implements BLEManager.ConnectionCallback {

    private static final String TAG = "HomeActivity";
    private static final int REQUEST_BLUETOOTH_SCAN = 3;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    private TextView tvWelcome;
    private TextView tvStatus;
    private Button btnConnectDevice;
    private Button btnControlPanel;
    private Button btnClockControl;
    private Button btnSettings;
    private Button btnLogout;

    // Clock Preview Elements
    private TextView tvHomeMin1;
    private TextView tvHomeMin2;
    private TextView tvHomeSec1;
    private TextView tvHomeSec2;
    private TextView tvHomeShotClock;

    // BLE management
    private BLEManager bleManager;

    private String currentDeviceAddress;
    private String currentDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views
        initializeViews();

        // Set welcome message
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("username", "User");
        tvWelcome.setText("Welcome, " + username + "!");

        // Initialize BLE Manager
        bleManager = BLEManager.getInstance();
        bleManager.initialize(getApplicationContext());
        bleManager.addConnectionCallback(this);

        // Set click listeners
        setupClickListeners();
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

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvStatus = findViewById(R.id.tv_status);
        btnConnectDevice = findViewById(R.id.btn_connect_device);
        btnControlPanel = findViewById(R.id.btn_control_panel);
        btnClockControl = findViewById(R.id.btn_clock_control);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout);

        // Initialize clock preview elements
        tvHomeMin1 = findViewById(R.id.tv_home_min1);
        tvHomeMin2 = findViewById(R.id.tv_home_min2);
        tvHomeSec1 = findViewById(R.id.tv_home_sec1);
        tvHomeSec2 = findViewById(R.id.tv_home_sec2);
        tvHomeShotClock = findViewById(R.id.tv_home_shotclock);

        // Initialize with default time (10:00)
        updateClockDisplay(10, 0);
        updateShotClockDisplay(24);
    }

    private void setupClickListeners() {
        // Connect to device button
        btnConnectDevice.setOnClickListener(v -> {
            if (!bleManager.isDeviceConnected(currentDeviceAddress)) {
                if (checkAndRequestPermissions()) {
                    tvStatus.setText("Connecting to " + currentDeviceName + "...");
                    bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                }
            } else {
                tvStatus.setText("Disconnecting...");
                bleManager.disconnectDevice(currentDeviceAddress);
                btnConnectDevice.setText("Connect to Scoreboard");
            }
        });

        // Open control panel button
        btnControlPanel.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ControlPanelActivity.class);
            startActivity(intent);
        });

        // Open clock control button
        btnClockControl.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ClockControlActivity.class);
            startActivity(intent);
        });

        // Settings button
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Logout button
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        // Clear logged-in state
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("isLoggedIn", false).apply();

        // Return to login screen
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close this activity
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if device address is configured
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentDeviceAddress = prefs.getString("deviceAddress1", null);
        currentDeviceName = prefs.getString("deviceName1", "Unknown");

        if (currentDeviceAddress == null || currentDeviceAddress.isEmpty()) {
            tvStatus.setText("No device configured. Please go to Settings.");
            btnConnectDevice.setEnabled(false);
        } else {
            // Check if device is already connected
            if (bleManager.isDeviceConnected(currentDeviceAddress)) {
                tvStatus.setText("Connected to " + currentDeviceName);
                btnConnectDevice.setText("Disconnect");
            } else {
                tvStatus.setText("Ready to connect to " + currentDeviceName);
                btnConnectDevice.setText("Connect to Scoreboard");
            }
            btnConnectDevice.setEnabled(true);
        }

        // Update clock display with saved values from preferences
        int minutes = prefs.getInt("clock_minutes", 10);
        int seconds = prefs.getInt("clock_seconds", 0);
        int shotClock = prefs.getInt("shot_clock", 24);
        updateClockDisplay(minutes, seconds);
        updateShotClockDisplay(shotClock);

        // Check if Bluetooth is enabled
        if (!bleManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestBluetoothPermission();
            }
        }
    }

    /**
     * Updates the clock display on the home screen
     * 
     * @param minutes Minutes to display
     * @param seconds Seconds to display
     */
    private void updateClockDisplay(int minutes, int seconds) {
        String mins = String.format("%02d", minutes);
        String secs = String.format("%02d", seconds);

        tvHomeMin1.setText(String.valueOf(mins.charAt(0)));
        tvHomeMin2.setText(String.valueOf(mins.charAt(1)));
        tvHomeSec1.setText(String.valueOf(secs.charAt(0)));
        tvHomeSec2.setText(String.valueOf(secs.charAt(1)));
    }

    /**
     * Updates the shot clock display on the home screen
     * 
     * @param seconds Shot clock seconds to display
     */
    private void updateShotClockDisplay(int seconds) {
        tvHomeShotClock.setText(String.format("%02d", seconds));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove ourselves as a callback to prevent leaks
        bleManager.removeConnectionCallback(this);
    }

    // BLEManager.ConnectionCallback Implementation
    @Override
    public void onDeviceConnected(String address, String name) {
        runOnUiThread(() -> {
            tvStatus.setText("Connected to " + name);
            btnConnectDevice.setText("Disconnect");
            Toast.makeText(this, "Connected to " + name, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDeviceDisconnected(String address) {
        runOnUiThread(() -> {
            tvStatus.setText("Disconnected from device");
            btnConnectDevice.setText("Connect to Scoreboard");
            Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onConnectionError(String address, int status) {
        runOnUiThread(() -> {
            tvStatus.setText("Connection error: " + status);
            btnConnectDevice.setText("Connect to Scoreboard");
            Toast.makeText(this, "Failed to connect: Error " + status, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onScanComplete() {
        runOnUiThread(() -> {
            // If not connected after scan completed, update UI
            if (!bleManager.isDeviceConnected(currentDeviceAddress)) {
                tvStatus.setText("Device not found. Try again.");
                Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FINE_LOCATION || requestCode == REQUEST_BLUETOOTH_SCAN) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with connection
                if (currentDeviceAddress != null && !currentDeviceAddress.isEmpty()) {
                    tvStatus.setText("Connecting to " + currentDeviceName + "...");
                    bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Permission required to connect to BLE devices", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Permission denied. Cannot connect.");
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
                    tvStatus.setText("Connecting to " + currentDeviceName + "...");
                    bleManager.connectToDevice(this, currentDeviceAddress, currentDeviceName);
                }
            } else {
                // User declined to enable Bluetooth
                Toast.makeText(this, "Bluetooth is required for device connection", Toast.LENGTH_SHORT).show();
                tvStatus.setText("Bluetooth disabled. Cannot connect.");
            }
        }
    }
}
