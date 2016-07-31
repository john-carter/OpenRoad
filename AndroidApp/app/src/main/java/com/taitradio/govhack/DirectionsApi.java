package com.taitradio.govhack;


import android.text.Layout;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by daniel on 28/07/16.
 */
public class DirectionsApi
{
    final DirectionsHandler _directionsHandler;
    //final String _urlBase = "https://maps.googleapis.com/maps/api/directions/json";
    final String _urlBase = "http://10.126.54.146/cgi-bin/route";

    public DirectionsApi(DirectionsHandler directionsHandler)
    {
        _directionsHandler = directionsHandler;
    }

    public String getDirections(final LatLng source, final String destination)
    {

        Thread thread = new Thread() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try
                {
                    String encodedDestination = URLEncoder.encode(destination, "UTF-8");
                    StringBuilder urlBuilder = new StringBuilder();
                    urlBuilder.append(_urlBase + "?");
                    urlBuilder.append("origin=" + String.valueOf(source.latitude) + "," + String.valueOf(source.longitude));
                    urlBuilder.append("&destination=" + encodedDestination );
                    //urlBuilder.append("&alternatives=" + "true" );
                    //urlBuilder.append("&key=AIzaSyCnNSIiNWSOTO1nHxD7f-MTczEvXeBgAJo");

                    Log.d("DirectionApi", "URL: " + urlBuilder.toString());
                    URL url = new URL(urlBuilder.toString());

                    urlConnection = (HttpURLConnection) url.openConnection();


                    BufferedReader bufferedReader = new
                            BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null)
                    {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    String directions = stringBuilder.toString();
                    GsonBuilder builder = new GsonBuilder();
                    Gson gson = builder.create();
                    Log.d("DirectionsApi", directions);
                    DirectionsResult result = gson.fromJson(directions, DirectionsResult.class );

                    _directionsHandler.showDirections(result);
                    return;
                }
                catch(Exception e)
                {
                    Log.e("ERROR", e.getMessage(), e);
                    return;
                }
                finally
                {
                    if(urlConnection != null)
                    {
                        urlConnection.disconnect();
                    }
                }
            }
        };

        thread.start();


        return "";
    }
}