package com.example.commuterx_java

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {
    var mAuth: FirebaseAuth? = null

    /*  @Override <--------------- UNCOMMENT IF U DON'T WANNA LOG IN
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            finish();
        }
    } */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        mAuth = FirebaseAuth.getInstance()
        val registration_email = findViewById<EditText>(R.id.registration_email)
        val registration_password = findViewById<EditText>(R.id.registration_password)
        val btnRegister = findViewById<View>(R.id.btnRegister) as MaterialButton

        val btnLogin2 = findViewById<View>(R.id.btnLogin2) as MaterialButton


        btnRegister.setOnClickListener(View.OnClickListener {
            val email = registration_email.text.toString()
            val password = registration_password.text.toString()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@SignupActivity, "Enter Email", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@SignupActivity, "Enter Password", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            mAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@SignupActivity, "Account Created!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@SignupActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            this@SignupActivity, "Account Creation Failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        })
    }
}