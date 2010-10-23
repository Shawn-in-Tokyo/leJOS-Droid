package lejos.pc.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;



public class NXTCommAndroid implements NXTComm {
	 private static Vector<BluetoothDevice> devices;
  
	
	 private BluetoothSocket nxtBTsocket = null;
	//private StreamConnection con;
	public OutputStream os;
	public InputStream is;
	private NXTInfo nxtInfo;
	private static Vector<NXTInfo> nxtInfos;
	 
	private final String TAG="NXTCommAndroid";
	private BluetoothAdapter mBtAdapter;
	//private static final UUID SP2 = UUID.fromString("1101");
	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//	public void setOs(OutputStream os) {
//		Log.i(TAG,"setOs os==null" + (os==null));
//		this.os = os;
//	}
//
//	public void setIs(InputStream is) {
//		Log.i(TAG,"setIs is==null" + (is==null));
//		this.is = is;
//	}
	
	public void close() throws IOException {
		if (os != null)
			os.close();
		if (is != null)
			is.close();
		if (nxtBTsocket != null)
			nxtBTsocket.close();

	}

	/**
	 * Sends a request to the NXT brick.
	 * 
	 * @param message
	 *            Data to send.
	 */
	public synchronized byte[] sendRequest(byte[] message, int replyLen)
			throws IOException {

	Log.i(TAG,"sendRequest");	
	Log.i(TAG,"os==null " + (os==null));
	Log.i(TAG,"is==null " + (is==null));
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
		if (len != replyLen) throw new IOException("Unexpected reply length");

		return (reply == null) ? new byte[0] : reply;
	}

	public int available() throws IOException {
		 
		return 0;
	}

	public InputStream getInputStream() {
		Log.i(TAG,"getInputStream returning NXTCommInputStream");	
		return new NXTCommInputStream(this);
		// return is;
	}

	public OutputStream getOutputStream() {
		Log.i(TAG,"getOutputStream returning NXTCommOutputStream");	
		return new NXTCommOutputStream(this);
		//return os;
	}

	public boolean open(NXTInfo nxt) throws NXTCommException {
		 return open(nxt, PACKET);
	}

	public boolean open(NXTInfo nxt, int mode) throws NXTCommException {
        if (mode == RAW) throw new NXTCommException("RAW mode not implemented");
//		// Construct URL if not present
//
      
		if (nxt.btResourceString == null || nxt.btResourceString.length() < 5
				|| !(nxt.btResourceString.substring(0, 5).equals("btspp"))) {
			nxt.btResourceString = "btspp://"
					+ stripColons(nxt.deviceAddress)
					+ ":1;authenticate=false;encrypt=false";
		}

       // BluetoothDevice mNxt = mBtAdapter.getRemoteDevice(nxt.deviceAddress);
       // mNxt.createRfcommSocketToServiceRecord(uuid)
		 Log.i(TAG,"nxt.btResourceString: "+nxt.btResourceString);	
          //  BluetoothSocket nxtBTsocketTEMPORARY;
            BluetoothDevice nxtDevice = null;
            Log.i(TAG,"nxt.deviceAddress: "+nxt.deviceAddress);	
            
            
            if (mBtAdapter==null){
            	mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            }
            
           
            
            nxtDevice = mBtAdapter.getRemoteDevice(nxt.deviceAddress);

//            if (nxtDevice == null) {
//                sendToast(myMINDdroid.getResources().getString(R.string.no_paired_nxt));
//                sendState(STATE_CONNECTERROR);
//                return;
//            }

          
        
        
        
		try {
			
			//nxtBTsocket = nxtDevice.createRfcommSocketToServiceRecord(SP2);
			 nxtBTsocket = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
		
			nxtBTsocket.connect();
	        //    nxtBTsocket = nxtBTsocketTEMPORARY;
	             
	            is =nxtBTsocket.getInputStream();
	            os =nxtBTsocket.getOutputStream();
			Log.d(TAG, "open");
			nxt.connectionState = (mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED);
			Log.d(TAG, "return nxt.connectionState !!"+nxt.connectionState);
			return true;
		} catch (Exception e) {
			nxt.connectionState = NXTConnectionState.DISCONNECTED;
			throw new NXTCommException("Open of " + nxt.name + " failed: " + e.getMessage());
		}
	}

	public byte[] read() throws IOException {
		Log.d(TAG, "read");
		   int lsb=0;
		try {
			lsb = is.read();
		 
		} catch (Exception e) {
			Log.d(TAG, "read err",e);
		}
	
		   Log.d(TAG, "read "+lsb);
			if (lsb < 0) return null;
			int msb = is.read();
			
			  Log.d(TAG, "read "+msb);
	        if (msb < 0) return null;
	        int len = lsb | (msb << 8);
			byte[] bb = new byte[len];
			for (int i=0;i<len;i++){
				 
			 bb[i] = (byte) is.read();
			 Log.d(TAG, "read  bb[i]: "+ bb[i]);
			}
			return bb;
	}

	public NXTInfo[] search(String name, int protocol) throws NXTCommException {
	Log.d(TAG, "search");
//		 NXTInfo myNXTs[] = new NXTInfo[1];
//		 NXTInfo myNXT = new NXTInfo(LCP,"Wall-E","");
//		 myNXTs[0] = myNXT;
//		return myNXTs;
		nxtInfos = new Vector<NXTInfo>();
		devices = new Vector<BluetoothDevice>();
		 // Context.getApplicationContext();
		
		// Register for broadcasts when a device is discovered
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
//        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        this.registerReceiver(mReceiver, filter);
		
		// Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
		
        for (BluetoothDevice device : pairedDevices) {
        	Log.d(TAG, "paired devices :"+device.getName() + "\n" + device.getAddress());
        	Log.d(TAG, "paired devices details :"+device.getBluetoothClass().getMajorDeviceClass() + "\n" + device.getBluetoothClass().getDeviceClass());
           // mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        	if (device.getBluetoothClass().getMajorDeviceClass()==2048){
        		devices.add(device);
        	}
        }
       
		for (Enumeration<BluetoothDevice> enum_d = devices.elements(); enum_d.hasMoreElements();) {
			BluetoothDevice d = enum_d.nextElement();

		//	try {
				nxtInfo = new NXTInfo();

				nxtInfo.name = d.getName();//d.getFriendlyName(false);
				if (nxtInfo.name == null || nxtInfo.name.length() == 0)
					nxtInfo.name = "Unknown";
				nxtInfo.deviceAddress = d.getAddress();//d.getBluetoothAddress();
				nxtInfo.protocol = NXTCommFactory.BLUETOOTH;

				if (name == null || name.equals(nxtInfo.name))
					nxtInfos.addElement(nxtInfo);
				else
					continue;

				Log.d(TAG, "Found: " + nxtInfo.name);
 
//				// We want additional attributes, ServiceName (0x100),
//				// ServiceDescription (0x101) and ProviderName (0x102).
//
//				int[] attributes = { 0x100, 0x101, 0x102 };
//
//				UUID[] uuids = new UUID[1];
//				uuids[0] = new UUID("1101", true); // Serial Port
//				synchronized (this) {
//					try {
//						mBtAdapter.getRemoteDevice(d.getAddress()).createRfcommSocketToServiceRecord("1101")
////						LocalDevice.getLocalDevice().getDiscoveryAgent()
////								.searchServices(attributes, uuids, d, this);
//						try {
//							wait();
//						} catch (InterruptedException e) {
//							System.err.println(e.getMessage());
//						}
//					} catch (BluetoothStateException e) {
//						System.err.println(e.getMessage());
//					}
//				}
//
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					System.err.println(e.getMessage());
//				}

//			} catch (IOException e) {
//				System.err.println(e.getMessage());
//
//			}

		}
        
//     // If we're already discovering, stop it
//        if (mBtAdapter.isDiscovering()) {
//            mBtAdapter.cancelDiscovery();
//        }
//
//        // Request discover from BluetoothAdapter
//        mBtAdapter.startDiscovery();
//        try {
//			wait();
//		} catch (InterruptedException e) {
//			System.err.println(e.getMessage());
//		}
		
		NXTInfo[] nxts = new NXTInfo[nxtInfos.size()];
		for (int i = 0; i < nxts.length; i++){
			nxts[i] = (NXTInfo) nxtInfos.elementAt(i);
			Log.d(TAG, "Returning: " + nxtInfo.name +" "+nxtInfo.deviceAddress);
		}return nxts;
	}

	public void write(byte[] data) throws IOException {
//		Log.d(TAG, "write: "+(byte)(data.length & 0xff));
//		Log.d(TAG, "write: "+ data.length );
//		os.write((byte)(data.length & 0xff));
//		 for (int i=0; i<data.length;i++){
//			 Log.d(TAG, "data["+i+"]"+ data[i] );
//		 }
//			os.write(data);
			//DataOutputStream dos = new DataOutputStream(os);
			//dos.writeInt(21);
			//dos.flush();
			//os.flush();
        //  Thread.yield();
		Log.d(TAG, "write to "+nxtBTsocket.getRemoteDevice().getName());
		//os=nxtBTsocket.getOutputStream();
		//DataOutputStream dos = new DataOutputStream(os);
		//os.write((byte)(data.length & 0xff));
 		//dos.write((byte)4);
		
		byte[] lsb_msb = new byte[2];
		lsb_msb[0] = (byte) data.length;
		lsb_msb[1] = (byte) ((data.length >> 8) & 0xff);
 		
 		os.write(concat(lsb_msb,data) );
 		os.flush();
	}
	
	private byte[] concat(byte[] data1, byte[] data2) {
		int l1 = data1.length;
		int l2 = data2.length;
		
		byte[] data = new byte[l1 + l2];
		System.arraycopy(data1, 0, data, 0, l1);
		System.arraycopy(data2, 0, data, l1, l2);
		for (int i = 0; i < data.length; i++) {
			Log.d(TAG, "concat data" +i+" "+data[i]);
		}
	
		return data;
	}
	
	private byte[] intToByteArray (final int integer) {
		int byteNum = (40 - Integer.numberOfLeadingZeros (integer < 0 ? ~integer : integer)) / 8;
		byte[] byteArray = new byte[4];
		
		for (int n = 0; n < byteNum; n++)
			byteArray[3 - n] = (byte) (integer >>> (n * 8));
		
		return (byteArray);
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
	
//	   private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			 
//			
//		}
//		   
//	   };

}
