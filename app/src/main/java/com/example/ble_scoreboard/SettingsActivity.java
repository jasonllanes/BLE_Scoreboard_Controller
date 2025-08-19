package com.example.ble_scoreboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.SwitchCompat;


import com.google.android.material.textfield.TextInputLayout;

public class SettingsActivity extends AppCompatActivity {

    private EditText etDeviceAddress1, etDeviceName1;
    private EditText etDeviceAddress2, etDeviceName2;
    private EditText etDeviceAddress3, etDeviceName3;
    private SwitchCompat switchAutoConnect;
    private Button btnSaveSettings, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        initializeViews();

        // Load saved settings
        loadSettings();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        etDeviceAddress1 = findViewById(R.id.et_device_address_1);
        etDeviceName1 = findViewById(R.id.et_device_name_1);
        etDeviceAddress2 = findViewById(R.id.et_device_address_2);
        etDeviceName2 = findViewById(R.id.et_device_name_2);
        etDeviceAddress3 = findViewById(R.id.et_device_address_3);
        etDeviceName3 = findViewById(R.id.et_device_name_3);

        switchAutoConnect = findViewById(R.id.switch_auto_connect);

        btnSaveSettings = findViewById(R.id.btn_save_settings);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void loadSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load device 1 settings
        etDeviceAddress1.setText(prefs.getString("deviceAddress1", ""));
        etDeviceName1.setText(prefs.getString("deviceName1", ""));

        // Load device 2 settings
        etDeviceAddress2.setText(prefs.getString("deviceAddress2", ""));
        etDeviceName2.setText(prefs.getString("deviceName2", ""));

        // Load device 3 settings
        etDeviceAddress3.setText(prefs.getString("deviceAddress3", ""));
        etDeviceName3.setText(prefs.getString("deviceName3", ""));

        // Load other settings
        switchAutoConnect.setChecked(prefs.getBoolean("autoConnect", false));
    }

    private void setupClickListeners() {
        // Save settings button
        btnSaveSettings.setOnClickListener(v -> saveSettings());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        // Save device 1 settings
        editor.putString("deviceAddress1", etDeviceAddress1.getText().toString().trim());
        editor.putString("deviceName1", etDeviceName1.getText().toString().trim());

        // Save device 2 settings
        editor.putString("deviceAddress2", etDeviceAddress2.getText().toString().trim());
        editor.putString("deviceName2", etDeviceName2.getText().toString().trim());

        // Save device 3 settings
        editor.putString("deviceAddress3", etDeviceAddress3.getText().toString().trim());
        editor.putString("deviceName3", etDeviceName3.getText().toString().trim());

        // Save other settings
        editor.putBoolean("autoConnect", switchAutoConnect.isChecked());

        // Apply changes
        editor.apply();

        // Show success message and close
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
