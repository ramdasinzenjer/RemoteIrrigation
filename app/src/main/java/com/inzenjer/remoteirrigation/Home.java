package com.inzenjer.remoteirrigation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cardiomood.android.controls.gauge.SpeedometerGauge;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;

public class Home extends AppCompatActivity {
    SpeedometerGauge temprature_meter;
    SpeedometerGauge humidity_meter;
    WaveSwipeRefreshLayout mWaveSwipeRefreshLayout;
    Switch toggle;
    String deviceid;
    int mortotState;
    TextView txt_humi;
    TextView txt_temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        startService();
        // Customize SpeedometerGauge
        temprature_meter = findViewById(R.id.temprature_meter);
        txt_humi = findViewById(R.id.humidity_text);
        txt_temp = findViewById(R.id.teprature_text);
        temprature_meter.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });
/////////////////////////////////////////space shared preference from backend service
        SharedPreferences share = getSharedPreferences("ip", MODE_PRIVATE);
        SharedPreferences.Editor ed = share.edit();
        int temp = Integer.parseInt(share.getString("temprature",""));
        int humi = Integer.parseInt(share.getString("humidity",""));
        humidity_meter.setSpeed(temp, 1000, 300);
        temprature_meter.setSpeed(humi, 1000, 300);
        txt_temp.setText(txt_temp.getText() + ":" + temp + "C");
        txt_humi.setText(txt_humi.getText() + ":" + humi);
////////////////////////end here
        // configure value range and ticks
        temprature_meter.setMaxSpeed(100);
        temprature_meter.setMajorTickStep(30);
        temprature_meter.setMinorTicks(2);
        temprature_meter.setSpeed(0, 1000, 300);
        temprature_meter.setLabelTextSize(25);

        // Configure value range colors
        temprature_meter.addColoredRange(20, 40, Color.GREEN);
        temprature_meter.addColoredRange(0, 20, Color.YELLOW);
        temprature_meter.addColoredRange(40, 100, Color.RED);

        //for humidity
        humidity_meter = findViewById(R.id.humidity_meter);

        humidity_meter.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        // configure value range and ticks
        humidity_meter.setMaxSpeed(12);
        humidity_meter.setMajorTickStep(2);
        humidity_meter.setMinorTicks(1);
        humidity_meter.setLabelTextSize(25);
        deviceid = "152";
        temprature_meter.setLabelTextSize(25);
        // Configure value range colors
        humidity_meter.addColoredRange(6, 12, Color.GREEN);
        humidity_meter.addColoredRange(3, 6, Color.YELLOW);
        humidity_meter.addColoredRange(0, 3, Color.RED);

        mWaveSwipeRefreshLayout = findViewById(R.id.main_swipe);

        mWaveSwipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchdata();

            }

        });
        toggle = (Switch) findViewById(R.id.motorOnButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    mortotState = 1;
                    MortarStateChange();

                } else {
                    // The toggle is disabled
                    mortotState = 0;
                    MortarStateChange();
                }
            }
        });
        callAsynchronousTask();

    }

    public void startService() {
        startService(new Intent(getBaseContext(), datarefresh.class));
    }

    public void fetchdata() {

        RequestQueue queue = Volley.newRequestQueue(Home.this);
        String response = "";
        final String finalResponse = response;
        //TODO url
        String S_URL = "http://192.168.1.13/ip/ip.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, S_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        try {
                            JSONObject object0 = new JSONObject(response);
                            JSONObject jobject1 = object0.getJSONObject("Event");
                            JSONArray ja = jobject1.getJSONArray("Details");
                            JSONObject data1 = ja.getJSONObject(0);
                            int humidity = Integer.parseInt(data1.getString("humidity"));
                            int temprature = Integer.parseInt(data1.getString("temprature"));
                            int mortorstatus = Integer.parseInt(data1.getString("mortorstatus"));
                            txt_temp.setText(txt_temp.getText() + ":" + temprature + "C");
                            txt_humi.setText(txt_humi.getText() + ":" + humidity);
                            humidity_meter.setSpeed(humidity, 1000, 300);
                            temprature_meter.setSpeed(temprature, 1000, 300);
                            mWaveSwipeRefreshLayout.setRefreshing(false);
                            if (mortorstatus == 0) {
                                toggle.setChecked(false);
                            } else {
                                toggle.setChecked(true);
                            }

                        } catch (JSONException e) {
                            mWaveSwipeRefreshLayout.setRefreshing(false);
                            e.printStackTrace();
                            Toast.makeText(Home.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(Home.this, error.toString(), Toast.LENGTH_SHORT).show();


                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("deviceid", deviceid);

                return params;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);

    }

    public void MortarStateChange() {
        int state;
        RequestQueue queue = Volley.newRequestQueue(Home.this);
        String response = "";
        final String finalResponse = response;
        //TODO url
        String S_URL = "http://192.168.1.13/ip/ip1.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, S_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(Home.this, error.toString(), Toast.LENGTH_SHORT).show();


                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("deviceid", deviceid);
                params.put("mortorstate", String.valueOf(mortotState));

                return params;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);

    }

    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            Background b = new Background();
                            b.execute();
                        } catch (Exception e) {

                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 1000);


    }

    private class Background extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {


            RequestQueue queue = Volley.newRequestQueue(Home.this);
            String response = "";
            final String finalResponse = response;
            //TODO url
            String S_URL = "http://192.168.1.13/ip/ip.php";
            StringRequest postRequest = new StringRequest(Request.Method.POST, S_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            try {
                                JSONObject object0 = new JSONObject(response);
                                JSONObject jobject1 = object0.getJSONObject("Event");
                                JSONArray ja = jobject1.getJSONArray("Details");
                                JSONObject data1 = ja.getJSONObject(0);
                                int humidity = Integer.parseInt(data1.getString("humidity"));
                                int temprature = Integer.parseInt(data1.getString("temprature"));
                                int mortorstatus = Integer.parseInt(data1.getString("mortorstatus"));
                                txt_temp.setText(txt_temp.getText() + ":" + temprature + "C");
                                txt_humi.setText(txt_humi.getText() + ":" + humidity);
                                humidity_meter.setSpeed(humidity, 1000, 300);
                                temprature_meter.setSpeed(temprature, 1000, 300);
                                if (mortorstatus == 0) {
                                    toggle.setChecked(false);
                                } else {
                                    toggle.setChecked(true);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(Home.this, e.toString(), Toast.LENGTH_SHORT).show();
                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(Home.this, error.toString(), Toast.LENGTH_SHORT).show();


                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("deviceid", deviceid);

                    return params;
                }
            };
            postRequest.setRetryPolicy(new DefaultRetryPolicy(0, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(postRequest);
            return null;
        }
    }
}
