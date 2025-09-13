package com.example.ble_scoreboard.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing game clock and shot clock
 */
public class ClockManager {
    private static final String TAG = "ClockManager";

    // Clock states
    public static final int STATE_STOPPED = 0;
    public static final int STATE_RUNNING = 1;
    public static final int STATE_PAUSED = 2;

    // Default values
    private static final int DEFAULT_GAME_MINUTES = 10;
    private static final int DEFAULT_GAME_SECONDS = 0;
    private static final int DEFAULT_SHOT_CLOCK = 24;
    private static final int DEFAULT_MSEC = 0;

    // Clock variables
    private int minutes;
    private int seconds;
    private int milliseconds;
    private int shotClock;

    // Individual digit variables as shown in the block-based code
    private int min1; // First digit of minutes
    private int min2; // Second digit of minutes
    private int sec1; // First digit of seconds
    private int sec2; // Second digit of seconds
    private int mSec; // Milliseconds (tenths of seconds)

    // Shot clock digits
    private int shot1; // First digit of shot clock
    private int shot2; // Second digit of shot clock

    // Clock state
    private int clockState;
    private boolean shotClockEnabled;

    // Listeners
    private List<ClockUpdateListener> listeners = new ArrayList<>();

    // Handler for timer
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;
    private static final int TICK_INTERVAL = 100; // 100ms interval - better for BLE stability

    // Singleton instance
    private static ClockManager instance;

    public static synchronized ClockManager getInstance() {
        if (instance == null) {
            instance = new ClockManager();
        }
        return instance;
    }

    private ClockManager() {
        resetToDefaults();
        initializeClockRunnable();
    }

    private void initializeClockRunnable() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (clockState == STATE_RUNNING) {
                    decrementClock();
                    updateDigitVariables();
                    notifyClockUpdate();
                    handler.postDelayed(this, TICK_INTERVAL);
                }
            }
        };
    }

    public void resetToDefaults() {
        minutes = DEFAULT_GAME_MINUTES;
        seconds = DEFAULT_GAME_SECONDS;
        milliseconds = DEFAULT_MSEC;
        shotClock = DEFAULT_SHOT_CLOCK;
        clockState = STATE_STOPPED;
        shotClockEnabled = true;
        updateDigitVariables();
    }

    public void startClock() {
        if (clockState != STATE_RUNNING) {
            clockState = STATE_RUNNING;
            handler.post(clockRunnable);
            notifyClockStateChange();
        }
    }

    public void stopClock() {
        if (clockState == STATE_RUNNING) {
            clockState = STATE_STOPPED;
            handler.removeCallbacks(clockRunnable);
            notifyClockStateChange();
        }
    }

    public void pauseClock() {
        if (clockState == STATE_RUNNING) {
            clockState = STATE_PAUSED;
            handler.removeCallbacks(clockRunnable);
            notifyClockStateChange();
        }
    }

    private void decrementClock() {
        // Decrease milliseconds by 100ms (1 tenth of a second)
        milliseconds -= 100;

        // Handle millisecond wrap-around
        if (milliseconds < 0) {
            milliseconds = 900; // Reset to 900ms
            seconds--;

            // Decrement shot clock on each second
            if (shotClockEnabled && shotClock > 0) {
                shotClock--;
                updateShotClockDigits();
            }

            // Handle seconds wrap-around
            if (seconds < 0) {
                seconds = 59;
                minutes--;

                // Handle clock reaching zero
                if (minutes < 0) {
                    clockState = STATE_STOPPED;
                    minutes = 0;
                    seconds = 0;
                    milliseconds = 0;
                    notifyGameClockExpired();
                }
            }
        }
    }

    // Update individual digit variables
    private void updateDigitVariables() {
        // Update game clock digits
        min1 = minutes / 10;
        min2 = minutes % 10;
        sec1 = seconds / 10;
        sec2 = seconds % 10;
        mSec = milliseconds / 100; // Convert to tenths of second

        // Update shot clock digits
        updateShotClockDigits();
    }

    private void updateShotClockDigits() {
        shot1 = shotClock / 10;
        shot2 = shotClock % 10;
    }

    // Set game clock
    public void setGameClock(int minutes, int seconds) {
        this.minutes = Math.min(Math.max(minutes, 0), 99); // Clamp between 0-99
        this.seconds = Math.min(Math.max(seconds, 0), 59); // Clamp between 0-59
        this.milliseconds = 0;
        updateDigitVariables();
        notifyClockUpdate();
    }

    // Set shot clock
    public void setShotClock(int seconds) {
        this.shotClock = Math.min(Math.max(seconds, 0), 99); // Clamp between 0-99
        updateShotClockDigits();
        notifyClockUpdate();
    }

    // Reset shot clock to standard values
    public void resetShotClockTo14() {
        setShotClock(14);
    }

    public void resetShotClockTo24() {
        setShotClock(24);
    }

    // Get current clock values
    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMilliseconds() {
        return milliseconds;
    }

    public int getShotClock() {
        return shotClock;
    }

    // Get individual digit values
    public int getMin1() {
        return min1;
    }

    public int getMin2() {
        return min2;
    }

    public int getSec1() {
        return sec1;
    }

    public int getSec2() {
        return sec2;
    }

    public int getMSec() {
        return mSec;
    }

    public int getShot1() {
        return shot1;
    }

    public int getShot2() {
        return shot2;
    }

    public int getClockState() {
        return clockState;
    }

    public boolean isShotClockEnabled() {
        return shotClockEnabled;
    }

    public void setShotClockEnabled(boolean enabled) {
        this.shotClockEnabled = enabled;
    }

    // Register for clock updates
    public void addClockUpdateListener(ClockUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeClockUpdateListener(ClockUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyClockUpdate() {
        for (ClockUpdateListener listener : listeners) {
            listener.onClockUpdate(minutes, seconds, milliseconds, shotClock);
        }
    }

    private void notifyClockStateChange() {
        for (ClockUpdateListener listener : listeners) {
            listener.onClockStateChanged(clockState);
        }
    }

    private void notifyGameClockExpired() {
        for (ClockUpdateListener listener : listeners) {
            listener.onGameClockExpired();
        }
    }

    private void notifyShotClockExpired() {
        for (ClockUpdateListener listener : listeners) {
            listener.onShotClockExpired();
        }
    }

    // Clock update listener interface
    public interface ClockUpdateListener {
        void onClockUpdate(int minutes, int seconds, int milliseconds, int shotClock);

        void onClockStateChanged(int state);

        void onGameClockExpired();

        void onShotClockExpired();
    }
}
