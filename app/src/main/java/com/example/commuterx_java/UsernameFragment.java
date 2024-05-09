package com.example.commuterx_java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class UsernameFragment extends Fragment {

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_username, container, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://commuterx-8854f-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference myRef = database.getReference("users");

        EditText usernameInput = view.findViewById(R.id.username_input);
        Button submitButton = view.findViewById(R.id.submit_button);

        submitButton.setOnClickListener(v -> {
            Log.d("UsernameFragment", "Submit button clicked");
            String username = usernameInput.getText().toString();
            Log.d("UsernameFragment", "Username: " + username);


            Map<String, Object> user = new HashMap<>();
            user.put("username", username);



            myRef.push().setValue(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getActivity(), "Username saved successfully", Toast.LENGTH_SHORT).show();

                            Bundle bundle = new Bundle();
                            bundle.putString("username", username);

                            NavHostFragment.findNavController(UsernameFragment.this)
                                    .navigate(R.id.action_UsernameFragment_to_FirstFragment, bundle);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "Error adding document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        return view;
    }
}