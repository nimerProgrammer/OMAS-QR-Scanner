package com.example.omas_qr_scanner;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class ResultActivity extends AppCompatActivity {

    private TextView resultView;
    private Button markReceivedBtn, backBtn;
    private String beneficiaryId = null;
    private final String CHECK_API_URL = "https://oras-seed-request-distribution.com/templates/php/checkVoucher.php";
    private final String UPDATE_API_URL = "https://oras-seed-request-distribution.com/templates/php/markReceived.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultView = findViewById(R.id.txt_result);
        markReceivedBtn = findViewById(R.id.btn_markReceived);
        backBtn = findViewById(R.id.btn_back);

        markReceivedBtn.setVisibility(View.GONE);
        backBtn.setVisibility(View.GONE);

        String scannedText = getIntent().getStringExtra("scanned_result");
        resultView.setText("Checking status...");

        markReceivedBtn.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);

            new AlertDialog.Builder(ResultActivity.this)
                    .setView(dialogView)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (beneficiaryId != null) {
                            new Thread(() -> updateStatus(beneficiaryId)).start();
                        } else {
                            Toast.makeText(ResultActivity.this, "Missing ID. Cannot update.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        backBtn.setOnClickListener(v -> finish());

        new Thread(() -> checkStatus(scannedText)).start();
    }

    private void checkStatus(String code) {
        try {
            String encodedCode = URLEncoder.encode(code, "UTF-8");
            URL url = new URL(CHECK_API_URL + "?code=" + encodedCode);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(response.toString());
            String status = json.optString("status", "Unknown");
            beneficiaryId = json.optString("id", null);

            StringBuilder display = new StringBuilder("Status: " + status);

            JSONObject parts = json.optJSONObject("code_parts");
            if (parts != null) {
                display.append("\n\nDetails:")
                        .append("\n1. ").append(parts.optString("part1", ""))
                        .append("\n2. ").append(parts.optString("part2", ""))
                        .append("\n3. ").append(parts.optString("part4", "")).append("kg")
                        .append("\n4. RSBSA No.-").append(parts.optString("part3", ""))
                        .append("\n5. ").append(parts.optString("ref", ""));
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                resultView.setText(display.toString());

                if (status.equalsIgnoreCase("For Receiving")) {
                    markReceivedBtn.setVisibility(View.VISIBLE
                    );
                    backBtn.setVisibility(View.VISIBLE);
                } else {
                    markReceivedBtn.setVisibility(View.GONE);
                    backBtn.setVisibility(View.VISIBLE);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() -> {
                resultView.setText("❌ Error checking status\n" + e.getMessage());
                Toast.makeText(this, "Error checking status", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void updateStatus(String id) {
        try {
            String encodedId = URLEncoder.encode(id, "UTF-8");
            URL url = new URL(UPDATE_API_URL + "?id=" + encodedId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(response.toString());
            boolean success = json.optBoolean("success", false);
            String message = json.optString("message", "");

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(ResultActivity.this, message, Toast.LENGTH_LONG).show();

                if (success) {
                    markReceivedBtn.setVisibility(View.GONE);
                    backBtn.setVisibility(View.VISIBLE);

                    // Replace the status line in resultView with "Status: Received"
                    String currentText = resultView.getText().toString();
                    String updatedText = currentText.replaceFirst("Status: .*", "Status: Received");

                    resultView.setText(updatedText + "\n\n✅ Updated to Received");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(ResultActivity.this, "Error updating status", Toast.LENGTH_LONG).show()
            );
        }
    }
}
