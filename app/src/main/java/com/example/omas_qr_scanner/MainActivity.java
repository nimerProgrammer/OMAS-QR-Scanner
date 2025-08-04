package com.example.omas_qr_scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {

    private boolean hasCameraPermission = false;
    private ActivityResultLauncher<String> permissionLauncher;
    private ActivityResultLauncher<ScanOptions> scanLauncher;
    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button scanButton = findViewById(R.id.btn_scan);

        hasCameraPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    hasCameraPermission = granted;
                    if (granted) {
                        startQRScan();
                    }
                });

        scanLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                // Launch ResultActivity with scanned content
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("scanned_result", result.getContents());
                startActivity(intent);
            } else {
                resultTextView.setText("Cancelled or no result");
            }
        });

        scanButton.setOnClickListener(v -> {
            if (hasCameraPermission) {
                startQRScan();
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    private void startQRScan() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scanning...");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class); // Optional custom CaptureActivity
        scanLauncher.launch(options);
    }
}
