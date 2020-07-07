package com.tonulab.velostracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Receiver for broadcasts sent by {@link LocationUpdatesService}.
 */
public class Receiver extends BroadcastReceiver {
    private MainActivity mainActivity;

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    @Override
    public void onReceive(Context context, Intent intent) {
        Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
        if (location != null){
            //                Toast.makeText(MainActivity.this, Utils.getLocationText(location),
//                        Toast.LENGTH_SHORT).show();
            mainActivity.updateLocation(location);
        }

        DataPack dataPack = intent.getParcelableExtra(LocationUpdatesService.EXTRA_DATAPACK);
        if (dataPack != null){
            mainActivity.updateDataPack(dataPack);
        }

//        String distance = intent.getStringExtra(LocationUpdatesService.EXTRA_DISTANCE);
//        if (distance != null){
//            mainActivity.updateDistance(distance);
//        }
//
//        Long time = intent.getLongExtra(LocationUpdatesService.EXTRA_TIME, -1);
//        if (time != -1){
//            mainActivity.updateTime(time);
//        }
//
//        ArrayList<PolyNode> polyline = intent.getParcelableArrayListExtra(LocationUpdatesService.EXTRA_POLYLINE);
//        if (polyline != null){
//            mainActivity.updatePolyline(polyline);
//        }
    }
}