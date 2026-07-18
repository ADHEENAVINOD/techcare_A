package com.techcare.app;

import android.content.Context;
import android.content.Intent;
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
import java.util.List;

public class ProductListActivity extends AppCompatActivity {

    private TextView tvCompanyNameHeader;
    private ListView lvProducts;
    private Button btnViewTechnicians;
    private List<JSONObject> productsList;
    private ProductAdapter adapter;
    private String coLid, companyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        tvCompanyNameHeader = findViewById(R.id.tv_company_name_header);
        lvProducts = findViewById(R.id.lv_products);
        btnViewTechnicians = findViewById(R.id.btn_view_technicians);

        coLid = getIntent().getStringExtra("co_lid");
        companyName = getIntent().getStringExtra("company_name");

        tvCompanyNameHeader.setText(companyName + " Catalog");

        productsList = new ArrayList<>();
        adapter = new ProductAdapter(this, productsList);
        lvProducts.setAdapter(adapter);

        btnViewTechnicians.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAndShowTechnicians();
            }
        });

        loadProducts();
    }

    private void loadProducts() {
        String url = AppConfig.API_PRODUCTS + "?co_lid=" + coLid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                productsList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    productsList.add(data.getJSONObject(i));
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
                        Toast.makeText(ProductListActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void fetchAndShowTechnicians() {
        String url = AppConfig.BASE_URL + "/api/view_technicians?company_id=" + coLid;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json = new JSONObject(response);
                            if (json.getString("status").equals("success")) {
                                JSONArray data = json.getJSONArray("data");
                                if (data.length() == 0) {
                                    Toast.makeText(ProductListActivity.this, "No service agents registered for this company", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject t = data.getJSONObject(i);
                                    sb.append("👨‍🔧 ").append(t.getString("name")).append("\n");
                                    sb.append("📍 Place: ").append(t.getString("place")).append("\n");
                                    sb.append("⚙️ Skills: ").append(t.getString("specialties")).append("\n");
                                    sb.append("💵 Rate: ₹").append(t.getString("starting_rate")).append(" visiting fee\n");
                                    
                                    String rating = t.optString("avg_rating", "0.0");
                                    if (rating.equals("0.0") || rating.equals("0")) {
                                        sb.append("⭐ Rating: No reviews yet\n");
                                    } else {
                                        sb.append("⭐ Rating: ").append(rating).append(" / 5\n");
                                    }
                                    
                                    JSONArray reviews = t.getJSONArray("reviews");
                                    if (reviews.length() > 0) {
                                        sb.append("💬 Client Comments:\n");
                                        for (int j = 0; j < Math.min(reviews.length(), 3); j++) {
                                            JSONObject r = reviews.getJSONObject(j);
                                            sb.append("  - \"").append(r.getString("feedback")).append("\" (").append(r.getString("rating")).append("★)\n");
                                        }
                                    }
                                    sb.append("\n---------------------------\n\n");
                                }

                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(ProductListActivity.this);
                                builder.setTitle("Company Certified Technicians");
                                
                                android.widget.ScrollView scrollView = new android.widget.ScrollView(ProductListActivity.this);
                                TextView tv = new TextView(ProductListActivity.this);
                                tv.setText(sb.toString());
                                tv.setPadding(32, 32, 32, 32);
                                tv.setTextSize(15);
                                tv.setTextColor(getResources().getColor(R.color.text_color));
                                scrollView.addView(tv);
                                
                                builder.setView(scrollView);
                                builder.setPositiveButton("OK", null);
                                builder.show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(ProductListActivity.this, "Error loading technician profiles", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProductListActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    // Custom Adapter for Products
    private class ProductAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public ProductAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_product, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_item_product_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_item_product_amount);
            TextView tvDesc = convertView.findViewById(R.id.tv_item_product_description);
            Button btnRequest = convertView.findViewById(R.id.btn_item_product_request);

            try {
                final JSONObject product = list.get(position);
                final String productId = product.getString("product_id");
                final String productName = product.getString("product_name");

                tvName.setText(productName);
                tvAmount.setText("₹" + product.getString("amount"));
                tvDesc.setText(product.optString("description", "No description available."));

                btnRequest.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ComplaintActivity.class);
                        intent.putExtra("product_id", productId);
                        intent.putExtra("product_name", productName);
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
