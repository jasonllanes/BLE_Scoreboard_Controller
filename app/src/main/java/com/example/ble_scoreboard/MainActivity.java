package com.example.ble_scoreboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        // Check if user is signed in with Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        Intent intent;
        if (currentUser != null) {
            // User is already signed in with Firebase, go to home screen
            intent = new Intent(MainActivity.this, HomeActivity.class);
        } else {
            // User is not signed in, go to login screen
            intent = new Intent(MainActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish(); // Close this activity
    }
}
