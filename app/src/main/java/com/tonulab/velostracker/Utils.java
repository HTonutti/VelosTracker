package com.tonulab.velostracker;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.math.BigDecimal;

class Utils {

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";
    static private String selectedMode = "Ciclismo";

    static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    static void setMode(String mode){
        selectedMode = mode;
    }

    static String getMode() {return selectedMode;}

    static Float getMtsRefresh(){
        switch (selectedMode){
            case "Pedestrismo":{
                return 2.0f;
            }
            case "Ciclismo":{
                return 10.0f;
            }
            case "Automovilismo":{
                return 30.0f;
            }
        }
        return 10.0f;
    }

    static String getLocationText(Location location) {
        return location == null ? "Ubicación desconocida" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    static String getNotificationText(BigDecimal distance, Long time){
        String strResult = "";
        if (distance != null){
            strResult += "Distancia recorrida: " + distance + " km en ";
        }
        if (time != null){
            int[] auxTime = splitToComponentTimes(time);
            String strTiempo = "";
            for (int i = 0; i < 3; i++) {
                if (auxTime[i] < 10){
                    strTiempo += "0" + auxTime[i];
                }
                else{
                    strTiempo += auxTime[i];
                }
                if (i != 2){
                    strTiempo += ':';
                }
            }
            strResult += strTiempo;
        }
        return strResult;
    }

    static String getNotificationTitle(Context context) {
        return "Seguimiento activo";
    }

    public static int[] splitToComponentTimes(long timeInSec)
    {
        int hours = (int) timeInSec / 3600;
        int remainder = (int) timeInSec - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        int[] ints = {hours , mins , secs};
        return ints;
    }
}