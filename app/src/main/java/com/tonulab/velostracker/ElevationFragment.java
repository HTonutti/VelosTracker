package com.tonulab.velostracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class ElevationFragment extends Fragment {

    GraphView graphView;
    private MainActivity mainActivity;
    private LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

    public void setMainActivity(MainActivity mainActivity){this.mainActivity = mainActivity;}

    public ElevationFragment(){
        series.setColor(Color.BLACK);
        series.setBackgroundColor(Color.YELLOW);
        series.setThickness(7);
        series.setDrawBackground(true);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.elevation, container, false);
        graphView = rootView.findViewById(R.id.graph_elevation);
        graphView.onDataChanged(true,true);
        graphView.addSeries(series);
        graphView.setBackgroundColor(Color.TRANSPARENT);
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Distancia [KM]");
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Altitud [MTS]");
        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        return rootView;
    }

    public void addPoint(Double distance, Double altitude){
        series.appendData(new DataPoint(distance, altitude), true, Integer.MAX_VALUE);
    }

    public void setPoints(ArrayList<PolyNode> polyNodes){
        resetPoints();
        for (int i = 0; i < polyNodes.size(); i++) {
            series.appendData(new DataPoint(polyNodes.get(i).getDistance(), polyNodes.get(i).getAltitude()), true, Integer.MAX_VALUE);
        }
    }

    public void resetPoints(){
        series.resetData(new DataPoint[0]);
    }

}
