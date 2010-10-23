package lejos.pc.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class NXTCommAndroid implements NXTComm {
	private static Vector<BluetoothDevice> devices;

	private BluetoothSocket nxtBTsocket = null;
	public OutputStream os;
	public InputStream is;
	private NXTInfo nxtInfo;
	private static Vector<NXTInfo> nxtInfos;
	private final String TAG = "NXTCommAndroid";
	private BluetoothAdapter mBtAdapter;

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public int available() throws IOException {
		return 0;
	}

	public void close() throws IOException {
		if (os != null)
			os.close();
		if (is != null)
			is.close();
		if (nxtBTsocket != null)
			nxtBTsocket.close();
	}

	private byte[] concat(byte[] data1, byte[] data2) {
		int l1 = data1.length;
		int l2 = data2.length;
		byte[] data = new byte[l1 + l2];
		System.arraycopy(data1, 0, data, 0, l1);
		System.arraycopy(data2, 0, data, l1, l2);
		return data;
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

		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		}

		nxtDevice = mBtAdapter.getRemoteDevice(nxt.deviceAddress);

		try {
			nxtBTsocket = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
			nxtBTsocket.connect();
			is = nxtBTsocket.getInputStream();
			os = nxtBTsocket.getOutputStream();
			// Log.d(TAG, "is/os open");
			nxt.connectionState = (mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED);
			return true;
		} catch (Exception e) {
			nxt.connectionState = NXTConnectionState.DISCONNECTED;
			throw new NXTCommException("Open of " + nxt.name + " failed: " + e.getMessage());
		}
	}

	public byte[] read() throws IOException {

		int lsb = is.read();

		if (lsb < 0)
			return null;
		int msb = is.read();

		if (msb < 0) {
			return null;
		}
		int len = lsb | (msb << 8);
		byte[] bb = new byte[len];
		for (int i = 0; i < len; i++) {
			bb[i] = (byte) is.read();
		}
		return bb;
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
			//Log.d(TAG, "paired devices details :"
			//+ device.getBluetoothClass().getMajorDeviceClass() + "\n"
			//+ device.getBluetoothClass().getDeviceClass());

			if (device.getBluetoothClass().getMajorDeviceClass() == 2048) {
				devices.add(device);
			}
		}

		for (Enumeration<BluetoothDevice> enum_d = devices.elements(); enum_d.hasMoreElements();) {
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
			//Log.d(TAG, "Returning: " + nxtInfo.name + " "
			// + nxtInfo.deviceAddress);
		}
		return nxts;
	}

	/**
	 * Sends a request to the NXT brick.
	 * 
	 * @param message
	 *            Data to send.
	 */
	public synchronized byte[] sendRequest(byte[] message, int replyLen) throws IOException {

		Log.i(TAG, "sendRequest");
		Log.i(TAG, "os==null " + (os == null));
		Log.i(TAG, "is==null " + (is == null));
		// length of packet (Least and Most significant byte)
		// * NOTE: Bluetooth only.
		int LSB = message.length;
		int MSB = message.length >>> 8;

		if (os == null)
			return new byte[0];

		// Send length of packet:
		os.write((byte) LSB);
		os.write((byte) MSB);

		os.write(message);
		os.flush();

		if (replyLen == 0)
			return new byte[0];

		byte[] reply = null;
		int length = -1;

		if (is == null)
			return new byte[0];

		do {
			length = is.read(); // First byte specifies length of packet.
		} while (length < 0);

		int lengthMSB = is.read(); // Most Significant Byte value
		length = (0xFF & length) | ((0xFF & lengthMSB) << 8);
		reply = new byte[length];
		int len = is.read(reply);
		if (len != replyLen)
			throw new IOException("Unexpected reply length");

		return (reply == null) ? new byte[0] : reply;
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
		//Log.d(TAG, "write to " + nxtBTsocket.getRemoteDevice().getName());
		byte[] lsb_msb = new byte[2];
		lsb_msb[0] = (byte) data.length;
		lsb_msb[1] = (byte) ((data.length >> 8) & 0xff);
		os.write(concat(lsb_msb, data));
		os.flush();
	}

}
