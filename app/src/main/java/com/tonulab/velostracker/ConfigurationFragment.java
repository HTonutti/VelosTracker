package com.tonulab.velostracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;

public class ConfigurationFragment extends Fragment {

    private ArrayList<String> modes = new ArrayList<>(Arrays.asList("Pedestrismo", "Ciclismo", "Automovilismo"));
    private String[] modesString = {"Pedestrismo", "Ciclismo", "Automovilismo"};

    private MainActivity mainActivity;

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.configuration, container, false);
        Switch swt_follow = rootView.findViewById(R.id.switch_follow);
        AppCompatImageButton btn_mode = rootView.findViewById(R.id.btn_mode);

        swt_follow.setChecked(mainActivity.getMapTracking());
        swt_follow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Utils.setTracking(getActivity().getApplicationContext(), isChecked);
//                mainActivity.setMapTracking(isChecked);
            }
        });

        btn_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Seleccione el tipo de actividad")
                    .setSingleChoiceItems(modesString, modes.indexOf(Utils.getMode(getActivity().getApplicationContext())), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            Utils.setMode(getActivity().getApplicationContext(), modes.get(which));
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    dialog.cancel();
                                }
                            }, 400);

                        }
                    })
                    .setCancelable(false);
                //Creating dialog box
                AlertDialog dialog  = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });

        return rootView;
    }
}
