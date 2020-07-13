package com.tonulab.velostracker;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.Context;

import java.util.LinkedHashMap;

public class FirebaseManager {

    private HistoricFragment historicFragment;
    private DatabaseReference mDatabase;
    private LinkedHashMap<String, DataPack> registers = new LinkedHashMap<>();
    private static final String TAG = FirebaseManager.class.getSimpleName();
    private long nroReg = 0;
    private int contRegRead = 0;
    private String userID = null;

    public void setHistoricFragment(HistoricFragment historicFragment){
        this.historicFragment = historicFragment;
    }

    public void setUserID(String userID){
        this.userID = userID;
        readFromFirebase();
    }

    public FirebaseManager(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
        readFromFirebase();
    }

    public void writeOnFirebase(DataPack reg){
        if (!checkUserId()){
            mDatabase.child(userID).child("Recorridos").push().setValue(reg, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null)
                        Log.e(TAG, "Error al guardar en la base de datos", error.toException());
                    else
                        Log.i(TAG, "Registro guardado satisfactoriamente");
                }
            });
            historicFragment.setRegisters(registers);
        }
        else
            Log.e(TAG, "Problema con el id de usuario al escribir en la base de datos");
    }

    public void readFromFirebase() {
        if (!checkUserId()) {
            mDatabase.child(userID).child("Recorridos").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    registers = new LinkedHashMap<>();
                    contRegRead = 0;
                    nroReg = snapshot.getChildrenCount();
                    if (nroReg == 0)
                        historicFragment.setRegisters(registers);
                    Log.i(TAG, nroReg + " registros leídos");

                    for (final DataSnapshot auxSnapshot : snapshot.getChildren()) {
                        mDatabase.child(userID).child("Recorridos").child(auxSnapshot.getKey()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                DataPack data = auxSnapshot.getValue(DataPack.class);
                                String key = snapshot.getKey();
                                contRegRead += 1;
                                registers.put(key, data);
                                if (contRegRead == nroReg)
                                    historicFragment.setRegisters(registers);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else
            Log.e(TAG, "Problema con el id de usuario al leer en la base de datos");
    }

    public void removeFromFirebase(String key){
        mDatabase.child(userID).child("Recorridos").child(key).removeValue();
        Log.i(TAG, "Registro eliminado");
    }

    private void removeAllFromFirebase(){
        String[] arrayKeys = registers.keySet().toArray(new String[0]);
        for (int i = 0; i < registers.size(); i++) {
            mDatabase.child(userID).child("Recorridos").child(arrayKeys[i]).removeValue();
        }
        Log.i(TAG, "Registros eliminados completamente");
    }

    private boolean checkUserId(){
        if (userID == "" || userID == null)
            return true;
        return false;
    }

}
