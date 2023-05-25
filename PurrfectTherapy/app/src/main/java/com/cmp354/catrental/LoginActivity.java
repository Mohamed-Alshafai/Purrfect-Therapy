package com.cmp354.catrental;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etEmail;
    private EditText etPassword;
    private TextView etSignUp;
    private Button btnLogin;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.email_edittext);
        etPassword = findViewById(R.id.password_edittext);
        etSignUp = findViewById(R.id.signup_edittext);
        btnLogin = findViewById(R.id.login_button);

        etSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Signup");
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View viewInflated = inflater.inflate(R.layout.signup_dialog, null);
                EditText emailInput = (EditText) viewInflated.findViewById(R.id.email_dialog);
                EditText passwordInput = (EditText) viewInflated.findViewById(R.id.password_dialog);
                EditText confirmInput = (EditText) viewInflated.findViewById(R.id.confirm_password_dialog);
                EditText contactInput = viewInflated.findViewById(R.id.contact_dialog);
                builder.setView(viewInflated)
                        // Add action buttons
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String email = emailInput.getText().toString().trim();
                                String password = passwordInput.getText().toString().trim();
                                String confirmPassword = confirmInput.getText().toString().trim();
                                String contactNumber = contactInput.getText().toString().trim();
                                String error = null;
                                if (email.isBlank()) {
                                    error = "Email is required";
                                }
                                else if (password.isBlank() || confirmPassword.isBlank()) {
                                    error = "Password is required";
                                }
                                else if (!password.equals(confirmPassword)) {
                                    error = "Passwords must match";
                                }
                                else if (contactNumber.isBlank() || contactNumber.length() < 10) {
                                    error = "Must provide a valid contact number";
                                }
                                if (error != null) {
                                    AlertDialog.Builder warn = new AlertDialog.Builder(LoginActivity.this);
                                    warn.setTitle("Error");
                                    warn.setMessage(error);
                                    warn.setPositiveButton("Ok", null);
                                    warn.show();
                                    return;
                                }
                                mAuth.getInstance().createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {
                                                    // Sign up successful, update UI accordingly
                                                    Map<String, String> contact = new HashMap<>();
                                                    contact.put("Contact", contactNumber);
                                                    FirebaseFirestore.getInstance().collection("Contacts").document(email).set(contact)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(LoginActivity.this, "Signup succeeded.", Toast.LENGTH_SHORT).show();

                                                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                                                intent.putExtra("email", email);
                                                                startActivity(intent);
                                                            }
                                                            else {
                                                                Toast.makeText(LoginActivity.this, "Signup failed. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                                mAuth.getCurrentUser().delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(TAG, "User Deteled");
                                                                            AlertDialog.Builder warn = new AlertDialog.Builder(LoginActivity.this);
                                                                            warn.setTitle("Error");
                                                                            warn.setMessage("Please Sign Up Again");
                                                                            warn.setPositiveButton("Ok", null);
                                                                            warn.show();
                                                                        }
                                                                        else {
                                                                            Log.d(TAG, "User Not Deleted!!! " + task.getException());
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    // Sign up failed, display an error message to the user
                                                    Log.w(TAG, "signUpWithEmail:failure", task.getException());
                                                    AlertDialog.Builder warn = new AlertDialog.Builder(LoginActivity.this);
                                                    warn.setTitle("Error");
                                                    warn.setMessage("Sign up failed: " + task.getException().getMessage());
                                                    warn.setPositiveButton("Ok", null);
                                                    warn.show();
                                                }
                                            }
                                        });
                                dialog.dismiss();
                            }
                        }).setNegativeButton("Cancel", null);
                builder.create();
                builder.show();
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();
                if (email.isBlank()) {
                    etEmail.setError("Email is required");
                    return;
                }
                if (password.isBlank()) {
                    etPassword.setError("Password is required");
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Authentication succeeded.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(LoginActivity.this, "Authentication failed. " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

}