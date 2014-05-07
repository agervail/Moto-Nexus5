package com.example.moto.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private static final String LOG_TAG = "SmsReceivedDialog";

    public class SmsMessageReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "SmsMessageReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.i(LOG_TAG, "onReceive");
            if (extras == null)
                return;
            Object[] pdus = (Object[]) extras.get("pdus");

            for (int i = 0; i < pdus.length; i++) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
                String fromAddress = message.getOriginatingAddress();
                String messageBody = message.getMessageBody().toString();
                Log.i(LOG_TAG, "From: " + fromAddress + " message: " + messageBody);
                MainActivity.this.received(fromAddress, messageBody);
            }
        }
    }

    private SmsMessageReceiver mSmsReceiver = new SmsMessageReceiver();
    private List<String> messages = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.onStart();
        Log.i(LOG_TAG, "CREATE");
        IntentFilter iff = new IntentFilter();
        iff.addAction("android.provider.Telephony.SMS_RECEIVED");
        this.registerReceiver(this.mSmsReceiver, iff);

        defineLocation();
        setContentView(R.layout.activity_main);
    }

    protected void onDestroy(){
        super.onDestroy();
        Log.i(LOG_TAG, "DESTROY");
        this.unregisterReceiver(this.mSmsReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        //SmsMessageReceiver smsReceiver = new SmsMessageReceiver();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void received(String sender, String message) {
        Log.i(LOG_TAG, "sender " + sender + " message " + message);
        ListView lv = (ListView) this.findViewById(R.id.listView);
        String contact = getContactDisplayNameByNumber(sender);
        messages.add(0, contact + " " + message);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                messages);
        lv.setAdapter(arrayAdapter);
    }

    public void updateLocation(Location loc){
        Log.i(LOG_TAG, "lat " + loc.getLatitude() + "long " + loc.getLongitude());
        TextView locT = (TextView) this.findViewById(R.id.location);
        locT.setText("lat " + loc.getLatitude() + "long " + loc.getLongitude());
    }

    public void defineLocation() {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                updateLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

// Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[]{BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }
}
