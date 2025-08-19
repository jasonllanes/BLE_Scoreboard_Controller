package com.example.ble_scoreboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1000; // 1 second delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handler to delay for a splash screen effect
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_DELAY);
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        Intent intent;
        if (isLoggedIn) {
            // User is already logged in, go to home screen
            intent = new Intent(MainActivity.this, HomeActivity.class);
        } else {
            // User is not logged in, go to login screen
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Close this activity
    }
}
