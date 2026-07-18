package com.techcare.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstimateActivity extends AppCompatActivity {

    private ListView lvUserComplaints;
    private List<JSONObject> complaintsList;
    private ComplaintsAdapter adapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estimate);

        lvUserComplaints = findViewById(R.id.lv_user_complaints);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);

        complaintsList = new ArrayList<>();
        adapter = new ComplaintsAdapter(this, complaintsList);
        lvUserComplaints.setAdapter(adapter);

        loadComplaints();
    }

    private void loadComplaints() {
        String userLid = sharedPreferences.getString("lid", "");
        String url = AppConfig.API_VIEW_COMPLAINTS + "?user_lid=" + userLid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                complaintsList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    complaintsList.add(data.getJSONObject(i));
                                }
                                adapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(EstimateActivity.this, "Failed to load requests", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void respondToEstimate(final String cid, final String status) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_APPROVE_ESTIMATE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(EstimateActivity.this, "Estimate " + status.toLowerCase() + " successfully!", Toast.LENGTH_SHORT).show();
                        loadComplaints(); // Reload list
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(EstimateActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                params.put("status", status);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    // Custom Adapter for Complaints & Estimates
    private class ComplaintsAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public ComplaintsAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_user_complaint, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_user_complaint, parent, false);
            }

            final JSONObject item = list.get(position);

            TextView tvProduct = convertView.findViewById(R.id.tv_item_comp_product);
            TextView tvDetails = convertView.findViewById(R.id.tv_item_comp_details);
            TextView tvDesc = convertView.findViewById(R.id.tv_item_comp_description);
            TextView tvEstimate = convertView.findViewById(R.id.tv_item_comp_estimate);
            
            LinearLayout layoutActions = convertView.findViewById(R.id.layout_item_comp_actions);
            Button btnApprove = convertView.findViewById(R.id.btn_item_comp_approve);
            Button btnReject = convertView.findViewById(R.id.btn_item_comp_reject);

            LinearLayout layoutAssignment = convertView.findViewById(R.id.layout_item_comp_assignment);
            TextView tvTech = convertView.findViewById(R.id.tv_item_comp_tech);
            Button btnChat = convertView.findViewById(R.id.btn_item_comp_chat);
            Button btnFeedback = convertView.findViewById(R.id.btn_item_comp_feedback);
            Button btnRate = convertView.findViewById(R.id.btn_item_comp_rate);
            Button btnReceipt = convertView.findViewById(R.id.btn_item_comp_receipt);
            Button btnOffers = convertView.findViewById(R.id.btn_item_comp_offers);

            try {
                final String cid = item.getString("cid");
                final String companyLid = item.optString("com_lid", ""); // Company login ID (or wait, get it from list)
                String product = item.getString("product_name");
                String date = item.getString("date");
                final String status = item.getString("status");
                String complaint = item.getString("complaint");
                String estimate = item.getString("estimate_amount");
                
                final String techLid = item.optString("tid", ""); // Wait, get assigned technician login ID if present
                final String techName = item.optString("agent_name", "");

                tvProduct.setText(product);
                tvDetails.setText("Date: " + date + " | Status: " + status);
                tvDesc.setText(complaint);

                if (estimate.equalsIgnoreCase("Pending")) {
                    tvEstimate.setText("Pending");
                    tvEstimate.setTextColor(context.getResources().getColor(R.color.text_muted));
                } else {
                    tvEstimate.setText("₹" + estimate);
                    tvEstimate.setTextColor(context.getResources().getColor(R.color.success));
                }

                // Show estimate action buttons if status is 'Estimated'
                if (status.equalsIgnoreCase("Estimated")) {
                    layoutActions.setVisibility(View.VISIBLE);
                    layoutAssignment.setVisibility(View.GONE);
                } else {
                    layoutActions.setVisibility(View.GONE);
                }

                // Show technician and rating buttons if technician is assigned
                if (status.equalsIgnoreCase("Assigned") || status.equalsIgnoreCase("In Progress") || status.equalsIgnoreCase("Completed")) {
                    layoutAssignment.setVisibility(View.VISIBLE);
                    tvTech.setText("Service Agent: " + (techName.isEmpty() ? "Assigned" : techName));
                } else {
                    layoutAssignment.setVisibility(View.GONE);
                }

                btnApprove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        respondToEstimate(cid, "Approved");
                    }
                });

                btnReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        respondToEstimate(cid, "Rejected");
                    }
                });

                btnChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Open Chat with Technician (using tech_lid)
                        Intent intent = new Intent(context, ChatActivity.class);
                        try {
                            intent.putExtra("recipient_id", item.optString("tid", "")); 
                            intent.putExtra("recipient_name", techName);
                            intent.putExtra("cid", cid);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Tech info missing", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                btnFeedback.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, FeedbackActivity.class);
                        try {
                            intent.putExtra("tech_lid", item.getString("tid"));
                            intent.putExtra("tech_name", techName);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Technician details missing", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                btnRate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, RatingActivity.class);
                        try {
                            // Find the product's company login ID
                            // In api_view_complaints, we join company com, let's retrieve com_lid.
                            // We can fetch it as 'com_lid' or 'company_id' from backend. In app.py, we join product p, so p.com_lid is available as com_lid in complaint JSON!
                            intent.putExtra("company_lid", item.getString("com_lid"));
                            intent.putExtra("company_name", item.getString("company_name"));
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Company details missing", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // Display receipt button only when the job is completed
                if (status.equalsIgnoreCase("Completed")) {
                    btnReceipt.setVisibility(View.VISIBLE);
                } else {
                    btnReceipt.setVisibility(View.GONE);
                }

                btnReceipt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ReceiptActivity.class);
                        intent.putExtra("cid", cid);
                        context.startActivity(intent);
                    }
                });

                // Display Offers/Bids button if job is Pending, Estimated or Accepted (waiting tech check)
                if (status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("Estimated") || status.equalsIgnoreCase("Accepted")) {
                    btnOffers.setVisibility(View.VISIBLE);
                } else {
                    btnOffers.setVisibility(View.GONE);
                }

                btnOffers.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, OffersActivity.class);
                        intent.putExtra("cid", cid);
                        intent.putExtra("product_name", product);
                        intent.putExtra("complaint", complaint);
                        context.startActivity(intent);
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
