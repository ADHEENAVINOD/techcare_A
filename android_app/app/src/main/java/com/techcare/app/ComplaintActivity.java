package com.techcare.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ComplaintActivity extends AppCompatActivity {

    private TextView tvProductInfo, tvPhotoPath;
    private EditText etComplaintText;
    private Button btnSelectPhoto, btnSubmit;
    private String productId, productName;
    private Uri selectedImageUri = null;
    private Bitmap selectedBitmap = null;
    private static final int PICK_IMAGE_REQUEST = 888;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        tvProductInfo = findViewById(R.id.tv_complaint_product_info);
        tvPhotoPath = findViewById(R.id.tv_complaint_photo_path);
        etComplaintText = findViewById(R.id.et_complaint_text);
        btnSelectPhoto = findViewById(R.id.btn_complaint_select_photo);
        btnSubmit = findViewById(R.id.btn_complaint_submit);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);

        productId = getIntent().getStringExtra("product_id");
        productName = getIntent().getStringExtra("product_name");

        tvProductInfo.setText("Product Name: " + productName);

        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComplaint();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                tvPhotoPath.setText("Attachment loaded");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void submitComplaint() {
        final String complaintText = etComplaintText.getText().toString().trim();
        final String userLid = sharedPreferences.getString("lid", "");

        if (complaintText.isEmpty()) {
            Toast.makeText(this, "Please describe the problem details", Toast.LENGTH_SHORT).show();
            return;
        }

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, AppConfig.API_SEND_COMPLAINT,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        try {
                            String result = new String(response.data);
                            JSONObject jsonObject = new JSONObject(result);
                            if (jsonObject.getString("status").equals("success")) {
                                Toast.makeText(ComplaintActivity.this, "Complaint filed successfully!", Toast.LENGTH_LONG).show();
                                // Go back to Home
                                Intent intent = new Intent(ComplaintActivity.this, UserHomeActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ComplaintActivity.this, jsonObject.optString("message", "Submission failed"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(ComplaintActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ComplaintActivity.this, "Failed to submit request. Check connection.", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("pid", productId);
                params.put("complaint", complaintText);
                params.put("user_lid", userLid);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                if (selectedBitmap != null) {
                    long imagename = System.currentTimeMillis();
                    params.put("photo", new DataPart(imagename + ".png", getFileDataFromDrawable(selectedBitmap), "image/png"));
                }
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(multipartRequest);
    }

    private byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
