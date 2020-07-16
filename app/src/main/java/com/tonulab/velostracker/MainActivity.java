package com.tonulab.velostracker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Stack;


public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private Receiver receiver;
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;

    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (!toShow)
                mService.requestSendUpdate();
            if (Utils.getUpdateState(getApplicationContext())) {
                tracing.setValue(true);
            } else {
                tracing.setValue(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private MapsFragment mapsFragment = null;
    private HistoricFragment historicFragment = null;
    private ConfigurationFragment configurationFragment = null;
    private ElevationFragment elevationFragment = null;
    private FragmentManager fm = getSupportFragmentManager();

    private TextView txtDistance;
    private TextView txtTime;
    private TextView txtAvg;
    private FloatingActionButton bPlay;
    private BottomNavigationView navView;

    private ActiveVariable tracing;
    private boolean mapTracking = false;
    private boolean toShow = false;
    private String distance = "0";
    private long time = 0;
    private BigDecimal avg = BigDecimal.valueOf(0);
    private String userId = "";
    private String provider = "";

    private ArrayList<PolyNode> polyNodeArray = null;
    private Stack<Integer> stackMenu = new Stack<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        setUserProvider();
        inicialization();
        setListeners();

        if (!checkPermissions()) {
            requestPermissions();
            navView.setSelectedItemId(R.id.menu_setting);
        }
        else {
            navView.setSelectedItemId(R.id.menu_map);
        }
    }

    private void setUserProvider() {
        if (getIntent().hasExtra(Utils.AUTH_PROVIDER) && getIntent().hasExtra(Utils.USER_ID)){
            provider = getIntent().getExtras().getString(Utils.AUTH_PROVIDER);
            userId = getIntent().getExtras().getString(Utils.USER_ID);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(Utils.AUTH_PROVIDER, provider)
                    .putString(Utils.USER_ID, userId)
                    .apply();
        }else{
            provider = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Utils.AUTH_PROVIDER, "");
            userId = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Utils.USER_ID, "");
        }
    }

    private void inicialization() {
        mapsFragment = new MapsFragment();

        configurationFragment = new ConfigurationFragment();
        configurationFragment.setMainActivity(this);

        elevationFragment = new ElevationFragment();
        elevationFragment.setMainActivity(this);

        receiver = new Receiver();
        receiver.setMainActivity(this);

        historicFragment = new HistoricFragment();
        historicFragment.setMainActivity(this);

        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        firebaseManager.setUserID(userId);
        firebaseManager.setHistoricFragment(historicFragment);

        historicFragment.setFirebaseManager(firebaseManager);

        tracing = new ActiveVariable();
        txtDistance = findViewById(R.id.txt_dist);
        txtTime = findViewById(R.id.txt_time);
        txtAvg = findViewById(R.id.txt_avg);
        bPlay = findViewById(R.id.b_play);
        navView = findViewById(R.id.bottom_navigation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(getApplicationContext(), LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        updateTextViews();
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        boolean frag = fm.popBackStackImmediate();
        if (frag) {
            stackMenu.pop();
            if (stackMenu.size() == 0)
                shutdown(false);
            else
                navView.setSelectedItemId(stackMenu.peek());
        }
    }

    private void setListeners(){

        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                menuItem.setChecked(true);
                switch (id){
                    case R.id.menu_map: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof MapsFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, mapsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_historic: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof HistoricFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, historicFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_elev: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof ElevationFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, elevationFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                    case R.id.menu_setting: {
                        if (!(fm.findFragmentById(R.id.fragment_container) instanceof ConfigurationFragment)) {
                            stackMenu.removeElement(id);
                            stackMenu.push(id);
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, configurationFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                        break;
                    }
                }
                return false;
            }
        });

        tracing.setListener(new ActiveVariable.ChangeListener() {
            @Override
            public void onChange() {
                if (tracing.getValue()){
                    setButtonImage(false);
                    if (!checkPermissions()) {
                        requestPermissions();
                    } else if(mBound){
                        if (!Utils.getUpdateState(getApplicationContext())){
                            stopService(new Intent(MainActivity.this, LocationUpdatesService.class));
                            toShow = false;
                            distance = "0";
                            time = 0;
                            avg = BigDecimal.valueOf(0);
                            elevationFragment.resetPoints();
                            mService.requestLocationUpdates();
                        }
                    }
                }
                else if(mBound){
                    setButtonImage(true);
                    if (Utils.getUpdateState(getApplicationContext())){
                        mService.removeLocationUpdates();
                    }
                }
            }
        });

        bPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tracing.setValue(!tracing.getValue());
            }
        });
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Mostrar justificación de permisos para proporcionar contexto adicional.");
            Snackbar.make(
                    findViewById(R.id.main_activity),
                    "Permisos",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Pidiendo permisos");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "El usuario ha cancelado la interacción");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                tracing.setValue(Boolean.FALSE);
                Snackbar.make(
                        findViewById(R.id.main_activity),
                        "Permisos denegados",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("Opciones", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Update the buttons state depending on whether location updates are being requested.
        if (key.equals(Utils.UPDATE_STATE)) {
            tracing.setValue(sharedPreferences.getBoolean(Utils.UPDATE_STATE,false));
        }
        else if (key.equals(Utils.TRACKING)){
            setMapTracking(sharedPreferences.getBoolean(Utils.TRACKING, false));
        }
    }

    private void setButtonImage(boolean state){
        if (state)
            bPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_play, getTheme()));
        else
            bPlay.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop, getTheme()));
    }

    public void setMapTracking(boolean mapTracking){
        this.mapTracking = mapTracking;
    }

    public boolean getTracing(){
        return tracing.getValue();
    }

    public void showRegister(DataPack dataPack){
        toShow = true;
        elevationFragment.resetPoints();
        mapsFragment.setStartLocation(false);
        navView.setSelectedItemId(R.id.menu_map);
        tracing.setValue(false);
        updateDataPack(dataPack);
        if (dataPack.getPolyline() != null) {
            Location auxLocation = new Location("");
            auxLocation.setLatitude(polyNodeArray.get(0).getLatitude());
            auxLocation.setLongitude(polyNodeArray.get(0).getLongitude());
            if (mapTracking)
                updateLocation(auxLocation);
            else{
                mapTracking = true;
                updateLocation(auxLocation);
                mapTracking = false;
            }
        }
    }

    public void updateLocation(Location loc){
        mapsFragment.setLat(loc.getLatitude());
        mapsFragment.setLon(loc.getLongitude());
        if (mapTracking)
            mapsFragment.moveCamera(16);
    }

    public void updateTime(long time){
        this.time = time;
    }

    public void updateDistance(String distance){
        this.distance = distance;
    }

    public void updatePolyline(ArrayList<PolyNode> polyNodeArray){
        this.polyNodeArray = polyNodeArray;
        mapsFragment.updatePolyline(this.polyNodeArray);
    }

    public void updateDataPack(DataPack dataPack){
        updateTime(Long.parseLong(dataPack.getTime()));
        updateDistance(dataPack.getDistance());
        if (dataPack.getPolyline() != null){
            if (dataPack.getPolyline().size() > 0){
                updatePolyline(dataPack.getPolyline());
                elevationFragment.setPoints(polyNodeArray);
            }
        }

        updateAverage();
        updateTextViews();

    }

    private void updateAverage(){
        try{
            if (time != 0 && !BigDecimal.valueOf(Float.parseFloat(distance)).equals(BigDecimal.valueOf(0)))
                avg = BigDecimal.valueOf(Float.parseFloat(distance)).multiply(BigDecimal.valueOf(3600)).divide(BigDecimal.valueOf(time), 1, RoundingMode.HALF_DOWN );
        }catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
        }catch (ArithmeticException ae) {
            System.out.println("ArithmeticException: " + ae.getMessage());
        }
    }

    private void updateTextViews(){
        txtTime.setText(DateUtils.formatElapsedTime(time));
        txtDistance.setText(String.format("%s km", distance));
        txtAvg.setText(String.format("%s km/h", avg));
    }

    public void shutdown(boolean logout){
//        mService.reset();
//        mapTracking = false;
//        toShow = false;
//        userId = "";
//        provider = "";
//        stackMenu = new Stack<>();
//        fm = getSupportFragmentManager();
        if (logout){
            FirebaseAuth.getInstance().signOut();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(Utils.AUTH_PROVIDER, "")
                    .putString(Utils.USER_ID, "")
                    .apply();
            this.finishAffinity();
            startActivity(new Intent(this, AuthenticationActivity.class));
        }else
            this.finishAffinity();
    }
}
