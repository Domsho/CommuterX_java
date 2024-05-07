package com.example.commuterx_java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UsernameFragment extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_username, container, false);

        FirebaseFirestore db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "asia-southeast1");

        EditText usernameInput = view.findViewById(R.id.username_input);
        Button submitButton = view.findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString();

            // Create a new user with a username
            Map<String, Object> user = new HashMap<>();
            user.put("username", username);

            // Add a new document with a generated ID
            db.collection("users")
                    .document() // Create a new document
                    .set(user) // Set the document with the user map
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getActivity(), "Username saved successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "Error adding document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            // Navigate to the FirstFragment
            NavHostFragment.findNavController(UsernameFragment.this)
                    .navigate(R.id.action_UsernameFragment_to_FirstFragment);
        });

        return view;
    }
}