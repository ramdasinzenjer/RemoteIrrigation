package com.inzenjer.remoteirrigation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class datarefresh extends Service {
    public datarefresh() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callAsynchronousTask();
        Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();
        return Service.START_STICKY;
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
                            PerformBackgroundTask performBackgroundTask = new PerformBackgroundTask();
                            // PerformBackgroundTask this class is the class that extends AsynchTask
                            performBackgroundTask.execute();
                        } catch (Exception e) {
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 30000);

    }

    class PerformBackgroundTask extends AsyncTask<String, String, String> {
        String deviceid = "152";

        @Override
        protected String doInBackground(String... strings) {
            RequestQueue queue = Volley.newRequestQueue(datarefresh.this);
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
                                SharedPreferences share = getSharedPreferences("ip", MODE_PRIVATE);
                                SharedPreferences.Editor ed = share.edit();
                                ed.putString("humidity", String.valueOf(humidity));
                                ed.putString("temprature", String.valueOf(temprature));
                                ed.putString("mortorStatus", String.valueOf(mortorstatus));
                                if (temprature > 40) {
                                    Notification n = new Notification.Builder(datarefresh.this)
                                            .setContentTitle("Temprature too High turn on the mortor")
                                            .setContentText("Temprature Hing Alert")
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setAutoCancel(true).build();
                                    NotificationManager notificationManager =
                                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                    notificationManager.notify(0, n);
                                }
                                if(humidity<3)
                                {
                                    Notification n = new Notification.Builder(datarefresh.this)
                                            .setContentTitle("Hummidity too low turn on the mortor")
                                            .setContentText("Low Humidity Alert")
                                            .setSmallIcon(R.mipmap.ic_launcher)
                                            .setAutoCancel(true).build();
                                    NotificationManager notificationManager =
                                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                    notificationManager.notify(0, n);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(datarefresh.this, e.toString(), Toast.LENGTH_SHORT).show();

                            }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                            Toast.makeText(datarefresh.this, error.toString(), Toast.LENGTH_SHORT).show();
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
