package com.example.ble_scoreboard.utils;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Singleton class to manage BLE connections across the application
 */
public class BLEManager {
    private static final String TAG = "BLEManager";

    // HM-10 UUIDs
    private static final UUID HM10_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private static final int SCAN_TIMEOUT = 10000; // 10 seconds timeout for scanning

    // Singleton instance
    private static BLEManager instance;

    // Bluetooth components
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private final Map<String, BluetoothGatt> connectedDevices = new HashMap<>();
    private final Map<String, BLECommandUtil> deviceCommandUtils = new HashMap<>();

    // Scanning state
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());

    // Connection callbacks
    private final List<ConnectionCallback> connectionCallbacks = new ArrayList<>();
    private Context applicationContext;

    // Callback interface for connection events
    public interface ConnectionCallback {
        void onDeviceConnected(String address, String name);

        void onDeviceDisconnected(String address);

        void onConnectionError(String address, int status);

        void onScanComplete();
    }

    private BLEManager() {
        // Private constructor to enforce singleton
    }

    /**
     * Get the singleton instance
     * 
     * @return The BLEManager instance
     */
    public static synchronized BLEManager getInstance() {
        if (instance == null) {
            instance = new BLEManager();
        }
        return instance;
    }

    /**
     * Initialize the BLE manager with the application context
     * 
     * @param context The application context
     * @return true if initialization was successful
     */
    public boolean initialize(Context context) {
        // Store the application context to avoid memory leaks
        this.applicationContext = context.getApplicationContext();

        // Check if BLE is supported on the device
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE is not supported on this device");
            return false;
        }

        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to get BluetoothManager");
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to get BluetoothAdapter");
            return false;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "Unable to get BluetoothLeScanner");
            return false;
        }

        return true;
    }

    /**
     * Add a connection callback
     * 
     * @param callback The callback to add
     */
    public void addConnectionCallback(ConnectionCallback callback) {
        if (!connectionCallbacks.contains(callback)) {
            connectionCallbacks.add(callback);
        }
    }

    /**
     * Remove a connection callback
     * 
     * @param callback The callback to remove
     */
    public void removeConnectionCallback(ConnectionCallback callback) {
        connectionCallbacks.remove(callback);
    }

    /**
     * Check if Bluetooth is enabled
     * 
     * @return true if Bluetooth is enabled
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * Connect to a device by address
     * 
     * @param context    Context for the connection
     * @param address    The device address to connect to
     * @param deviceName The name of the device (for display)
     * @return true if connection was initiated
     */
    public boolean connectToDevice(Context context, String address, String deviceName) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not initialized");
            return false;
        }

        // Check if we're already connected to this device
        if (connectedDevices.containsKey(address)) {
            Log.d(TAG, "Already connected to device: " + address);
            return true;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission");
            return false;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            Log.d(TAG, "Connecting to " + address);

            // Connect to GATT server
            BluetoothGatt gatt = device.connectGatt(context, false, gattCallback);
            if (gatt != null) {
                // Add to pending connections
                connectedDevices.put(address, gatt);
                return true;
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid Bluetooth address: " + address, e);
        }

        return false;
    }

    /**
     * Disconnect from a device
     * 
     * @param address The device address to disconnect from
     */
    public void disconnectDevice(String address) {
        BluetoothGatt gatt = connectedDevices.get(address);
        if (gatt != null) {
            if (ActivityCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.disconnect();
            }
        }
    }

    /**
     * Disconnect from all devices
     */
    public void disconnectAll() {
        for (Map.Entry<String, BluetoothGatt> entry : new HashMap<>(connectedDevices).entrySet()) {
            disconnectDevice(entry.getKey());
        }
    }

    /**
     * Start scanning for BLE devices with the specified addresses
     * 
     * @param context         Context for the scan
     * @param deviceAddresses List of device addresses to scan for
     * @return true if scan started successfully
     */
    public boolean startScan(Context context, List<String> deviceAddresses) {
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available");
            return false;
        }

        if (isScanning) {
            Log.d(TAG, "Scan already in progress");
            return true;
        }

        // Check permissions
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission required for BLE scanning");
            return false;
        }

        // Start the scan
        try {
            isScanning = true;
            Log.d(TAG, "Starting BLE scan for devices: " + deviceAddresses);

            // Set a timeout for the scan
            scanHandler.postDelayed(() -> {
                if (isScanning) {
                    stopScan();
                    notifyScanComplete();
                }
            }, SCAN_TIMEOUT);

            // Start the scan
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.startScan(scanCallback);
                return true;
            } else {
                Log.e(TAG, "BLUETOOTH_SCAN permission required");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting BLE scan", e);
            isScanning = false;
            return false;
        }
    }

    /**
     * Stop the BLE scan
     */
    public void stopScan() {
        if (bluetoothLeScanner != null && isScanning) {
            if (ActivityCompat.checkSelfPermission(applicationContext,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
                Log.d(TAG, "BLE scan stopped");
            }
        }
    }

    /**
     * Send a command to a specific device
     * 
     * @param address The device address
     * @param command The command byte to send
     * @return true if command sent successfully
     */
    public boolean sendCommand(String address, byte command) {
        BLECommandUtil commandUtil = deviceCommandUtils.get(address);
        if (commandUtil != null) {
            return commandUtil.sendCommand(command);
        } else {
            Log.e(TAG, "No command utility for device: " + address);
            return false;
        }
    }

    /**
     * Send a command to all connected devices
     * 
     * @param command The command byte to send
     * @return true if command sent to at least one device
     */
    public boolean sendCommandToAll(byte command) {
        boolean success = false;
        for (BLECommandUtil commandUtil : deviceCommandUtils.values()) {
            if (commandUtil != null) {
                success |= commandUtil.sendCommand(command);
            }
        }
        return success;
    }

    /**
     * Check if a device is connected
     * 
     * @param address The device address
     * @return true if device is connected
     */
    public boolean isDeviceConnected(String address) {
        return connectedDevices.containsKey(address) && connectedDevices.get(address) != null;
    }

    /**
     * Get the number of connected devices
     * 
     * @return The number of connected devices
     */
    public int getConnectedDeviceCount() {
        return connectedDevices.size();
    }

    /**
     * Get list of connected device addresses
     * 
     * @return List of connected device addresses
     */
    public List<String> getConnectedDeviceAddresses() {
        return new ArrayList<>(connectedDevices.keySet());
    }

    /**
     * Get BluetoothGatt object for a connected device
     * 
     * @param address Device address
     * @return BluetoothGatt object or null if not connected
     */
    public BluetoothGatt getBluetoothGatt(String address) {
        return connectedDevices.get(address);
    }

    /**
     * Close all GATT connections and clean up
     */
    public void close() {
        // Disconnect all devices
        disconnectAll();

        // Clear all collections
        connectedDevices.clear();
        deviceCommandUtils.clear();
        connectionCallbacks.clear();

        // Stop any ongoing scan
        stopScan();
    }

    // Callback for scan results
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && device.getAddress() != null) {
                Log.d(TAG, "Found device: " + device.getAddress());
                // We just try to connect to any device we find during scan
                // Actual filtering will be done when getting stored device addresses
                if (ActivityCompat.checkSelfPermission(applicationContext,
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.stopScan(this);
                    isScanning = false;
                    // Attempt to connect to the device if it's in our list
                    String deviceName = ActivityCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                                    ? device.getName()
                                    : "Unknown";
                    connectToDevice(applicationContext, device.getAddress(),
                            deviceName != null ? deviceName : "Unknown");
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error code: " + errorCode);
            isScanning = false;
            notifyScanComplete();
        }
    };

    // GATT callback
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String address = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to device: " + address);

                    // Discover services
                    if (ActivityCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.discoverServices();
                    }

                    String deviceName = ActivityCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                                    ? gatt.getDevice().getName()
                                    : "Unknown";
                    if (deviceName == null)
                        deviceName = "Unknown";

                    final String finalDeviceName = deviceName;
                    // Notify connected on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (ConnectionCallback callback : new ArrayList<>(connectionCallbacks)) {
                            callback.onDeviceConnected(address, finalDeviceName);
                        }
                    });

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from device: " + address);

                    // Clean up
                    deviceCommandUtils.remove(address);
                    connectedDevices.remove(address);
                    if (ActivityCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt.close();
                    }

                    // Notify disconnected on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (ConnectionCallback callback : new ArrayList<>(connectionCallbacks)) {
                            callback.onDeviceDisconnected(address);
                        }
                    });
                }
            } else {
                // Connection error
                Log.e(TAG, "Connection error: " + status + " for device: " + address);

                // Clean up
                deviceCommandUtils.remove(address);
                connectedDevices.remove(address);
                if (ActivityCompat.checkSelfPermission(applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.close();
                }

                // Notify error on the main thread
                final int finalStatus = status;
                new Handler(Looper.getMainLooper()).post(() -> {
                    for (ConnectionCallback callback : new ArrayList<>(connectionCallbacks)) {
                        callback.onConnectionError(address, finalStatus);
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for device: " + address);

                // Find the HM-10 service
                BluetoothGattService service = gatt.getService(HM10_SERVICE_UUID);
                if (service != null) {
                    Log.d(TAG, "Found HM-10 service");

                    // Create a command utility for this device
                    BLECommandUtil commandUtil = new BLECommandUtil(gatt, applicationContext, null);
                    deviceCommandUtils.put(address, commandUtil);
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }
    };

    // Notify all callbacks that scan is complete
    private void notifyScanComplete() {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (ConnectionCallback callback : new ArrayList<>(connectionCallbacks)) {
                callback.onScanComplete();
            }
        });
    }
}
