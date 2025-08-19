package com.example.ble_scoreboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvStatus;
    private Button btnConnectDevice;
    private Button btnControlPanel;
    private Button btnSettings;
    private Button btnLogout;

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

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvStatus = findViewById(R.id.tv_status);
        btnConnectDevice = findViewById(R.id.btn_connect_device);
        btnControlPanel = findViewById(R.id.btn_control_panel);
        btnSettings = findViewById(R.id.btn_settings);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        // Connect to device button
        btnConnectDevice.setOnClickListener(v -> {
            // This will be implemented to connect to a BLE device
            tvStatus.setText("Attempting to connect to device...");
            // The actual connection logic will be implemented elsewhere
        });

        // Open control panel button
        btnControlPanel.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ControlPanelActivity.class);
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
        String deviceAddress1 = prefs.getString("deviceAddress1", null);

        if (deviceAddress1 == null || deviceAddress1.isEmpty()) {
            tvStatus.setText("No device configured. Please go to Settings.");
        } else {
            tvStatus.setText("Ready to connect to device.");
        }
    }
}
