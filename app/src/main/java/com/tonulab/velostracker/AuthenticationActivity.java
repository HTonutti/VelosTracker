package com.tonulab.velostracker;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthenticationActivity extends AppCompatActivity {

    private static final String TAG = AuthenticationActivity.class.getSimpleName();
    Button btnGoogle;
    Button btnRegister;
    Button btnAccess;
    EditText mail;
    EditText pass;
    private int GOOGLE_SIGN_IN = 17;
    private boolean firstEntry = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authentication_activity);
        btnGoogle = findViewById(R.id.btn_auth_google);
        btnRegister = findViewById(R.id.btn_auth_reg);
        btnAccess = findViewById(R.id.btn_auth_acc);
        mail = findViewById(R.id.txt_mail);
        pass = findViewById(R.id.txt_pass);

        setListeners();
    }

    private void setListeners(){

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mail.getText().toString().equals("") && !pass.getText().toString().equals("")){
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(mail.getText().toString(), pass.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    signComplete(task, "MAIL", FirebaseAuth.getInstance().getUid());
                                }
                            });
                }
            }
        });

        btnAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mail.getText().toString().equals("") && !pass.getText().toString().equals("")){
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(mail.getText().toString(), pass.getText().toString())
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    signComplete(task, "MAIL", FirebaseAuth.getInstance().getUid());
                                }
                            });
                }
            }
        });

        btnGoogle.setOnClickListener(new View.OnClickListener() {
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        mail.setText("");
        pass.setText("");
        String provider = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.AUTH_PROVIDER, "");
        String userId = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Utils.USER_ID, "");
        if (!provider.equals("") && !userId.equals("")){
            findViewById(R.id.auth_layout).setVisibility(View.INVISIBLE);
            if (firstEntry){
                showMain(provider, userId);
                firstEntry = false;
            }else
                this.finishAffinity();
        }
        else{
            findViewById(R.id.auth_layout).setVisibility(View.VISIBLE);
        }

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
                            signComplete(task, "GOOGLE", FirebaseAuth.getInstance().getUid());
                        }
                    });
                }
            }
            catch (ApiException e){
                Toast.makeText(getApplicationContext(), "Error al autenticar", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                Log.i(TAG, "Excepción: error al autenticar en cuenta de google");
            }
        }
    }

    private void signComplete(Task<AuthResult> task, String provider, String userId){
        if (task.isSuccessful()){
            showMain(provider, userId);
            Log.i(TAG, "Autenticación completada");
        }
        else{
            Exception e = task.getException();
            if (e instanceof FirebaseAuthUserCollisionException){
                Toast.makeText(getApplicationContext(), "El correo ya se encuentra registrado", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Excepción: correo ya registrado");
            }
            else if(e instanceof FirebaseAuthInvalidUserException){
                Toast.makeText(getApplicationContext(), "El correo no se encuentra registrado", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Excepción: correo no registrado");
            }
            else if (e instanceof FirebaseAuthInvalidCredentialsException){
                Toast.makeText(getApplicationContext(), "La contraseña es incorrecta o el correo está mal ingresado", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Excepción: contraseña incorrecta o correo mal ingresado");
            }
            else{
                Toast.makeText(getApplicationContext(), "Error al autenticar", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Excepción: error al autenticar con cuenta de correo");
            }
        }
    }

    private void showMain(String provider, String userId){
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(Utils.AUTH_PROVIDER, provider)
                .putExtra(Utils.USER_ID, userId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
