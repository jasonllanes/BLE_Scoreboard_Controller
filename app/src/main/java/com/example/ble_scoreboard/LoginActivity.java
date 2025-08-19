package com.example.ble_scoreboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    // Demo credentials (in a real app, these would be securely stored/verified)
    private static final String DEMO_USERNAME = "admin";
    private static final String DEMO_PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        // Set click listener for login button
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        // Reset errors
        etUsername.setError(null);
        etPassword.setError(null);

        // Get values
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            focusView = etPassword;
            cancel = true;
        }

        // Check for a valid username
        if (TextUtils.isEmpty(username)) {
            etUsername.setError("Username is required");
            focusView = etUsername;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with
            // an error
            if (focusView != null) {
                focusView.requestFocus();
            }
        } else {
            // Perform the login attempt
            if (username.equals(DEMO_USERNAME) && password.equals(DEMO_PASSWORD)) {
                // Login success
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().putBoolean("isLoggedIn", true).apply();

                // Save username for welcome message
                prefs.edit().putString("username", username).apply();

                // Navigate to home screen
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Close this activity
            } else {
                // Login failed
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
