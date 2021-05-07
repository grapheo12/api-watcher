package com.example.apiwatcher;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    RadioGroup requestMethod;
    EditText url;
    EditText body;

    EditText editHeaders;
    HashMap<String, String> headers;

    EditText duration;
    Button watch;
    TextView response;

    Context context;

    RequestQueue queue;

    boolean isWatching = false;
    final String filename = "response";
    final int MAX_RESPONSE_SIZE = 10000;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestMethod = (RadioGroup)findViewById(R.id.method);
        url = (EditText)findViewById(R.id.editUrl);
        body = (EditText)findViewById(R.id.editBody);
        editHeaders = (EditText)findViewById(R.id.editHeaders);
        duration = (EditText)findViewById(R.id.editInterval);
        watch = (Button)findViewById(R.id.button);
        response = (TextView) findViewById(R.id.response);

        context = getApplicationContext();
        queue = Volley.newRequestQueue(context);

        try {
            FileInputStream fin = context.openFileInput(filename);
            isWatching = true;
            watch.setBackgroundTintList(context.getResources().getColorStateList(R.color.design_default_color_error));
            watch.setText("Stop Watching");
            byte content[] = new byte[MAX_RESPONSE_SIZE];
            fin.read(content);
            System.out.println(new String(content));
            response.setText(new String(content));
            fin.close();
        }catch (Exception e){
            // Nothing to do
        }

        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWatching){
                    isWatching = false;
                    watch.setBackgroundTintList(context.getResources().getColorStateList(R.color.teal_700));
                    watch.setText("Watch");
                    stopWatch();
                }else{
                    isWatching = true;
                    watch.setBackgroundTintList(context.getResources().getColorStateList(R.color.design_default_color_error));
                    watch.setText("Stop Watching");
                    startWatch();
                }
            }
        });

    }

    public void startWatch(){
        try {
            String Url = url.getText().toString();
            String Headers = editHeaders.getText().toString();
            headers = new HashMap<>();
            for (String s: Headers.split("\n")){
                String kv[] = s.split(":");
                if (kv.length != 2)
                    continue;
                headers.put(kv[0].trim(), kv[1].trim());
            }
            String Body = body.getText().toString();
            int btnId = requestMethod.getCheckedRadioButtonId();
            int method = Request.Method.GET;
            if (btnId == R.id.getBtn){
                method = Request.Method.GET;
            }else if (btnId == R.id.postBtn) {
                method = Request.Method.POST;
            }
            int finalMethod = method;
            StringRequest req = new StringRequest(method, Url, new Response.Listener<String>() {
                @Override
                public void onResponse(String respo) {
                    try {
                        FileOutputStream fout = context.openFileOutput(filename, MODE_PRIVATE);
                        fout.write(respo.getBytes());
                        fout.close();

                        Intent intent = new Intent(context, MainWatcher.class);
                        intent.putExtra("url", Url);
                        intent.putExtra("headers", headers);
                        intent.putExtra("body", Body);
                        intent.putExtra("duration", Integer.parseInt(duration.getText().toString()));
                        intent.putExtra("method", finalMethod);
                        ContextCompat.startForegroundService(context, intent);

                    } catch (Exception e) {
                        Toast.makeText(context, "Error: " + e.toString(), Toast.LENGTH_LONG).show();
                    }

                    response.setText(respo);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(context, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    return headers;
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    return Body.getBytes();
                }
            };

            req.setShouldCache(false);
            queue.add(req);

        }catch (Exception e){
            Toast.makeText(context, e.getStackTrace()[0].getLineNumber() + "  " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void stopWatch(){
        deleteFile(filename);

        Intent intent = new Intent(context, MainWatcher.class);
        stopService(intent);
    }


}