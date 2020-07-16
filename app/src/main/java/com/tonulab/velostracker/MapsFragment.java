package com.tonulab.velostracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    //Latitud y longitud de donde se situa la camara
    private double lat;
    private double lon;

    private boolean startLocation = true;

    private View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.maps, container, false);
        } catch (InflateException e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.lat = 0;
        this.lon = 0;

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }else{
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null && startLocation) {
                                // Logic to handle location object
                                setLat(location.getLatitude());
                                setLon(location.getLongitude());
                                moveCamera(16);
                            }else
                                startLocation = true;
                        }
                    });
        }
    }

    //Setters, getters y demas utilidades
    public void setLat(double l) {
        this.lat = l;
    }

    public void setLon(double l) {
        this.lon = l;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public boolean getMapState() {
        return mMap != null;
    }

    public void setStartLocation(boolean startLocation){
        this.startLocation = startLocation;
    }

    public void updatePolyline(ArrayList<PolyNode> polyNodeArray){
        if (getMapState()){
            PolylineOptions polyline = new PolylineOptions().width(15).color(Color.RED);
            for (int i = 0; i < polyNodeArray.size(); i++) {
                polyline.add(new LatLng(polyNodeArray.get(i).getLatitude(), polyNodeArray.get(i).getLongitude()) );
            }
            clearMap();
            mMap.addPolyline(polyline);
        }

    }

    public void clearMap(){
        mMap.clear();
    }

    public void moveCamera(Integer zoom) {
        LatLng position = new LatLng(lat, lon);
        if (getMapState()){
            if(zoom != null) {
                CameraUpdate localizacion = CameraUpdateFactory.newLatLngZoom(position, zoom);
                mMap.moveCamera(localizacion);
//            mMap.animateCamera(localizacion);
            }
            else {
                CameraUpdate localizacion = CameraUpdateFactory.newLatLng(position);
                mMap.moveCamera(localizacion);
            }
        }

    }
}
