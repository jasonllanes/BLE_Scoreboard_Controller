package com.example.ble_scoreboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ble_scoreboard.utils.BLECommandUtil;
import com.example.ble_scoreboard.utils.BLEManager;
import com.example.ble_scoreboard.utils.ClockManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClockControlActivity extends AppCompatActivity
        implements ClockManager.ClockUpdateListener, BLEManager.ConnectionCallback {
    private static final String TAG = "ClockControlActivity";

    // UI Elements for Game Clock
    private TextView tvMinutes1;
    private TextView tvMinutes2;
    private TextView tvSeconds1;
    private TextView tvSeconds2;
    private TextView tvMilliseconds;

    // UI Elements for Shot Clock
    private TextView tvShotClock1;
    private TextView tvShotClock2;

    // UI Elements for Team Scores
    private TextView tvTeamAScore;
    private TextView tvTeamBScore;
    private TextView tvTeamAFouls;
    private TextView tvTeamBFouls;
    private TextView tvTeamATOL;
    private TextView tvTeamBTOL;

    // UI Elements for Clock Input
    private EditText etMinutes;
    private EditText etSeconds;

    // UI Elements for Device Status
    private TextView tvDevice1Status;
    private TextView tvDevice2Status;
    private TextView tvDevice3Status;

    // Buttons
    private Button btnStart;
    private Button btnStop;
    private Button btnReset;
    private Button btnSetTime;
    private Button btnShotClock14;
    private Button btnShotClock24;
    private Button btnShotClockStart;
    private Button btnShotClockStop;
    private Button btnShotClockReset;

    // BLE management
    private BLEManager bleManager;
    private List<String> deviceAddresses = new ArrayList<>();

    // Clock manager
    private ClockManager clockManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock_control);

        // Initialize ClockManager
        clockManager = ClockManager.getInstance();
        clockManager.addClockUpdateListener(this);

        // Initialize BLEManager
        bleManager = BLEManager.getInstance();

        // Initialize UI elements
        initializeViews();
        setupListeners();
        loadConnectedDevices();
        updateDeviceStatusDisplay();
    }

    private void initializeViews() {
        // Game Clock display
        tvMinutes1 = findViewById(R.id.tv_clock_min1);
        tvMinutes2 = findViewById(R.id.tv_clock_min2);
        tvSeconds1 = findViewById(R.id.tv_clock_sec1);
        tvSeconds2 = findViewById(R.id.tv_clock_sec2);
        // Milliseconds don't seem to have a corresponding view in the layout

        // Shot Clock display (only has one TextView in the layout)
        TextView shotClockView = findViewById(R.id.tv_shotclock);
        // We'll use this single view for now

        // Team info display
        tvTeamAScore = findViewById(R.id.tv_score_team_a);
        tvTeamBScore = findViewById(R.id.tv_score_team_b);
        tvTeamAFouls = findViewById(R.id.tv_fouls_team_a);
        tvTeamBFouls = findViewById(R.id.tv_fouls_team_b);
        tvTeamATOL = findViewById(R.id.tv_tol_team_a);
        tvTeamBTOL = findViewById(R.id.tv_tol_team_b);

        // Time input fields
        etMinutes = findViewById(R.id.et_clock_minutes);
        etSeconds = findViewById(R.id.et_clock_seconds);

        // Device status indicators - based on the layout XML
        tvDevice1Status = findViewById(R.id.tv_main_clock_status);
        tvDevice2Status = findViewById(R.id.tv_shotclock1_status);
        tvDevice3Status = findViewById(R.id.tv_shotclock2_status);
        // The layout doesn't have ImageViews for the status, adjust the code

        // Control buttons
        btnStart = findViewById(R.id.btn_clock_start);
        btnStop = findViewById(R.id.btn_clock_stop);
        btnSetTime = findViewById(R.id.btn_set_clock);
        // btnReset is missing in the layout
        btnShotClock14 = findViewById(R.id.btn_shotclock_reset_14);
        btnShotClock24 = findViewById(R.id.btn_shotclock_reset_24);
        // Shot clock Start/Stop/Reset buttons are missing in the layout

        // Initialize clock displays
        updateClockDisplay();
        updateShotClockDisplay();
    }

    private void setupListeners() {
        // Game clock control buttons
        btnStart.setOnClickListener(v -> {
            clockManager.startClock();
            sendCommandToAllDevices(BLECommandUtil.COMMAND_START_CLOCK);
        });

        btnStop.setOnClickListener(v -> {
            clockManager.stopClock();
            sendCommandToAllDevices(BLECommandUtil.COMMAND_STOP_CLOCK);
        });

        // No reset button in the layout, so we handle it differently
        // We'll add this functionality to the set time button as a reset when blank

        btnSetTime.setOnClickListener(v -> {
            try {
                // If both fields are empty, reset to defaults
                if (etMinutes.getText().toString().isEmpty() &&
                        etSeconds.getText().toString().isEmpty()) {
                    clockManager.resetToDefaults();
                    sendCommandToAllDevices(BLECommandUtil.COMMAND_RESET_CLOCK);
                } else {
                    int minutes = etMinutes.getText().toString().isEmpty() ? 0
                            : Integer.parseInt(etMinutes.getText().toString());
                    int seconds = etSeconds.getText().toString().isEmpty() ? 0
                            : Integer.parseInt(etSeconds.getText().toString());

                    clockManager.setGameClock(minutes, seconds);
                    sendTimeUpdateToDevices(minutes, seconds);
                }

                updateClockDisplay();
                updateShotClockDisplay();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid time values", Toast.LENGTH_SHORT).show();
            }
        });

        // Shot clock control buttons
        btnShotClock14.setOnClickListener(v -> {
            clockManager.resetShotClockTo14();
            sendCommandToAllDevices(BLECommandUtil.COMMAND_SHOT_CLOCK_14);
            updateShotClockDisplay();
        });

        btnShotClock24.setOnClickListener(v -> {
            clockManager.resetShotClockTo24();
            sendCommandToAllDevices(BLECommandUtil.COMMAND_SHOT_CLOCK_24);
            updateShotClockDisplay();
        });

        // Add listener for set shot clock button which is in the layout
        Button btnSetShotClock = findViewById(R.id.btn_set_shotclock);
        if (btnSetShotClock != null) {
            btnSetShotClock.setOnClickListener(v -> {
                try {
                    EditText etShotClock = findViewById(R.id.et_shotclock_value);
                    if (etShotClock != null) {
                        int shotClockValue = etShotClock.getText().toString().isEmpty() ? 24
                                : Integer.parseInt(etShotClock.getText().toString());
                        clockManager.setShotClock(shotClockValue);
                        updateShotClockDisplay();
                        // No specific command for custom shot clock values, so send the digits
                        sendShotClockUpdateToDevices(shotClockValue);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid shot clock value", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // The Shot clock Start/Stop/Reset buttons aren't in the layout
        // Using the main clock controls will also affect the shot clock as per the
        // ClockManager implementation
    }

    private void loadConnectedDevices() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        deviceAddresses.clear();

        // Load device addresses from preferences
        String device1Address = preferences.getString("deviceAddress1", "");
        String device2Address = preferences.getString("deviceAddress2", "");
        String device3Address = preferences.getString("deviceAddress3", "");

        // Add non-empty addresses to our list
        if (!device1Address.isEmpty()) {
            deviceAddresses.add(device1Address);
        }
        if (!device2Address.isEmpty()) {
            deviceAddresses.add(device2Address);
        }
        if (!device3Address.isEmpty()) {
            deviceAddresses.add(device3Address);
        }
    }

    private void updateDeviceStatusDisplay() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Device 1
        String device1Address = preferences.getString("deviceAddress1", "");
        String device1Name = preferences.getString("deviceName1", "Main Clock");
        boolean device1Connected = bleManager.isDeviceConnected(device1Address);
        tvDevice1Status.setText(device1Connected ? "Connected" : "Disconnected");
        tvDevice1Status.setTextColor(getResources()
                .getColor(device1Connected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        // Device 2
        String device2Address = preferences.getString("deviceAddress2", "");
        String device2Name = preferences.getString("deviceName2", "Shot Clock 1");
        boolean device2Connected = bleManager.isDeviceConnected(device2Address);
        tvDevice2Status.setText(device2Connected ? "Connected" : "Disconnected");
        tvDevice2Status.setTextColor(getResources()
                .getColor(device2Connected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        // Device 3
        String device3Address = preferences.getString("deviceAddress3", "");
        String device3Name = preferences.getString("deviceName3", "Shot Clock 2");
        boolean device3Connected = bleManager.isDeviceConnected(device3Address);
        tvDevice3Status.setText(device3Connected ? "Connected" : "Disconnected");
        tvDevice3Status.setTextColor(getResources()
                .getColor(device3Connected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
    }

    @Override
    public void onClockUpdate(int minutes, int seconds, int milliseconds, int shotClock) {
        runOnUiThread(() -> {
            // Update local display
            updateClockDisplay();
            updateShotClockDisplay();

            // Send clock updates to BLE devices
            // Update on every second change or when tenths digit changes to zero (for more
            // frequent updates)
            if (milliseconds == 0 || milliseconds % 100 == 0) {
                sendTimeUpdateToDevices(minutes, seconds);

                // Also update shot clock if needed
                if (clockManager.isShotClockEnabled() && milliseconds == 0) {
                    // Send shot clock update - we'll send this less frequently
                    int shotClockValue = clockManager.getShotClock();
                    sendShotClockUpdateToDevices(shotClockValue);
                }
            }
        });
    }

    @Override
    public void onClockStateChanged(int state) {
        runOnUiThread(() -> {
            // Update UI based on clock state
            boolean isRunning = (state == ClockManager.STATE_RUNNING);

            btnStart.setEnabled(!isRunning);
            btnStop.setEnabled(isRunning);
            btnSetTime.setEnabled(!isRunning);

            // Also update shot clock button states
            btnShotClock14.setEnabled(!isRunning);
            btnShotClock24.setEnabled(!isRunning);

            // Send state change commands if needed
        });
    }

    @Override
    public void onGameClockExpired() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Game Clock Expired!", Toast.LENGTH_SHORT).show();
            // Send buzzer command to devices
            sendCommandToAllDevices(BLECommandUtil.COMMAND_BUZZER);
        });
    }

    @Override
    public void onShotClockExpired() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Shot Clock Expired!", Toast.LENGTH_SHORT).show();
            // Send shot clock buzzer command
            sendCommandToAllDevices(BLECommandUtil.COMMAND_SHOT_CLOCK_BUZZER);
        });
    }

    private void updateClockDisplay() {
        // Update game clock display
        tvMinutes1.setText(String.valueOf(clockManager.getMin1()));
        tvMinutes2.setText(String.valueOf(clockManager.getMin2()));
        tvSeconds1.setText(String.valueOf(clockManager.getSec1()));
        tvSeconds2.setText(String.valueOf(clockManager.getSec2()));
        // tvMilliseconds is not in the layout, so we skip it
    }

    private void updateShotClockDisplay() {
        // Update shot clock display - in the layout it's just one TextView
        TextView shotClockView = findViewById(R.id.tv_shotclock);
        int shotClock = clockManager.getShotClock();
        shotClockView.setText(String.valueOf(shotClock));
    }

    private void sendCommandToAllDevices(byte command) {
        // Send command to all connected devices using the BLEManager
        Log.d(TAG, "Sending command " + (char) command + " to all devices");

        // Loop through all registered device addresses
        for (String address : deviceAddresses) {
            if (bleManager.isDeviceConnected(address)) {
                bleManager.sendCommand(address, command);
                Log.d(TAG, "Sent command " + (char) command + " to device " + address);
            } else {
                Log.d(TAG, "Device " + address + " not connected, couldn't send command");
            }
        }

        // If no devices registered, just log it
        if (deviceAddresses.isEmpty()) {
            Log.d(TAG, "No registered devices to send command to");
        }
    }

    private void sendTimeUpdateToDevices(int minutes, int seconds) {
        // Break down the time into individual digits
        int min1 = minutes / 10;
        int min2 = minutes % 10;
        int sec1 = seconds / 10;
        int sec2 = seconds % 10;

        Log.d(TAG, "Updating clock display to: " + min1 + min2 + ":" + sec1 + sec2);

        // Protocol:
        // 1. Send position command (which position to update)
        // 2. Send digit command (what digit to display at that position)

        // Update minutes first digit
        sendCommandToAllDevices(BLECommandUtil.COMMAND_CLOCK_MIN1_POS); // Position command
        sendCommandToAllDevices((byte) (BLECommandUtil.COMMAND_DIGIT_0 + min1)); // Digit command

        // Update minutes second digit
        sendCommandToAllDevices(BLECommandUtil.COMMAND_CLOCK_MIN2_POS);
        sendCommandToAllDevices((byte) (BLECommandUtil.COMMAND_DIGIT_0 + min2));

        // Update seconds first digit
        sendCommandToAllDevices(BLECommandUtil.COMMAND_CLOCK_SEC1_POS);
        sendCommandToAllDevices((byte) (BLECommandUtil.COMMAND_DIGIT_0 + sec1));

        // Update seconds second digit
        sendCommandToAllDevices(BLECommandUtil.COMMAND_CLOCK_SEC2_POS);
        sendCommandToAllDevices((byte) (BLECommandUtil.COMMAND_DIGIT_0 + sec2));
    }

    private void sendShotClockUpdateToDevices(int shotClock) {
        // Break down shot clock into individual digits
        int shot1 = shotClock / 10;
        int shot2 = shotClock % 10;

        Log.d(TAG, "Updating shot clock display to: " + shot1 + shot2);

        // For now, we'll use the same protocol as the game clock
        // You may need to use different position commands for shot clock digits

        // Assuming there are commands for shot clock positions
        // If there are no specific commands, you could use predefined shot clock values
        // (14s, 24s)

        // For now, we'll use the standard shot clock commands
        if (shotClock == 14) {
            sendCommandToAllDevices(BLECommandUtil.COMMAND_SHOT_CLOCK_14);
        } else if (shotClock == 24) {
            sendCommandToAllDevices(BLECommandUtil.COMMAND_SHOT_CLOCK_24);
        } else {
            // For custom values, we'd need to have specific shot clock digit positions
            // This is a placeholder until you define those commands
            Log.d(TAG, "Custom shot clock value: " + shotClock);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register as connection callback
        bleManager.addConnectionCallback(this);

        // Update device status display
        updateDeviceStatusDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listeners to prevent memory leaks
        clockManager.removeClockUpdateListener(this);
        bleManager.removeConnectionCallback(this);
    }

    // BLEManager.ConnectionCallback Implementation
    @Override
    public void onDeviceConnected(String address, String name) {
        runOnUiThread(this::updateDeviceStatusDisplay);
    }

    @Override
    public void onDeviceDisconnected(String address) {
        runOnUiThread(this::updateDeviceStatusDisplay);
    }

    @Override
    public void onConnectionError(String address, int status) {
        runOnUiThread(() -> {
            updateDeviceStatusDisplay();
            Toast.makeText(this, "Connection error: " + status, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onScanComplete() {
        // Not used in this activity
    }
}
