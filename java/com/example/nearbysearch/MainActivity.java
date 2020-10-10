package com.example.nearbysearch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest.permission;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    //Initialize Variable
    Spinner spType;
    Button btFind;
    SupportMapFragment supportMapFragment;
    GoogleMap map;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentlat=0,currentlong=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Assign Variable
        spType=(Spinner)findViewById(R.id.sp_type);
        btFind=(Button)findViewById(R.id.bt_find);
        supportMapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);

        //Intialize array of place type
        final String[] placeTypeList={"atm","bank","hospital","movies","restaurant"};
        String[] placeNameList={"ATM","Bank","Hospital","Movies","Restaurant"};

        //Set adapter on Spinner

        spType.setAdapter(new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,placeNameList));

        //Intialize fused location provider client

        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);

        //check permission

        if(ActivityCompat.checkSelfPermission(MainActivity.this, permission.ACCESS_FINE_LOCATION) == PERMISSION_GRANTED)
        {
            //when permission granted
            //call method

            getCurrentLocation();
        }

        else {
            //When Permission Denied
            //Request permission

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{permission.ACCESS_FINE_LOCATION},44);
        }

        btFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i=spType.getSelectedItemPosition();

                String url="https://maps.googleapis.com/maps/api/place/nearbysearch/json"+
                        "?location="+currentlat+","+currentlong+"&radius=5000"+
                        "&types="+placeTypeList[i]+"&sensor=true"+
                        "&key="+getResources().getString(R.string.google_map_key);
                Log.d("Url",url);

            }
        });


    }

    private void getCurrentLocation() {

        Task<Location> task=fusedLocationProviderClient.getLastLocation();

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location !=null)
                {
                  currentlat=location.getLatitude();
                  currentlong=location.getLongitude();

                  supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                      @Override
                      public void onMapReady(GoogleMap googleMap) {

                          map=googleMap;

                          map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                  new LatLng(currentlat,currentlong),10
                          ));
                      }
                  });
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==44)
        {
            if (grantResults.length > 0 && grantResults[0]== PERMISSION_GRANTED) {
                return;
            }

            //when Permission Granted

            getCurrentLocation();

        }
    }

    private class PlaceTask extends AsyncTask<String,Integer,String>
    {

        @Override
        protected String doInBackground(String... strings) {

            String data=null;

            try {
                //Initialize Data
                   data=downloadUrl(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return data;
        }

        @Override
        protected void onPostExecute(String s) {
            //Execute parser task
            new ParserTask().execute(s);

        }
    }

    private String downloadUrl(String string) throws IOException {
        //Initialize URL

        URL url=new URL(string);

        HttpURLConnection connection=(HttpURLConnection) url.openConnection();
        connection.connect();

        InputStream stream=connection.getInputStream();

        BufferedReader reader=new BufferedReader(new InputStreamReader(stream));

        StringBuilder builder =new StringBuilder();

        String line="";

        while((line=reader.readLine())!=null) {

            builder.append(line);
        }
        String data=builder.toString();

        reader.close();

        return data;
        }


    private class ParserTask extends AsyncTask<String,Integer, List<HashMap<String,String>>> {


        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {
          //create json parser class

            JsonParser jsonParser=new JsonParser();

            List<HashMap<String,String>> mapList=null;

            JSONObject object=null;

            try {
                 object=new JSONObject((strings[0]));
                 mapList=jsonParser.parseResult(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return mapList;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> hashMaps) {
            map.clear();
            for(int i=0;i<hashMaps.size();i++)
            {
                HashMap<String,String> hashMapList=hashMaps.get(i);

                double lat=Double.parseDouble(hashMapList.get("lat"));
                double lng=Double.parseDouble(hashMapList.get("lng"));

                String name=hashMapList.get("name");

                LatLng latLng=new LatLng(lat,lng);

                MarkerOptions markerOptions=new MarkerOptions();

                markerOptions.position(latLng);

                markerOptions.title(name);

                map.addMarker(markerOptions);

            }
        }
    }


}

