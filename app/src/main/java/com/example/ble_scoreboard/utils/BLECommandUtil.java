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
    public static final byte CMD_NULL = '-'; // -: null (changed from '0')
    public static final byte CMD_GAMETIME_SHOTCLOCK_HORN = '_'; // _: gametime Shotclock Horn
    public static final byte CMD_SHOTCLOCK_RESET_14 = 'q'; // q: shotclock reset to 14
    public static final byte CMD_SHOTCLOCK_RESET_24 = 'r'; // r: shotclock reset to 24
    public static final byte CMD_NEW_GAME = 'v'; // v: new game

    // Clock commands
    public static final byte COMMAND_START_CLOCK = 's'; // s: start clock
    public static final byte COMMAND_STOP_CLOCK = 't'; // t: stop clock
    public static final byte COMMAND_RESET_CLOCK = 'u'; // u: reset clock
    public static final byte COMMAND_START_SHOT_CLOCK = 'x'; // x: start shot clock
    public static final byte COMMAND_STOP_SHOT_CLOCK = 'y'; // y: stop shot clock
    public static final byte COMMAND_RESET_SHOT_CLOCK = 'z'; // z: reset shot clock
    // Reusing existing constants for shot clock
    public static final byte COMMAND_SHOT_CLOCK_14 = CMD_SHOTCLOCK_RESET_14; // q: set shot clock to 14
    public static final byte COMMAND_SHOT_CLOCK_24 = CMD_SHOTCLOCK_RESET_24; // r: set shot clock to 24
    // Reusing existing constant for buzzer
    public static final byte COMMAND_BUZZER = CMD_GAMETIME_SHOTCLOCK_HORN; // _: buzzer/horn
    public static final byte COMMAND_SHOT_CLOCK_BUZZER = CMD_GAMETIME_SHOTCLOCK_HORN; // _: shot clock buzzer (same as
                                                                                      // game buzzer)

    // Clock digit commands - digits 0-9 are sent as '0'-'9' characters
    public static final byte COMMAND_DIGIT_0 = '0'; // Digit 0
    public static final byte COMMAND_DIGIT_1 = '1'; // Digit 1
    public static final byte COMMAND_DIGIT_2 = '2'; // Digit 2
    public static final byte COMMAND_DIGIT_3 = '3'; // Digit 3
    public static final byte COMMAND_DIGIT_4 = '4'; // Digit 4
    public static final byte COMMAND_DIGIT_5 = '5'; // Digit 5
    public static final byte COMMAND_DIGIT_6 = '6'; // Digit 6
    public static final byte COMMAND_DIGIT_7 = '7'; // Digit 7
    public static final byte COMMAND_DIGIT_8 = '8'; // Digit 8
    public static final byte COMMAND_DIGIT_9 = '9'; // Digit 9

    // Clock position commands - where to place the digits
    public static final byte COMMAND_CLOCK_MIN1_POS = 'M'; // First minute digit position
    public static final byte COMMAND_CLOCK_MIN2_POS = 'N'; // Second minute digit position
    public static final byte COMMAND_CLOCK_SEC1_POS = 'S'; // First second digit position
    public static final byte COMMAND_CLOCK_SEC2_POS = 'T'; // Second second digit position

    // Team A commands
    public static final byte CMD_TEAM_A_SCORE_PLUS_1 = 'j'; // j: team a score +1
    public static final byte CMD_TEAM_A_SCORE_PLUS_2 = 'k'; // k: team a score +2
    public static final byte CMD_TEAM_A_SCORE_MINUS_1 = 'm'; // l: team a score -1
    public static final byte CMD_TEAM_A_FOUL_PLUS_1 = 'l'; // m: team a foul +1 (was incorrectly labeled as -1)
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
    public static final byte CMD_RIGHT_ARROW = 'W'; // V: right arrow
    public static final byte CMD_LEFT_ARROW = 'V'; // W: left arrow

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
     * Static method to get description for command byte
     * 
     * @param commandByte The command byte
     * @return Description of the command
     */
    public static String getCommandDescription(byte commandByte) {
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
            // Clock commands
            case COMMAND_START_CLOCK:
                return "Start Clock";
            case COMMAND_STOP_CLOCK:
                return "Stop Clock";
            case COMMAND_RESET_CLOCK:
                return "Reset Clock";
            case COMMAND_START_SHOT_CLOCK:
                return "Start Shot Clock";
            case COMMAND_STOP_SHOT_CLOCK:
                return "Stop Shot Clock";
            case COMMAND_RESET_SHOT_CLOCK:
                return "Reset Shot Clock";
            // Clock digit commands
            case COMMAND_DIGIT_0:
                return "Digit 0";
            case COMMAND_DIGIT_1:
                return "Digit 1";
            case COMMAND_DIGIT_2:
                return "Digit 2";
            case COMMAND_DIGIT_3:
                return "Digit 3";
            case COMMAND_DIGIT_4:
                return "Digit 4";
            case COMMAND_DIGIT_5:
                return "Digit 5";
            case COMMAND_DIGIT_6:
                return "Digit 6";
            case COMMAND_DIGIT_7:
                return "Digit 7";
            case COMMAND_DIGIT_8:
                return "Digit 8";
            case COMMAND_DIGIT_9:
                return "Digit 9";
            // Clock position commands
            case COMMAND_CLOCK_MIN1_POS:
                return "First Minute Position";
            case COMMAND_CLOCK_MIN2_POS:
                return "Second Minute Position";
            case COMMAND_CLOCK_SEC1_POS:
                return "First Second Position";
            case COMMAND_CLOCK_SEC2_POS:
                return "Second Second Position";
            default:
                return "Unknown Command: " + commandByte;
        }
    }

    /**
     * Sends a time update to the BLE device with individual digit commands
     * 
     * @param minutes Minutes to display (0-99)
     * @param seconds Seconds to display (0-59)
     * @return true if all commands were sent successfully
     */
    public boolean sendTimeUpdate(int minutes, int seconds) {
        // Break down the time into individual digits
        int min1 = minutes / 10;
        int min2 = minutes % 10;
        int sec1 = seconds / 10;
        int sec2 = seconds % 10;

        // Send each position and digit command
        boolean result = true;

        // Minutes first digit
        result &= sendCommand(COMMAND_CLOCK_MIN1_POS);
        result &= sendCommand((byte) (COMMAND_DIGIT_0 + min1));

        // Minutes second digit
        result &= sendCommand(COMMAND_CLOCK_MIN2_POS);
        result &= sendCommand((byte) (COMMAND_DIGIT_0 + min2));

        // Seconds first digit
        result &= sendCommand(COMMAND_CLOCK_SEC1_POS);
        result &= sendCommand((byte) (COMMAND_DIGIT_0 + sec1));

        // Seconds second digit
        result &= sendCommand(COMMAND_CLOCK_SEC2_POS);
        result &= sendCommand((byte) (COMMAND_DIGIT_0 + sec2));

        updateStatus("Time update: " + min1 + min2 + ":" + sec1 + sec2);
        return result;
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
