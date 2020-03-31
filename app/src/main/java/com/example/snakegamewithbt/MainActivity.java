package com.example.snakegamewithbt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnConnect, btnPlay;
    TextView tvStatus;

    boolean isConnected = false;
    String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        Intent getStatus = getIntent();
        Bundle b = getStatus.getExtras();
        if(b != null){
            isConnected = (Boolean) b.get("STATUS");
            if(isConnected == true){
                tvStatus.setText("Connected!");
            }

        }

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent connect = new Intent(MainActivity.this, ConnectActivity.class);
                startActivity(connect);
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected == true)
                {
                    Intent connect = new Intent(MainActivity.this, GameActivity.class);
                    startActivity(connect);
                }
                else{
                    Toast.makeText(MainActivity.this, "You are not connected to the device", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }
}



