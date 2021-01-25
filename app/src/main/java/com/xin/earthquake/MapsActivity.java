package com.xin.earthquake;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.xin.earthquake.Model.EarthQuake;
import com.xin.earthquake.UI.CustomInfoWindow;
import com.xin.earthquake.Util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapClickListener{

    private GoogleMap mMap;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private RequestQueue queue;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button KbBtn;
    StringBuilder stringBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        KbBtn = (Button) findViewById(R.id.kbBtn);
        KbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, QuakeKb.class));
            }
        });

        queue = Volley.newRequestQueue(this);
        getEarthQuakes();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        //mMap.setOnMarkerClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (Build.VERSION.SDK_INT < 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Ask for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);


            } else {
                //we have permission
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                mMap.addMarker(new MarkerOptions()
//                        .position(latLng)
//                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
//                        .title("Hello"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8));

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        }
    }

    public void getEarthQuakes(){
        final EarthQuake earthQuake = new EarthQuake();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < Constants.LIMIT; i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);

                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setLat(lat);
                                earthQuake.setLon(lon);
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setDetailLink(properties.getString("detail"));

                                java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                                String formattedDate =  dateFormat.format(new Date(Long.valueOf(properties.getLong("time"))).getTime());
                                if (earthQuake.getMagnitude() >= 2.0) {
                                    MarkerOptions markerOptions = new MarkerOptions();

                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                                    markerOptions.title(earthQuake.getPlace());
                                    markerOptions.position(new LatLng(lat, lon));
                                    markerOptions.snippet("Magnitude: " + earthQuake.getMagnitude() + "\n"
                                            + "Date: " + formattedDate);

                                    if (earthQuake.getMagnitude() >= 4.5) {
                                        CircleOptions circleOptions = new CircleOptions();
                                        circleOptions.center(new LatLng(earthQuake.getLat(), earthQuake.getLon()));
                                        circleOptions.radius(30000);
                                        circleOptions.strokeWidth(3.6f);
                                        circleOptions.fillColor(Color.RED);
                                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                        mMap.addCircle(circleOptions);
                                    }

                                    Marker marker = mMap.addMarker(markerOptions);
                                    marker.setTag(earthQuake.getDetailLink());
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 1));
                                }//Log.d("Quake: ", lon + ", " + lat);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        getQuakeDetails(marker.getTag().toString());
    }

    private void getQuakeDetails(final String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        String detailUrl = "";
                        String nearbyCity = "nearby-cities";
                        try {
                            JSONObject properties = response.getJSONObject("properties");
                            JSONObject products = properties.getJSONObject("products");
                            JSONArray nearby = products.getJSONArray(nearbyCity);
                            for (int i = 0; i < nearby.length();i++) {
                                JSONObject nearbyCityObj = nearby.getJSONObject(i);

                                JSONObject contentObj = nearbyCityObj.getJSONObject("contents");
                                JSONObject cityJsonObj = contentObj.getJSONObject("nearby-cities.json");

                                detailUrl = cityJsonObj.getString("url");
                                 if (detailUrl.equals("") || detailUrl == null) {
                                Toast.makeText(getApplicationContext(), "There is no data can be retrieving", Toast.LENGTH_SHORT).show();

                                 }
                            }
                            //Log.d("URL: ", detailUrl);

                            getMoreDetails(detailUrl);
                            getMoreDetails2(url);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
        queue.add(jsonObjectRequest);
    }
    public void getMoreDetails(String url) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        try {

                            for (int i = 0; i < response.length(); i++) {
                                JSONObject citiesObj = response.getJSONObject(i);

                                stringBuilder.append("City: " + citiesObj.getString("name")
                                        + "\n" + "Distance: " + citiesObj.getString("distance") + "km from the origin"
                                        + "\n" + "Population: "
                                        + citiesObj.getString("population"));

                                stringBuilder.append("\n\n");

                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        queue.add(jsonArrayRequest);
    }

    public void getMoreDetails2(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                        View view = getLayoutInflater().inflate(R.layout.popup, null);

                        Button dismissButton = (Button) view.findViewById(R.id.dismissPopUp);
                        Button dismissTopButton = (Button) view.findViewById(R.id.dismissPopTop);
                        TextView popUpList = (TextView) view.findViewById(R.id.popList);

                        try {

                            JSONObject properties = response.getJSONObject("properties");
                            JSONObject products = properties.getJSONObject("products");
                            JSONArray origin = products.getJSONArray("phase-data");

                            for (int i = 0; i < 1;i++) {
                                JSONObject phaseDataObj = origin.getJSONObject(i);

                                JSONObject contentObj = phaseDataObj.getJSONObject("properties");

                                stringBuilder.append("Azimuthal Gap: " + contentObj.getString("azimuthal-gap")+ " degree"  +"\n"+
                                        "Earthquake Depth: " +
                                        contentObj.getString("depth") + " km");

                                stringBuilder.append("\n\n");

                            }
                            Log.d("URL: ", stringBuilder.toString());
                            stringBuilder.append("**When Azimuthal Gap > 180 degree, it might not be an accurate earthquake." +"\n"+"\n"+
                                    "**When Earthquake Depth > 70 km, it would be a low damage earthquake.");

                            popUpList.setText(stringBuilder);

                            dismissButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    stringBuilder.setLength(0);
                                }
                            });
                            dismissTopButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                    stringBuilder.setLength(0);
                                }
                            });
                            dialogBuilder.setView(view);
                            dialog = dialogBuilder.create();
                            dialog.show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        queue.add(jsonObjectRequest);
    }



    @Override
    public void onMapClick(LatLng latLng) {

    }
}