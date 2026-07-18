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
import android.widget.ListView;
import android.widget.Spinner;
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

public class TechHomeActivity extends AppCompatActivity {

    private TextView tvTechWelcome;
    private ListView lvTechJobs;
    private Button btnTechLogout;
    private Button btnTabMyTasks, btnTabFindJobs;
    private TextView tvSectionTitle;
    private boolean isFindJobsTab = false;
    private SharedPreferences sharedPreferences;
    private List<JSONObject> jobsList;
    private JobsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_home);

        tvTechWelcome = findViewById(R.id.tv_tech_welcome);
        lvTechJobs = findViewById(R.id.lv_tech_jobs);
        btnTechLogout = findViewById(R.id.btn_tech_logout);
        btnTabMyTasks = findViewById(R.id.btn_tab_my_tasks);
        btnTabFindJobs = findViewById(R.id.btn_tab_find_jobs);
        tvSectionTitle = findViewById(R.id.tv_tech_section_title);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "Technician");
        tvTechWelcome.setText("Welcome, " + name + "!");

        jobsList = new ArrayList<>();
        adapter = new JobsAdapter(this, jobsList);
        lvTechJobs.setAdapter(adapter);

        btnTabMyTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFindJobsTab = false;
                btnTabMyTasks.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                btnTabMyTasks.setTextColor(getResources().getColor(R.color.white));
                btnTabFindJobs.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_light)));
                btnTabFindJobs.setTextColor(getResources().getColor(R.color.primary));
                tvSectionTitle.setText("Active Assignments");
                loadAssignedJobs();
            }
        });

        btnTabFindJobs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFindJobsTab = true;
                btnTabFindJobs.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
                btnTabFindJobs.setTextColor(getResources().getColor(R.color.white));
                btnTabMyTasks.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_light)));
                btnTabMyTasks.setTextColor(getResources().getColor(R.color.primary));
                tvSectionTitle.setText("Open Customer Requests");
                loadAvailableJobs();
            }
        });

        btnTechLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        loadAssignedJobs();
    }

    private void loadAssignedJobs() {
        String techLid = sharedPreferences.getString("lid", "");
        String url = AppConfig.API_TECH_ASSIGNED + "?tech_lid=" + techLid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                jobsList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    jobsList.add(data.getJSONObject(i));
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
                        Toast.makeText(TechHomeActivity.this, "Failed to load assignments", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void loadAvailableJobs() {
        String techLid = sharedPreferences.getString("lid", "");
        String url = AppConfig.API_TECH_AVAILABLE_JOBS + "?tech_lid=" + techLid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                jobsList.clear();
                                JSONArray data = jsonObject.getJSONArray("data");
                                for (int i = 0; i < data.length(); i++) {
                                    jobsList.add(data.getJSONObject(i));
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
                        Toast.makeText(TechHomeActivity.this, "Failed to load open requests", Toast.LENGTH_SHORT).show();
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void offerJob(final String cid) {
        final String techLid = sharedPreferences.getString("lid", "");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_TECH_OFFER_JOB,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(TechHomeActivity.this, "Bid/Offer submitted successfully!", Toast.LENGTH_SHORT).show();
                        loadAvailableJobs(); // refresh list
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Failed to submit bid", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                params.put("tech_lid", techLid);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void confirmJob(final String cid) {
        final String techLid = sharedPreferences.getString("lid", "");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_TECH_CONFIRM_JOB,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(TechHomeActivity.this, "Job started!", Toast.LENGTH_SHORT).show();
                        loadAssignedJobs(); // refresh list
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                params.put("tech_lid", techLid);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void cancelJob(final String cid) {
        final String techLid = sharedPreferences.getString("lid", "");
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_TECH_CANCEL_JOB,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(TechHomeActivity.this, "Job declined/closed", Toast.LENGTH_SHORT).show();
                        loadAssignedJobs(); // refresh list
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                params.put("tech_lid", techLid);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void updateJobStatus(final String cid, final String status) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_TECH_UPDATE_STATUS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(TechHomeActivity.this, "Status updated to " + status, Toast.LENGTH_SHORT).show();
                        loadAssignedJobs(); // Reload jobs
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
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

    private void logout() {
        sharedPreferences.edit().clear().apply();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(TechHomeActivity.this, LoginActivity.class));
        finish();
    }

    private void fetchAndShowPartsDialog(final String cid, String companyId) {
        String url = AppConfig.API_PRODUCTS + "?co_lid=" + companyId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                final JSONArray data = jsonObject.getJSONArray("data");
                                if (data.length() == 0) {
                                    Toast.makeText(TechHomeActivity.this, "No products registered for this company", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                final String[] partNames = new String[data.length()];
                                final String[] partIds = new String[data.length()];
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject p = data.getJSONObject(i);
                                    partNames[i] = p.getString("product_name") + " (₹" + p.getString("amount") + ")";
                                    partIds[i] = p.getString("product_id");
                                }

                                final int[] selectedIdx = {-1};
                                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(TechHomeActivity.this);
                                builder.setTitle("Order Genuine Part from Company");
                                builder.setSingleChoiceItems(partNames, -1, new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        selectedIdx[0] = which;
                                    }
                                });
                                builder.setPositiveButton("Order", new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        if (selectedIdx[0] != -1) {
                                            requestPart(cid, partIds[selectedIdx[0]]);
                                        } else {
                                            Toast.makeText(TechHomeActivity.this, "Please select a part", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                                builder.setNegativeButton("Cancel", null);
                                builder.show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Error fetching parts catalog", Toast.LENGTH_SHORT).show();
                    }
                });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void requestPart(final String cid, final String productId) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_REQUEST_PART,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(TechHomeActivity.this, "Genuine Part Sourcing Request Sent to Manufacturer!", Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(TechHomeActivity.this, "Failed to submit parts request", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                params.put("product_id", productId);
                return params;
            }
        };
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    // Custom Adapter for Technician Assignments
    private class JobsAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public JobsAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_tech_job, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_tech_job, parent, false);
            }

            final JSONObject job = list.get(position);

            TextView tvCustomer = convertView.findViewById(R.id.tv_tech_job_customer);
            TextView tvAddress = convertView.findViewById(R.id.tv_tech_job_address);
            TextView tvProduct = convertView.findViewById(R.id.tv_tech_job_product);
            TextView tvComplaint = convertView.findViewById(R.id.tv_tech_job_complaint);
            final Spinner spinnerStatus = convertView.findViewById(R.id.spinner_job_status);
            Button btnUpdate = convertView.findViewById(R.id.btn_job_update);
            Button btnChat = convertView.findViewById(R.id.btn_job_chat);
            Button btnParts = convertView.findViewById(R.id.btn_job_parts);
            Button btnBid = convertView.findViewById(R.id.btn_job_bid);
            LinearLayout layoutConfirmations = convertView.findViewById(R.id.layout_tech_job_confirmations);
            Button btnConfirm = convertView.findViewById(R.id.btn_job_confirm);
            Button btnDecline = convertView.findViewById(R.id.btn_job_decline);

            try {
                final String cid = job.getString("cid");
                final String userLid = job.getString("user_id"); // Customer Login ID
                final String userName = job.getString("user_name");
                String product = job.getString("product_name");
                String complaint = job.getString("complaint");
                String address = "House: " + job.getString("user_house") + ", " + job.getString("user_place") + "\nPhone: " + job.getString("user_phone");
                final String currentStatus = job.getString("status");

                tvCustomer.setText(userName);
                tvAddress.setText(address);
                tvProduct.setText("Product: " + product);
                tvComplaint.setText(complaint);

                if (isFindJobsTab) {
                    spinnerStatus.setVisibility(View.GONE);
                    btnUpdate.setVisibility(View.GONE);
                    btnChat.setVisibility(View.GONE);
                    btnParts.setVisibility(View.GONE);
                    layoutConfirmations.setVisibility(View.GONE);
                    btnBid.setVisibility(View.VISIBLE);

                    btnBid.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            offerJob(cid);
                        }
                    });
                } else {
                    btnBid.setVisibility(View.GONE);

                    if (currentStatus.equalsIgnoreCase("Accepted")) {
                        spinnerStatus.setVisibility(View.GONE);
                        btnUpdate.setVisibility(View.GONE);
                        btnChat.setVisibility(View.GONE);
                        btnParts.setVisibility(View.GONE);
                        layoutConfirmations.setVisibility(View.VISIBLE);

                        btnConfirm.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                confirmJob(cid);
                            }
                        });

                        btnDecline.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cancelJob(cid);
                            }
                        });
                    } else {
                        spinnerStatus.setVisibility(View.VISIBLE);
                        btnUpdate.setVisibility(View.VISIBLE);
                        btnChat.setVisibility(View.VISIBLE);
                        btnParts.setVisibility(View.VISIBLE);
                        layoutConfirmations.setVisibility(View.GONE);

                        // Setup Spinner
                        String[] statuses = {"Assigned", "In Progress", "Completed"};
                        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, statuses);
                        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerStatus.setAdapter(spinAdapter);

                        // Set current status selection
                        for (int i = 0; i < statuses.length; i++) {
                            if (statuses[i].equalsIgnoreCase(currentStatus)) {
                                spinnerStatus.setSelection(i);
                                break;
                            }
                        }

                        btnUpdate.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String newStatus = spinnerStatus.getSelectedItem().toString();
                                updateJobStatus(cid, newStatus);
                            }
                        });

                        btnChat.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(context, ChatActivity.class);
                                intent.putExtra("recipient_id", userLid);
                                intent.putExtra("recipient_name", userName);
                                intent.putExtra("cid", cid);
                                context.startActivity(intent);
                            }
                        });

                        btnParts.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    String companyId = job.getString("company_id");
                                    fetchAndShowPartsDialog(cid, companyId);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
