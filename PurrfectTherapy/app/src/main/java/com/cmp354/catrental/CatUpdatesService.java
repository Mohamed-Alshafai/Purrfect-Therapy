package com.cmp354.catrental;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;

public class CatUpdatesService extends Service {
    private final String TAG = "CatUpdateService";
    private String email;
    private final String CHANNEL_ID = String.valueOf(System.currentTimeMillis());
    private NotificationManager manager;
    public CatUpdatesService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Started Updates Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Creating persistent notification");
        email = intent.getStringExtra("email");
        //Notification code in the onStartCommand()
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,"Cat Updates Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(
                this, CHANNEL_ID)
                .setContentTitle("Cat Updates")
                .setContentText("Checking for your cat updates")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setOngoing(true) //sticky notification
                .build();

        startForeground(1, notification);
        // adding the listener to check for new updates
        FirebaseFirestore.getInstance().collection("Cats").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Listen failed: ", error);
                    return;
                }

                for (DocumentChange dc : value.getDocumentChanges()) {
                    Cat kitty = dc.getDocument().toObject((Cat.class));
                    if (dc.getType() == DocumentChange.Type.ADDED // it was added
                            && kitty.getRenter().equals("") // it has no renter
                            && !kitty.getOwner().equals(email) // it isn't owned by you
                    && (((new Date()).getTime() - kitty.getStamp().toDate().getTime()) <= (1000*60*60))) { // and it has been added just now
                        DocumentSnapshot document = dc.getDocument();
                        Log.d(TAG, "New document added with ID: " + document.getId());
                        postNotif(document.getString("Name"), "A New Cat has been Added!");
                    }
                }
            }
        });
        return super.START_STICKY;
    }
    private void postNotif(String catName, String type) {
        Intent notificationIntent = new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("cat name", catName);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int icon = R.drawable.cat_logo;
        CharSequence tickerText = type;
        CharSequence contentTitle = getText(R.string.app_name);
        CharSequence contentText = "Say Hi to " + catName;
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId(CHANNEL_ID)
                .build();
        final int NOTIFICATION_ID = (int)(System.currentTimeMillis());
        manager.notify(NOTIFICATION_ID, notification);
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}