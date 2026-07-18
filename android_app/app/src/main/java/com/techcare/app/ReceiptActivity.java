package com.techcare.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReceiptActivity extends AppCompatActivity {

    private TextView tvTechName, tvTechSpecialties, tvBaseRate, tvTotal;
    private LinearLayout layoutItems;
    private Button btnClose;
    private String cid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);

        tvTechName = findViewById(R.id.tv_receipt_tech_name);
        tvTechSpecialties = findViewById(R.id.tv_receipt_tech_specialties);
        tvBaseRate = findViewById(R.id.tv_receipt_base_rate);
        tvTotal = findViewById(R.id.tv_receipt_total);
        layoutItems = findViewById(R.id.layout_receipt_items);
        btnClose = findViewById(R.id.btn_receipt_close);

        cid = getIntent().getStringExtra("cid");

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadReceiptData();
    }

    private void loadReceiptData() {
        String url = AppConfig.BASE_URL + "/api/view_receipt?cid=" + cid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.getString("status").equals("success")) {
                                tvTechName.setText("Technician: " + json.getString("tech_name"));
                                tvTechSpecialties.setText("Specialties: " + json.getString("specialties"));
                                tvBaseRate.setText("₹" + json.getString("starting_rate"));
                                tvTotal.setText("₹" + json.getString("total_amount"));

                                JSONArray parts = json.getJSONArray("parts");
                                // Inject parts dynamically
                                for (int i = 0; i < parts.length(); i++) {
                                    JSONObject part = parts.getJSONObject(i);
                                    addPartRow(part.getString("product_name"), part.getString("amount"));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(ReceiptActivity.this, "Error parsing receipt details", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ReceiptActivity.this, "Failed to load receipt details", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void addPartRow(String name, String price) {
        RelativeLayout row = new RelativeLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.setMargins(0, 0, 0, 24); // 24px margins bottom
        row.setLayoutParams(lp);

        TextView tvName = new TextView(this);
        tvName.setText("🔧 " + name + " (Genuine)");
        tvName.setTextSize(14sp);
        tvName.setTextColor(getResources().getColor(R.color.text_color));

        TextView tvPrice = new TextView(this);
        tvPrice.setText("₹" + price);
        tvPrice.setTextSize(14sp);
        tvPrice.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvPrice.setTextColor(getResources().getColor(R.color.text_color));

        RelativeLayout.LayoutParams paramsPrice = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        paramsPrice.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        tvPrice.setLayoutParams(paramsPrice);

        row.addView(tvName);
        row.addView(tvPrice);

        layoutItems.addView(row);
    }

    // Convert sp unit
    private float sp(int value) {
        return value;
    }
}
