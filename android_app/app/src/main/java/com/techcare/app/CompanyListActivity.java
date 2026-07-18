package com.techcare.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

public class CompanyListActivity extends AppCompatActivity {

    private EditText etCompanySearch;
    private ListView lvCompanies;
    private List<JSONObject> fullCompanyList;
    private List<JSONObject> filteredCompanyList;
    private CompanyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        etCompanySearch = findViewById(R.id.et_company_search);
        lvCompanies = findViewById(R.id.lv_companies);

        fullCompanyList = new ArrayList<>();
        filteredCompanyList = new ArrayList<>();
        adapter = new CompanyAdapter(this, filteredCompanyList);
        lvCompanies.setAdapter(adapter);

        etCompanySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCompanies(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        lvCompanies.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    JSONObject selectedCompany = filteredCompanyList.get(position);
                    Intent intent = new Intent(CompanyListActivity.this, ProductListActivity.class);
                    intent.putExtra("co_lid", selectedCompany.getString("co_lid"));
                    intent.putExtra("company_name", selectedCompany.getString("name"));
                    startActivity(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        loadCompanies();
    }

    private void loadCompanies() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, AppConfig.API_COMPANIES,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                fullCompanyList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    fullCompanyList.add(data.getJSONObject(i));
                                }
                                filterCompanies(""); // Show all initially
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(CompanyListActivity.this, "Failed to load companies", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void filterCompanies(String text) {
        filteredCompanyList.clear();
        if (text.isEmpty()) {
            filteredCompanyList.addAll(fullCompanyList);
        } else {
            String query = text.toLowerCase();
            for (JSONObject company : fullCompanyList) {
                try {
                    if (company.getString("name").toLowerCase().contains(query) ||
                            company.getString("place").toLowerCase().contains(query)) {
                        filteredCompanyList.add(company);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Custom Adapter to display companies and ratings
    private class CompanyAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public CompanyAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_company, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_company, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_item_company_name);
            TextView tvDetails = convertView.findViewById(R.id.tv_item_company_details);
            TextView tvRating = convertView.findViewById(R.id.tv_item_company_rating);

            try {
                JSONObject company = list.get(position);
                tvName.setText(company.getString("name"));
                
                String details = "Email: " + company.getString("email") + "\n" +
                                 "Phone: " + company.getString("phone") + "\n" +
                                 "Place: " + company.getString("place") + ", " + company.getString("district");
                tvDetails.setText(details);

                String rating = company.optString("avg_rating", "0.0");
                if (rating.equals("0.0")) {
                    tvRating.setText("No ratings yet");
                } else {
                    tvRating.setText("★ " + rating + " / 5");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
