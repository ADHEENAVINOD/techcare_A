package com.techcare.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoCallActivity extends AppCompatActivity {

    private TextView tvRecipientName;
    private Button btnEndCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        tvRecipientName = findViewById(R.id.tv_video_recipient_name);
        btnEndCall = findViewById(R.id.btn_end_call);

        String recipientName = getIntent().getStringExtra("recipient_name");
        if (recipientName != null && !recipientName.isEmpty()) {
            tvRecipientName.setText(recipientName);
        }

        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to Chat
                finish();
            }
        });
    }
}
