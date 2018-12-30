package com.imperialllama.llamagram.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.imperialllama.llamagram.R;
import com.imperialllama.llamagram.models.Image;
import com.imperialllama.llamagram.models.Like;
import com.imperialllama.llamagram.models.User;
import com.imperialllama.llamagram.utilities.ImageAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import es.dmoral.toasty.Toasty;

public class FeedActivity extends AppCompatActivity {
    private static final String TAG = FeedActivity.class.getSimpleName();
    private static final int RC_PERMISSION_READ_EXTERNAL_STORAGE = 1;
    private static final int RC_IMAGE_GALLERY = 2;

    private FirebaseUser fbUser;
    private DatabaseReference database;

    private Toast mToast;

    @BindView(R.id.recycler_view)
    public RecyclerView recyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private ImageAdapter mAdapter;
    ArrayList<Image> images = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        ButterKnife.bind(this);

        fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            finish();
        }

        database = FirebaseDatabase.getInstance().getReference();

        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new ImageAdapter(images, this);
        recyclerView.setAdapter(mAdapter);

        Query imagesQuery = database.child("images").orderByKey().limitToFirst(100);
        imagesQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // new image added, add it to the display list
                final Image image = dataSnapshot.getValue(Image.class);

                // get the image user
                database.child("users").child(image.userId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        image.user = user;
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        databaseError.toException().printStackTrace();
                        Log.d(TAG, "Error happened: " + databaseError.getMessage());
                    }
                });

                // get the image's likes
                Query likesQuery = database.child("likes").orderByChild("imageId").equalTo(image.key);
                likesQuery.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Like like = dataSnapshot.getValue(Like.class);
                        image.addLike();
                        if (like.userId.equals(fbUser.getUid())) {
                            image.hasLiked = true;
                            image.userLike = dataSnapshot.getKey();
                        }
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Like like = dataSnapshot.getValue(Like.class);
                        image.removeLike();
                        if (like.userId.equals(fbUser.getUid())) {
                            image.hasLiked = false;
                            image.userLike = null;
                        }
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                mAdapter.addImage(image);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, RC_IMAGE_GALLERY);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_IMAGE_GALLERY:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                    StorageReference imagesRef = storageRef.child("images");
                    StorageReference userRef = imagesRef.child(fbUser.getUid());
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String filename = fbUser.getUid() + "_" + timeStamp;
                    StorageReference fileRef = userRef.child(filename);

                    if (mToast != null)
                    	mToast.cancel();
                    mToast = Toasty.info(FeedActivity.this, getString(R.string.image_uploading), Toast.LENGTH_SHORT);
                    mToast.show();

                    UploadTask uploadTask = fileRef.putFile(uri);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle unsuccessful uploads
                            Log.d(TAG, "Upload failed: " + e.getMessage());
                            if (mToast != null)
                            	mToast.cancel();
                            mToast = Toasty.error(FeedActivity.this, getString(R.string.error_upload) + e.getMessage(), Toast.LENGTH_LONG);
                            mToast.show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Upload succeeded
                            Uri downloadUri = taskSnapshot.getDownloadUrl();
                            Log.d(TAG, "Upload succeeded: " + downloadUri.toString());
                            if (mToast != null)
                            	mToast.cancel();
                            mToast = Toasty.success(FeedActivity.this, getString(R.string.success_upload), Toast.LENGTH_SHORT);
                            mToast.show();

                            // Save image to the database
                            String key = database.child("images").push().getKey();
                            Image image = new Image(key, fbUser.getUid(), downloadUri.toString());
                            database.child("images").child(key).setValue(image);
                        }
                    });
                }
                break;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.user_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.signout_menu:
				AuthUI.getInstance()
						.signOut(this)
						.addOnCompleteListener(new OnCompleteListener<Void>() {
							@Override
							public void onComplete(@NonNull Task<Void> task) {
								// user is now signed out
								startActivity(new Intent(FeedActivity.this, MainActivity.class));
								finish();
							}
						});
				return true;
            case R.id.add_photo_menu:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                                                this,
                                                new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                                                RC_PERMISSION_READ_EXTERNAL_STORAGE);
                } else {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, RC_IMAGE_GALLERY);
                }
                return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

    public void setLiked(Image image) {
        if (!image.hasLiked) {
            image.hasLiked = true;
            Like like = new Like(image.key, fbUser.getUid());
            String key = database.child("likes").push().getKey();
            database.child("likes").child(key).setValue(like);
            image.userLike = key;
        } else {
            image.hasLiked = false;
            if (image.userLike != null)
                database.child("likes").child(image.userLike).removeValue();
        }
    }
}
