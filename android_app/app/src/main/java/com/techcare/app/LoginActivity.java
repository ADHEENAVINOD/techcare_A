package com.techcare.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);

        // Check if user is already logged in
        if (sharedPreferences.contains("lid")) {
            redirectUser(sharedPreferences.getString("type", ""));
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void performLogin() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_LOGIN,
                new Response.Listener<String>() {
                    @Override
                    public void onClick(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                String lid = jsonObject.getString("lid");
                                String type = jsonObject.getString("type");
                                String name = jsonObject.getString("name");
                                
                                // Save session
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("lid", lid);
                                editor.putString("username", username);
                                editor.putString("type", type);
                                editor.putString("name", name);
                                editor.apply();

                                Toast.makeText(LoginActivity.this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
                                redirectUser(type);
                            } else {
                                Toast.makeText(LoginActivity.this, jsonObject.optString("message", "Login failed"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("LoginError", e.getMessage());
                            Toast.makeText(LoginActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errMsg = "Connection Error. Make sure Flask server is running and AppConfig IP is correct.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                JSONObject obj = new JSONObject(new String(error.networkResponse.data));
                                errMsg = obj.optString("message", errMsg);
                            } catch (Exception e) {}
                        }
                        Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void redirectUser(String type) {
        if (type.equalsIgnoreCase("user")) {
            startActivity(new Intent(LoginActivity.this, UserHomeActivity.class));
            finish();
        } else if (type.equalsIgnoreCase("technician")) {
            startActivity(new Intent(LoginActivity.this, TechHomeActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Admin logins are only available on the Web Console", Toast.LENGTH_LONG).show();
            // Clear preferences since admin cannot log in on Android
            sharedPreferences.edit().clear().apply();
        }
    }
}
