package com.example.commuterx_java

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth

class UsernameFragment : Fragment() {
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_username, container, false)

        database = FirebaseDatabase.getInstance("https://commuterx-8854f-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val usersRef = database.getReference("users")

        val usernameInput = view.findViewById<EditText>(R.id.username_input)
        val submitButton = view.findViewById<Button>(R.id.submit_button)
        submitButton.setBackgroundResource(R.drawable.rounded_button)
        submitButton.invalidate()

        val uid = arguments?.getString("uid") ?: FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            // Handle error: No UID available
            Toast.makeText(context, "Error: No user ID found", Toast.LENGTH_SHORT).show()
            return view
        }

        submitButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                saveUsername(uid, username)
            }
        }

        return view
    }

    private fun saveUsername(uid: String, username: String) {
        val userRef = database.getReference("users").child(uid)

        val user = mapOf("username" to username)

        userRef.setValue(user)
            .addOnSuccessListener {
                Toast.makeText(context, "Username saved successfully", Toast.LENGTH_SHORT).show()
                navigateToMainContent(username)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMainContent(username: String) {
        val bundle = Bundle().apply {
            putString("username", username)
        }
        findNavController().navigate(R.id.action_UsernameFragment_to_FirstFragment, bundle)
    }

    companion object {
        private const val TAG = "UsernameFragment"
    }
}