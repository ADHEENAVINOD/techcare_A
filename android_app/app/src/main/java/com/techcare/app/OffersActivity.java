package com.techcare.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class OffersActivity extends AppCompatActivity {

    private TextView tvProductName, tvComplaintDesc;
    private ListView lvOffers;
    private List<JSONObject> offersList;
    private OffersAdapter adapter;
    private String cid, productName, complaintDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers);

        tvProductName = findViewById(R.id.tv_offers_product_name);
        tvComplaintDesc = findViewById(R.id.tv_offers_complaint_desc);
        lvOffers = findViewById(R.id.lv_technician_offers);

        cid = getIntent().getStringExtra("cid");
        productName = getIntent().getStringExtra("product_name");
        complaintDesc = getIntent().getStringExtra("complaint");

        tvProductName.setText("Product: " + productName);
        tvComplaintDesc.setText(complaintDesc);

        offersList = new ArrayList<>();
        adapter = new OffersAdapter(this, offersList);
        lvOffers.setAdapter(adapter);

        loadOffers();
    }

    private void loadOffers() {
        String url = AppConfig.API_USER_COMPLAINT_OFFERS + "?cid=" + cid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                offersList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    offersList.add(data.getJSONObject(i));
                                }
                                adapter.notifyDataSetChanged();
                                if (offersList.isEmpty()) {
                                    Toast.makeText(OffersActivity.this, "No bids from service agents yet.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(OffersActivity.this, "Failed to load technician offers", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void acceptOffer(final String aid) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_USER_ACCEPT_OFFER,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(OffersActivity.this, "Technician connected! Waiting for confirmation.", Toast.LENGTH_LONG).show();
                        finish(); // return to previous screen
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(OffersActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("aid", aid);
                params.put("cid", cid);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private class OffersAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public OffersAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_offer, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_offer, parent, false);
            }

            final JSONObject item = list.get(position);

            TextView tvName = convertView.findViewById(R.id.tv_offer_tech_name);
            TextView tvRate = convertView.findViewById(R.id.tv_offer_rate);
            TextView tvSpecs = convertView.findViewById(R.id.tv_offer_specialties);
            TextView tvRating = convertView.findViewById(R.id.tv_offer_rating);
            TextView tvReview = convertView.findViewById(R.id.tv_offer_last_review);
            Button btnAccept = convertView.findViewById(R.id.btn_offer_accept);

            try {
                final String aid = item.getString("aid");
                String name = item.getString("name");
                String rate = item.getString("starting_rate");
                String specialties = item.getString("specialties");
                String rating = item.optString("avg_rating", "0.0");
                String status = item.getString("offer_status");

                tvName.setText("👨‍🔧 " + name);
                tvRate.setText("₹" + rate + " Visit Fee");
                tvSpecs.setText("Specialties: " + specialties);

                if (rating.equals("0.0") || rating.equals("0")) {
                    tvRating.setText("⭐ Rating: No reviews yet");
                } else {
                    tvRating.setText("⭐ Rating: " + rating + " / 5");
                }

                JSONArray reviews = item.getJSONArray("reviews");
                if (reviews.length() > 0) {
                    JSONObject lastReview = reviews.getJSONObject(0);
                    tvReview.setText("Last Comment: \"" + lastReview.getString("feedback") + "\"");
                    tvReview.setVisibility(View.VISIBLE);
                } else {
                    tvReview.setVisibility(View.GONE);
                }

                if (status.equalsIgnoreCase("Accepted")) {
                    btnAccept.setText("Accepted (Pending Tech Confirmation)");
                    btnAccept.setEnabled(false);
                    btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.text_muted)));
                } else if (status.equalsIgnoreCase("Assigned")) {
                    btnAccept.setText("Connected");
                    btnAccept.setEnabled(false);
                    btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success)));
                } else {
                    btnAccept.setText("Accept Offer & Connect");
                    btnAccept.setEnabled(true);
                    btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                    btnAccept.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            acceptOffer(aid);
                        }
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
