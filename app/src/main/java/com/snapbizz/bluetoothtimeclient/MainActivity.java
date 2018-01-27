package com.snapbizz.bluetoothtimeclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice device = null;
    private TextView textView = null;
    private int REQUEST_ENABLE_BT = 1;
    private Button button = null;
    private String timeBytesString = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textViewMain);
        button = findViewById(R.id.button);
        button.setOnClickListener(this);
        setBluetoothAdapter();
    }

    private void setBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            enableBluetoothAdapter();
        }
    }

    private void enableBluetoothAdapter() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        setPairedDevice();
    }

    private void setPairedDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) device = (BluetoothDevice) pairedDevices.toArray()[0];
    }

    public void updateTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText(timeBytesString);
            }
        });
    }

    public void onClick(View v) {
        new GetServerTimeThread(bluetoothAdapter, device, this).start();
    }

    private class GetServerTimeThread extends Thread {
        private BluetoothAdapter bluetoothAdapter;
        private BluetoothDevice device;
        private MainActivity mainActivity;
        private BluetoothSocket socket = null;
        private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        public GetServerTimeThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, MainActivity mainActivity) {
            this.bluetoothAdapter = bluetoothAdapter;
            this.device = device;
            this.mainActivity = mainActivity;
            try {
                socket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            InputStream inputStream = null;
            bluetoothAdapter.cancelDiscovery();
            try {
                socket.connect();
                inputStream = socket.getInputStream();
                byte timeBytes[] = new byte[1024];
                inputStream.read(timeBytes);
                timeBytesString = new String(timeBytes);
                try {
                    Thread.sleep(1000);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                updateTime();
            } catch (IOException connectException) {
                connectException.printStackTrace();
            }
        }
    }
}
