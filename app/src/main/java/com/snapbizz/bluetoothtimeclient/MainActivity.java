package com.snapbizz.bluetoothtimeclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
    private CheckBox checkBox = null;
    private String timeBytesString = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textViewMain);
        button = findViewById(R.id.button);
        button.setX(950);
        button.setY(40);
        button.setOnClickListener(this);
        checkBox = findViewById(R.id.checkBox);
        checkBox.setX(1100);
        checkBox.setY(50);
        checkBox.setOnClickListener(this);
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
        if (v.getId() == R.id.button) {
            new GetServerTimeThread(bluetoothAdapter, device, false).start();
        } else if (v.getId() == R.id.checkBox) {
            if (checkBox.isChecked()) {
                button.setEnabled(false);
                new GetServerTimeThread(bluetoothAdapter, device, true).start();
            } else {
                button.setEnabled(true);
            }
        }
    }

    private class GetServerTimeThread extends Thread {
        private BluetoothAdapter bluetoothAdapter;
        private BluetoothSocket socket = null;
        private UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private boolean automatic;
        private InputStream inputStream = null;

        public GetServerTimeThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device, boolean automatic) {
            this.bluetoothAdapter = bluetoothAdapter;
            this.automatic = automatic;
        }

        public void run() {
            if (automatic) {
                while (true && checkBox.isChecked()) {
                    getServerTime();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                getServerTime();
            }
        }

        private void getServerTime() {
            try {
                try {
                    socket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
                    bluetoothAdapter.cancelDiscovery();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket.connect();
                inputStream = socket.getInputStream();
                byte timeBytes[] = new byte[1024];
                inputStream.read(timeBytes);
                timeBytesString = new String(timeBytes);
                updateTime();
                cleanup();
            } catch (IOException connectException) {
                connectException.printStackTrace();
            }
        }

        private void cleanup() {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }
}
