package com.example.commuterx_java

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        mAuth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnRegister)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)
        val btnFacebook = findViewById<Button>(R.id.btnFacebook)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            when {
                TextUtils.isEmpty(email) -> {
                    Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                TextUtils.isEmpty(password) -> {
                    Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !isPasswordValid(password) -> {
                    Toast.makeText(this,
                        "Password must contain:\n" +
                                "- At least 8 characters\n" +
                                "- One uppercase letter\n" +
                                "- One lowercase letter\n" +
                                "- One number\n" +
                                "- One special character (@#\$%^&+=)",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                else -> {
                    registerUser(email, password)
                }
            }
        }

        btnGoogle.setOnClickListener {
            // Implement Google Sign-In
            Toast.makeText(this, "Google Sign-In not implemented", Toast.LENGTH_SHORT).show()
        }

        btnFacebook.setOnClickListener {
            // Implement Facebook Sign-In
            Toast.makeText(this, "Facebook Sign-In not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun registerUser(email: String, password: String) {
        if (!isPasswordValid(password)) {
            Toast.makeText(this,
                "Password must contain at least 8 characters, including uppercase, lowercase, number, and special character",
                Toast.LENGTH_LONG).show()
            return
        }

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Account Creation Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}