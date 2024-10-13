package com.example.commuterx_java

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.facebook.AccessToken
import com.facebook.CallbackManager
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainFragment : Fragment() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var callbackManager: CallbackManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_main, container, false)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://commuterx-8854f-default-rtdb.asia-southeast1.firebasedatabase.app/")
        callbackManager = CallbackManager.Factory.create()

        setupViews(view)
        setupGoogleSignIn(view)
        checkCurrentUser()

        return view
    }

    private fun setupViews(view: View) {
        val loginEmail = view.findViewById<TextView>(R.id.registration_email)
        val loginPassword = view.findViewById<TextView>(R.id.registration_password)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btnLogin2)

        btnLogin.setOnClickListener {
            val email = loginEmail.text.toString()
            val password = loginPassword.text.toString()
            if (validateInputs(email, password)) {
                signInWithEmailAndPassword(email, password)
            }
        }

        btnRegister.setOnClickListener {
            // Navigate to SignupActivity
            startActivity(Intent(requireContext(), SignupActivity::class.java))
        }
    }


    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                updateUI(null)
            }
        }
    }

    private fun setupGoogleSignIn(view: View) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("862287981749-ktdkk509qujb3spdtroci6ovnhe3qa3b.apps.googleusercontent.com")
            .build()

        val mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        view.findViewById<MaterialButton>(R.id.sign_in_button).setOnClickListener {
            googleSignInLauncher.launch(mGoogleSignInClient.signInIntent)
        }
    }

    private fun checkCurrentUser() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            checkExistingUsername(currentUser.uid)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(requireContext(), "Enter Email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(requireContext(), "Enter Password", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = mAuth.currentUser
                    updateUI(user)
                    Toast.makeText(requireContext(), "Login Successful!", Toast.LENGTH_SHORT).show()
                    // Navigate to HomeActivity or FirstFragment
                    findNavController().navigate(R.id.action_MainFragment_to_FirstFragment)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = mAuth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    checkExistingUsername(user!!.uid)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun checkExistingUsername(uid: String) {
        database.getReference("users").child(uid).child("username")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        navigateToMainContent(snapshot.value as String)
                    } else {
                        navigateToUsernameFragment(uid)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "checkExistingUsername:onCancelled", error.toException())
                }
            })
    }

    private fun navigateToMainContent(username: String) {
        val bundle = Bundle().apply {
            putString("username", username)
        }
        try {
            findNavController().navigate(R.id.action_MainFragment_to_FirstFragment, bundle)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unable to navigate to FirstFragment", e)
            // Handle navigation failure (e.g., show an error message)
        }
    }

    private fun navigateToUsernameFragment(uid: String) {
        val bundle = Bundle().apply {
            putString("uid", uid)
        }
        findNavController().navigate(R.id.action_MainFragment_to_UsernameFragment, bundle)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            checkExistingUsername(user.uid)
        }
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val RC_SIGN_IN = 9001
    }
}