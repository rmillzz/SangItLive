package com.rtmillerprojects.sangitlive.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rtmillerprojects.sangitlive.EventBus;
import com.rtmillerprojects.sangitlive.R;
import com.rtmillerprojects.sangitlive.model.GoogleLocation.CustomLocationResult;
import com.rtmillerprojects.sangitlive.model.GoogleLocation.Result;
import com.rtmillerprojects.sangitlive.model.EventCalls.CompletedForceRefresh;
import com.rtmillerprojects.sangitlive.util.EventManagerService;
import com.rtmillerprojects.sangitlive.util.SharedPreferencesHelper;
import com.squareup.otto.Subscribe;

import org.parceler.Parcels;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Ryan on 8/25/2016.
 */
public class ActivitySettings extends AppCompatActivity {

    RelativeLayout locationLayout;
    RelativeLayout forceRefresh;
    EventManagerService ems;
    RecyclerView.LayoutManager layoutManager;
    ListView listView;
    Toolbar toolbar;

    TextView emptyView;
    ArrayAdapter<Result> mAdapter;
    List<Result> r;
    String zipReturn;
    private final int ZIP_REQUEST_CODE = 1;
    CustomLocationResult location;
    TextView textViewLocation;
    ProgressDialog progressDialog;
    Context context;
    RelativeLayout refreshFrequencyLayout;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPref;
    TextView refreshFrequencyValue;
    TextView tvNextRefreshDate;
    TextView tvLastRefreshDate;
    Date lastRefresh;
    Date nextRefresh;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        handleIntent(getIntent());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        locationLayout = (RelativeLayout) findViewById(R.id.location_layout);
        forceRefresh = (RelativeLayout) findViewById(R.id.force_refresh_layout);
        refreshFrequencyLayout = (RelativeLayout) findViewById(R.id.refresh_frequency_layout);
        textViewLocation = (TextView) findViewById(R.id.location_value);
        refreshFrequencyValue = (TextView) findViewById(R.id.refresh_requency_value);
        listView = (ListView) findViewById(R.id.location_results);
        emptyView = (TextView) findViewById(R.id.empty_view);
        tvLastRefreshDate = (TextView) findViewById(R.id.last_refresh_value);
        tvNextRefreshDate = (TextView) findViewById(R.id.next_refresh_value);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        final Context context = this;

        populateUserLocation();
        populateRefreshDates();

        forceRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ems = EventManagerService.getInstance(context);
                ems.forceRefreshCalls();
                progressDialog = new ProgressDialog(context);
                progressDialog.show();
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Please wait...");
                progressDialog.setTitle("Refreshing your feeds");
            }
        });

        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        locationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(context, ActivityZipSearch.class), ZIP_REQUEST_CODE);
            }
        });

        String frequencyDisplay = sharedPref.getInt(getString(R.string.user_refresh_frequency),60) + " days";
        refreshFrequencyValue.setText(frequencyDisplay);

        refreshFrequencyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder b = new AlertDialog.Builder(context);
                b.setTitle("Refresh Frequency");
                String[] types = {"7 days", "21 days", "60 days"};
                b.setItems(types, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String frequencyDisplay;
                        int frequency;
                        long newDate;

                        dialog.dismiss();
                        switch(which){
                            case 0:
                                editor = sharedPref.edit();
                                frequency = 7;
                                editor.putInt(getString(R.string.user_refresh_frequency), frequency);
                                editor.commit();
                                frequencyDisplay = frequency + " days";
                                refreshFrequencyValue.setText(frequencyDisplay);
                                newDate = calculateNextRefreshDate(frequency);
                                tvNextRefreshDate.setText(longToString(newDate));
                                break;
                            case 1:
                                editor = sharedPref.edit();
                                frequency = 21;
                                editor.putInt(getString(R.string.user_refresh_frequency), frequency);
                                editor.commit();
                                frequencyDisplay = frequency + " days";
                                refreshFrequencyValue.setText(frequencyDisplay);
                                newDate = calculateNextRefreshDate(frequency);
                                tvNextRefreshDate.setText(longToString(newDate));
                                break;
                            case 2:
                                editor = sharedPref.edit();
                                frequency = 60;
                                editor.putInt(getString(R.string.user_refresh_frequency), frequency);
                                editor.putLong(getString(R.string.user_next_refresh), calculateNextRefreshDate(frequency));
                                editor.commit();
                                frequencyDisplay = 60 + " days";
                                refreshFrequencyValue.setText(frequencyDisplay);
                                newDate = calculateNextRefreshDate(frequency);
                                tvNextRefreshDate.setText(longToString(newDate));
                                break;
                        }
                    }

                });

                b.show();

            }
        });





    }

    private void populateRefreshDates() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int frequency = sharedPref.getInt(getString(R.string.user_refresh_frequency),0);
        nextRefresh = new Date(sharedPref.getLong(getString(R.string.user_next_refresh), calculateNextRefreshDate(frequency/*Zero is for default*/)));
        lastRefresh = new Date(sharedPref.getLong(getString(R.string.user_last_refresh), calculateLastRefreshDate()));
        tvLastRefreshDate.setText(dateToString(lastRefresh));
        tvNextRefreshDate.setText(dateToString(nextRefresh));
    }

    private long calculateLastRefreshDate() {
        Date d = new Date();
        return d.getTime();
    }

    private long calculateNextRefreshDate(int frequency) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Now use today date.
        if(frequency==0){frequency=60;}
        c.add(Calendar.DATE, frequency); // Adding 5 days
        Date d = c.getTime();
        return d.getTime();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.m 23qv

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            Toast.makeText(this,query,Toast.LENGTH_SHORT).show();
        }
    }

    private String dateToString(Date d) {
        DateFormat formatter = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH);
        return formatter.format(d);
    }

    @Subscribe
    public void getResponseFromForceRefresh(CompletedForceRefresh response){
        progressDialog.cancel();
        int numNewShows = 0;
        String message;
        for (int i = 0; i < response.getNewEventsList().size(); i++) {
            if(response.getNewEventsList().get(i)!=null) {
                numNewShows = numNewShows + response.getNewEventsList().get(i).getNumOfNewShows();
            }
        }

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("New events")
                .setMessage(numNewShows+ " new shows found")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        nextRefresh = new Date(sharedPref.getLong(getString(R.string.user_next_refresh), calculateNextRefreshDate(0/*Zero is for default*/)));
        int frequency = sharedPref.getInt(getString(R.string.user_refresh_frequency), 0);
        editor.putLong(getString(R.string.user_next_refresh), calculateNextRefreshDate(frequency));
        editor.putLong(getString(R.string.user_last_refresh), calculateLastRefreshDate());
        editor.commit();
        tvLastRefreshDate.setText(dateToString(lastRefresh));
        tvNextRefreshDate.setText(dateToString(nextRefresh));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                location = Parcels.unwrap(data.getParcelableExtra("result"));
                textViewLocation.setText(location.toString());
                //Put user location data into Shared Preferences
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_location_state_abr), location.getStateAbv());
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_location_state), location.getState());
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_location_address_fmt), location.getFormattedAddress());
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_location_country), location.getCountry());
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_location_zip), location.getZip());
                SharedPreferencesHelper.putStringPreference(getString(R.string.user_city), location.city);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    private void populateUserLocation(){
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        location = new CustomLocationResult();
        location.city = sharedPref.getString(getString(R.string.user_city), location.city);
        location.zip = sharedPref.getString(getString(R.string.user_location_zip), location.getZip());
        location.country = sharedPref.getString(getString(R.string.user_location_country), location.getCountry());
        location.formattedAddress = sharedPref.getString(getString(R.string.user_location_address_fmt), location.getFormattedAddress());
        location.state = sharedPref.getString(getString(R.string.user_location_state), location.getState());
        location.stateAbv = sharedPref.getString(getString(R.string.user_location_state_abr), location.getStateAbv());
        textViewLocation.setText(location.toString());
    }

    private String longToString(long longDate){
        Date d = new Date(longDate); //* 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
        return sdf.format(d);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.unregister(this);
    }

}
//Toast.makeText(this,"ARTIST IS RETURNED",Toast.LENGTH_SHORT).show();