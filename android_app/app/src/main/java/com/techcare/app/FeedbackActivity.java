package com.techcare.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private TextView tvTechInfo;
    private EditText etFeedbackText;
    private Button btnSubmit;
    private String techLid, techName;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        tvTechInfo = findViewById(R.id.tv_feedback_tech_info);
        etFeedbackText = findViewById(R.id.et_feedback_text);
        btnSubmit = findViewById(R.id.btn_feedback_submit);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);

        techLid = getIntent().getStringExtra("tech_lid");
        techName = getIntent().getStringExtra("tech_name");

        tvTechInfo.setText("Service Agent: " + techName);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitFeedback();
            }
        });
    }

    private void submitFeedback() {
        final String feedbackText = etFeedbackText.getText().toString().trim();
        final String userLid = sharedPreferences.getString("lid", "");

        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "Please enter your review text", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_SEND_FEEDBACK,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(FeedbackActivity.this, "Feedback submitted successfully. Thank you!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(FeedbackActivity.this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_lid", userLid);
                params.put("tech_lid", techLid);
                params.put("feedback", feedbackText);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }
}
