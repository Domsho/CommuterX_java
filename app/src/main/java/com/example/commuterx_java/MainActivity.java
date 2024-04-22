package com.example.commuterx_java;

import static androidx.core.content.ContextCompat.startActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 2023;
    SignInButton signInButton;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            /* recreate(); Don't uncomment */
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView login_email = findViewById(R.id.registration_email);
        TextView login_password = findViewById(R.id.registration_password);

        String clientId = getString(R.string.client_id);

        mAuth = FirebaseAuth.getInstance();


        MaterialButton btnLogin = (MaterialButton) findViewById(R.id.btnLogin);
        MaterialButton btnRegister = (MaterialButton) findViewById(R.id.btnLogin2);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("862287981749-ktdkk509qujb3spdtroci6ovnhe3qa3b.apps.googleusercontent.com")
                .build();

        if (gso == null) {
            Log.d(TAG, "GoogleSignInOptions is not configured correctly.");
        } else {
            Log.d(TAG, "GoogleSignInOptions is configured correctly.");
        }

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email, password;
                email = String.valueOf(login_email.getText());
                password = String.valueOf(login_password.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    updateUI(user);
                                    Toast.makeText(MainActivity.this, "Login Successfull!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                                else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(MainActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                    updateUI((FirebaseUser) null);
                                    Toast.makeText(MainActivity.this, "Incorrect Email or Password",
                                            Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }
        });


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }


    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            if (account != null) {
                String idToken = account.getIdToken();
                String email = account.getEmail(); // Get the user's email address

                Log.d(TAG, "handleSignInResult:account: " + account);
                Log.d(TAG, "handleSignInResult:email: " + email); // Log the email address
                Log.d(TAG, "handleSignInResult:idToken: " + idToken);

                sendIdTokenToServer(idToken);

                updateUI(account);

                // Start the home activity
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // This line is optional, it will close the login activity so it's not in the back stack
            } else {
                ApiException apiException = (ApiException) completedTask.getException();
                Log.w(TAG, "signInResult:failed code=" + apiException.getStatusCode(), apiException);
                updateUI((GoogleSignInAccount) null);
            }
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode(), e);
            updateUI((GoogleSignInAccount) null);
        }
    }


    private void updateUI(GoogleSignInAccount account) {
        // Add a null check here
        if (signInButton != null) {
            if (account != null) {
                Log.d(TAG, "updateUI:account: " + account);
                signInButton.setVisibility(View.GONE);
                TextView userEmail = findViewById(R.id.registration_email);
                userEmail.setText(account.getEmail());
                userEmail.setVisibility(View.VISIBLE);
            } else {
                signInButton.setVisibility(View.VISIBLE);
                TextView userEmail = findViewById(R.id.registration_email);
                userEmail.setVisibility(View.GONE);
            }
        }
    }

    private void sendIdTokenToServer(String idToken) {
        // This is where you would send the idToken to your server
        // You would typically use an HTTP client like OkHttp or Retrofit to send the request
        // This is just a placeholder and won't actually send the idToken anywhere
        Log.d(TAG, "sendIdTokenToServer:idToken: " + idToken);
    }

    private void updateUI(FirebaseUser user) {
        // Add a null check here
        if (signInButton != null) {
            if (user != null) {
                Log.d(TAG, "updateUI:user: " + user);
                signInButton.setVisibility(View.GONE);
                TextView userEmail = findViewById(R.id.registration_email);
                userEmail.setText(user.getEmail());
                userEmail.setVisibility(View.VISIBLE);
            } else {
                signInButton.setVisibility(View.VISIBLE);
                TextView userEmail = findViewById(R.id.registration_email);
                userEmail.setVisibility(View.GONE);
            }
        }
    }
}