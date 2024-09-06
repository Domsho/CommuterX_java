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
import com.google.firebase.database.FirebaseDatabase

class UsernameFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_username, container, false)

        val database =
            FirebaseDatabase.getInstance("https://commuterx-8854f-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val myRef = database.getReference("users")

        val usernameInput = view.findViewById<EditText>(R.id.username_input)
        val submitButton = view.findViewById<Button>(R.id.submit_button)
        submitButton.setBackgroundResource(R.drawable.rounded_button)
        submitButton.invalidate()

        val activity: Activity? = activity
        if (activity != null) {
            val sharedPreferences =
                activity.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", "")
            Log.d(TAG, "UID: $uid")

            submitButton.setOnClickListener { v: View? ->
                val username = usernameInput.text.toString()
                if (username.isEmpty()) {
                    Toast.makeText(getActivity(), "Username cannot be empty", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Log.d(TAG, "Username: $username")
                    // Save the username to the Firebase Realtime Database
                }

                val user: MutableMap<String, Any> = HashMap()
                user["username"] = username

                myRef.child(uid!!).setValue(user)
                    .addOnSuccessListener {
                        Toast.makeText(
                            getActivity(),
                            "Username saved successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            getActivity(),
                            "Error adding document: " + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                val bundle = Bundle()
                bundle.putString("username", username)
                Log.d(TAG, "Username added to Bundle: $username")
                NavHostFragment.findNavController(this@UsernameFragment)
                    .navigate(R.id.action_UsernameFragment_to_FirstFragment, bundle)
            }
        }
        return view
    }

    companion object {
        private const val TAG = "UsernameFragment"
    }
}