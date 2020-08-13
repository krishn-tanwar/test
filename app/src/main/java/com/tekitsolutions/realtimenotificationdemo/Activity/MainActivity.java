package com.tekitsolutions.realtimenotificationdemo.Activity;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.tekitsolutions.realtimenotificationdemo.Model.NamedGeofence;
import com.tekitsolutions.realtimenotificationdemo.Model.PlaceList;
import com.tekitsolutions.realtimenotificationdemo.R;
import com.tekitsolutions.realtimenotificationdemo.RestApi.ApiClient;
import com.tekitsolutions.realtimenotificationdemo.RestApi.ApiInterface;
import com.tekitsolutions.realtimenotificationdemo.Utils.Constants;
import com.tekitsolutions.realtimenotificationdemo.Utils.ConvertLongitudeToString;
import com.tekitsolutions.realtimenotificationdemo.Utils.GeocodingLocation;
import com.tekitsolutions.realtimenotificationdemo.Utils.GeofenceController;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final String TAG = "MainActivity";

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    LocationManager manager;
    private FusedLocationProviderClient mFusedLocationClient;
    private Toolbar toolbar;
    private TextView tvToolbarTitle;
    private Button btnStart, btnStop;
    private TextView tvCurrentLocation;
    private Spinner spDestination;
    private String selectedDestination;


    private List<String> placeList = new ArrayList<>();
    private String apiKey = "AIzaSyAgm2bbpHpGG4wRRI9se9LxP-YytA2fQF4";
    private int radius = 50000;
    private String location;

    private Double latitude, longitude;


    private GeofencingRequest geofencingRequest;
    private GoogleApiClient googleApiClient;
    private boolean isMonitoring = false;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        tvToolbarTitle.setText("Real Time Notification Demo");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        GeofenceController.getInstance().init(this);
    }

    private void init() {
        toolbar = findViewById(R.id.toolbar);
        tvToolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvCurrentLocation = findViewById(R.id.tv_current_location);
        spDestination = findViewById(R.id.sp_destination);
        checkPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            init();
        } else {
            Toast.makeText(this, "Please on Your GPS for Fetch Current Location", Toast.LENGTH_LONG).show();
            finish();
        }

        googleApiClient.reconnect();
    }

    private void checkPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }

    private void fetchLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1);
        locationRequest.setFastestInterval(1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).getLastLocation();

        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    String address = ConvertLongitudeToString.getCompleteAddressString(latitude, longitude, getApplicationContext());
                    tvCurrentLocation.setText(address);
                    getDestinationList();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            }
        }
    }


    private void getDestinationList() {
        String lat = String.valueOf(latitude);
        String lng = String.valueOf(longitude);
        location = lat + "," + lng;
        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<PlaceList> places = apiInterface.getDestination(location, radius, apiKey);
        places.enqueue(new Callback<PlaceList>() {
            @Override
            public void onResponse(Call<PlaceList> call, Response<PlaceList> response) {

                for (int i = 0; i < response.body().getResults().size(); i++) {
                    placeList.add(response.body().getResults().get(i).getVicinity());
                }


                setSpinner();
                checkValidation();
            }

            @Override
            public void onFailure(Call<PlaceList> call, Throwable t) {

                Log.d("Failed", "showError : " + t.getMessage());
            }
        });
    }


    private void setSpinner() {
        ArrayAdapter<String> adp = new ArrayAdapter<String>(this, R.layout.row_spinner_item, placeList);
        adp.setDropDownViewResource(R.layout.row_spinner_item);

        spDestination.setAdapter(adp);
        spDestination.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedDestination = adapterView.getItemAtPosition(i).toString();

                GeocodingLocation locationAddress = new GeocodingLocation();
                GeocodingLocation.getAddressFromLocation(selectedDestination,
                        getApplicationContext(), new GeocoderHandler());

                if (dataIsValid()){
                    NamedGeofence geofence = new NamedGeofence();
                    geofence.name = selectedDestination;
                    geofence.latitude = Double.parseDouble("22.7571");
                    geofence.longitude = Double.parseDouble("75.8822");
                    geofence.radius = Float.parseFloat("10") * 1000.0f;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private boolean dataIsValid() {
        boolean validData = true;

        String name = selectedDestination;
        String latitudeString = "22.7533";
        String longitudeString = "75.8937";
        String radiusString = "2";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(latitudeString)
                || TextUtils.isEmpty(longitudeString) || TextUtils.isEmpty(radiusString)) {
            validData = false;
        } else {
            double latitude = Double.parseDouble(latitudeString);
            double longitude = Double.parseDouble(longitudeString);
            float radius = Float.parseFloat(radiusString);
            if ((latitude < Constants.Geometry.MinLatitude || latitude > Constants.Geometry.MaxLatitude)
                    || (longitude < Constants.Geometry.MinLongitude || longitude > Constants.Geometry.MaxLongitude)
                    || (radius < Constants.Geometry.MinRadius || radius > Constants.Geometry.MaxRadius)) {
                validData = false;
            }
        }

        return validData;
    }

    private void checkValidation() {
        if (spDestination.getCount() == 0) {
            btnStart.setVisibility(View.GONE);
        } else {
            btnStart.setVisibility(View.VISIBLE);
            btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Toast.makeText(MainActivity.this, "start geofence", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    //this inner class for convert destination address to latlng

    @Override
    protected void onResume() {
        super.onResume();

        int googlePlayServicesCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        Log.i(MainActivity.class.getSimpleName(), "googlePlayServicesCode = " + googlePlayServicesCode);

        if (googlePlayServicesCode == 1 || googlePlayServicesCode == 2 || googlePlayServicesCode == 3) {
            GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCode, this, 0).show();
        }
    }

    //this code for notification using geofence

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            Toast.makeText(MainActivity.this, "latlng:" + locationAddress, Toast.LENGTH_LONG).show();
        }
    }
}
