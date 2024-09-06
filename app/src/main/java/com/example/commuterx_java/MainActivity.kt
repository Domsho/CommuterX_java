package com.example.commuterx_java

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.FacebookSdk.setClientToken
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    var mAuth: FirebaseAuth? = null
    var signInButton: SignInButton? = null
    private var callbackManager: CallbackManager? = null
    private val firebaseAuth: FirebaseAuth? = null
    private var navController: NavController? = null

    public override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        if (currentUser != null) {
            /* recreate(); Don't uncomment */
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val login_email = findViewById<TextView>(R.id.registration_email)
        val login_password = findViewById<TextView>(R.id.registration_password)

        setClientToken("958247419075931")

        sdkInitialize(applicationContext)

        val clientId = getString(R.string.client_id)

        mAuth = FirebaseAuth.getInstance()

        navController = findNavController(this, R.id.nav_host_fragment)


        val accessToken: AccessToken? = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired


        callbackManager = create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    val accessToken = loginResult.accessToken
                }

                override fun onCancel() {
                    // App code
                }

                override fun onError(exception: FacebookException) {
                    // App code
                }
            })

        val loginButton = findViewById<LoginButton>(R.id.login_button)
        loginButton.setReadPermissions("email")

        loginButton.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this@MainActivity,
                mutableListOf("public_profile", "email")
            )
        }

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    // Get the access token
                    val accessToken = loginResult.accessToken
                    // Use the access token
                }

                override fun onCancel() {
                    // Handle cancellation
                }

                override fun onError(exception: FacebookException) {
                    // Handle error
                }
            })

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            val databaseReference =
                FirebaseDatabase.getInstance("https://commuterx-8854f-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
            databaseReference.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // User already has a username, navigate to FirstFragment
                            navController!!.navigate(R.id.FirstFragment)
                        } else {
                            // User does not have a username, navigate to UsernameFragment
                            navController!!.navigate(R.id.UsernameFragment)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log error
                    }
                })
        } else {
            // No user is signed in, navigate to UsernameFragment
            navController!!.navigate(R.id.UsernameFragment)
        }

        val btnLogin = findViewById<View>(R.id.btnLogin) as MaterialButton
        val btnRegister = findViewById<View>(R.id.btnLogin2) as MaterialButton

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("862287981749-ktdkk509qujb3spdtroci6ovnhe3qa3b.apps.googleusercontent.com")
            .build()

        if (gso == null) {
            Log.d(TAG, "GoogleSignInOptions is not configured correctly.")
        } else {
            Log.d(TAG, "GoogleSignInOptions is configured correctly.")
        }


        val mGoogleSignInClient = GoogleSignIn.getClient(
            this, gso
        )

        val signInButton = findViewById<SignInButton>(R.id.sign_in_button)
        signInButton.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        btnLogin.setOnClickListener(View.OnClickListener {
            val email = login_email.text.toString()
            val password = login_password.text.toString()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@MainActivity, "Enter Email", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@MainActivity, "Enter Password", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = mAuth!!.currentUser
                        updateUI(user)
                        Toast.makeText(this@MainActivity, "Login Successfull!", Toast.LENGTH_SHORT)
                            .show()
                        val intent = Intent(applicationContext, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this@MainActivity, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(null as FirebaseUser?)
                        Toast.makeText(
                            this@MainActivity, "Incorrect Email or Password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        })


        btnRegister.setOnClickListener {
            val intent = Intent(this@MainActivity, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    // Sign in success
                } else {
                    // Sign in failure
                }
            }
    }


    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager!!.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            if (account != null) {
                val idToken = account.idToken
                val email = account.email // Get the user's email address

                Log.d(TAG, "handleSignInResult:account: $account")
                Log.d(TAG, "handleSignInResult:email: $email") // Log the email address
                Log.d(TAG, "handleSignInResult:idToken: $idToken")

                sendIdTokenToServer(idToken)

                updateUI(account)

                val sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
                val myEdit = sharedPreferences.edit()
                myEdit.putString("uid", account.id)
                myEdit.apply()

                // Start the home activity
                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                startActivity(intent)
                finish() // This line is optional, it will close the login activity so it's not in the back stack
            } else {
                val apiException = completedTask.exception as ApiException?
                Log.w(TAG, "signInResult:failed code=" + apiException!!.statusCode, apiException)
                updateUI(null as GoogleSignInAccount?)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode, e)
            updateUI(null as GoogleSignInAccount?)
        }
    }


    private fun updateUI(account: GoogleSignInAccount?) {
        // Add a null check here
        if (signInButton != null) {
            if (account != null) {
                Log.d(TAG, "updateUI:account: $account")
                signInButton!!.visibility = View.GONE
                val userEmail = findViewById<TextView>(R.id.registration_email)
                userEmail.text = account.email
                userEmail.visibility = View.VISIBLE
            } else {
                signInButton!!.visibility = View.VISIBLE
                val userEmail = findViewById<TextView>(R.id.registration_email)
                userEmail.visibility = View.GONE
            }
        }
    }

    private fun sendIdTokenToServer(idToken: String?) {
        // This is where you would send the idToken to your server
        // You would typically use an HTTP client like OkHttp or Retrofit to send the request
        // This is just a placeholder and won't actually send the idToken anywhere
        Log.d(TAG, "sendIdTokenToServer:idToken: $idToken")
    }


    private fun updateUI(user: FirebaseUser?) {
        // Add a null check here
        if (signInButton != null) {
            if (user != null) {
                Log.d(TAG, "updateUI:user: $user")
                signInButton!!.visibility = View.GONE
                val userEmail = findViewById<TextView>(R.id.registration_email)
                userEmail.text = user.email
                userEmail.visibility = View.VISIBLE
            } else {
                signInButton!!.visibility = View.VISIBLE
                val userEmail = findViewById<TextView>(R.id.registration_email)
                userEmail.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 2023
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Find the current fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navHostFragment?.let { navFragment ->
            // Iterate through child fragments to find FirstFragment
            navFragment.childFragmentManager.fragments.forEach { fragment ->
                if (fragment is FirstFragment) {
                    // Delegate the permission result to FirstFragment
                    fragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
                    return
                }
            }
        }

        // If FirstFragment is not found, log an error
        Log.e("MainActivity", "FirstFragment not found")
    }
}