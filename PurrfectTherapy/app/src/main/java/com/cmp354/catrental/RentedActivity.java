package com.cmp354.catrental;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RentedActivity extends AppCompatActivity {
    protected final String TAG = "RentedActivity";
    FirebaseFirestore db;
    String email;
    ArrayList<String> docIds;
    ListView listViewRented;

    ImageView uploadImage;
    boolean rentedCats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rented);

        // retrieving firebase instance
        db = FirebaseFirestore.getInstance();
        // retrieving email
        email = getIntent().getStringExtra("email");
        rentedCats = getIntent().getBooleanExtra("rented", true);
        ((TextView) findViewById(R.id.activityTitle)).setText(rentedCats == true ? "Cats Away in Sessions" : "Cats for my Therapy");
        // get list view and implement listener
        listViewRented = findViewById(R.id.listViewRented);
        if (rentedCats) {
            listViewRented.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d(TAG, "Returning My Cat");
                    // retrieve old info of cat
                    db.collection("Cats").document(docIds.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            // if cat is retrieved
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Retrieved my cat for return");
                                // retrieve cat to be edited
                                Cat cat = task.getResult().toObject(Cat.class);
                                // if the cat is not rented out
                                if (cat.getRenter().isBlank()) {
                                    Log.d(TAG, "Cat is not booked. ");
                                    AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                    warn.setTitle("Error");
                                    warn.setMessage("Can't recieve back a non-booked cat.");
                                    warn.setPositiveButton("Ok", null);
                                    warn.show();
                                    return;
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(RentedActivity.this);
                                LayoutInflater inflater = getLayoutInflater();
                                View viewInflated = inflater.inflate(R.layout.rented_dialog, null);
                                // retrieve the renter's contact info
                                db.collection("Contacts").document(email).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        // if the retrieval is successful
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Retrieved ");
                                            // creating alert dialog

                                            // place old names
                                            uploadImage = (ImageView) (viewInflated.findViewById(R.id.imageView4));
                                            new DownloadImageTask().execute(cat.getImgURL());
                                            ((TextView) (viewInflated.findViewById(R.id.catNameTextView))).setText(cat.getName());
                                            ((TextView) (viewInflated.findViewById(R.id.catPriceTextView))).setText(String.valueOf(cat.getPrice()));
                                            ((TextView) (viewInflated.findViewById(R.id.catBreedTextView))).setText(cat.getBreed());
                                            ((TextView) (viewInflated.findViewById(R.id.catCityTextView))).setText(cat.getCity());
                                            ((TextView) (viewInflated.findViewById(R.id.catOwnerTextView))).setText(cat.getOwner());
                                            ((TextView) (viewInflated.findViewById(R.id.catRenterTextView))).setText(cat.getRenter() + ", " + task.getResult().getString("Contact"));
                                            builder.setView(viewInflated)
                                                    // if the user wants to get their cat back
                                                    .setPositiveButton("Return Cat", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            Log.d(TAG, "Receiving Cat");
                                                            // update the cat's "Renter" field in firebase
                                                            Map<String, Object> data1 = new HashMap<>();
                                                            // reset Renter contact
                                                            data1.put("Renter", "");
                                                            db.collection("Cats").document(docIds.get(i)).update(data1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Log.d(TAG, "Cat returned");
                                                                        Toast.makeText(RentedActivity.this, "Cat Recieved", Toast.LENGTH_LONG);
                                                                        hitFireBase();
                                                                    } else {
                                                                        Log.d(TAG, "Cat wasn't recieved back. " + task.getException());
                                                                        AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                                                        warn.setTitle("Error");
                                                                        warn.setMessage("Cat wasn't recieved back. " + task.getException().getMessage());
                                                                        warn.setPositiveButton("Ok", null);
                                                                        warn.show();
                                                                    }
                                                                }
                                                            });
                                                            dialog.dismiss();
                                                        }
                                                    }).setNegativeButton("Cancel", null);
                                            builder.create();
                                            builder.show();
                                        }
                                    }
                                });
                            } else {
                                Log.d(TAG, "Cat retrieval failed. " + task.getException());
                                AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                warn.setTitle("Error");
                                warn.setMessage("Couldn't retrieve cat for return. " + task.getException().getMessage());
                                warn.setPositiveButton("Ok", null);
                                warn.show();
                            }
                        }
                    });
                }
            });
        } else {
            listViewRented.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.d(TAG, "Returning Someone's Cat");
                    // retrieve old info of cat
                    AlertDialog.Builder builder = new AlertDialog.Builder(RentedActivity.this);
                    LayoutInflater inflater = getLayoutInflater();
                    View viewInflated = inflater.inflate(R.layout.renting_dialog, null);
                    db.collection("Cats").document(docIds.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            // if cat is retrieved
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Retrieved someone's cat for return");
                                // retrieve cat to be edited
                                Cat cat = task.getResult().toObject(Cat.class);
                                // if the cat is not rented out
                                if (cat.getRenter().isBlank()) {
                                    Log.d(TAG, "Cat is not booked.");
                                    AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                    warn.setTitle("Error");
                                    warn.setMessage("Can't return back a non-booked cat.");
                                    warn.setPositiveButton("Ok", null);
                                    warn.show();
                                    return;
                                }
                                // place old names
                                uploadImage = (ImageView) (viewInflated.findViewById(R.id.imageView3));
                                new DownloadImageTask().execute(cat.getImgURL());
                                ((TextView) (viewInflated.findViewById(R.id.catNameTextView))).setText(cat.getName());
                                ((TextView) (viewInflated.findViewById(R.id.catPriceTextView))).setText(String.valueOf(cat.getPrice()));
                                ((TextView) (viewInflated.findViewById(R.id.catBreedTextView))).setText(cat.getBreed());
                                ((TextView) (viewInflated.findViewById(R.id.catCityTextView))).setText(cat.getCity());
                                ((TextView) (viewInflated.findViewById(R.id.catOwnerTextView))).setText(cat.getOwner());
                                builder.setView(viewInflated)
                                        // if the user wants to get their cat back
                                        .setPositiveButton("Return Cat", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int id) {
                                                Log.d(TAG, "Cat returning");
                                                // update the cat's "Renter" field in firebase
                                                Map<String, Object> data1 = new HashMap<>();
                                                // reset Renter contact
                                                data1.put("Renter", "");
                                                db.collection("Cats").document(docIds.get(i)).update(data1).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "Cat returned");
                                                            Toast.makeText(RentedActivity.this, "Cat Returned", Toast.LENGTH_LONG);
                                                            hitFireBase();
                                                        } else {
                                                            Log.d(TAG, "Cat wasn't returned back. " + task.getException());
                                                            AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                                            warn.setTitle("Error");
                                                            warn.setMessage("Cat wasn't returned back. " + task.getException().getMessage());
                                                            warn.setPositiveButton("Ok", null);
                                                            warn.show();
                                                        }
                                                    }
                                                });
                                                dialog.dismiss();
                                            }
                                        }).setNegativeButton("Cancel", null);
                                builder.create();
                                builder.show();
                            } else {
                                Log.d(TAG, "Cat retrieval failed. " + task.getException());
                                AlertDialog.Builder warn = new AlertDialog.Builder(RentedActivity.this);
                                warn.setTitle("Error");
                                warn.setMessage("Couldn't retrieve cat for return. " + task.getException().getMessage());
                                warn.setPositiveButton("Ok", null);
                                warn.show();
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hitFireBase();
    }

    private void hitFireBase() {
        // retireve all cats under owner that are rented
        if (rentedCats) {
            db.collection("Cats").whereEqualTo("Owner", email).whereNotEqualTo("Renter", "")
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            // if the retrieval is successful
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Retrieving user's booked cats");
                                ArrayList<HashMap<String, String>> cats = new ArrayList<>();
                                docIds = new ArrayList<>();
                                List<QueryDocumentSnapshot> imgDocs = new ArrayList<>();
                                // for every document in firebase
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // add the cat name and the document id
                                    HashMap<String, String> pair = new HashMap<>();
                                    pair.put("Name", document.toObject(Cat.class).getName());
                                    cats.add(pair);
                                    docIds.add(document.getId());
                                    imgDocs.add(document);
                                }
                                listViewRented.setAdapter(new ImageAdapter(RentedActivity.this,R.layout.mine_list_item,imgDocs));

                            } else {
                                Log.d(TAG, "Failed to retrieve cats booked from " + email + ". " + task.getException());
                                Toast.makeText(RentedActivity.this, "Failed to retrieve cats under your name. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        } else {
            // retrieve docs where the user is the renter
            db.collection("Cats").whereEqualTo("Renter", email)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            // if the retrieval is successful
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Retrieving user booked out cats");
                                ArrayList<HashMap<String, String>> cats = new ArrayList<>();
                                docIds = new ArrayList<>();
                                List<QueryDocumentSnapshot> imgDocs = new ArrayList<>();
                                // for every document in firebase
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // add the cat name and the document id
                                    HashMap<String, String> pair = new HashMap<>();
                                    pair.put("Name", document.toObject(Cat.class).getName());
                                    cats.add(pair);
                                    docIds.add(document.getId());
                                    imgDocs.add(document);

                                }
                                listViewRented.setAdapter(new ImageAdapter(RentedActivity.this,R.layout.mine_list_item,imgDocs));
                            } else {
                                Log.d(TAG, "Failed to retrieve booked cat of " + email + ". " + task.getException());
                                Toast.makeText(RentedActivity.this, "Failed to retrieve cats booked under your name. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
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