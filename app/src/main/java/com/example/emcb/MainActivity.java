package com.example.emcb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.emcb.BLE.BleMainActivity;
import com.example.emcb.NFCTag.NfcMainActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView mBluetoothTextView = findViewById(R.id.bluetoothText);
        mBluetoothTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BleMainActivity.class);
                intent.setAction(Intent.ACTION_DEFAULT);
                startActivity(intent);
            }
        });

        TextView mNfcTagTextView = findViewById(R.id.nfcTagText);
        mNfcTagTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NfcMainActivity.class);
                intent.setAction(Intent.ACTION_DEFAULT);
                startActivity(intent);
            }
        });
    }
}