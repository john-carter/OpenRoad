package com.taitradio.govhack;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements DirectionsHandler, OnMapReadyCallback, LocationListener
{
    GoogleMap _map;
    LocationManager _locationManager;
    EditText _destinationEditText;
    int[] _colors = new int[5];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        _locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if( PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION))
        {
            _locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10, this);
        }

        _colors[0] = Color.parseColor("#05b1fb"); //blue
        _colors[1] = Color.parseColor("#FF4500"); //orrange red
        _colors[2] = Color.parseColor("#DB7093"); //Pale Violet Red
        _colors[3] = Color.parseColor("#FFFF00"); //yellow
        _colors[4] = Color.parseColor("#9ACD32"); //yellow green

        _destinationEditText = (EditText)findViewById(R.id.toAddress);
        _destinationEditText.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                        {
                            if (event == null || !event.isShiftPressed())
                            {
                                // the user is done typing.
                                getDirections();
                                View view = getCurrentFocus();
                                if (view != null)
                                {
                                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                                }
                                return true; // consume.
                            }
                        }
                        return false; // pass on to other listeners.
                    }
                });

    }

    private void getDirections()
    {
        getDirections(_destinationEditText.getText().toString());
    }

    private void getDirections(String destination)
    {
        DirectionsApi directionsApi = new DirectionsApi(this);
        LatLng origin = _currentPosition;
        if(origin == null)
        {
            //fall back to GovHack location
            origin = new LatLng(-43.531741, 172.631707);
        }
        directionsApi.getDirections(origin, destination);
    }



    private void drawRoute(Route route, int color)
    {
        String encodedString = route.overview_polyline.points;
        List<LatLng> list = decodePoly(encodedString);
        for(int index = 0; index < list.size(); index++)
        {
            addPosition(list.get(index));
        }

        com.google.android.gms.maps.model.Polyline line = _map.addPolyline(new PolylineOptions()
                .addAll(list)
                .width(12)
                .color(color)//Google maps blue color
                .geodesic(true)
        );

        if(route.impediments == null)
        {
            return;
        }

        List<Impediment> _impediments = route.impediments;

        for(int index = 0; index < _impediments.size(); index++)
        {
            Impediment impediment = _impediments.get(index);
            LatLng point = new LatLng(impediment.lat, impediment.lng);

            addClosedMarker(point, impediment.description);
        }
    }

    @Override
    public void showDirections(final DirectionsResult result) {
        Log.d("MainActivity", result.status);
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(result.routes.size() == 0)
                {
                    Toast.makeText(getBaseContext(), "No route found",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                _map.clear();
                clearPositions();

                List<Route> routes = result.routes;

                for(int index = 0; index < routes.size(); index++)
                {
                    drawRoute(routes.get(index), _colors[index]);
                }

                Location startLocation = result.routes.get(0).legs.get(0).steps.get(0).start_location;
                LatLng start = new LatLng(startLocation.lat, startLocation.lng);

                Marker startMarker = _map.addMarker(new MarkerOptions()
                    .position(start)
                    .title("Start"));

                //addClosedMarker(new LatLng(-43.529857,172.634394), "Vehicle Lane(s) Closed, Detours In Place, Shoulder Closure");

                LatLng destination = new LatLng(startLocation.lat, startLocation.lng);
                Marker destinationMarker = _map.addMarker(new MarkerOptions()
                    .position(getDestination(result.routes.get(0)))
                    .title("Destination"));

                addPosition(startMarker.getPosition());
                addPosition(destinationMarker.getPosition());

                zoomToPositions();
                //Calculate the markers to get their position
            }
        });
    }

    private void addClosedMarker(LatLng position, String description)
    {
        Marker destinationMarker = _map.addMarker(new MarkerOptions()
            .position(position)
            .title(description)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.closeiconsmall)));
    }

    private void zoomToPositions()
    {
        //Calculate the markers to get their position
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for(int index = 0; index < _currentPoints.size(); index++ )
        {
            b.include(_currentPoints.get(index));
        }

        LatLngBounds bounds = b.build();
        //Change the padding as per needed
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 30);
        _map.animateCamera(cu);
    }

    List<LatLng> _currentPoints = new ArrayList<LatLng>();

    void addPosition(LatLng latLng)
    {
        _currentPoints.add(latLng);
    }

    void clearPositions()
    {
        _currentPoints.clear();
    }

    private LatLng getDestination(Route route)
    {

        Leg lastLeg = route.legs.get(route.legs.size() - 1);
        Step lastStep = lastLeg.steps.get(lastLeg.steps.size() - 1);

        Location destinationLocation = lastStep.end_location;
        return new LatLng(destinationLocation.lat, destinationLocation.lng);
    }

    private List<LatLng> decodePoly(String encoded)
    {
        Log.d("MainActivity", "ployline: " + encoded);
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        _map = googleMap;
        _map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0)
            {
                getDirections(String.valueOf(arg0.latitude) + "," + String.valueOf(arg0.longitude));
                // TODO Auto-generated method stub
                Log.d("arg0", arg0.latitude + "-" + arg0.longitude);
            }
        });

        _map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter()
        {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(getBaseContext());
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getBaseContext());
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(getBaseContext());
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    LatLng _currentPosition = null;

    @Override
    public void onLocationChanged(android.location.Location location)
    {
        _currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider)
    {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
