package com.tonulab.velostracker;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.math.BigDecimal;

import static com.tonulab.velostracker.Utils.MODES.AUTOMOVILISMO;
import static com.tonulab.velostracker.Utils.MODES.CICLISMO;

class Utils {

    public enum MODES {
        CICLISMO {
            public String toString() {
                return "Ciclismo";
            }
        },
        PEDESTRISMO {
            public String toString() {
                return "Pedestrismo";
            }
        },
        AUTOMOVILISMO {
            public String toString() {
                return "Automovilismo";
            }
        }
    }

    static final String UPDATE_STATE = "update_state";
    static final String PAUSED_UPDATE = "paused_update";
    static final String MODE = "mode";
    static final String TRACKING = "tracking";
    static final String AUTH_PROVIDER = "auth_provider";
    static final String USER_ID = "user_id";
    static private String selectedMode = CICLISMO.toString();


    static boolean getUpdateState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(UPDATE_STATE, false);
    }

    static void setUpdateState(Context context, boolean updateState) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(UPDATE_STATE, updateState)
                .apply();
    }

    static boolean getPausedState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PAUSED_UPDATE, false);
    }

    static void setPausedState(Context context, boolean updateState) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PAUSED_UPDATE, updateState)
                .apply();
    }

    static boolean getTracking(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(TRACKING, false);
    }

    static void setTracking(Context context, boolean tracking) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(TRACKING, tracking)
                .apply();
    }

    static void setMode(Context context, String mode){
        selectedMode = mode;
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(MODE, mode)
                .apply();
    }

    static String getMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(MODE, CICLISMO.toString());
    }

    static Float getMtsRefresh(){
        if (selectedMode.equals(MODES.PEDESTRISMO.toString()))
            return 2.0f;
        if (selectedMode.equals(AUTOMOVILISMO.toString()))
            return 30.0f;
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