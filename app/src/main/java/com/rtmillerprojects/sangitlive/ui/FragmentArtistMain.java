package com.rtmillerprojects.sangitlive.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.rtmillerprojects.sangitlive.R;
import com.rtmillerprojects.sangitlive.adapter.ArtistsSectionsPagerAdapter;
import com.rtmillerprojects.sangitlive.listener.GetMbid;
import com.rtmillerprojects.sangitlive.listener.MainListener;
import com.rtmillerprojects.sangitlive.model.ArtistDetails;
import com.rtmillerprojects.sangitlive.model.EventCalls.LastFmArtistDetails;
import com.rtmillerprojects.sangitlive.model.lastfmartistsearch.ArtistLastFm;
import com.rtmillerprojects.sangitlive.util.DatabaseHelper;
import com.rtmillerprojects.sangitlive.util.EventManagerService;

/**
 * Created by Ryan on 9/2/2016.
 */
public class FragmentArtistMain extends BaseFragment{

    Toolbar toolbar;
    ViewPager viewPager;
    TabLayout tabLayout;
    FloatingActionButton fab;
    String mbid;
    GetMbid mbidListener;
    CheckBox starArtist;
    DatabaseHelper db;
    boolean isFavorite;
    String artistName;
    ArtistDetails ad;
    EventManagerService ems;

    private MainListener listener;
    private ArtistsSectionsPagerAdapter artistPagerAdapter;

    public static FragmentArtistMain newInstance() {
        FragmentArtistMain fragment = new FragmentArtistMain();
        return fragment;
    }

    public FragmentArtistMain() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main_artist, container, false);
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        viewPager = (ViewPager) rootView.findViewById(R.id.view_pager);
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        starArtist = (CheckBox) rootView.findViewById(R.id.save_artist_star);
        mbid = mbidListener.getMbid();
        artistName = mbidListener.getArtistName();
        ad = new ArtistDetails();
        ad.setMbid(mbid);
        ad.setName(artistName);

        fab.hide();
        String toolbarTitle=getArguments().getString("artistName");

        //Configure Toolbar back button
        toolbar.setTitle(toolbarTitle);
        ACA.setSupportActionBar(toolbar);
        ACA.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ACA.getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ACA.onBackPressed();
            }
        });

        // Create the adapter that will return fragments for the view pager
        //UNCOMMENT!!!
        artistPagerAdapter = new ArtistsSectionsPagerAdapter(getChildFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager.setAdapter(artistPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override public void onPageSelected(int position) {
                    fab.hide();
            }

            @Override public void onPageScrollStateChanged(int state) {
            }
        });

        /* ARTIST STAR LOGIC */
        //Determine if saved already to set star state
        db = DatabaseHelper.getInstance(ACA);
        final ArtistLastFm artist = db.getArtistById(mbid);

        if(artist==null) {
            starArtist.setChecked(false);
        }
        else{
            starArtist.setChecked(true);
        }


        starArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(starArtist.isChecked()==false){

                    new AlertDialog.Builder(ACA)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Confirm selection")
                            .setMessage("Remove this event?")
                            .setPositiveButton("Remove", new DialogInterface.OnClickListener() {


                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Delete from db
                                    DatabaseHelper db = DatabaseHelper.getInstance(ACA);
                                    db.deleteArtist(artist.getArtist().getMbid());
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    starArtist.setChecked(true);
                                }
                            })
                            .show();
                }
                else{
                    DatabaseHelper db = DatabaseHelper.getInstance(ACA);
                    ems = ems.getInstance(ACA);
                    db.insertArtist(ad);
                }

            }
        });


        // Set up the TabLayout with the ViewPager
        tabLayout.setupWithViewPager(viewPager);


        // Listen for fab clicks
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {

            }
        });


        return rootView;
    }

    @Override public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mbidListener = (ActivityArtistDetail) context;
    }
}
