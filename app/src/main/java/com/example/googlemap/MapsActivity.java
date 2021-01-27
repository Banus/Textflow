package com.example.googlemap;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.googlemap.activityrecognition.Constants;
import com.example.googlemap.activityrecognition.DetectedActivitiesIntentService;
import com.example.googlemap.activityrecognition.Utils;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

    protected static final String TAG = "MapsActivity";

    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LatLng mDefaultLocation = new LatLng(39.773533, -86.177118);  // IUPUI campus
    private Location mLastKnownLocation;
    static final float DEFAULT_ZOOM = 16;
    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
    static final int PERMISSIONS_REQUEST_ACCESS_CALENDAR = 0;

    private static final Map<String, LatLng> known_locations = new HashMap<String, LatLng>() {{
        put("work", new LatLng(39.776180, -86.167313));  // Walker Plaza
        put("home", new LatLng(39.977280, -86.154484));  // N Pennsylvania St
    }};

    /**
     * The entry point for interacting with activity recognition.
     */
    private ActivityRecognitionClient mActivityRecognitionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        getLocationPermission();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        requestActivityUpdates();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
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

        updateLocationUI();
        getDeviceLocation();

        // IMPORTANT: months start from 0
        ArrayList<HashMap<String, String>> events = CalendarUtility.readCalendarEvent(
                this, new GregorianCalendar(2020, 2, 1).getTime(),
                new GregorianCalendar(2020, 2, 20).getTime());
        for (HashMap<String, String> event : events) {
            Log.d(TAG, event.toString());
        }

//        search_nearby_locations();
    }

    // activity recognition handling

    /**
     * Processes the list of freshly detected activities. Asks the adapter to update its list of
     * DetectedActivities with new {@code DetectedActivity} objects reflecting the latest detected
     * activities.
     */
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = Utils.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(Constants.KEY_DETECTED_ACTIVITIES, ""));

        Log.d(TAG, "Detected activities:");
        for (DetectedActivity act : detectedActivities) {
            Log.d(TAG, Utils.getActivityString(this, act.getType()) + " " +
                    act.getConfidence());
        }
    }

    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private void setUpdatesRequestedState(boolean requesting) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, requesting)
                .apply();
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Registers for activity recognition updates using
     * {@link ActivityRecognitionClient#requestActivityUpdates(long, PendingIntent)}.
     * Registers success and failure callbacks.
     */
    public void requestActivityUpdates() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent());

        task.addOnSuccessListener(result -> {
            Log.d(TAG, "Enabled activity updates.");
            setUpdatesRequestedState(true);
            updateDetectedActivitiesList();
        });

        task.addOnFailureListener(e -> {
            Log.w(TAG, "Failed to enable activity updates.");
            setUpdatesRequestedState(false);
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(Constants.KEY_DETECTED_ACTIVITIES)) {
            updateDetectedActivitiesList();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    // location handling

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR,
                            Manifest.permission.WRITE_CALENDAR},
                    PERMISSIONS_REQUEST_ACCESS_CALENDAR);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
            }
        }
        updateLocationUI();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener((@NonNull Task<Location> task) -> {
            if (task.isSuccessful()) {
                // Set the map's camera position to the current location of the device.
                mLastKnownLocation = task.getResult();
                if (mLastKnownLocation != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLastKnownLocation.getLatitude(),
                                    mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        });
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.setTrafficEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.setTrafficEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
            getLocationPermission();
        }
    }

    private ArrayList<PlacesSearchResult> to_list(PlacesSearchResponse response) {
        if (response.results != null) return new ArrayList<>(Arrays.asList(response.results));
        return new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private void search_nearby_locations() {
        LatLng loc = known_locations.get("home");
        if (loc == null) return;

        NearbySearch locator = new NearbySearch(this);
        com.google.maps.model.LatLng location_cast = new com.google.maps.model.LatLng(
                loc.latitude, loc.longitude);
        ArrayList<PlacesSearchResult> placesSearchResults = to_list(locator.run(
                location_cast, PlaceType.RESTAURANT));
        placesSearchResults.addAll(to_list(locator.run(location_cast, PlaceType.BUS_STATION)));
        // AIRPORT, BUS_STATION, TRAIN_STATION, TRANSIT_STATION, LIGHT_RAIL_STATION, LIBRARY

        for (PlacesSearchResult psr: placesSearchResults) {
            Log.e(TAG, psr.toString());
        }

        if (mLastKnownLocation == null) return;

        LatLng curr_location = new LatLng(mLastKnownLocation.getLatitude(),
                                          mLastKnownLocation.getLongitude());
        for(Map.Entry<String, LatLng> entry : known_locations.entrySet()) {
            LatLng location = entry.getValue();
            if (distance(curr_location.latitude, curr_location.longitude,
                         location.latitude, location.longitude) < 0.1) {  // threshold in miles
                Log.e(TAG, entry.getKey());
            }
        }
    }

    private double distance(double lat1, double lng1, double lat2, double lng2) {
        // use it with known_locations
        double earth_radius = 3958.75; // distance in miles

        double sin_lat = Math.sin(Math.toRadians(lat2-lat1) / 2);
        double sin_lng = Math.sin(Math.toRadians(lng2-lng1) / 2);

        double a = sin_lat * sin_lat + sin_lng * sin_lng
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        return 2 * earth_radius * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
