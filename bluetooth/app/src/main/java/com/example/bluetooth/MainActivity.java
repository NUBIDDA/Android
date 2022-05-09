package com.example.bluetooth;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class  MainActivity extends AppCompatActivity {

//    Button mBtnBluetooth;

    // false == CLOSE / true == OPEN
    boolean fanState = false;
    boolean coverState = false;

    TextView mTvBluetoothStatus;
    TextView mTvReceiveData;
    TextView mTvSendData;
    Button mBtnBluetoothOn;
    Button mBtnBluetoothOff;
    Button mBtnConnect;
    Button mBtnSendData;

    // Control manual/automatic button

    Button mBtnManual;
    Button mBtnAutomatic;
    Button mBtnCoverOn;
    Button mBtnCoverOff;
    Button mBtnFanOn;
    Button mBtnFanOff;

    Button mBtnBabyCry;
    Button mBtnNaverMap;

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    TextView temp;
    TextView dust;
    TextView uv;


    TextView mTvBowlMovement;  // 배변 활동 감지

    String fanStr = "";
    String coverStr = "";

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mBtnBluetooth = (Button)findViewById(R.id.btnBluetooth);
//        mBtnBluetooth.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), BluetoothActivity.class);
//                startActivity(intent);
//            }
//        });

        mTvBluetoothStatus = (TextView)findViewById(R.id.tvBluetoothStatus);
        mTvReceiveData = (TextView)findViewById(R.id.tvReceiveData);
        mTvSendData =  (EditText) findViewById(R.id.tvSendData);
        mBtnBluetoothOn = (Button)findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = (Button)findViewById(R.id.btnBluetoothOff);
        mBtnConnect = (Button)findViewById(R.id.btnConnect);
        mBtnSendData = (Button)findViewById(R.id.btnSendData);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBtnManual = (Button) findViewById(R.id.btnManual);
        mBtnAutomatic = (Button) findViewById(R.id.btnAutomatic);
        mBtnCoverOn = (Button) findViewById(R.id.btnCoverOn);
        mBtnCoverOff = (Button) findViewById(R.id.btnCoverOff);
        mBtnFanOn = (Button) findViewById(R.id.btnFanOn);
        mBtnFanOff = (Button) findViewById(R.id.btnFanOff);

        mBtnBabyCry = (Button) findViewById(R.id.btnBabyCry);
        mBtnNaverMap = (Button) findViewById(R.id.btnNaverMap);

        // add new (05.02)
        temp =  (TextView)findViewById(R.id.temp);
        dust = (TextView)findViewById(R.id.dust);
        uv = (TextView)findViewById(R.id.uv);

        mTvBowlMovement = (TextView)findViewById(R.id.tvBowelMovement);

        mBtnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });
        mBtnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });
        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });

        mBtnSendData.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    mThreadConnectedBluetooth.write(mTvSendData.getText().toString());
                    mTvSendData.setText("");
                }
            }
        });


        mBtnBabyCry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BabyCryActivity.class);
                startActivity(intent);
            }
        });

        mBtnNaverMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NaverMapActivity.class);
                startActivity(intent);
            }
        });

        //Button.OnclickListener 로 변경?
        mBtnCoverOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    // 현재 선풍기 On/Off 여부에 따른 전송 문자열 변환
                    if(fanState == false) {
                        fanStr = "pc";
                    } else {
                        fanStr = "po";
                    }
                    coverState = true;
                    coverStr = "po";
                    mThreadConnectedBluetooth.write(coverStr+fanStr+"e");
                    // On일 경우 Off 버튼의 글자를 지움.
                    mBtnCoverOff.setText("");
                    mBtnCoverOn.setText("On..");
                }
            }
        });

        mBtnCoverOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    if(fanState == false) {
                        fanStr = "pc";
                    } else {
                        fanStr = "po";
                    }
                    coverState = false;
                    coverStr = "pc";
                    mThreadConnectedBluetooth.write(coverStr+fanStr+"e");
                    mBtnCoverOff.setText("Off..");
                    mBtnCoverOn.setText("");
                }
            }
        });

        mBtnFanOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    if(coverState == false) {
                        coverStr = "pc";
                    } else {
                        coverStr = "po";
                    }
                    fanState = true;
                    fanStr = "po";
                    mThreadConnectedBluetooth.write(coverStr+fanStr+"e");
                    mBtnFanOn.setText("On..");
                    mBtnFanOff.setText("");
                }
            }
        });

        mBtnFanOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    if(coverState == false) {
                        coverStr = "pc";
                    } else {
                        coverStr = "po";
                    }
                    fanState = false;
                    fanStr = "pc";
                    mThreadConnectedBluetooth.write(coverStr+fanStr+"e");
                    mBtnFanOn.setText("");
                    mBtnFanOff.setText("Off..");
                }
            }
        });

        mBtnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    fanState = false;
                    coverState = false;
                    mThreadConnectedBluetooth.write("popoe");
                    mBtnFanOff.setText("Off");
                    mBtnFanOn.setText("On");
                    mBtnCoverOff.setText("Off");
                    mBtnCoverOn.setText("On");
                    mBtnManual.setText("On");
                    mBtnAutomatic.setText("");
                }
            }
        });

        mBtnAutomatic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mThreadConnectedBluetooth != null) {
                    mThreadConnectedBluetooth.write("aaaae");
                    mBtnFanOff.setText("");
                    mBtnFanOn.setText("");
                    mBtnCoverOff.setText("");
                    mBtnCoverOn.setText("");
                    mBtnManual.setText("");
                    mBtnAutomatic.setText("Off");
                }
            }
        });


        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // ","로 분할.
                    mTvReceiveData.setText(readMessage);
//                    System.out.println("msg's type: " + readMessage.getClass().getName());
                    System.out.println("readMessage: " + readMessage);
                    String[] array = readMessage.split(",");
//                    System.out.println("temperature: " + array[0]);
//                    System.out.println("dust: " + array[1]);
//                    System.out.println("uv: " + array[2]);
                    temp.setText(array[0].concat("°C"));
                    dust.setText(array[1].concat("㎛"));
                    uv.setText(array[2].concat("μω"));

                    String bowlMovementState = array[3];
                    if(bowlMovementState == "o") {
                        mTvBowlMovement.setText("기저귀 확인이 필요해요!");
                    } else {
                        mTvBowlMovement.setText("아직 안쌌어요.");
                    }
                }
            }
        };
    }

    void bluetoothOn() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }
        else {
            if (mBluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                mTvBluetoothStatus.setText("활성화");
            }
            else {
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }
    void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            mTvBluetoothStatus.setText("비활성화");
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("활성화");
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
                    mTvBluetoothStatus.setText("비활성화");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                // AlterDialog Library check
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
//            byte[] buffer = new byte[1024];
//            int bytes;
            while (true) {
                byte[] buffer = new byte[1024];
                int bytes;
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
//                        for(int i = 0; i < bytes; i++) {
//                            System.out.println("buffer[" + i + "]: " + buffer[i]);
//                        }
                        System.out.println(" type: " + buffer.getClass().getName());
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}