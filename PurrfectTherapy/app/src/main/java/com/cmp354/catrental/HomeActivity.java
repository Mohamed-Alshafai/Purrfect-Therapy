package com.cmp354.catrental;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.AnimationTypes;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query.Direction;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = "HomeActivity";
    static FirebaseFirestore db;
    private ListView listViewCats;
    private Spinner spinnerSortBy;
    private EditText etSearchCat;
    private RadioGroup rgSortBy;
    private RadioButton rbAsc;
    private RadioButton rbDesc;

    ImageView catImage;
    private String email;
    ImageSlider imageSlider;
    Toolbar toolbar;
    ArrayList<String> docIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // retrieving member variables
        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.homepage_menu);
        db = FirebaseFirestore.getInstance();
        listViewCats = findViewById(R.id.listViewCats);
        spinnerSortBy = findViewById(R.id.spinnerSortBy);
        etSearchCat = findViewById(R.id.editTextCatName);
        rgSortBy = findViewById(R.id.rgSortOrder);
        rbAsc = findViewById(R.id.rbAsc);
        rbDesc = findViewById(R.id.rbDesc);
        email = getIntent().getStringExtra("email");

        // in case the user came from a notification get the cat's name
        if (getIntent().hasExtra("cat name")) {
            etSearchCat.setText(getIntent().getStringExtra("cat name"));
        }
        //code adapted from https://youtu.be/fiSWKebAZg8
        ArrayList<SlideModel> imageList = new ArrayList<>(); // Create image list
        imageSlider = findViewById(R.id.image_slider);
        imageList.add(new SlideModel(R.drawable.shaf3icatimg1, "This", ScaleTypes.FIT)); // for one image
        imageList.add(new SlideModel(R.drawable.shaf3icatimg2, "Could", ScaleTypes.FIT)); // you can with title
        imageList.add(new SlideModel(R.drawable.shaf3icatimg3, "Be", ScaleTypes.FIT));
        imageList.add(new SlideModel(R.drawable.shaf3icatimg4, "You", ScaleTypes.FIT));

        imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP);
        imageSlider.setSlideAnimation(AnimationTypes.FLIP_HORIZONTAL);
        imageSlider.startSliding(2500);
        // Menu inflater for toolbar
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // handle choice from options menu
                if (item.getItemId() == R.id.menuItemMap) {
                    Log.d("Menu: ", "Map menu item clicked");
                    Intent map = new Intent(HomeActivity.this, MapsActivity.class);
                    map.putExtra("email", email);
                    map.putExtra("browsing", true);
                    startActivity(map);
                } else if (item.getItemId() == R.id.menuItemHomeHelp) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                    builder.setTitle("Help")
                            .setMessage("Below is a list of avaialble cats for therapy sessions.\n\n" +
                                    "By clicking on a cat you can book your session with it.\n\n" +
                                    "Click Map in the menu to find cats near your location.\n\n" +
                                    "To enter your profile for more options, click on Profile in the menu.\n")
                            .show();
                } else {
                    Intent profile = new Intent(HomeActivity.this, ProfileActivity.class);
                    profile.putExtra("email", email);
                    startActivity(profile);
                }
                return false;
            }
        });

        // setting edittext listener
        etSearchCat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                // if the user finished typing the name to be searched and it isn't empty or just spaces
                if (!textView.getText().toString().trim().isBlank()) {
                    Log.d(TAG, "Finished Typing Search String");
                    // retrieve all cats where the name is equal to the search string
                    db.collection("Cats").
                            whereEqualTo("Name", textView.getText().toString().trim()).
                            get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    // if there are cats
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Matches found for: " +
                                                textView.getText().toString().trim());
                                        // instantiate arrays to hold cat and docId objects that match the search string
                                        ArrayList<HashMap<String, String>> cats = new ArrayList<>();
                                        ArrayList<String> ids = new ArrayList<>();
                                        List<QueryDocumentSnapshot> imgDocs = new ArrayList<>();

                                        // for every document in firebase
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            // add the cat and the document id
                                            HashMap<String, String> pair = new HashMap<>();
                                            Cat kitty = document.toObject(Cat.class);
                                            if (kitty.getRenter().equals("") && !kitty.getOwner().equals(email)) {
                                                pair.put("Price", String.valueOf(kitty.getPrice()));
                                                pair.put("Name", kitty.getName());
                                                pair.put("Breed", kitty.getBreed());
                                                pair.put("Owner", kitty.getOwner());
                                                pair.put("City", kitty.getCity());
                                                cats.add(pair);
                                                ids.add(document.getId());
                                                imgDocs.add(document);
                                            }
                                        }
                                        // creating alert dialog
                                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                                        LayoutInflater inflater = getLayoutInflater();
                                        View viewInflated = inflater.inflate(R.layout.search_result_dialog, null);
                                        // create adapter to hold the cats
                                        String[] from = new String[]{"Price", "Name", "Breed", "Owner", "City"};
                                        int[] to = new int[]{R.id.rentPrice, R.id.catName, R.id.catBreed};

                                        // set the adapter to the list
                                        ListView listViewSearch = viewInflated.findViewById(R.id.listViewSearch);
                                        listViewSearch.setAdapter(new ImageAdapter(HomeActivity.this,R.layout.list_item,imgDocs));
                                        listViewSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                                                Log.d(TAG, "Search booking cat onItemClick listener");
                                                // create dialog for booking
                                                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                                                LayoutInflater inflater = getLayoutInflater();
                                                View viewInflated = inflater.inflate(R.layout.rent_dialog, null);
                                                // retrieve cat where document has the ids[i] path
                                                db.collection("Cats").document(ids.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "Retrieved cat for booking");
                                                            Cat cat = task.getResult().toObject(Cat.class);
                                                            // set the fields of the new dialog

                                                            catImage = viewInflated.findViewById(R.id.imageView_rent);
                                                            catImage.setVisibility(View.GONE);
                                                            new DownloadImageTask().execute(cat.getImgURL());
                                                            ((TextView) (viewInflated.findViewById(R.id.breedTextView))).setText("Breed: " + cat.getBreed());
                                                            ((TextView) (viewInflated.findViewById(R.id.priceTextView))).setText("Price: " + cat.getPrice());
                                                            ((TextView) (viewInflated.findViewById(R.id.ownerTextView))).setText("Owner: " + cat.getOwner());
                                                            ((TextView) (viewInflated.findViewById(R.id.cityTextView))).setText("City: " + cat.getCity());
                                                            ((TextView) (viewInflated.findViewById(R.id.nameTextView))).setText("Name: " + cat.getName());
                                                        } else {
                                                            Log.d(TAG, "Search cat failed. " + task.getException());
                                                            AlertDialog.Builder warn = new AlertDialog.Builder(HomeActivity.this);
                                                            warn.setTitle("Error");
                                                            warn.setMessage("Cat not found. " + task.getException().getMessage());
                                                            warn.setPositiveButton("Ok", null);
                                                            warn.show();
                                                            return;
                                                        }
                                                    }
                                                });
                                                builder.setView(viewInflated)
                                                        // if the user wants to rent
                                                        .setPositiveButton("Book", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                Log.d(TAG, "Cat booked to " + email);
                                                                // update the cat's "Renter" field in firebase
                                                                Map<String, Object> data1 = new HashMap<>();
                                                                data1.put("Renter", email);
                                                                db.collection("Cats").document(docIds.get(i)).update(data1);
                                                                hitFireBase();
                                                                dialog.dismiss();
                                                            }
                                                        }).setNegativeButton("Cancel", null);
                                                builder.create();
                                                builder.show();
                                            }
                                        });
                                        // launch
                                        builder.setView(viewInflated)
                                                // if the user doesn't want to rent
                                                .setPositiveButton("Ok", null);
                                        builder.create();
                                        builder.show();
                                    } else {
                                        Log.d(TAG, "No matches found for search " + textView.getText().toString());
                                        Toast.makeText(HomeActivity.this,
                                                "No matches found for " + textView.getText().toString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                }
                return false;
            }
        });
        // setting radio group listener
        rgSortBy.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                hitFireBase();
            }
        });
        // setting spinner adapter
        String[] sortingBy = new String[]{
                "Breed", "Price", "Name"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, sortingBy);
        spinnerSortBy.setAdapter(adapter);
        spinnerSortBy.setOnItemSelectedListener(itemSelectedListener);
        // setting list onClick
        listViewCats.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "Booking cat onItemClick listener");

                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                LayoutInflater inflater = getLayoutInflater();
                View viewInflated = inflater.inflate(R.layout.rent_dialog, null);
                db.collection("Cats").document(docIds.get(i)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Retrieved cat for booking");
                            Cat cat = task.getResult().toObject(Cat.class);
                            catImage = ((ImageView) (viewInflated.findViewById(R.id.imageView_rent)));
                            catImage.setVisibility(View.GONE);
                            new DownloadImageTask().execute(cat.getImgURL());
                            ((TextView) (viewInflated.findViewById(R.id.breedTextView))).setText("Breed: " + cat.getBreed());
                            ((TextView) (viewInflated.findViewById(R.id.priceTextView))).setText("Price: " + cat.getPrice());
                            ((TextView) (viewInflated.findViewById(R.id.ownerTextView))).setText("Owner: " + cat.getOwner());
                            ((TextView) (viewInflated.findViewById(R.id.cityTextView))).setText("City: " + cat.getCity());
                            ((TextView) (viewInflated.findViewById(R.id.nameTextView))).setText("Name: " + cat.getName());
                        } else {
                            Log.d(TAG, "Cat retrieval failed. " + task.getException());
                            AlertDialog.Builder warn = new AlertDialog.Builder(HomeActivity.this);
                            warn.setTitle("Error");
                            warn.setMessage("Couldn't retrieve cat for booking. " + task.getException().getMessage());
                            warn.setPositiveButton("Ok", null);
                            warn.show();
                        }
                    }
                });
                builder.setView(viewInflated)
                        // if the user wants to rent
                        .setPositiveButton("Book", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Log.d(TAG, "Cat booked to " + email);
                                // update the cat's "Renter" field in firebase
                                Map<String, Object> data1 = new HashMap<>();
                                data1.put("Renter", email);
                                db.collection("Cats").document(docIds.get(i)).update(data1);
                                hitFireBase();
                                dialog.dismiss();
                            }
                        }).setNegativeButton("Cancel", null);
                builder.create();
                builder.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // to account for activity changing
        hitFireBase();
    }

    protected void hitFireBase() {
        Log.d(TAG, "Retrieving cats from firebase");
        // retrieve all Cats entries ordered by specified item and direction
        db.collection("Cats").orderBy(spinnerSortBy.getSelectedItem().toString(),
                        rbAsc.isChecked() == true ? Direction.ASCENDING : Direction.DESCENDING)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // if firebase hit was successful
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Retrieval success");
                            // instantiate arrays to hold cat and docId objects
                            String[] from = new String[]{"Price", "Name", "Breed"};
                            int[] to = new int[]{R.id.rentPrice, R.id.catName, R.id.catBreed};

                            ArrayList<HashMap<String, String>> cats = new ArrayList<>();
                            docIds = new ArrayList<>();
                            List<QueryDocumentSnapshot> imgDocs = new ArrayList<>();

                            // for every document in firebase
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // add the cat and the document id
                                Cat kitty = document.toObject(Cat.class);
                                if (kitty.getRenter().equals("") && !kitty.getOwner().equals(email)) {
                                    HashMap<String, String> pair = new HashMap<>();
                                    pair.put("Price", String.valueOf(kitty.getPrice()));
                                    pair.put("Name", kitty.getName());
                                    pair.put("Breed", kitty.getBreed());
                                    cats.add(pair);
                                    docIds.add(document.getId());
                                    imgDocs.add(document);
                                }
                            }
                            // create adapter to hold the cats
                            SimpleAdapter catsAdapter = new SimpleAdapter(HomeActivity.this,
                                    cats, R.layout.list_item, from, to);
                            // set the adapter to the list
                            //listViewCats.setAdapter(catsAdapter);
                            listViewCats.setAdapter(new ImageAdapter(HomeActivity.this,R.layout.list_item,imgDocs));
                        } else {
                            Log.d(TAG, "Error getting cats: ", task.getException());
                        }
                    }
                });
    }

    // spinner OnItemSelectedListener
    AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            hitFireBase();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

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
                catImage.setVisibility(View.VISIBLE);
                catImage.setImageBitmap(result);
            } else {
                catImage.setVisibility(View.GONE);
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
            // TODO Auto-generated method stub

            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(layout, viewGroup, false);

            ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView);
            ((TextView) rowView.findViewById(R.id.catName)).setText(imgDocs.get(index).getString("Name"));
            ((TextView) rowView.findViewById(R.id.catBreed)).setText(imgDocs.get(index).getString("Breed"));
            ((TextView) rowView.findViewById(R.id.rentPrice)).setText(imgDocs.get(index).getDouble("Price").toString());

            if(imgDocs.get(index).getString("imgURL") != null)
                Picasso.get().load(imgDocs.get(index).getString("imgURL")).into(imageView);

            return rowView;

        }
    }


}