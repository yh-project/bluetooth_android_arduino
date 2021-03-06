package com.example.btex;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public BluetoothAdapter mBluetoothAdapter;
    public Set<BluetoothDevice> mDevices;
    private BluetoothSocket bSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private BluetoothDevice mRemoteDevice;
    public boolean onBT = false;
    public byte[] sendByte;
    public String sendStr = "";
    public TextView tvBT;
    public EditText sendContent;
    private Button BTButton;
    private Button btnSend;
    public ProgressDialog asyncDialog;
    private static final int REQUEST_ENABLE_BT = 1;
    public int i = 0;

    public RecyclerView recordRecyclerView;
    public ArrayList<LogData> items;
    public LogAdapter logAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBT = findViewById(R.id.tvBT);
        BTButton = findViewById(R.id.btnBTCon);
        btnSend = findViewById(R.id.btnSend);
        sendContent = findViewById(R.id.sendContent);

        recordRecyclerView = findViewById(R.id.recordRecyclerView);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        recordRecyclerView.setLayoutManager(mLayoutManager);

        items = new ArrayList<>();
        LogData start_log = new LogData("", null, 2);
        items.add(start_log);

        logAdapter = new LogAdapter(items);
        recordRecyclerView.setAdapter(logAdapter);

        BTButton.setOnClickListener(new Button.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if (!onBT) { //Connect
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter == null) { //????????? ??????????????? ???????????? ?????? ??????.
                        Toast.makeText(getApplicationContext(), "Bluetooth ????????? ?????? ?????? ???????????????.", Toast.LENGTH_SHORT).show();
                    } else { // ????????? ??????????????? ???????????? ??????.
                        if (!mBluetoothAdapter.isEnabled()) {
                            // ??????????????? ??????????????? ????????? ????????? ??????
                            // ??????????????? ?????? ????????? ????????? ?????? ????????? ?????? ??????
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            // ??????????????? ???????????? ?????? ????????? ??????
                            // ???????????? ?????? ????????? ???????????? ????????? ????????? ??????.
                            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                            if (pairedDevices.size() > 0) {
                                // ????????? ??? ????????? ?????? ??????.
                                selectDevice();
                            } else {
                                // ????????? ??? ????????? ?????? ??????.
                                Toast.makeText(getApplicationContext(), "?????? Bluetooth ????????? ????????? ???????????? ????????? ?????????.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }else{ //DisConnect
                    try {
                        BTSend.interrupt();   // ????????? ?????? ????????? ??????
                        mInputStream.close();
                        mOutputStream.close();
                        bSocket.close();
                        onBT = false;
                        BTButton.setText("connect");
                    } catch(Exception ignored) { }

                }
            }
        });

        btnSend.setOnClickListener(new Button.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                try {
                    Date date = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdate = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
                    sdate.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

                    sendStr = sendContent.getText().toString();
                    sendbtData();
                    items.add(new LogData(sendStr, sdate.format(date), 0));
                    Log.d("sibal", " " + items.size());
                    logAdapter.notifyDataSetChanged();

                } catch (IOException e) {
                    Log.d("sibal", "????????? ??????... " + e.getMessage());
                }
            }
        });
    }

    public void selectDevice() {
        mDevices = mBluetoothAdapter.getBondedDevices();
        final int mPairedDeviceCount = mDevices.size();

        if (mPairedDeviceCount == 0) {
            //  ????????? ??? ????????? ?????? ??????
            Toast.makeText(getApplicationContext(),"????????? ????????? ????????????!",Toast.LENGTH_SHORT).show();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("???????????? ?????? ??????");

        // ????????? ??? ???????????? ????????? ?????? ?????? ??????
        List<String> listItems = new ArrayList<>();
        for(BluetoothDevice device : mDevices) {
            listItems.add(device.getName());
        }
        listItems.add("??????");    // ?????? ?????? ??????

        final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setItems(items,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == mPairedDeviceCount) {
                    // ????????? ????????? ???????????? ?????? '??????'??? ?????? ??????
                    //finish();
                }
                else {
                    // ????????? ????????? ????????? ??????
                    // ????????? ????????? ????????? ?????????
                    connectToSelectedDevice(items[item].toString());
                }
            }
        });

        builder.setCancelable(false);    // ?????? ?????? ?????? ?????? ??????
        AlertDialog alert = builder.create();
        alert.show();
    }


    public void connectToSelectedDevice(final String selectedDeviceName) {
        mRemoteDevice = getDeviceFromBondedList(selectedDeviceName);

        //Progress Dialog
        asyncDialog = new ProgressDialog(MainActivity.this);
        asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        asyncDialog.setMessage("???????????? ?????????..");
        asyncDialog.show();
        asyncDialog.setCancelable(false);

        Thread BTConnect = new Thread(new Runnable() {
            public void run() {
                try {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //HC-06 UUID
                    // ?????? ??????
                    bSocket = mRemoteDevice.createRfcommSocketToServiceRecord(uuid);

                    // RFCOMM ????????? ?????? ??????
                    bSocket.connect();

                    // ????????? ???????????? ?????? ????????? ??????
                    mOutputStream = bSocket.getOutputStream();
                    mInputStream = bSocket.getInputStream();

                    runOnUiThread(new Runnable() {
                        @SuppressLint({"ShowToast", "SetTextI18n"})
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), selectedDeviceName + " ?????? ??????", Toast.LENGTH_LONG).show();
                            tvBT.setText(selectedDeviceName + " Connected");
                            BTButton.setText("disconnect");
                            asyncDialog.dismiss();
                        }
                    });
                    onBT = true;
                    while(true) {
                        try {
                            Date date = new Date(System.currentTimeMillis());
                            SimpleDateFormat sdate = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
                            sdate.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                            if (mInputStream != null) {
                                int input = mInputStream.available();
                                if (input > 0) {
                                    byte[] arr = new byte[input];
                                    mInputStream.read(arr);
                                    String str = new String(arr, StandardCharsets.UTF_8);
                                    String msg[] = str.split("\n");
                                    int pos = items.size();
                                    for (String message:msg) {
                                        items.add(new LogData(message, sdate.format(date), 1));
                                    }
                                    logAdapter.notifyDataSetChanged();
                                    recordRecyclerView.scrollToPosition(items.size()-1);
                                    Log.d("hooo", str);
                                }
                            }
                        } catch (IOException e) {
                            Log.d("hooo..", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // ???????????? ?????? ??? ?????? ??????
                    runOnUiThread(new Runnable() {
                        @SuppressLint({"ShowToast", "SetTextI18n"})
                        @Override
                        public void run() {
                            tvBT.setText("?????? ?????? -- BT ?????? ??????????????????.");
                            asyncDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "???????????? ?????? ??????", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        BTConnect.start();
    }

    public BluetoothDevice getDeviceFromBondedList(String name) {
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : mDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    Thread BTSend  = new Thread(new Runnable() {
        public void run() {
            try {
                mOutputStream.write(sendByte);    // ???????????? ??????
                Log.d("sibal", "please");
            } catch (Exception e) {
                Log.d("sibal", "waegoorae");
                // ????????? ?????? ?????? ????????? ????????? ??????.
            }
        }
    });

    public void sendbtData() throws IOException {
        sendByte = sendStr.getBytes();
        Log.d("sibal", sendStr);
        BTSend.run();
    }
}