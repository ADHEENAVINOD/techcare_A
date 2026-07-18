package com.techcare.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

public class ChatActivity extends AppCompatActivity {

    private TextView tvRecipientName;
    private ListView lvChatMessages;
    private EditText etChatMessage;
    private Button btnSend;
    private Button btnVideoCall;
    private Button btnBlock;
    
    private String myLid;
    private String recipientLid;
    private String recipientName;
    private String cid;
    private SharedPreferences sharedPreferences;

    private List<JSONObject> chatMessages;
    private ChatAdapter adapter;
    private Handler handler;
    private Runnable refreshRunnable;
    private static final int POLL_INTERVAL_MS = 3000; // Poll every 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        tvRecipientName = findViewById(R.id.tv_chat_recipient);
        lvChatMessages = findViewById(R.id.lv_chat_messages);
        etChatMessage = findViewById(R.id.et_chat_message);
        btnSend = findViewById(R.id.btn_chat_send);
        btnVideoCall = findViewById(R.id.btn_chat_video);
        btnBlock = findViewById(R.id.btn_chat_block);

        sharedPreferences = getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE);
        myLid = sharedPreferences.getString("lid", "");

        recipientLid = getIntent().getStringExtra("recipient_id");
        recipientName = getIntent().getStringExtra("recipient_name");
        cid = getIntent().getStringExtra("cid");

        tvRecipientName.setText("Chat: " + recipientName);

        chatMessages = new ArrayList<>();
        adapter = new ChatAdapter(this, chatMessages);
        lvChatMessages.setAdapter(adapter);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, VideoCallActivity.class);
                intent.putExtra("recipient_name", recipientName);
                startActivity(intent);
            }
        });

        btnBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ChatActivity.this)
                    .setTitle("Stop Conversation")
                    .setMessage("Are you sure you want to stop this conversation? This will permanently block messaging for both parties.")
                    .setPositiveButton("Stop/Block", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performBlockChat();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        // Configure auto-refresh polling
        handler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadChatHistory();
                handler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable); // Start polling
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable); // Stop polling when leaving activity
    }

    private void loadChatHistory() {
        if (recipientLid == null || recipientLid.isEmpty()) return;

        String url = AppConfig.API_CHAT_GET + "?from_id=" + myLid + "&to_id=" + recipientLid;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.getString("status").equals("success")) {
                                if (jsonObject.optBoolean("blocked", false)) {
                                    etChatMessage.setEnabled(false);
                                    etChatMessage.setHint("This chat is closed/blocked");
                                    btnSend.setEnabled(false);
                                    btnVideoCall.setEnabled(false);
                                    btnBlock.setEnabled(false);
                                    btnBlock.setText("Blocked");
                                }
                                JSONArray data = jsonObject.getJSONArray("data");
                                chatMessages.clear();
                                for (int i = 0; i < data.length(); i++) {
                                    chatMessages.add(data.getJSONObject(i));
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
                        // Suppress logs for polling stability
                    }
                });

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void sendMessage() {
        final String message = etChatMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        etChatMessage.setText(""); // Clear input immediately for UX

        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_CHAT_SEND,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        loadChatHistory(); // Refresh immediately
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("from_id", myLid);
                params.put("to_id", recipientLid);
                params.put("message", message);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void performBlockChat() {
        if (cid == null || cid.isEmpty()) {
            Toast.makeText(this, "Complaint details missing. Cannot block chat.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, AppConfig.API_CHAT_BLOCK,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(ChatActivity.this, "Conversation blocked successfully.", Toast.LENGTH_SHORT).show();
                        loadChatHistory(); // refresh UI state
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ChatActivity.this, "Failed to block chat", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("cid", cid);
                return params;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    // Custom Chat Adapter
    private class ChatAdapter extends ArrayAdapter<JSONObject> {
        private Context context;
        private List<JSONObject> list;

        public ChatAdapter(Context context, List<JSONObject> list) {
            super(context, R.layout.item_chat_message, list);
            this.context = context;
            this.list = list;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
            }

            LinearLayout layoutLeft = convertView.findViewById(R.id.layout_chat_left);
            LinearLayout layoutRight = convertView.findViewById(R.id.layout_chat_right);
            TextView tvTextLeft = convertView.findViewById(R.id.tv_chat_text_left);
            TextView tvTextRight = convertView.findViewById(R.id.tv_chat_text_right);
            TextView tvTimeLeft = convertView.findViewById(R.id.tv_chat_time_left);
            TextView tvTimeRight = convertView.findViewById(R.id.tv_chat_time_right);

            try {
                JSONObject msg = list.get(position);
                String from = msg.getString("from_id");
                String text = msg.getString("message");
                String time = msg.getString("time");

                // Toggle bubble visibility based on sender
                if (from.equals(myLid)) {
                    layoutLeft.setVisibility(View.GONE);
                    layoutRight.setVisibility(View.VISIBLE);
                    tvTextRight.setText(text);
                    tvTimeRight.setText(time);
                } else {
                    layoutLeft.setVisibility(View.VISIBLE);
                    layoutRight.setVisibility(View.GONE);
                    tvTextLeft.setText(text);
                    tvTimeLeft.setText(time);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
