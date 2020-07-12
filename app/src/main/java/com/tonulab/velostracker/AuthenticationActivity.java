package com.tonulab.velostracker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthenticationActivity extends AppCompatActivity {

    private static final String TAG = AuthenticationActivity.class.getSimpleName();
    Button btnAuth;
    private int GOOGLE_SIGN_IN = 17;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication_activity);
        btnAuth = findViewById(R.id.btn_auth);

        btnAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GoogleSignInOptions googleConf = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                       .requestIdToken(getString(R.string.default_web_client_id))
                       .requestEmail()
                       .build();

                GoogleSignInClient googleClient = GoogleSignIn.getClient(getApplicationContext(), googleConf);
                googleClient.signOut();
                startActivityForResult(googleClient.getSignInIntent(), GOOGLE_SIGN_IN);
            }
        });

        String provider = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.AUTH_PROVIDER, "");
        String userId = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.USER_ID, "");
        if (provider != ""  && userId != ""){
            findViewById(R.id.auth_layout).setVisibility(View.INVISIBLE);
            showMain(provider, userId.toString());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        findViewById(R.id.auth_layout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GOOGLE_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                final GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null){
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                showMain("GOOGLE", account.getId());
                                Log.i(TAG, "Autenticación completada");
                            }
                            else{
                                Log.i(TAG, "Error al autenticar");
                                Toast.makeText(getApplicationContext(), "Error al autenticar, intentelo nuevamente", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            } catch (ApiException e) {
                Toast.makeText(getApplicationContext(), "Error al autenticar", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.i(TAG, "Excepción al autenticar");
            }

        }
    }

    private void showMain(String provider, String userId){
        Intent intent = new Intent(this, MainActivity.class).putExtra(Utils.AUTH_PROVIDER, provider).putExtra(Utils.USER_ID, userId);
        startActivity(intent);
    }
}
