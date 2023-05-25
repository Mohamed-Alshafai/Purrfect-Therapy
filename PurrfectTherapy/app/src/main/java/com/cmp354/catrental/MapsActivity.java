package com.cmp354.catrental;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cmp354.catrental.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    final private String TAG = "MapsActivity";
    private String email;
    private boolean browsing;
    //    ArrayList<String> catsInfo;
    EditText catBreed;
    EditText catPrice;
    EditText catCity;
    EditText catName;
    Button uploadButton;
    Button removeImageButton;
    ImageView uploadImage;
    Uri imageUri;

    ActivityResultLauncher<Intent> activityResultLauncher;
    ArrayList<String> docIds;
    FirebaseFirestore db;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference();


    // TODO: check scope
    String[] from = new String[]{"Price", "Name", "Breed", "Owner", "City"};
    int[] to = new int[]{R.id.priceTextView, R.id.nameTextView, R.id.breedTextView, R.id.ownerTextView, R.id.cityTextView};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        // get user's email
        email = getIntent().getStringExtra("email");
        browsing = getIntent().getBooleanExtra("browsing", true);
        // get firebase instance
        db = FirebaseFirestore.getInstance();
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    // TODO: Add reference
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            imageUri = data.getData();
                            uploadImage.setVisibility(View.VISIBLE);
                            uploadImage.setImageURI(imageUri);
                            removeImageButton.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MapsActivity.this, "No Image Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // add zoom buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (browsing) {        // to show more detailed info when clicked
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(@NonNull Marker marker) {

                    // for all cats in firebase
                    db.collection("Cats")
                            .document(docIds.get(Integer.parseInt(marker.getTitle().toString())))
                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    // if we retrieved them
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Retrieved cat info for display of booking");
                                        // create a dialog with the info of the cat and ask to rent or cancel
                                        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                        LayoutInflater inflater = getLayoutInflater();
                                        View viewInflated = inflater.inflate(R.layout.cat_info_rent_dialogue, null);
                                        Cat cat = task.getResult().toObject(Cat.class);
                                        ((TextView) (viewInflated.findViewById(R.id.tvCatInfo))).setText(getCatInfo(cat));
                                        uploadImage = (ImageView) (viewInflated.findViewById(R.id.catinfoImage));
                                        new DownloadImageTask().execute(cat.getImgURL());
                                        // if they rent
                                        builder.setView(viewInflated).setPositiveButton("Book", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                Log.d(TAG, "Cat booked to " + email);
                                                // update the cat's "Renter" field in firebase
                                                Map<String, Object> data1 = new HashMap<>();
                                                data1.put("Renter", email);
                                                db.collection("Cats")
                                                        .document(docIds.get(Integer.parseInt(marker.getTitle())))
                                                        .update(data1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                // if the update is successful
                                                                if (task.isSuccessful()) {
                                                                    Log.d(TAG, "Booked cat successfully");
                                                                    Toast.makeText(MapsActivity.this,
                                                                            "Enjoy your time with " + cat.getName(),
                                                                            Toast.LENGTH_LONG).show();
                                                                    // remove marker from map
                                                                    marker.remove();
                                                                }
                                                                // if it fails print exception
                                                                else {
                                                                    Log.d(TAG, "Failed to Book cat. " + task.getException());
                                                                    Toast.makeText(MapsActivity.this,
                                                                            "We couldn't get " + cat.getName()
                                                                                    + " to you at this time, please try later. "
                                                                                    + task.getException().getMessage(),
                                                                            Toast.LENGTH_LONG).show();
                                                                }
                                                            }
                                                        });
                                                dialog.dismiss();
                                            }
                                        }).setNegativeButton("Cancel", null);
                                        builder.show();
                                    }
                                    else {
                                        Log.d(TAG, "Cat no longer exists");
                                        Toast.makeText(MapsActivity.this, "Sorry! It appears this cat no longer exists." , Toast.LENGTH_LONG).show();
                                        marker.remove();
                                    }
                                }
                            });
                }
            });
            // update markers on map
            hitFireBase();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            Toast.makeText(MapsActivity.this, "Long hold on a location to add a cat at it.", Toast.LENGTH_LONG).show();
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(@NonNull LatLng latLng) {
                    Log.d(TAG, "Adding Cat");
                    // instantiate builder
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    LayoutInflater inflater = getLayoutInflater();
                    View viewInflated = inflater.inflate(R.layout.update_cat_dialog, null);
                    // get fields
                    catBreed = (EditText) (viewInflated.findViewById(R.id.etNewCatBreed));
                    catPrice = (EditText) (viewInflated.findViewById(R.id.etNewCatPrice));
                    catCity = (EditText) (viewInflated.findViewById(R.id.etNewCatCity));
                    catName = (EditText) (viewInflated.findViewById(R.id.etNewCatName));
                    uploadImage = (ImageView) (viewInflated.findViewById(R.id.newCatImage));
                    uploadImage.setVisibility(View.GONE);
                    uploadButton = (Button) (viewInflated.findViewById(R.id.uploadButton));
                    removeImageButton = (Button) (viewInflated.findViewById(R.id.removeImage));
                    removeImageButton.setVisibility(View.GONE);
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

                    builder.setView(viewInflated)
                            // Add action buttons
                            .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (catBreed.getText().toString().isBlank() || catPrice.getText().toString().isBlank()
                                            || catCity.getText().toString().isBlank() || catName.getText().toString().isBlank()) {
                                        Log.d(TAG, "Make sure all fields are typed in.");
                                        AlertDialog.Builder warn = new AlertDialog.Builder(MapsActivity.this);
                                        warn.setTitle("Error");
                                        warn.setMessage("Make sure all fields are typed in.");
                                        warn.setPositiveButton("Ok", null);
                                        warn.show();
                                    }
                                    // update the cat's "Renter" field in firebase
                                    uploadToFirebase(imageUri, latLng.latitude, latLng.longitude);
                                }
                            }).setNegativeButton("Cancel", null);
                    builder.create();
                    builder.show();
                }
            });
        }
    }

    protected void hitFireBase() {
        Log.d(TAG, "Retrieving cats from firebase");
        // retrieve all Cats entries
        db.collection("Cats")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // if firebase hit was successful
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Retrieval success");
                            // instantiate arrays to hold cat and docId objects
                            docIds = new ArrayList<>();
                            // for every document in firebase
                            LatLng latLng = new LatLng(25.31431, 55.495869);
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // add location of cat to map with its description
                                if (document.toObject(Cat.class).getRenter().equals("") && !document.toObject(Cat.class).getOwner().equals(email)) {
                                    latLng = new LatLng(document.toObject(Cat.class).getLat(), document.toObject(Cat.class).getLongit());
                                    mMap.addMarker(new
                                            MarkerOptions().position(latLng)
                                            .title(docIds.size() + "")
                                            .snippet("Click for more about " + document.toObject(Cat.class)));
                                    docIds.add(document.getId());
                                }
                            }
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMap.getCameraPosition().zoom));
                        } else {
                            Log.d(TAG, "Error getting cats: ", task.getException());
                            Toast.makeText(MapsActivity.this, "Couldn't retrieve cats at this time.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private String getCatInfo(Cat cat) {
        return "Here is " + cat.getName() +
                ", a " + cat.getBreed() + " cat" +
                " from " + cat.getCity() +
                " and its owner is " + cat.getOwner() +
                ".\nYou can enjoy your time with " + cat.getName() + " for $" + cat.getPrice() + " an hour.";
    }

    private void uploadToFirebase(Uri uri, double lat, double longit) {
        Toast.makeText(MapsActivity.this, "Please wait while we add your cat to the system!", Toast.LENGTH_LONG).show();
        if (uri == null) {
            Map<String, Object> data1 = new HashMap<>();
            try {
                data1.put("Price", Double.parseDouble(catPrice.getText().toString()));
                data1.put("Name", catName.getText().toString());
                data1.put("City", catCity.getText().toString());
                data1.put("Breed", catBreed.getText().toString());
                data1.put("Owner", email);
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
                            Toast.makeText(MapsActivity.this, "Cat Added Successfully", Toast.LENGTH_LONG).show();
                            browsing = true;
                            //hitFireBase();
                        } else {
                            Log.d(TAG, "Cat wasn't added. " + task.getException());
                            AlertDialog.Builder warn = new AlertDialog.Builder(MapsActivity.this);
                            warn.setTitle("Error");
                            warn.setMessage("Cat wasn't added. " + task.getException().getMessage());
                            warn.setPositiveButton("Ok", null);
                            warn.show();
                        }
                    }
                });
            } catch (NumberFormatException e) {
                Log.d(TAG, "Make sure you type in a floating point number. ");
                AlertDialog.Builder warn = new AlertDialog.Builder(MapsActivity.this);
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
                                    data1.put("Owner", email);
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
                                                Toast.makeText(MapsActivity.this, "Cat Added Successfully", Toast.LENGTH_LONG).show();
                                                //hitFireBase();
                                            } else {
                                                Log.d(TAG, "Cat wasn't added. " + task.getException());
                                                AlertDialog.Builder warn = new AlertDialog.Builder(MapsActivity.this);
                                                warn.setTitle("Error");
                                                warn.setMessage("Cat wasn't added. " + task.getException().getMessage());
                                                warn.setPositiveButton("Ok", null);
                                                warn.show();
                                            }
                                        }
                                    });
                                } catch (NumberFormatException e) {
                                    Log.d(TAG, "Make sure you type in a floating point number. ");
                                    AlertDialog.Builder warn = new AlertDialog.Builder(MapsActivity.this);
                                    warn.setTitle("Error");
                                    warn.setMessage("Make sure you type in a floating point number.");
                                    warn.setPositiveButton("Ok", null);
                                    warn.show();
                                }
                            }
                        });
                        Toast.makeText(MapsActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MapsActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
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
            if (result != null) {
                uploadImage.setVisibility(View.VISIBLE);
                uploadImage.setImageBitmap(result);
            } else {
                uploadImage.setVisibility(View.GONE);
            }
        }
    }
}