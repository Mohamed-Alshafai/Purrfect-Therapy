package com.cmp354.catrental;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ProfileActivity extends AppCompatActivity {
    private final String TAG = "ProfileActivity";
    TextView tvEmail;
    TextView tvNumber;
    ListView listViewMyCats;
    ArrayList<String> docIds;
    FirebaseFirestore db;
    // GPS member variables
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    EditText catBreed;
    EditText catPrice;
    EditText catCity;
    EditText catName;
    Button uploadButton;
    Button removeImageButton;
    ImageView uploadImage;
    Toolbar toolbar;
    Uri imageUri;
    Bitmap bitmap;
    private LocationManager locationManager;//for requesting updates and checking if GPS is on

    //Intent that returns Uri for the image
    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        imageUri = data.getData();
                        uploadImage.setVisibility(View.VISIBLE);
                        uploadImage.setImageURI(imageUri);
                        removeImageButton.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(ProfileActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // get firebase instance
        db = FirebaseFirestore.getInstance();
        // retrieve views
        tvEmail = findViewById(R.id.editTextMyEmail);
        tvNumber = findViewById(R.id.editTextMyNumber);
        listViewMyCats = findViewById(R.id.listViewMyCats);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //set menu to toolbar
        toolbar.inflateMenu(R.menu.profile_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // handle choice from options menu

                if (item.getItemId() == R.id.menuItemRentingCats) {
                    Intent renting = new Intent(ProfileActivity.this, RentedActivity.class);
                    renting.putExtra("email", tvEmail.getText().toString());
                    renting.putExtra("rented", false);
                    startActivity(renting);
                } else if (item.getItemId() == R.id.menuItemRentedCats) {
                    Intent rented = new Intent(ProfileActivity.this, RentedActivity.class);
                    rented.putExtra("email", tvEmail.getText().toString());
                    rented.putExtra("rented", true);
                    startActivity(rented);
                } else if (item.getItemId() == R.id.menuItemProfileHelp) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                    builder.setTitle("Help")
                            .setMessage("Below is a list of your cats that help in therapy sessions.\n\n" +
                                    "By clicking on a cat, you can edit their info or remove them if they aren't booked.\n\n" +
                                    "Click Cats Away for Therapy in the menu to find your cats that are helping others.\n\n" +
                                    "Click Cats for my Therapy in the menu to find the cats that help you.\n")
                            .show();
                } else {
                    Log.d(TAG, "Adding Cat");
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                    builder.setTitle("Cat Entry Method");
                    builder.setMessage("How would you like to add a cat?");
                    builder.setPositiveButton("GPS", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // get location manager service from system
                            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                            // if the gps isn't enabled
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                // tell user and ask for permission
                                Toast.makeText(ProfileActivity.this, "Please enable GPS!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                            // if the permissions for locations aren't granted
                            if (ActivityCompat.checkSelfPermission(ProfileActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(ProfileActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED) {
                                // ask for them
                                ActivityCompat.requestPermissions(
                                        ProfileActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 123);
                            } else {
                                addThroughGPS();
                            }
                        }
                    }).setNegativeButton("Map", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent map = new Intent(ProfileActivity.this, MapsActivity.class);
                            map.putExtra("email", tvEmail.getText().toString());
                            map.putExtra("browsing", false);
                            startActivity(map);
                        }
                    }).setNeutralButton("Cancel", null);
                    builder.show();
                }
                return false;
            }
        });

        // set email
        tvEmail.setText(getIntent().getStringExtra("email"));
        db.collection("Contacts").document(tvEmail.getText().toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    tvNumber.setText(task.getResult().getString("Contact"));
                } else {
                    Log.d(TAG, "Couldn't retrieve Contact. " + task.getException());
                    AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                    warn.setTitle("Error");
                    warn.setMessage("Can't edit a booked cat. " + task.getException().getMessage());
                    warn.setPositiveButton("Ok", null);
                    warn.show();
                }
            }
        });
        // set list of cats listener to update a cat
        listViewMyCats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Editing My Cat");
                // retrieve old info of cat
                db.collection("Cats").document(docIds.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        // if cat is retrieved
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Retrieved my cat for editing");
                            // retrieve cat to be edited
                            Cat cat = task.getResult().toObject(Cat.class);
                            // if the cat is rented out
                            if (!cat.getRenter().isBlank()) {
                                Log.d(TAG, "Cat is booked out. ");
                                AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                warn.setTitle("Error");
                                warn.setMessage("Can't edit a booked cat.");
                                warn.setPositiveButton("Ok", null);
                                warn.show();
                                return;
                            }
                            // creating alert dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);/**/
                            LayoutInflater inflater = getLayoutInflater();
                            View viewInflated = inflater.inflate(R.layout.update_cat_dialog, null);
                            // place old names
                            catBreed = (EditText) (viewInflated.findViewById(R.id.etNewCatBreed));
                            catPrice = (EditText) (viewInflated.findViewById(R.id.etNewCatPrice));
                            catCity = (EditText) (viewInflated.findViewById(R.id.etNewCatCity));
                            catName = (EditText) (viewInflated.findViewById(R.id.etNewCatName));
                            uploadImage = (ImageView) (viewInflated.findViewById(R.id.newCatImage));
                            uploadButton = (Button) (viewInflated.findViewById(R.id.uploadButton));
                            removeImageButton = (Button) (viewInflated.findViewById(R.id.removeImage));

                            removeImageButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    imageUri = null;
                                    uploadImage.setVisibility(View.GONE);
                                    removeImageButton.setVisibility(View.GONE);
                                }
                            });


                            //Listener for upload button to open gallery
                            uploadButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //intent type image to prompt the user to pick an image from gallery
                                    Intent photoPicker = new Intent();
                                    photoPicker.setAction(Intent.ACTION_GET_CONTENT);
                                    photoPicker.setType("image/*");
                                    activityResultLauncher.launch(photoPicker);
                                }
                            });
                            //Use asynch task to download existing image from db if any exists
                            new DownloadImageTask().execute(cat.getImgURL());
                            catBreed.setText(cat.getBreed());
                            catPrice.setText("" + cat.getPrice());
                            catCity.setText(cat.getCity());
                            catName.setText(cat.getName());
                            builder.setView(viewInflated)
                                    // if the user wants to change
                                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            Log.d(TAG, "Cat edited");

                                            //upload image to firebase storage
                                            updateToFirebase(imageUri, i, cat.getImgURL());
                                            dialog.dismiss();

                                            // update the cat's "Renter" field in firebase

                                        }
                                    }).setNegativeButton("Cancel", null)
                                    .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int id) {
                                            //if cat has image, delete image from storage then the cat from db
                                            if (cat.getImgURL() != null) {

                                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                                StorageReference storageRef = storage.getReferenceFromUrl(cat.getImgURL());
                                                storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        // File deleted successfully
                                                        db.collection("Cats").document(docIds.get(i)).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d(TAG, "Deleted record of " + catName.getText().toString() + " successfully");
                                                                    Toast.makeText(ProfileActivity.this, "Deleted record of " + catName.getText().toString() + " successfully", Toast.LENGTH_LONG).show();
                                                                    hitFireBase();
                                                                } else {
                                                                    Log.d(TAG, "Failed to delete record of " + catName.getText().toString() + ". " + task.getException());
                                                                    AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                                                    warn.setTitle("Error");
                                                                    warn.setMessage("Failed to delete record of " + catName.getText().toString() + task.getException().getMessage());
                                                                    warn.setPositiveButton("Ok", null);
                                                                    warn.show();
                                                                }
                                                            }
                                                        });
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception exception) {
                                                        // Handle any errors
                                                        Toast.makeText(ProfileActivity.this, "Failed to delete Image", Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                            } else {// if cat has no image, delete the cat
                                                db.collection("Cats").document(docIds.get(i)).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "Deleted record of " + catName.getText().toString() + " successfully");
                                                            Toast.makeText(ProfileActivity.this, "Deleted record of " + catName.getText().toString() + " successfully", Toast.LENGTH_LONG).show();
                                                            hitFireBase();
                                                        } else {
                                                            Log.d(TAG, "Failed to delete record of " + catName.getText().toString() + ". " + task.getException());
                                                            AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                                            warn.setTitle("Error");
                                                            warn.setMessage("Failed to delete record of " + catName.getText().toString() + task.getException().getMessage());
                                                            warn.setPositiveButton("Ok", null);
                                                            warn.show();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                            builder.create();
                            builder.show();

                        } else {
                            Log.d(TAG, "Cat retrieval failed. " + task.getException());
                            AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                            warn.setTitle("Error");
                            warn.setMessage("Couldn't retrieve cat for booking. " + task.getException().getMessage());
                            warn.setPositiveButton("Ok", null);
                            warn.show();
                        }
                    }
                });
            }
        });
        // set checkbox listener
        ((CheckBox) (findViewById(R.id.checkBoxNotifyMe))).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked) {
                    Intent serviceIntent = new Intent(ProfileActivity.this, CatUpdatesService.class);
                    serviceIntent.putExtra("email", tvEmail.getText().toString());
                    ContextCompat.startForegroundService(ProfileActivity.this, serviceIntent);
                } else {
                    Intent serviceIntent = new Intent(ProfileActivity.this, CatUpdatesService.class);
                    stopService(serviceIntent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hitFireBase();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addThroughGPS();
            } else
                finish();
        }
    }

    private void hitFireBase() {
        // retireve all cats under owner
        db.collection("Cats").whereEqualTo("Owner", tvEmail.getText().toString()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                // if the retrieval is successful
                if (task.isSuccessful()) {
                    Log.d(TAG, "Retrieving user owned cats");
                    ArrayList<HashMap<String, String>> cats = new ArrayList<>();
                    docIds = new ArrayList<>();
                    List<QueryDocumentSnapshot> imgDocs = new ArrayList<>();
                    //for every document in firebase
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        //add the cat name and the document id
                        HashMap<String, String> pair = new HashMap<>();
                        pair.put("Name", document.toObject(Cat.class).getName());
                        cats.add(pair);
                        docIds.add(document.getId());
                        imgDocs.add(document);
                    }
                    // set the adapter to the list
                    listViewMyCats.setAdapter(new ImageAdapter(ProfileActivity.this,R.layout.mine_list_item,imgDocs));
                } else {
                    Log.d(TAG, "Failed to retrieve cats under " + tvEmail.getText().toString() + ". " + task.getException());
                    Toast.makeText(ProfileActivity.this, "Failed to retrieve cats under your name. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //Code adapted from https://androidknowledge.com/store-re...
    private void uploadToFirebase(Uri uri, double lat, double longit) {
        Toast.makeText(ProfileActivity.this, "Please wait while we add your cat to the system!", Toast.LENGTH_LONG).show();
        if (uri == null) {
            Map<String, Object> data1 = new HashMap<>();
            try {
                data1.put("Price", Double.parseDouble(catPrice.getText().toString()));
                data1.put("Name", catName.getText().toString());
                data1.put("City", catCity.getText().toString());
                data1.put("Breed", catBreed.getText().toString());
                data1.put("Owner", tvEmail.getText().toString());
                data1.put("Renter", "");
                data1.put("Lat", lat);
                data1.put("Longit", longit);
                data1.put("imgURL", null);
                data1.put("stamp", Timestamp.now().toDate());
                db.collection("Cats").add(data1).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Cat Added Successfully");
                            Toast.makeText(ProfileActivity.this, "Cat Added Successfully", Toast.LENGTH_LONG).show();
                            hitFireBase();
                        } else {
                            Log.d(TAG, "Cat wasn't added. " + task.getException());
                            AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                            warn.setTitle("Error");
                            warn.setMessage("Cat wasn't added. " + task.getException().getMessage());
                            warn.setPositiveButton("Ok", null);
                            warn.show();
                        }
                    }
                });
            } catch (NumberFormatException e) {
                Log.d(TAG, "Make sure you type in a floating point number. ");
                AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                warn.setTitle("Error");
                warn.setMessage("Make sure you type in a floating point number.");
                warn.setPositiveButton("Ok", null);
                warn.show();
            }
            return;
        }

        final StorageReference imageReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(uri));
        imageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                String profileImageUrl = task.getResult().toString();
                                Log.i("URL", profileImageUrl);
                                Map<String, Object> data1 = new HashMap<>();
                                try {
                                    data1.put("Price", Double.parseDouble(catPrice.getText().toString()));
                                    data1.put("Name", catName.getText().toString());
                                    data1.put("City", catCity.getText().toString());
                                    data1.put("Breed", catBreed.getText().toString());
                                    data1.put("Owner", tvEmail.getText().toString());
                                    data1.put("Renter", "");
                                    data1.put("Lat", lat);
                                    data1.put("Longit", longit);
                                    data1.put("imgURL", profileImageUrl);
                                    data1.put("stamp", Timestamp.now().toDate());
                                    db.collection("Cats").add(data1).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "Cat Added Successfully");
                                                Toast.makeText(ProfileActivity.this, "Cat Added Successfully", Toast.LENGTH_LONG).show();
                                                hitFireBase();
                                            } else {
                                                Log.d(TAG, "Cat wasn't added. " + task.getException());
                                                AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                                warn.setTitle("Error");
                                                warn.setMessage("Cat wasn't added. " + task.getException().getMessage());
                                                warn.setPositiveButton("Ok", null);
                                                warn.show();
                                            }
                                        }
                                    });
                                } catch (NumberFormatException e) {
                                    Log.d(TAG, "Make sure you type in a floating point number. ");
                                    AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                    warn.setTitle("Error");
                                    warn.setMessage("Make sure you type in a floating point number.");
                                    warn.setPositiveButton("Ok", null);
                                    warn.show();
                                }
                            }
                        });
                        Toast.makeText(ProfileActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateToFirebase(Uri uri, int i, String URL) {
        Toast.makeText(ProfileActivity.this, "Please wait while we reflect your cat's info on the system!", Toast.LENGTH_LONG).show();
        if (uri == null) {
            Map<String, Object> data1 = new HashMap<>();
            try {
                data1.put("Price", Double.parseDouble(catPrice.getText().toString()));
                data1.put("Name", catName.getText().toString());
                data1.put("City", catCity.getText().toString());
                data1.put("Breed", catBreed.getText().toString());
                data1.put("Owner", tvEmail.getText().toString());
                data1.put("Renter", "");
                data1.put("imgURL", null);

                // if cat has existing image, delete it
                if (URL != null) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReferenceFromUrl(URL);
                    Log.d(TAG, "Delete: " + URL);
                    storageRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                Log.d(TAG, "Image deleted from Storage");
                            else
                                Log.d(TAG, "Failed to delete image from Storage");
                        }
                    });
                }

                db.collection("Cats").document(docIds.get(i)).update(data1).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isComplete()) {
                            Log.d(TAG, "Updated old info successfully");
                            Toast.makeText(ProfileActivity.this, "Info updated successfully!", Toast.LENGTH_LONG).show();
                            hitFireBase();
                        } else {
                            Log.d(TAG, "Update Failed. " + task.getException());
                            AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                            warn.setTitle("Error");
                            warn.setMessage("Couldn't update cat info. " + task.getException().getMessage());
                            warn.setPositiveButton("Ok", null);
                            warn.show();
                        }
                    }
                });
            } catch (NumberFormatException e) {
                Log.d(TAG, "Make sure you type in a floating point number. ");
                AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                warn.setTitle("Error");
                warn.setMessage("Make sure you type in a floating point number.");
                warn.setPositiveButton("Ok", null);
                warn.show();
            }
            return;
        }
        final StorageReference imageReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(uri));
        imageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        imageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                String profileImageUrl = task.getResult().toString();
                                Log.d(TAG, "URL: " +profileImageUrl);
                                Map<String, Object> data1 = new HashMap<>();
                                data1.put("Price", Double.parseDouble(catPrice.getText().toString()));
                                data1.put("Name", catName.getText().toString());
                                data1.put("City", catCity.getText().toString());
                                data1.put("Breed", catBreed.getText().toString());
                                data1.put("imgURL", profileImageUrl);

                                db.collection("Cats").document(docIds.get(i)).update(data1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isComplete()) {
                                            Log.d(TAG, "Updated old info successfully");
                                            Toast.makeText(ProfileActivity.this, "Info updated successfully!", Toast.LENGTH_LONG).show();
                                            hitFireBase();
                                        } else {
                                            Log.d(TAG, "Update Failed. " + task.getException());
                                            AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                            warn.setTitle("Error");
                                            warn.setMessage("Couldn't update cat info. " + task.getException().getMessage());
                                            warn.setPositiveButton("Ok", null);
                                            warn.show();
                                        }
                                    }
                                });
                            }
                        });
                        Toast.makeText(ProfileActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void addThroughGPS() {
        // create dialog to enter info
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View viewInflated = inflater.inflate(R.layout.update_cat_dialog, null);

        uploadImage = (ImageView) (viewInflated.findViewById(R.id.newCatImage));
        catBreed = (EditText) (viewInflated.findViewById(R.id.etNewCatBreed));
        catPrice = (EditText) (viewInflated.findViewById(R.id.etNewCatPrice));
        catCity = (EditText) (viewInflated.findViewById(R.id.etNewCatCity));
        catName = (EditText) (viewInflated.findViewById(R.id.etNewCatName));
        uploadImage.setVisibility(View.GONE);
        uploadButton = (Button) (viewInflated.findViewById(R.id.uploadButton));
        removeImageButton = (Button) (viewInflated.findViewById(R.id.removeImage));
        removeImageButton.setVisibility(View.GONE);
        // button to remove image
        removeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageUri = null;
                uploadImage.setVisibility(View.GONE);
                removeImageButton.setVisibility(View.GONE);
            }
        });

        //Listener for upload button to open gallery
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //intent type image to prompt the user to pick an image from gallery
                Intent photoPicker = new Intent();
                photoPicker.setAction(Intent.ACTION_GET_CONTENT);
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
        });

        builder.setView(viewInflated).setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, getApplication().getMainExecutor(), new Consumer<Location>() {
                    @Override
                    public void accept(Location location) {
                        Log.d(TAG, "Location Changed");

                        if (location != null) {
                            if (catBreed.getText().toString().isBlank() || catPrice.getText().toString().isBlank()
                                    || catCity.getText().toString().isBlank() || catName.getText().toString().isBlank()) {
                                Log.d(TAG, "Make sure all fields are typed in.");
                                AlertDialog.Builder warn = new AlertDialog.Builder(ProfileActivity.this);
                                warn.setTitle("Error");
                                warn.setMessage("Make sure all fields are typed in.");
                                warn.setPositiveButton("Ok", null);
                                warn.show();
                                return;
                            }
                            //call upload here
                            uploadToFirebase(imageUri, location.getLatitude(), location.getLongitude());
                        }
                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                            builder.setTitle("Error")
                                    .setMessage("Couldn't Retrieve Location")
                                    .setPositiveButton("Use Maps", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            Intent map = new Intent(ProfileActivity.this, MapsActivity.class);
                                            map.putExtra("email", tvEmail.getText().toString());
                                            map.putExtra("browsing", false);
                                            startActivity(map);
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    }
                });
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private String getFileExtension(Uri fileUri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(fileUri));
    }

    public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        protected Bitmap doInBackground(String... urls) {
            Bitmap bmp = null;
            try {

                URL newurl = new URL(urls[0]);
                bmp = BitmapFactory.decodeStream(newurl.openConnection().getInputStream());
            } catch (Exception e) {

                e.printStackTrace();
            }
            return bmp;
        }
        protected void onPostExecute(Bitmap result) {
            bitmap = result;
            if (bitmap != null) {
                uploadImage.setVisibility(View.VISIBLE);
                uploadImage.setImageBitmap(bitmap);
                removeImageButton.setVisibility(View.VISIBLE);
            } else {
                uploadImage.setVisibility(View.GONE);
                removeImageButton.setVisibility(View.GONE);
            }
        }
    }

    //custom Adapter for ListView
    class ImageAdapter extends BaseAdapter
    {
        private Context mContext;
        private List<QueryDocumentSnapshot> imgDocs;

        int layout ;


        public ImageAdapter(Context context, int layout, List<QueryDocumentSnapshot> imgDocs)
        {
            mContext = context;

            this.layout = layout;
            this.imgDocs = imgDocs;

        }

        public int getCount() {
            return imgDocs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }


        // Override this method according to your need
        public View getView(int index, View view, ViewGroup viewGroup)
        {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(layout, viewGroup, false);

            ImageView imageView = (ImageView) rowView.findViewById(R.id.catImageView);
            ((TextView) rowView.findViewById(R.id.myCatName)).setText(imgDocs.get(index).getString("Name"));

            if(imgDocs.get(index).getString("imgURL") != null)
                Picasso.get().load(imgDocs.get(index).getString("imgURL")).into(imageView);

            return rowView;

        }
    }

}