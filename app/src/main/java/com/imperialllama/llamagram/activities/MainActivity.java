package com.imperialllama.llamagram.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.imperialllama.llamagram.BuildConfig;
import com.imperialllama.llamagram.R;
import com.imperialllama.llamagram.models.User;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 123;

    @BindView(R.id.button_sign_in)
    public ImageButton buttonSignIn;

    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build()))
                                .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                                .build(),
                        RC_SIGN_IN
                );
            }
        });

        database = FirebaseDatabase.getInstance().getReference();

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            // User already signed in
            // get the FCM token
            String token = FirebaseInstanceId.getInstance().getToken();

            // save the user info in the database to /users/$UID/
            User user = new User(fbUser.getUid(), fbUser.getDisplayName(), token);
            database.child("users").child(user.uid).setValue(user);

            Log.d(TAG, "Signed in as " + fbUser.getDisplayName());
            Intent i = new Intent(this, FeedActivity.class);
            startActivity(i);
        } else {
            startActivityForResult(
            		AuthUI.getInstance()
				            .createSignInIntentBuilder()
				            .setAvailableProviders(Arrays.asList(
				            		new AuthUI.IdpConfig.EmailBuilder().build(),
						            new AuthUI.IdpConfig.GoogleBuilder().build()))
                            .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
				            .build(),
		            RC_SIGN_IN
            );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                IdpResponse response = IdpResponse.fromResultIntent(data);

                if (resultCode == RESULT_OK) {
                    // Successfully signed in
                    // get the current user
                    FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();

                    // get the FCM token
                    String token = FirebaseInstanceId.getInstance().getToken();

                    // save the user info in the database to /users/$UID/
                    User user = new User(fbUser.getUid(), fbUser.getDisplayName(), token);
                    database.child("users").child(user.uid).setValue(user);

                    // go to FeedActivity
                    Intent i = new Intent(this, FeedActivity.class);
                    startActivity(i);
                } else {
                    // Sign in failed, check response for error code
                    if (response != null)
                        Toasty.error(this, response.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }


}
