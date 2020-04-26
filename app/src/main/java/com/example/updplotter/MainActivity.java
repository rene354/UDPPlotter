package com.example.updplotter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener {
    private LineChart chart;

    private static final int PERMISSION_STORAGE = 0;
    private volatile boolean running = false;
    private UDPSocket UPDReceiver;
    private LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_realtime_linechart);

        setTitle("RealtimeLineChartActivity");

        //Typeface tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");


        chart = findViewById(R.id.chart1);
        chart.setOnChartValueSelectedListener(this);

        // enable description text
        chart.getDescription().setEnabled(true);

        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);


        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);

        // set an alternative background color
        chart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        chart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        l.setEnabled(false);

        XAxis xl = chart.getXAxis();
        //xl.setTypeface(tfLight);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        //leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    private void addEntries(String[] value) {
        for(int i = 0; i<2; i++) {
            addEntry(value[i], i);
        }
    }

    private void addEntry(String value, int index){
        LineData data = chart.getData();
        Log.i("AddEntry", value);
        try {
            if (data != null) {

                ILineDataSet set = data.getDataSetByIndex(index);

                // set.addEntry(...); // can be called as well

                if (set == null) {
                    set = createSet();
                    data.addDataSet(set);
                }

                data.addEntry(new Entry(set.getEntryCount(), Float.parseFloat(value)), index);

                data.notifyDataChanged();

                // let the chart know it's data has changed
                chart.notifyDataSetChanged();

                // limit the number of visible entries
                chart.setVisibleXRangeMaximum(120);
                // chart.setVisibleYRange(30, AxisDependency.LEFT);

                // move to the latest entry
                chart.moveViewToX(data.getEntryCount());
            }

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }catch (Exception e){
            e.printStackTrace();

        }
    }

    private void addAll(){
        LineData data = chart.getData();
        try {
            if (data != null) {
                LinkedList<String> buffer = new LinkedList<String>();
                lbq.drainTo(buffer);
                for(String element : buffer) {
                    String[] values = element.split(",");
                    for(int i = 1; i< 2; i++) {

                        ILineDataSet set = data.getDataSetByIndex(i-1);

                        // set.addEntry(...); // can be called as well

                        if (set == null) {
                            set = createSet();
                            data.addDataSet(set);
                        }

                        data.addEntry(new Entry(set.getEntryCount(), Float.parseFloat(values[i])), i-1);
                    }

                    data.notifyDataChanged();

                    // let the chart know it's data has changed

                    chart.notifyDataSetChanged();

                    // limit the number of visible entries
                    chart.setVisibleXRangeMaximum(120);
                    // chart.setVisibleYRange(30, AxisDependency.LEFT);

                    // move to the latest entry
                    chart.moveViewToX(data.getEntryCount());
                }
            }

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }catch (Exception e){
            e.printStackTrace();

        }
    }


    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setFillAlpha(65);
        set.setDrawCircles(false);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    private Thread thread;

    private void listen(){

    }

    private void handleMessage(String s){
        lbq.add(s);
    }

    private void startThread(){
        running = true;

        if(UPDReceiver == null)
            UPDReceiver = new UDPSocket(8080, new CustomRunnable() {
                @Override
                public void run(String message) {
                    handleMessage(message);
                }
            });

        UPDReceiver.start();



        if (thread != null)
            thread.interrupt();

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (running) {
                        //String s = lbq.take();
                        Log.i("LBQ size", "" + lbq.size());
                        //String[] stringArray = s.split(",");

                        //addEntries(stringArray);
                        addAll();
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        Toast.makeText(this, "sucessfully started", Toast.LENGTH_SHORT).show();
    }

    private void stopThread(){
        boolean successful = true;
        if (UPDReceiver != null)
            UPDReceiver.stop();

        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            successful = false;
        }

        thread = null;
        if(successful)
            Toast.makeText(this, "sucessfully stopped", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "an error ocurred", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.realtime, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionStart:{
                startThread();
                break;
            }
            case R.id.actionStop:{
                stopThread();
                break;
            }
            case R.id.actionClear: {
                chart.clearValues();
                Toast.makeText(this, "Chart cleared!", Toast.LENGTH_SHORT).show();
                break;
            }
            case R.id.actionSave: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveToGallery();
                } else {
                    requestStoragePermission(chart);
                }
                break;
            }
        }
        return true;
    }


    private void saveToGallery() {
        String name = "RealtimeLineChartActivity";
        if (this.chart.saveToGallery(name + "_" + System.currentTimeMillis(), 70))
            Toast.makeText(getApplicationContext(), "Saving SUCCESSFUL!",
                    Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "Saving FAILED!", Toast.LENGTH_SHORT)
                    .show();
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Entry selected", e.toString());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Nothing selected", "Nothing selected.");
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
    }

    private void requestStoragePermission(View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(view, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                        }
                    }).show();
        } else {
            Toast.makeText(getApplicationContext(), "Permission Required!", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
        }
    }
}
