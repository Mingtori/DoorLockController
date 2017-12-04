package com.min.doorlockcontroller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener{
	private static final String TAG = "Main";
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	private Button btn_Connect;
	private BluetoothService btService = null;


	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case BluetoothService.MESSAGE_STATE_CHANGE:
					switch (msg.arg1) {
						case BluetoothService.STATE_CONNECTED:
							Toast.makeText(getApplicationContext(), "블루투스 연결됨", Toast.LENGTH_SHORT);
							break;
					}
				break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button openButton = findViewById(R.id.openButton);
		Button closeButton = findViewById(R.id.closeButton);

		btn_Connect = findViewById(R.id.btn_connect);
		btn_Connect.setOnClickListener(this);

		openButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				btService.write("1".getBytes());
			}
		});

		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				btService.write("2".getBytes());
			}
		});

		if(btService == null) {
			btService = new BluetoothService(this, mHandler);
		}
	}

	@Override
	public void onClick(View v) {
		if(btService.getDeviceState()) {
			btService.enableBluetooth();
		} else {
			finish();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE:
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				if (resultCode == Activity.RESULT_OK) {
					btService.connectDevice(address);
				}
				break;
			case REQUEST_ENABLE_BT:
				if (resultCode == Activity.RESULT_OK) {
					btService.scanDevice();
				} else {
					Log.d(TAG, "Bluetooth is not enabled");
				}
				break;
		}
	}
}
