package com.techcare.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import java.util.HashMap;
import java.util.Map;

public class RatingActivity extends AppCompatActivity {

    private TextView tvCompanyInfo;
    private RatingBar ratingBar;
    private Button btnSubmit;
    private String companyLid, companyName;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        tvCompanyInfo = findViewById(R.id.tv_rating_company_info);
        ratingBar = findViewById(R.id.rating_bar);
        btnSubmit = findViewById(R.id.btn_rating_submit);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);

        companyLid = getIntent().getStringExtra("company_lid");
        companyName = getIntent().getStringExtra("company_name");

        tvCompanyInfo.setText("Company: " + companyName);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRating();
            }
        });
    }

    private void submitRating() {
        final float rating = ratingBar.getRating();
        final String userLid = sharedPreferences.getString("lid", "");

        if (rating < 1.0f) {
            Toast.makeText(this, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_GIVE_RATING,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(RatingActivity.this, "Rating submitted successfully. Thank you!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(RatingActivity.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_lid", userLid);
                params.put("service_lid", companyLid);
                params.put("rating", String.valueOf((int) rating));
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }
}
