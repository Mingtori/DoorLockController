package com.min.doorlockcontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {
	private static final String TAG = "BluetoothService";

	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final String SERVICE_HANDLER_MSG_KEY_DEVICE_NAME = "device_name";
	public static final String SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS = "device_address";
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private BluetoothAdapter btAdapter;
	private Activity mActivity;
	private Handler mHandler;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;

	private int mState;

	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;

	public BluetoothService(Activity ac, Handler h) {
		mActivity = ac;
		mHandler = h;

		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	public boolean getDeviceState() {
		Log.i(TAG, "Check the Bluetooth support");

		if (btAdapter == null) {
			Log.d(TAG, "Bluetooth is not available");
			return false;
		} else {
			Log.d(TAG, "Bluetooth is available");
			return true;
		}
	}

	public void enableBluetooth() {
		Log.i(TAG, "Check the enabled Bluetooth");

		if (btAdapter.isEnabled()) {
			Log.d(TAG, "Bluetooth Enable Now");
			scanDevice();
		} else {
			Log.d(TAG, "Bluetooth Enable Request");
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);
		}
	}

	public void scanDevice() {
		Log.d(TAG, "Scan Device");
		Intent serverIntent = new Intent(mActivity, DeviceListActivity.class);
		mActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	public void connectDevice(String address) {
		if(btAdapter != null) {
			BluetoothDevice device = btAdapter.getRemoteDevice(address);
			Log.d(TAG, "Get Device Info \n" + "address : " + address);
			if(device != null) {
				connect(device);
			}
		}
	}

	private synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;
		mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	public synchronized void start() {
		Log.d(TAG, "Starting BluetoothManager...");
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		setState(STATE_LISTEN);
	}

	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);
		if (mState == STATE_CONNECTING) {
			if (mConnectThread == null) {

			} else {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread == null) {}
		else {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		Log.d(TAG, "connected");
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS, device.getAddress());
		bundle.putString(SERVICE_HANDLER_MSG_KEY_DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	public void write(byte[] out) {
		ConnectedThread r;
		synchronized (this) {
			if (mState != STATE_CONNECTED) return;
			r = mConnectedThread;
		}
		r.write(out);
	}

	private void connectionFailed() {
		setState(STATE_LISTEN);
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");
			btAdapter.cancelDiscovery();

			try {
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				BluetoothService.this.start();
				return;
			}
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
		}

		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

}