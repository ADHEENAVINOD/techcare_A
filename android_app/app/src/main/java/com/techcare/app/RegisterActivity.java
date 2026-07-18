package com.techcare.app;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etHouse, etPlace, etCity, etState, etPin, etUsername, etPassword;
    private Button btnSelectPhoto, btnRegister;
    private TextView tvSelectedPhotoPath, tvLoginLink;
    private Uri selectedImageUri = null;
    private Bitmap selectedBitmap = null;
    private static final int PICK_IMAGE_REQUEST = 999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.et_reg_name);
        etPhone = findViewById(R.id.et_reg_phone);
        etEmail = findViewById(R.id.et_reg_email);
        etHouse = findViewById(R.id.et_reg_house);
        etPlace = findViewById(R.id.et_reg_place);
        etCity = findViewById(R.id.et_reg_city);
        etState = findViewById(R.id.et_reg_state);
        etPin = findViewById(R.id.et_reg_pin);
        etUsername = findViewById(R.id.et_reg_username);
        etPassword = findViewById(R.id.et_reg_password);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        btnRegister = findViewById(R.id.btn_register);
        tvSelectedPhotoPath = findViewById(R.id.tv_selected_photo_path);
        tvLoginLink = findViewById(R.id.tv_login_link);

        btnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performRegistration();
            }
        });

        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Goes back to LoginActivity
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
                tvSelectedPhotoPath.setText("Image selected successfully");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performRegistration() {
        final String name = etName.getText().toString().trim();
        final String phone = etPhone.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String house = etHouse.getText().toString().trim();
        final String place = etPlace.getText().toString().trim();
        final String city = etCity.getText().toString().trim();
        final String state = etState.getText().toString().trim();
        final String pin = etPin.getText().toString().trim();
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || house.isEmpty() ||
                place.isEmpty() || city.isEmpty() || state.isEmpty() || pin.isEmpty() ||
                username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, AppConfig.API_REGISTER,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        try {
                            String result = new String(response.data);
                            JSONObject jsonObject = new JSONObject(result);
                            if (jsonObject.getString("status").equals("success")) {
                                Toast.makeText(RegisterActivity.this, "Registration successful! Please Sign In.", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(RegisterActivity.this, jsonObject.optString("message", "Registration failed"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(RegisterActivity.this, "Parsing error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errMsg = "Registration failed. Check connection.";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                JSONObject obj = new JSONObject(new String(error.networkResponse.data));
                                errMsg = obj.optString("message", errMsg);
                            } catch (Exception e) {}
                        }
                        Toast.makeText(RegisterActivity.this, errMsg, Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("phone", phone);
                params.put("email", email);
                params.put("house", house);
                params.put("place", place);
                params.put("city", city);
                params.put("state", state);
                params.put("pin", pin);
                params.put("username", username);
                params.put("password", password);
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
