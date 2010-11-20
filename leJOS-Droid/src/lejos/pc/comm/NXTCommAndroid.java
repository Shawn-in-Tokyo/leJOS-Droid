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
	// public Boolean socketConnected = false;
	private final BluetoothSocket mmSocket;
	private final BluetoothDevice mmDevice;
	private SynchronousQueue<Boolean> connectQueue;

	public ConnectThread(BluetoothDevice device,
		SynchronousQueue<Boolean> connectQueue) {
	    mmDevice = device;
	    BluetoothSocket tmp = null;
	    this.connectQueue = connectQueue;
	    // Get a BluetoothSocket for a connection with the
	    // given BluetoothDevice
	    try {
		tmp = device
			.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
	    } catch (IOException e) {
		Log.e(TAG, "create() failed", e);
	    }
	    mmSocket = tmp;
	}

	public void cancel() {
	    try {
		mmSocket.close();
	    } catch (IOException e) {
		Log.e(TAG, "close() of connect socket failed", e);
	    }
	}

	private void indicateConnectionFailure(IOException e) {
	    try {
		// notify calling thread that connection failed
		connectQueue.put(new Boolean(false));
		Log.e(TAG,
			"Connection failure -- unable to connect to socket ", e);
	    } catch (InterruptedException e1) {

	    }

	    try {
		mmSocket.close();
	    } catch (IOException e2) {
		Log.e(TAG,
			"unable to close() socket during connection failure",
			e2);
	    }
	}

	private void indicateConnectionSuccess() {
	    try {// notify calling thread that connection succeeded
		connectQueue.put(new Boolean(true));
	    } catch (InterruptedException e) {

	    }
	    // socketConnected = true;
	    Log.d(TAG,
		    "Connection success -- is connected to "
			    + mmDevice.getName());
	    yield(); // allow main NXTCommAndroid thread to read connected
		     // status and finish NXTComm setup
	}

	@Override
	public void run() {

	    Log.i(TAG, "BEGIN mConnectThread");
	    setName("ConnectThread");

	    // Make a connection to the BluetoothSocket
	    // This is a blocking call and will only return on a
	    // successful connection or an exception
	    try {
		mmSocket.connect();
	    } catch (IOException e) {
		indicateConnectionFailure(e);
		return;
	    }

	    indicateConnectionSuccess();
	    startIOThreads(mmSocket, mmDevice);
	}

    }

    private class ReadThread extends Thread {
	public InputStream is;
	boolean running = true;
	LinkedBlockingQueue<byte[]> mReadQueue;

	public ReadThread(BluetoothSocket socket,
		LinkedBlockingQueue<byte[]> mReadQueue) {
	    try {
		is = socket.getInputStream();
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
	    int lsb = 0;
	    try {
		lsb = is.read();

	    } catch (Exception e) {
		Log.d(TAG, "read err", e);
	    }

	    Log.d(TAG, "read " + lsb);
	    if (lsb < 0)
		return null;
	    int msb = 0;
	    try {
		msb = is.read();
	    } catch (IOException e1) {
		Log.e(TAG, "ReadThread read error ", e1);
	    }

	    Log.d(TAG, "read " + msb);
	    if (msb < 0) {
		return null;
	    }
	    int len = lsb | (msb << 8);
	    byte[] bb = new byte[len];
	    for (int i = 0; i < len; i++) {

		try {
		    bb[i] = (byte) is.read();
		} catch (IOException e) {
		    Log.e(TAG, "ReadThread read error ", e);
		}
		Log.d(TAG, "read  bb[i]: " + bb[i]);
	    }
	    Log.i(TAG, "read (size of array): " + bb.length);
	    return bb;
	}

	private byte[] readLCP() {

	    byte[] reply = null;
	    int length = -1;

	    try {
		do {
		    length = is.read(); // First byte specifies length of
					// packet.
		} while (running && length < 0);

		int lengthMSB = is.read(); // Most Significant Byte value
		length = (0xFF & length) | ((0xFF & lengthMSB) << 8);
		reply = new byte[length];
		is.read(reply);
	    } catch (IOException e) {
		Log.e(TAG,"readLCP error:", e);
	    }

	    return (reply == null) ? new byte[0] : reply;
	}

	@Override
	public void run() {

	    byte[] tmp_data;
	    while (running) {
		tmp_data = null;
		if (nxtInfo.connectionState == NXTConnectionState.LCP_CONNECTED) {
		    tmp_data = readLCP();
		} else {
		    tmp_data = read();
		}
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

	public WriteThread(BluetoothSocket socket,
		LinkedBlockingQueue<byte[]> mWriteQueue) {
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
	    while (running) {
		try {
		    byte[] test;
		    test = mWriteQueueT.take();
		 
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

  //  public static final String DEVICE_NAME = "device_name";
    // Message types sent from the BluetoothChatService Handler
//    public static final int MESSAGE_STATE_CHANGE = 1;
//    public static final int MESSAGE_READ = 2;
//    public static final int MESSAGE_WRITE = 3;
//    public static final int MESSAGE_DEVICE_NAME = 4;
//
//    public static final int STATE_NONE = 10;
//    public static final int STATE_CONNECTING = 20; // now initiating an outgoing
//    public static final int STATE_CONNECTED = 30; // now connected to a remote
//						  // device
    private static Vector<BluetoothDevice> devices;
    private NXTInfo nxtInfo;

    private static Vector<NXTInfo> nxtInfos;
    private final String TAG = " ** NXTCommAndroid>>>>";
    private BluetoothAdapter mBtAdapter;

    private ConnectThread mConnectThread;
    private ReadThread mReadThread;
    private WriteThread mWriteThread;
    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID
	    .fromString("00001101-0000-1000-8000-00805F9B34FB");
    // private static final int WRITE = 100;
    // private static final int READ = 200;

    public static final String MESSAGE_CONTENT = "String_message";
    public static final int MESSAGE = 1000;
    public static final int TOAST = 2000;

    private LinkedBlockingQueue<byte[]> mReadQueue;
    private LinkedBlockingQueue<byte[]> mWriteQueue;

    protected String mConnectedDeviceName;

    private Handler mUIMessageHandler;

    private SynchronousQueue<Boolean> connectQueue;

    // private final Handler mHandler = new Handler() {
    // @Override
    // public void handleMessage(Message msg) {
    // switch (msg.what) {
    // case MESSAGE_STATE_CHANGE:
    // // if(D)
    // Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
    // switch (msg.arg1) {
    // case STATE_CONNECTED:
    // // mTitle.setText(R.string.title_connected_to);
    // // mTitle.append(mConnectedDeviceName);
    // // sentMessageToUIThread("Connected to mConnectedDeviceName");
    // Log.i(TAG, "Connected to mConnectedDeviceName" + mConnectedDeviceName);
    // break;
    // case STATE_CONNECTING:
    // Log.i(TAG, "Connecting to" + mConnectedDeviceName);
    // // sentMessageToUIThread("Connecting to mConnectedDeviceName");
    // // mTitle.setText(R.string.title_connecting);
    // break;
    //
    // case STATE_NONE:
    // Log.i(TAG, "no connection" + mConnectedDeviceName);
    // // sentMessageToUIThread("no connection");
    // // mTitle.setText(R.string.title_not_connected);
    // break;
    // }
    // break;
    //
    // case MESSAGE_DEVICE_NAME:
    // // save the connected device's name
    // mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
    // // Toast.makeText(getApplicationContext(), "Connected to "
    // // + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
    // break;
    // // case MESSAGE_CON_FAILED:
    // // Log.i(TAG, "connection attempt failed");
    // // // Toast.makeText(getApplicationContext(),
    // // // msg.getData().getString(TOAST),
    // // // Toast.LENGTH_SHORT).show();
    // // break;
    // }
    // }
    // };

    public int available() throws IOException {
	return 0;
    }

    private void cancelThreads() {
	if (mConnectThread != null) {
	    mConnectThread.cancel();
	    mConnectThread = null;
	}

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
	cancelThreads();
	mConnectedDeviceName = "";
	// setState(NXTCommAndroid.STATE_NONE);
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

    // private void connectionFailed() {
    // setState(STATE_NONE);
    // // Send a failure message back to the Activity
    // Message msg = mHandler.obtainMessage(MESSAGE_CON_FAILED);
    // Bundle bundle = new Bundle();
    // bundle.putString(TOAST, "Unable to connect device");
    // msg.setData(bundle);
    // mHandler.sendMessage(msg);
    // }

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
	    // setState(STATE_CONNECTING);

	    boolean socketConnected = false;

	    try {

		Boolean socketEstablished = connectQueue.take();
		Thread.yield();
		socketConnected = socketEstablished.booleanValue();

	    } catch (Exception e) {
		// may have been not null when entered but nulled by other
		// thread before call
	    }

	    Log.d(TAG, "open socketConnected: " + socketConnected);
	    nxt.connectionState = (mode == LCP
		    ? NXTConnectionState.LCP_CONNECTED
		    : NXTConnectionState.PACKET_STREAM_CONNECTED);
	    return socketConnected;
	} catch (Exception e) {
	    Log.e(TAG, "ERROR in open: ", e);
	    nxt.connectionState = NXTConnectionState.DISCONNECTED;
	    throw new NXTCommException("ERROR in open: " + nxt.name
		    + " failed: " + e.getMessage());
	}
    }

    public byte[] read() throws IOException {
	Log.d(TAG, "read called");
	byte b[] = null;

	while (b == null) {
	    b = mReadQueue.poll();
	    // Log.d(TAG, "read polled and yeilded- will try again");
	    Thread.yield();
	}
	Log.d(TAG, "read called mReadQueue.poll() length " + b.length);
	return b;
	// return mReadQueue.poll();//returns null if no data waiting
    }

    public NXTInfo[] search(String name, int protocol) throws NXTCommException {
	Log.d(TAG, "search");
	nxtInfos = new Vector<NXTInfo>();
	devices = new Vector<BluetoothDevice>();
	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	// Get a set of currently paired devices
	Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

	for (BluetoothDevice device : pairedDevices) {
	    Log.d(TAG,
		    "paired devices :" + device.getName() + "\n"
			    + device.getAddress());

	    if (device.getBluetoothClass().getMajorDeviceClass() == 2048) {
		devices.add(device);
	    }
	}

	for (Enumeration<BluetoothDevice> enum_d = devices.elements(); enum_d
		.hasMoreElements();) {
	    BluetoothDevice d = enum_d.nextElement();
	    nxtInfo = new NXTInfo();

	    nxtInfo.name = d.getName();
	    if (nxtInfo.name == null || nxtInfo.name.length() == 0)
		nxtInfo.name = "Unknown";
	    nxtInfo.deviceAddress = d.getAddress();
	    nxtInfo.protocol = NXTCommFactory.BLUETOOTH;

	    if (name == null || name.equals(nxtInfo.name))
		nxtInfos.addElement(nxtInfo);
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
    public synchronized byte[] sendRequest(byte[] message, int replyLen)
	    throws IOException {

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

    public synchronized void startIOThreads(BluetoothSocket socket,
	    BluetoothDevice device) {

	// Cancel the thread that completed the connection

	//
	// Log.d("NXTCommAndroid connected", "socket connected to : "
	// + socket.getRemoteDevice().getName());
	// mConnectedDeviceName = socket.getRemoteDevice().getName();
	cancelThreads();

	if (mReadQueue != null) {
	    mReadQueue = null;
	}
	if (mWriteQueue != null) {
	    mWriteQueue = null;
	}

	mReadQueue = new LinkedBlockingQueue<byte[]>();
	mWriteQueue = new LinkedBlockingQueue<byte[]>();
	// Start the thread to manage the connection and perform transmissions
	mReadThread = new ReadThread(socket, mReadQueue);
	mReadThread.start();

	mWriteThread = new WriteThread(socket, mWriteQueue);
	mWriteThread.start();

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

    public void write(byte[] data) throws IOException {
	Log.d(TAG, "NXTCommAndroid write " + data.length);
	Log.d(TAG, "mq==null: " + (mWriteQueue == null));
	try {
	    if (data != null) {
		mWriteQueue.put(data);
	    }
	    Log.d(TAG, "NXTCommAndroid mWriteQueue.put " + data.length);
	    Thread.yield();
	} catch (InterruptedException e) {
	    Log.e(TAG, "write error ", e);
	    e.printStackTrace();
	}

    }

}
