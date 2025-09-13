package com.example.ble_scoreboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    
    // Firebase Authentication
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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
            // Show progress (in a real app, use a progress dialog or indicator)
            btnLogin.setEnabled(false);
            btnLogin.setText("Signing in...");
            
            // Perform Firebase authentication
            mAuth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        
                        if (task.isSuccessful()) {
                            // Login success
                            FirebaseUser user = mAuth.getCurrentUser();
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                            
                            // Save username for welcome message (use email or display name)
                            String displayName = user.getDisplayName();
                            String email = user.getEmail();
                            prefs.edit().putString("username", displayName != null ? displayName : email).apply();
                            
                            // Navigate to home screen
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Close this activity
                        } else {
                            // Login failed - show generic message instead of exposing Firebase error details
                            Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                            
                            // Log the actual error for debugging (optional)
                            if (task.getException() != null) {
                                Log.d("LoginActivity", "Auth error: " + task.getException().getMessage());
                            }
                        }
                    }
                });
        }
    }
}
