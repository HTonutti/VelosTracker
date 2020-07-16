package com.tonulab.velostracker;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME =
            "package com.tonulab.velostracker";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    // Nombre del canal para las notificaciones
    private static final String CHANNEL_ID = "channel_01";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    static final String EXTRA_DATAPACK = PACKAGE_NAME + ".datapack";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    // Minima distancia en metros recorridos para que actualice la polilinea y cuente como metro recorrido
    private float MINIMUN_DISTANCE_IN_METERS;

    // Intervalo en el cual se realizan actualizaciones
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    // Intervalo mas rapido de actualizacion, estas nuncas seran mas rapidas que este valor
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Minimo desplazamineto para que se realice una actualizacion de ubicacion
    private static final float MINIMUN_DISPLACEMENT_IN_METERS = 1.0f;

    // Idetificador para la notificacion del servicio cuando esta en primer plano
    private static final int NOTIFICATION_ID = 12345678;

    /**
     Se utiliza para verificar si la actividad vinculada realmente se ha ido y no se
     ha desvinculado como parte de un cambio de orientación. Creamos una notificación
     de servicio en primer plano solo si se lleva a cabo la primera.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    // Contiene los parametros usados en {@link com.google.android.gms.location.FusedLocationProviderClient}.
    private LocationRequest mLocationRequest;

    // Da acceso al Fused Location Provider API.
    private FusedLocationProviderClient mFusedLocationClient;

    // Callback por los cambios de ubicacion
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    private FirebaseManager firebaseManager = null;

    // Ubicacion actual
    private Location mLocation;

    private Location antLocation;

    private ArrayList<PolyNode> polyNodeArray;

    private BigDecimal realDistance = BigDecimal.valueOf(0);

    private Double roundedDistance = 0D;

    private String startDate;

    private static Timer timer;

    private Long startTime;

    private Long currentTime = 0L;

    private Long accumulatedTime = 0L;

    private boolean startedFromNotification;

    private boolean firstTime = false;
    // Constructor
    public LocationUpdatesService() {}

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseManager = FirebaseManager.getInstance();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Servicio comenzado ID: " + startId);
        startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,false);
        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            Intent intentAct = new Intent(this, MainActivity.class);
            startActivity(intentAct);
            Intent intentCloseNoticationPanel = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            getApplicationContext().sendBroadcast(intentCloseNoticationPanel);
            removeLocationUpdates();
        }

        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "La actividad se ha enlazado al servicio por primera vez");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "La actividad se ha enlazado nuevamente al servicio");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "La actividad se ha desenlazado del servicio");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.getUpdateState(this)) {
            Log.i(TAG, "Ejecutando servicio en primer plano");
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        if (!Utils.getPausedState(this)) {
            startTime = System.currentTimeMillis();
            currentTime = 0L;
            realDistance = BigDecimal.valueOf(0);
            roundedDistance = 0D;
            polyNodeArray = new ArrayList<>();
            DateFormat DFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            startDate = DFormat.format(new Date());
            MINIMUN_DISTANCE_IN_METERS = Utils.getMtsRefresh();

            startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        }
        else
            Utils.setPausedState(this, true);
        firstTime = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new timeUpdateTask(), 0, 1000);

        Log.i(TAG, "Requiriendo actualizaciones de ubicación");
        Utils.setUpdateState(this, true);
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setUpdateState(this, false);
            Log.e(TAG, "Permiso de ubicación perdido. No se pudieron solicitar actualizaciones" + unlikely);
        }
    }

    private void pauseLocationUpdate(){
        accumulatedTime = accumulatedTime + currentTime;
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        Utils.setPausedState(this, true);
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Finalizando actualizaciones de ubicación");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setUpdateState(this, false);
            writeOnDatabase();
            timer.cancel();
        } catch (SecurityException unlikely) {
            Utils.setUpdateState(this, true);
            Log.e(TAG, "Permiso de ubicación perdido. No se pudieron solicitar actualizaciones " + unlikely);
        }
    }

    public void requestSendUpdate(){
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_DATAPACK, new DataPack(String.valueOf(roundedDistance), String.valueOf(currentTime), startDate, null, polyNodeArray));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        if (!Utils.getUpdateState(this))
            stopSelf();
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getNotificationText(BigDecimal.valueOf(roundedDistance), currentTime);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

//        PendingIntent pauseIntent = PendingIntent.getForegroundService();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .addAction(R.drawable.ic_play, "Ir a la aplicación",
//                        activityPendingIntent)
                .setContentIntent(activityPendingIntent)
                .addAction(R.drawable.ic_stop, "Detener seguimiento",
                        servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Utils.getNotificationTitle(this))
                .setOnlyAlertOnce(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification)
                .setTicker(text)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis());

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Error al obtener ubicacion");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Permiso de ubicación perdido." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "Nueva ubicación: " + Utils.getLocationText(location));

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        refreshDistance(location);
        if (firstTime){
            polyNodeArray.add(new PolyNode(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), roundedDistance));
            firstTime = false;
        }
        intent.putExtra(EXTRA_LOCATION, mLocation);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Sets the location request parameters.
     */
    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(MINIMUN_DISPLACEMENT_IN_METERS);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshDistance(Location loc){
        float[] distanceRes = new float[3];
        distanceRes[0] = 0;
        if (antLocation != null){
            Location.distanceBetween(mLocation.getLatitude(), mLocation.getLongitude(), antLocation.getLatitude(), antLocation.getLongitude(), distanceRes);
        }
        else{
            antLocation = mLocation;
        }
        Location auxLocation = mLocation;
        mLocation = loc;
        if (distanceRes[0] > MINIMUN_DISTANCE_IN_METERS) {
            realDistance = BigDecimal.valueOf(distanceRes[0]).divide(BigDecimal.valueOf(1000)).add(realDistance);
            roundedDistance = realDistance.divide(BigDecimal.valueOf(1), 2, RoundingMode.HALF_EVEN).doubleValue();
            Log.i(TAG, "Distancia actualizada: " + realDistance);
            polyNodeArray.add(new PolyNode(mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getAltitude(), roundedDistance));
            antLocation = auxLocation;
        }
    }

    public void reset(){
        startTime = 0L;
        accumulatedTime = 0L;
        currentTime = 0L;
        realDistance = BigDecimal.valueOf(0);
        roundedDistance = 0D;
        polyNodeArray = new ArrayList<>();
        mLocation = null;
        antLocation = null;
        realDistance = BigDecimal.valueOf(0);
        roundedDistance = 0D;
        startDate = null;
        firstTime = false;
    }

    private void writeOnDatabase() {
        if (startDate.equals(""))
            startDate = "Sin fecha";
        BigDecimal avg = BigDecimal.valueOf(0);
        try{
            if (currentTime != 0 && !realDistance.equals(BigDecimal.valueOf(0)))
                avg = realDistance.multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(currentTime), 1, RoundingMode.HALF_DOWN);
        }catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }catch (ArithmeticException ae) {
            System.out.println("ArithmeticException: " + ae.getMessage());
        }
        DataPack reg = new DataPack(roundedDistance.toString(), String.valueOf(currentTime), startDate, String.valueOf(avg), polyNodeArray);
        firebaseManager.writeOnFirebase(reg);
    }

    private class timeUpdateTask extends TimerTask
    {
        public void run()
        {
            currentTime = accumulatedTime + (System.currentTimeMillis() - startTime) / 1000;
            // Notify anyone listening for broadcasts about the new location.
            Intent intent = new Intent(ACTION_BROADCAST);
            intent.putExtra(EXTRA_DATAPACK, new DataPack(String.valueOf(roundedDistance), String.valueOf(currentTime), startDate, null, polyNodeArray));
            LocalBroadcastManager.getInstance(LocationUpdatesService.this).sendBroadcast(intent);

            // Update notification content if running as a foreground service.
            if (serviceIsRunningInForeground(LocationUpdatesService.this)) {
                mNotificationManager.notify(NOTIFICATION_ID, getNotification());
            }
        }
    }
}