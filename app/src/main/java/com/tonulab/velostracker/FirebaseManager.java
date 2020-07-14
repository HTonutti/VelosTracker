package com.tonulab.velostracker;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedHashMap;

public final class FirebaseManager {

    private static HistoricFragment historicFragment;
    private static DatabaseReference mDatabase;
    private static LinkedHashMap<String, DataPack> registers = new LinkedHashMap<>();
    private static String TAG = FirebaseManager.class.getSimpleName();
    private static long nroReg = 0;
    private static int contRegRead = 0;
    private static String userID = null;

    public static void setHistoricFragment(HistoricFragment historicFragment){
        FirebaseManager.historicFragment = historicFragment;
    }

    public static void setUserID(String userID){
        FirebaseManager.userID = userID;
        readFromFirebase();
    }

    public FirebaseManager(){
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public static void writeOnFirebase(DataPack reg){
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

    public static void readFromFirebase() {
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

    public static void removeFromFirebase(String key){
        mDatabase.child(userID).child("Recorridos").child(key).removeValue();
        Log.i(TAG, "Registro eliminado");
    }

    private static void removeAllFromFirebase(){
        String[] arrayKeys = registers.keySet().toArray(new String[0]);
        for (int i = 0; i < registers.size(); i++) {
            mDatabase.child(userID).child("Recorridos").child(arrayKeys[i]).removeValue();
        }
        Log.i(TAG, "Registros eliminados completamente");
    }

    private static boolean checkUserId(){
        return userID == null || userID.equals("");
    }

}
