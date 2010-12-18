package lejos.pc.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NXTCommAndroid implements NXTComm {

	private class ConnectThread extends Thread {
		private BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		private SynchronousQueue<Boolean> connectQueue;

		public ConnectThread(BluetoothDevice device, SynchronousQueue<Boolean> connectQueue) {
			mmDevice = device;
			BluetoothSocket tmp = null;
			this.connectQueue = connectQueue;
			// Get a BluetoothSocket for a connection with the
			// given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void cancel() {
			//Log.d(TAG, "closing socket TEST");
				    try {
					mmSocket.close();
				    } catch (IOException e) {
					Log.e(TAG, "close() of connect socket failed", e);
				    }finally {
					mmSocket=null;
				    }
		}

		private void relayConnectionSuccess() {
			try {// notify calling thread that connection succeeded
				connectQueue.put(new Boolean(true));
			} catch (InterruptedException e) {

			}
			// socketConnected = true;
			Log.d(TAG, "Connection success -- is connected to " + mmDevice.getName());
			yield(); // allow main NXTCommAndroid thread to read connected
			// status and finish NXTComm setup

		}

		private void relyConnectionFailure(IOException e) {
			try {
				// notify calling thread that connection failed
				connectQueue.put(new Boolean(false));
				Log.e(TAG, "Connection failure -- unable to connect to socket ", e);
			} catch (InterruptedException e1) {

			}

			if (mmSocket != null) {
				cancel();
			}
		}

		@Override
		public void run() {

			Log.i(TAG, "BEGIN mConnectThread");
			setName("NCA ConnectThread");

			// Make a connection to the BluetoothSocket
			// This is a blocking call and will only return on a
			// successful connection or an exception
			try {
				mmSocket.connect();
			} catch (IOException e) {
				relyConnectionFailure(e);
				return;
			}

			relayConnectionSuccess();
			startIOThreads(mmSocket, mmDevice);
		}

	}

	private class ReadThread extends Thread {
		public InputStream is;
		boolean running = true;
		LinkedBlockingQueue<byte[]> mReadQueue;
		BluetoothSocket socket;

		public ReadThread(BluetoothSocket socket, LinkedBlockingQueue<byte[]> mReadQueue) {
			try {
				this.socket = socket;
				is = socket.getInputStream();
				Log.d(TAG, "is opened in ReadThread constructor");
				this.mReadQueue = mReadQueue;
			} catch (IOException e) {
				Log.e(TAG, "ReadThread is error ", e);
			}
		}

		public void cancel() {
			running = false;
			mReadQueue.clear();
		}

		private byte[] read() {
			//try{
			int lsb = -1;
			try {
				Log.d(TAG, "socket is connected to: " + socket.getRemoteDevice().getName());
				Log.d(TAG, "is is null?: " + (is == null));
				lsb = is.read();

			} catch (Exception e) {
				Log.d(TAG, "read err lsb", e);
				//		try {
				//			is = socket.getInputStream();
				//			lsb = is.read();
				//		} catch (IOException e1) {
				//			 Log.e(TAG, "tried to open is and read from it");
				//		}

			}

			Log.d(TAG, "lsb : " + lsb);
			if (lsb < 0)
				return null;
			int msb = 0;

			try {
				msb = is.read();
				Log.d(TAG, "msb: " + msb);
			} catch (IOException e1) {
				Log.e(TAG, "ReadThread read error msb", e1);
			}

			if (msb < 0) {
				return null;
			}
			int len = lsb | (msb << 8);
			byte[] bb = new byte[len];
			for (int i = 0; i < len; i++) {

				try {
					bb[i] = (byte) is.read();
				} catch (IOException e) {
					Log.e(TAG, "ReadThread read error data", e);
				}
			}
			return bb;
		}

		private byte[] readLCP() {

			byte[] reply = null;
			int length = -1;

			try {
				do {
					Thread.yield();
					length = is.read(); // First byte specifies length of
					// packet.
				} while (running && length < 0);

				int lengthMSB = is.read(); // Most Significant Byte value
				length = (0xFF & length) | ((0xFF & lengthMSB) << 8);
				reply = new byte[length];
				is.read(reply);
			} catch (IOException e) {
				Log.e(TAG, "readLCP error:", e);
			}

			return (reply == null) ? new byte[0] : reply;
		}

		@Override
		public void run() {
			setName("NCA read thread");
			byte[] tmp_data;
			while (running) {
				Thread.yield();
				tmp_data = null;

				if (nxtInfo.connectionState == NXTConnectionState.LCP_CONNECTED) {
					tmp_data = readLCP();
				} else {
					tmp_data = read();
				}

				// Log.i(TAG," after attempt to read() " );
				if (tmp_data != null) {
					try {
						mReadQueue.put(tmp_data);
					} catch (InterruptedException e) {
						Log.e(TAG, "ReadThread queue error ", e);
					}
				}
			}
		}

	}

	private class WriteThread extends Thread {
		public OutputStream os;
		private boolean running = true;
		LinkedBlockingQueue<byte[]> mWriteQueueT;

		public WriteThread(BluetoothSocket socket, LinkedBlockingQueue<byte[]> mWriteQueue) {
			try {
				os = socket.getOutputStream();
				this.mWriteQueueT = mWriteQueue;
			} catch (IOException e) {
				Log.e(TAG, "WriteThread OutputStream error ", e);
			}
		}

		public void cancel() {
			running = false;
			mReadQueue.clear();
		}

		@Override
		public void run() {
			setName("NCA - write thread");
			while (running) {
				try {
					byte[] test;
					test = mWriteQueueT.take();
					//Log.d(TAG, "mWriteQueueT.take() found byte array");
					write(test);
				} catch (InterruptedException e) {
					Log.e(TAG, "WriteThread write error ", e);
				}
			}

		}

		void write(byte[] data) {
			byte[] lsb_msb = new byte[2];
			lsb_msb[0] = (byte) data.length;
			lsb_msb[1] = (byte) ((data.length >> 8) & 0xff);
			try {
				os.write(concat(lsb_msb, data));
				os.flush();
			} catch (IOException e) {
				Log.e(TAG, "WriteThread write error ", e);
			}

		}
	}

	private static Vector<BluetoothDevice> devices;
	private BluetoothAdapter mBtAdapter;

	private NXTInfo nxtInfo;
	private static Vector<NXTInfo> nxtInfos;

	private final String TAG = "NXTCommAndroid >>>>";
	protected String mConnectedDeviceName;

	private ConnectThread mConnectThread;
	private ReadThread mReadThread;
	private WriteThread mWriteThread;

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public static final String MESSAGE_CONTENT = "String_message";
	public static final int MESSAGE = 1000;
	public static final int TOAST = 2000;

	private LinkedBlockingQueue<byte[]> mReadQueue;
	private LinkedBlockingQueue<byte[]> mWriteQueue;

	private Handler mUIMessageHandler;

	private SynchronousQueue<Boolean> connectQueue;

	public int available() throws IOException {
		return 0;
	}
	
	private void cancelConnectThread(){
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
	}

	private void cancelIOThreads() {
	

		if (mReadThread != null) {
			mReadThread.cancel();
			mReadThread = null;
		}
		if (mWriteThread != null) {
			mWriteThread.cancel();
			mWriteThread = null;
		}
	}

	public void close() throws IOException {
		cancelIOThreads();
		mConnectedDeviceName = "";
	}

	private byte[] concat(byte[] data1, byte[] data2) {
		int l1 = data1.length;
		int l2 = data2.length;

		byte[] data = new byte[l1 + l2];
		System.arraycopy(data1, 0, data, 0, l1);
		System.arraycopy(data2, 0, data, l1, l2);
		for (int i = 0; i < data.length; i++) {
			Log.d(TAG, "concat data" + i + " " + data[i]);
		}

		return data;
	}

	public void displayToastOnUIThread(String message) {
		Message message_holder = formMessage(message);
		message_holder.what = TOAST;
		mUIMessageHandler.sendMessage(message_holder);

	}

	private Message formMessage(String message) {
		Bundle b = new Bundle();
		b.putString(MESSAGE_CONTENT, message);
		Message message_holder = new Message();
		message_holder.setData(b);
		return message_holder;
	}

	public InputStream getInputStream() {
		return new NXTCommInputStream(this);
	}

	public OutputStream getOutputStream() {
		return new NXTCommOutputStream(this);
	}

	public boolean open(NXTInfo nxt) throws NXTCommException {
		return open(nxt, PACKET);
	}

	public boolean open(NXTInfo nxt, int mode) throws NXTCommException {
		if (mode == RAW)
			throw new NXTCommException("RAW mode not implemented");
		BluetoothDevice nxtDevice = null;
		connectQueue = new SynchronousQueue<Boolean>();
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		}

		nxtDevice = mBtAdapter.getRemoteDevice(nxt.deviceAddress);

		try {

			mConnectThread = new ConnectThread(nxtDevice, connectQueue);
			mConnectThread.start();

			Boolean socketEstablished = connectQueue.take();//blocking call to wait for connection status 
			Thread.yield();

			boolean socketConnected = socketEstablished.booleanValue();
			if (socketConnected) {
				nxt.connectionState = (mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED);
			} else {
				nxt.connectionState = NXTConnectionState.DISCONNECTED;
			}
			nxtInfo = nxt;

			return socketConnected;
		} catch (Exception e) {
			Log.e(TAG, "ERROR in open: ", e);
			nxt.connectionState = NXTConnectionState.DISCONNECTED;
			throw new NXTCommException("ERROR in open: " + nxt.name + " failed: " + e.getMessage());
		}
	}

	/**
	 * Will block until data is available
	 * 
	 * @return read data
	 */
	public byte[] read() throws IOException {
		Log.d(TAG, "read called");
		byte b[] = null;

		while (b == null) {
			b = mReadQueue.poll();
			Thread.yield();
		}
		return b;
	}

	public NXTInfo[] search(String name, int protocol) throws NXTCommException {
		Log.d(TAG, "search");
		nxtInfos = new Vector<NXTInfo>();
		devices = new Vector<BluetoothDevice>();
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		for (BluetoothDevice device : pairedDevices) {
			Log.d(TAG, "paired devices :" + device.getName() + "\n" + device.getAddress());

			if (device.getBluetoothClass().getMajorDeviceClass() == 2048) {
				devices.add(device);
			}
		}

		for (Enumeration<BluetoothDevice> enum_d = devices.elements(); enum_d.hasMoreElements();) {
			BluetoothDevice d = enum_d.nextElement();
			Log.d(TAG, "creating nxtInfo");
			nxtInfo = new NXTInfo();

			nxtInfo.name = d.getName();
			if (nxtInfo.name == null || nxtInfo.name.length() == 0)
				nxtInfo.name = "Unknown";
			nxtInfo.deviceAddress = d.getAddress();
			nxtInfo.protocol = NXTCommFactory.BLUETOOTH;

			if (name == null || name.equals(nxtInfo.name)) {

				Log.d(TAG, "adding " + d.getName());
				nxtInfos.addElement(nxtInfo);
			}
		}

		NXTInfo[] nxts = new NXTInfo[nxtInfos.size()];
		for (int i = 0; i < nxts.length; i++) {
			nxts[i] = nxtInfos.elementAt(i);

		}
		return nxts;
	}

	public void sendMessageToUIThread(String message) {
		Message message_holder = formMessage(message);
		message_holder.what = MESSAGE;
		mUIMessageHandler.sendMessage(message_holder);
	}

	/**
	 * Sends a request to the NXT brick.
	 * 
	 * @param message
	 *            Data to send.
	 */
	public synchronized byte[] sendRequest(byte[] message, int replyLen) throws IOException {

		Log.i(TAG, "sendRequest");
		// Log.i(TAG, "os==null " + (os == null));
		// Log.i(TAG, "is==null " + (is == null));
		// // length of packet (Least and Most significant byte)
		// // * NOTE: Bluetooth only.
		// int LSB = message.length;
		// int MSB = message.length >>> 8;

		write(message);

		// if (os == null)
		// return new byte[0];
		//
		// // Send length of packet:
		// os.write((byte) LSB);
		// os.write((byte) MSB);
		//
		// os.write(message);
		// os.flush();

		if (replyLen == 0)
			return new byte[0];

		byte[] b = read();

		if (b.length != replyLen) {
			throw new IOException("Unexpected reply length");
		}

		//
		// if (is == null)
		// return new byte[0];
		return b;
		// return null;
	}

	// private synchronized void setState(int state) {
	// mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	// }

	public void setUIHander(Handler mUIMessageHandler) {
		this.mUIMessageHandler = mUIMessageHandler;

	}

	public synchronized void startIOThreads(BluetoothSocket socket, BluetoothDevice device) {

		// Cancel the thread that completed the connection

		//
		// Log.d("NXTCommAndroid connected", "socket connected to : "
		// + socket.getRemoteDevice().getName());
		// mConnectedDeviceName = socket.getRemoteDevice().getName();
		cancelIOThreads();

		if (mReadQueue != null) {
			mReadQueue = null;
		}
		if (mWriteQueue != null) {
			mWriteQueue = null;
		}

		mReadQueue = new LinkedBlockingQueue<byte[]>();
		mWriteQueue = new LinkedBlockingQueue<byte[]>();

		mWriteThread = new WriteThread(socket, mWriteQueue);

		// Start the thread to manage the connection and perform transmissions
		mReadThread = new ReadThread(socket, mReadQueue);

		mWriteThread.start();
		mReadThread.start();

		// Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
		// Bundle bundle = new Bundle();
		// bundle.putString(DEVICE_NAME, device.getName());
		// msg.setData(bundle);
		// mHandler.sendMessage(msg);

		// setState(STATE_CONNECTED);
	}

	public String stripColons(String s) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c != ':') {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	/**
	 * Put data into write queue to be written by write thread
	 * 
	 * Will block if no space in queue. Queue size is 2147483647, so this is not
	 * likely.
	 * 
	 * @param data
	 *            Data to send.
	 */
	public void write(byte[] data) throws IOException {

		try {
			if (data != null) {
				mWriteQueue.put(data);
			}
			Thread.yield();
		} catch (InterruptedException e) {
			Log.e(TAG, "write error ", e);
			e.printStackTrace();
		}

	}

}
