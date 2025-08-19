package com.example.ble_scoreboard.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import java.util.UUID;

/**
 * Utility class to handle BLE scoreboard commands
 */
public class BLECommandUtil {

    // HM-10 UUIDs
    private static final UUID HM10_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID HM10_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // Command constants
    public static final byte CMD_NULL = '0'; // 0: null
    public static final byte CMD_GAMETIME_SHOTCLOCK_HORN = '_'; // _: gametime Shotclock Horn
    public static final byte CMD_SHOTCLOCK_RESET_14 = 'q'; // q: shotclock reset to 14
    public static final byte CMD_SHOTCLOCK_RESET_24 = 'r'; // r: shotclock reset to 24
    public static final byte CMD_NEW_GAME = 'v'; // v: new game

    // Team A commands
    public static final byte CMD_TEAM_A_SCORE_PLUS_1 = 'j'; // j: team a score +1
    public static final byte CMD_TEAM_A_SCORE_PLUS_2 = 'k'; // k: team a score +2
    public static final byte CMD_TEAM_A_SCORE_MINUS_1 = 'l'; // l: team a score -1
    public static final byte CMD_TEAM_A_FOUL_PLUS_1 = 'm'; // m: team a foul +1 (was incorrectly labeled as -1)
    public static final byte CMD_TEAM_A_FOUL_MINUS_1 = 'C'; // C: team a foul -1 (was incorrectly labeled as +1)
    public static final byte CMD_TEAM_A_TOL_MINUS_1 = 'n'; // n: team a TOL -1
    public static final byte CMD_TEAM_A_TOL_PLUS_1 = 'D'; // D: team a TOL +1

    // Team B commands
    public static final byte CMD_TEAM_B_SCORE_PLUS_1 = 'a'; // a: team b score +1
    public static final byte CMD_TEAM_B_SCORE_PLUS_2 = 'b'; // b: team b score +2
    public static final byte CMD_TEAM_B_SCORE_MINUS_1 = 'd'; // d: team b score -1
    public static final byte CMD_TEAM_B_FOUL_PLUS_1 = 'c'; // c: team b foul +1
    public static final byte CMD_TEAM_B_FOUL_MINUS_1 = 'A'; // A: team b foul -1
    public static final byte CMD_TEAM_B_TOL_MINUS_1 = 'e'; // e: team b TOL -1
    public static final byte CMD_TEAM_B_TOL_PLUS_1 = 'B'; // B: team b TOL +1

    // Arrow commands
    public static final byte CMD_RIGHT_ARROW = 'V'; // V: right arrow
    public static final byte CMD_LEFT_ARROW = 'W'; // W: left arrow

    private final BluetoothGatt bluetoothGatt;
    private final Context context;
    private final TextView statusTextView;

    public BLECommandUtil(BluetoothGatt bluetoothGatt, Context context, TextView statusTextView) {
        this.bluetoothGatt = bluetoothGatt;
        this.context = context;
        this.statusTextView = statusTextView;
    }

    /**
     * Send command to BLE device
     * 
     * @param commandByte The command byte to send
     * @return true if command was sent, false otherwise
     */
    public boolean sendCommand(byte commandByte) {
        if (bluetoothGatt == null) {
            updateStatus("Not connected to a BLE device.");
            return false;
        }

        BluetoothGattService service = bluetoothGatt.getService(HM10_SERVICE_UUID);
        if (service == null) {
            updateStatus("Service not found.");
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(HM10_CHARACTERISTIC_UUID);
        if (characteristic == null) {
            updateStatus("Characteristic not found.");
            return false;
        }

        return writeByteToCharacteristic(characteristic, commandByte);
    }

    /**
     * Process command by byte code
     * 
     * @param commandCode The command code to process
     * @return true if command was processed, false otherwise
     */
    public boolean processCommand(byte commandCode) {
        String commandDescription = getCommandDescription(commandCode);
        updateStatus("Sending command: " + commandDescription);
        return sendCommand(commandCode);
    }

    /**
     * Get description for command byte
     * 
     * @param commandByte The command byte
     * @return Description of the command
     */
    public String getCommandDescription(byte commandByte) {
        switch (commandByte) {
            case CMD_NULL:
                return "Null";
            case CMD_GAMETIME_SHOTCLOCK_HORN:
                return "Gametime/Shotclock Horn";
            case CMD_SHOTCLOCK_RESET_14:
                return "Shotclock Reset to 14";
            case CMD_SHOTCLOCK_RESET_24:
                return "Shotclock Reset to 24";
            case CMD_NEW_GAME:
                return "New Game";
            case CMD_TEAM_A_SCORE_PLUS_1:
                return "Team A Score +1";
            case CMD_TEAM_A_SCORE_PLUS_2:
                return "Team A Score +2";
            case CMD_TEAM_A_SCORE_MINUS_1:
                return "Team A Score -1";
            case CMD_TEAM_A_FOUL_MINUS_1:
                return "Team A Foul -1";
            case CMD_TEAM_A_FOUL_PLUS_1:
                return "Team A Foul +1";
            case CMD_TEAM_A_TOL_MINUS_1:
                return "Team A TOL -1";
            case CMD_TEAM_A_TOL_PLUS_1:
                return "Team A TOL +1";
            case CMD_TEAM_B_SCORE_PLUS_1:
                return "Team B Score +1";
            case CMD_TEAM_B_SCORE_PLUS_2:
                return "Team B Score +2";
            case CMD_TEAM_B_SCORE_MINUS_1:
                return "Team B Score -1";
            case CMD_TEAM_B_FOUL_PLUS_1:
                return "Team B Foul +1";
            case CMD_TEAM_B_FOUL_MINUS_1:
                return "Team B Foul -1";
            case CMD_TEAM_B_TOL_MINUS_1:
                return "Team B TOL -1";
            case CMD_TEAM_B_TOL_PLUS_1:
                return "Team B TOL +1";
            case CMD_RIGHT_ARROW:
                return "Right Arrow";
            case CMD_LEFT_ARROW:
                return "Left Arrow";
            default:
                return "Unknown Command: " + commandByte;
        }
    }

    private boolean writeByteToCharacteristic(BluetoothGattCharacteristic characteristic, byte value) {
        characteristic.setValue(new byte[] { value });
        if (ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return bluetoothGatt.writeCharacteristic(characteristic);
        } else {
            updateStatus("Bluetooth connect permission required.");
            return false;
        }
    }

    private void updateStatus(String message) {
        if (statusTextView != null) {
            statusTextView.setText(message);
        }
    }
}
